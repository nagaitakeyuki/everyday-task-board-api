package io.taskboard.app.controller;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import io.taskboard.app.form.AddTasksForm;
import io.taskboard.app.form.ChangeSortOrderForm;
import io.taskboard.app.form.ChangeTaskStatusForm;
import io.taskboard.app.response.*;
import io.taskboard.domain.StoryIndexItem;
import io.taskboard.domain.TaskItem;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class TaskBoardRestController {

    @RequestMapping("/sprints")
    public AllDataResponse getSprints() {

        DynamoDBMapper mapper = createMapper();

        DynamoDBQueryExpression<TaskItem> gettingTaskBoardDataOfSingleUserQuery
                = new DynamoDBQueryExpression<TaskItem>()
                    .withKeyConditionExpression("UserId = :userId")
                    .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {
                        {
                            put(":userId", new AttributeValue().withS("user1"));
                        }
                    });

        List<TaskItem> all = mapper.query(TaskItem.class, gettingTaskBoardDataOfSingleUserQuery);


        AllDataResponse response = new AllDataResponse();

        all.stream()
            .filter(item -> item.getItemId().startsWith("sprint"))
            .forEach(item -> {
                Sprint sprint = new Sprint();
                sprint.setSprintId(item.getItemId());
                sprint.setSprintName(item.getName());
                sprint.setSpringStatus(item.getStatus());
                response.putSprint(sprint.getSprintId(), sprint);
            });

        all.stream()
            .filter(item -> item.getItemId().startsWith("backlogCategory"))
            .forEach(item -> {
                BacklogCategory backlogCategory = new BacklogCategory();
                backlogCategory.setBacklogCategoryId(item.getItemId());
                backlogCategory.setBacklogCategoryName(item.getName());
                response.putBacklogCategory(backlogCategory.getBacklogCategoryId(), backlogCategory);
            });

        Map<String, Story> stories =
            all.stream()
                .filter(item -> item.getItemId().startsWith("story"))
                .map(item -> {
                    Story story = new Story();
                    story.setStoryId(item.getItemId());
                    story.setStoryName(item.getName());
                    story.setStoryStatus(item.getStatus());
                    story.setBaseSprintId(item.getBaseSprintId());
                    story.setBacklogCategoryId(item.getBacklogCategoryId());
                    return story;
                })
                .collect(Collectors.toMap(story -> story.getStoryId(), story -> story));

        stories.forEach((storyId, story) -> {
            if (story.getBacklogCategoryId() != null) {
                // バックログに紐づく場合

                response.getBacklogCategory(story.getBacklogCategoryId())
                        .putStory(storyId, story);

            } else {
                // スプリントに紐づく場合
                response.getSprint(story.getBaseSprintId())
                        .putStory(storyId, story);
            }
        });

        all.stream()
            .filter(item -> item.getItemId().startsWith("task"))
            .forEach(item -> {
                Task task = new Task();
                task.setTaskId(item.getItemId());
                task.setTaskName(item.getName());
                task.setTaskStatus(item.getStatus());
                task.setBaseStoryId(item.getBaseStoryId());
                task.setSortOrder(item.getSortOrder());

                stories.get(task.getBaseStoryId()).putTask(task.getTaskId(), task);
            });

        return response;

    }


    @RequestMapping(value = "/sprints/taskStatus", method= RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void changeTaskStatus(@RequestBody ChangeTaskStatusForm form) {

        DynamoDBMapper mapper = createMapper();

        // 対象タスクのストーリーに属する、全タスクのIDを取得する
        DynamoDBQueryExpression<StoryIndexItem> query
                = new DynamoDBQueryExpression<StoryIndexItem>()
                        .withIndexName("StoryIndex")
                        .withKeyConditionExpression("UserId = :userId and BaseStoryId = :baseStoryId")
                        .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {
                            {
                                put(":userId", new AttributeValue().withS("user1"));
                                put(":baseStoryId", new AttributeValue().withS(form.getStoryId()));
                            }
                        });

        List<StoryIndexItem> taskIds = mapper.query(StoryIndexItem.class, query);

        // 各タスクの詳細を取得する
        List<TaskItem> tasksToGet = taskIds.stream().map(taskId -> {
                                                            TaskItem item = new TaskItem();
                                                            item.setUserId("user1");
                                                            item.setItemId(taskId.getItemId());
                                                            return item;
                                                        })
                                                    .collect(Collectors.toList());

        Map<String, TaskItem> tasks = mapper.batchLoad(tasksToGet).get("TaskBoard")
                                            .stream()
                                            .map(task -> (TaskItem) task)
                                            .collect(Collectors.toMap(task -> task.getItemId(), task -> task));


        TaskItem statusChangedTask = tasks.get(form.getTaskId());

        String oldStatus = statusChangedTask.getStatus();

        statusChangedTask.setStatus(form.getNewStatus());
        statusChangedTask.setSortOrder(form.getNewIndex());

        // ステータスごとにタスク順を再設定する
        //   1. 変更前のステータス
        //      ステータス変更されたタスクが無くなると、抜け番ができる。その抜け番を詰める。
        List<TaskItem> reorderedOldStatusTasks = new ArrayList<TaskItem>(tasks.values())
                .stream()
                .filter(taskItem -> taskItem.getStatus().equals(oldStatus))
                .sorted(Comparator.comparingInt(TaskItem::getSortOrder))
                .collect(Collectors.toList());

        for (int i = 0; i < reorderedOldStatusTasks.size(); i++) {
            reorderedOldStatusTasks.get(i).setSortOrder(i);
        }

        // 　2. 変更後のステータス
        List<TaskItem> reorderedNewStatusTasks = new ArrayList<TaskItem>(tasks.values())
                .stream()
                .filter(taskItem -> taskItem.getStatus().equals(form.getNewStatus()))
                .sorted((a, b) -> {
                    // ユーザーにより変更されたタスクを優先的に前に並べる
                    if(a.getSortOrder() == b.getSortOrder() && a.getItemId().equals(form.getTaskId())) {
                        return -1;
                    }

                    // その他の場合は単純に昇順に並べる
                    return a.getSortOrder() - b.getSortOrder();
                })
                .collect(Collectors.toList());

        for (int i = 0; i < reorderedNewStatusTasks.size(); i++) {
            reorderedNewStatusTasks.get(i).setSortOrder(i);
        }

        mapper.batchSave(Stream.concat(reorderedOldStatusTasks.stream(), reorderedNewStatusTasks.stream())
                .collect(Collectors.toList()));

    }

    @RequestMapping(value = "/sprints/taskSortOrder", method= RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void changeTaskSortIndex(@RequestBody ChangeSortOrderForm form) {

        DynamoDBMapper mapper = createMapper();

        // 対象タスクのストーリーに属する、全タスクのIDを取得する
        DynamoDBQueryExpression<StoryIndexItem> query
                = new DynamoDBQueryExpression<StoryIndexItem>()
                .withIndexName("StoryIndex")
                .withKeyConditionExpression("UserId = :userId and BaseStoryId = :baseStoryId")
                .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {
                    {
                        put(":userId", new AttributeValue().withS("user1"));
                        put(":baseStoryId", new AttributeValue().withS(form.getStoryId()));
                    }
                });

        List<StoryIndexItem> taskIds = mapper.query(StoryIndexItem.class, query);

        // 各タスクの詳細を取得する
        List<TaskItem> tasksToGet = taskIds.stream().map(taskId -> {
            TaskItem item = new TaskItem();
            item.setUserId("user1");
            item.setItemId(taskId.getItemId());
            return item;
        }).collect(Collectors.toList());

        Map<String, TaskItem> tasks = mapper.batchLoad(tasksToGet).get("TaskBoard")
                .stream()
                .map(task -> (TaskItem) task)
                .collect(Collectors.toMap(task -> task.getItemId(), task -> task));

        TaskItem orderChangedTask = tasks.get(form.getTaskId());

        orderChangedTask.setSortOrder(form.getNewIndex());

        List<TaskItem> reorderedTasks = new ArrayList<TaskItem>(tasks.values())
                .stream()
                .filter(taskItem -> taskItem.getStatus().equals(orderChangedTask.getStatus()))
                .sorted((a, b) -> {
                    // ユーザーにより変更されたタスクを優先的に前に並べる
                    if(a.getSortOrder() == b.getSortOrder() && a.getItemId().equals(form.getTaskId())) {
                        return -1;
                    }

                    // その他の場合は単純に昇順に並べる
                    return a.getSortOrder() - b.getSortOrder();
                })
                .collect(Collectors.toList());

        for (int i = 0; i < reorderedTasks.size(); i++) {
            reorderedTasks.get(i).setSortOrder(i);
        }

        mapper.batchSave(reorderedTasks);

    }

    @RequestMapping(value = "/sprints/tasks", method= RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public AddTasksResponse addTasks(@RequestBody AddTasksForm form) {

        DynamoDBMapper mapper = createMapper();

        // 対象ストーリーに属する、全タスクのIDを取得する
        DynamoDBQueryExpression<StoryIndexItem> query
                = new DynamoDBQueryExpression<StoryIndexItem>()
                .withIndexName("StoryIndex")
                .withKeyConditionExpression("UserId = :userId and BaseStoryId = :baseStoryId")
                .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {
                    {
                        put(":userId", new AttributeValue().withS("user1"));
                        put(":baseStoryId", new AttributeValue().withS(form.getStoryId()));
                    }
                });

        List<StoryIndexItem> taskIds = mapper.query(StoryIndexItem.class, query);

        // 各タスクの詳細を取得する
        List<TaskItem> tasksToGet = taskIds.stream().map(taskId -> {
            TaskItem item = new TaskItem();
            item.setUserId("user1");
            item.setItemId(taskId.getItemId());
            return item;
        }).collect(Collectors.toList());

        List<TaskItem> tasksOfCurrentStoryAndStatusNew = mapper.batchLoad(tasksToGet).get("TaskBoard")
                                                                .stream()
                                                                .map(task -> (TaskItem) task)
                                                                .filter(task -> task.getStatus().equals("new"))
                                                                .collect(Collectors.toList());

        int newItemSortOrder = tasksOfCurrentStoryAndStatusNew.size();

        final List<TaskItem> newTasks = Arrays.stream(form.getTaskNames())
                                                .map(taskName ->
                                                        {
                                                            TaskItem newTask = new TaskItem();
                                                            newTask.setUserId("user1");
                                                            newTask.setItemId("task" + UUID.randomUUID().toString());
                                                            newTask.setName(taskName);
                                                            newTask.setStatus("new");
                                                            newTask.setBaseStoryId(form.getStoryId());

                                                            return newTask;
                                                        }
                                                ).collect(Collectors.toList());

        for (TaskItem item: newTasks) {
            item.setSortOrder(newItemSortOrder++);
        }

        mapper.batchSave(newTasks);

        List<Task> newTasksForResponse = newTasks.stream().map(taskItem -> {
            Task task = new Task();
            task.setTaskId(taskItem.getItemId());
            task.setTaskName(taskItem.getName());
            task.setTaskStatus(taskItem.getStatus());
            task.setBaseStoryId(taskItem.getBaseStoryId());
            task.setSortOrder(taskItem.getSortOrder());

            return task;
        }).collect(Collectors.toList());

        AddTasksResponse response = new AddTasksResponse();
        response.setNewTasks(newTasksForResponse);

        return response;

    }

    private DynamoDBMapper createMapper() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration("http://localhost:8000",
                                "ap-northeast-1"))
                .build();

        return new DynamoDBMapper(client);
    }
}
