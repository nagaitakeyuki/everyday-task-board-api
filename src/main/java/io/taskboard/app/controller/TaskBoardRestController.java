package io.taskboard.app.controller;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import io.taskboard.app.form.AddTasksForm;
import io.taskboard.app.form.ChangeSortOrderForm;
import io.taskboard.app.form.ChangeTaskStatusForm;
import io.taskboard.app.response.*;
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
    public SprintsResponse getSprints() {
        DynamoDBMapper mapper = createMapper();

        List<TaskItem> tasks = mapper.scan(TaskItem.class, new DynamoDBScanExpression());

        SprintsResponse response = new SprintsResponse();

        tasks.forEach(item -> {
            if (item.getSk().startsWith("sprint")) {
                Sprint sprint = new Sprint();
                sprint.setSprintId(item.getSk());
                sprint.setSprintName(item.getData());
                sprint.setSpringStatus(item.getStatus());
                response.putSprint(item.getSk(), sprint);
                return;
            }

            if (item.getSk().startsWith("story")) {
                Story story = new Story();
                story.setStoryId(item.getSk());
                story.setStoryName(item.getData());
                story.setStoryStatus(item.getStatus());
                story.setBaseSprintId(item.getBaseSprintId());
                response.getSprint(item.getPk()).putStory(story.getStoryId(), story);
            }

            if (item.getSk().startsWith("task")) {
                Task task = new Task();
                task.setTaskId(item.getSk());
                task.setTaskName(item.getData());
                task.setTaskStatus(item.getStatus());
                task.setBaseStoryId(item.getBaseStoryId());
                task.setSortIndex(item.getSortIndex());
                response.getSprint(item.getPk()).getStory(item.getBaseStoryId()).putTask(item.getSk(), task);
            }

        });

        return response;

    }

    @RequestMapping(value = "/sprints/taskStatus", method= RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void changeTaskStatus(@RequestBody ChangeTaskStatusForm form) {

        DynamoDBMapper mapper = createMapper();

        DynamoDBQueryExpression<TaskItem> gettingSprintQuery
                = new DynamoDBQueryExpression<TaskItem>()
                .withKeyConditionExpression("PK = :PK")
                .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {
                    {
                        put(":PK", new AttributeValue().withS(form.getSprintId()));
                    }
                });

        List<TaskItem> currentSprintItems = mapper.query(TaskItem.class, gettingSprintQuery);

        TaskItem statusChangedTask
                = currentSprintItems.stream()
                .filter(taskItem -> taskItem.getSk().equals(form.getTaskId()))
                .collect(Collectors.toList()).get(0);

        String oldStatus = statusChangedTask.getStatus();

        statusChangedTask.setStatus(form.getNewStatus());
        statusChangedTask.setSortIndex(form.getNewIndex());

        // ステータスごとにタスク順を再設定する
        //   1. 変更前のステータス
        //      ステータス変更されたタスクが無くなると、抜け番ができる。その抜け番を詰める。
        List<TaskItem> reorderedOldStatusTasks = currentSprintItems.stream()
                .filter(taskItem -> taskItem.getSk().startsWith("task")
                        && taskItem.getBaseStoryId().equals(form.getStoryId())
                        && taskItem.getStatus().equals(oldStatus))
                .sorted(Comparator.comparingInt(TaskItem::getSortIndex))
                .collect(Collectors.toList());

        for (int i = 0; i < reorderedOldStatusTasks.size(); i++) {
            reorderedOldStatusTasks.get(i).setSortIndex(i);
        }

        // 　2. 変更後のステータス
        List<TaskItem> reorderedNewStatusTasks = currentSprintItems.stream()
                .filter(taskItem -> taskItem.getSk().startsWith("task")
                        && taskItem.getBaseStoryId().equals(form.getStoryId())
                        && taskItem.getStatus().equals(form.getNewStatus()))
                .sorted((a, b) -> {
                    // ユーザーにより変更されたタスクを優先的に前に並べる
                    if(a.getSortIndex() == b.getSortIndex() && a.getSk().equals(form.getTaskId())) {
                        return -1;
                    }

                    // その他の場合は単純に昇順に並べる
                    return a.getSortIndex() - b.getSortIndex();
                })
                .collect(Collectors.toList());

        for (int i = 0; i < reorderedNewStatusTasks.size(); i++) {
            reorderedNewStatusTasks.get(i).setSortIndex(i);
        }

        mapper.batchSave(Stream.concat(reorderedOldStatusTasks.stream(), reorderedNewStatusTasks.stream())
                                 .collect(Collectors.toList()));

    }

    @RequestMapping(value = "/sprints/taskSortIndex", method= RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void changeTaskSortIndex(@RequestBody ChangeSortOrderForm form) {

        DynamoDBMapper mapper = createMapper();

        DynamoDBQueryExpression<TaskItem> queryExpression
                = new DynamoDBQueryExpression<TaskItem>()
                .withKeyConditionExpression("PK = :PK")
                .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {
                    {
                        put(":PK", new AttributeValue().withS(form.getSprintId()));
                    }
                });

        List<TaskItem> currentSprintItems = mapper.query(TaskItem.class, queryExpression);

        TaskItem orderChangedTask
                = currentSprintItems.stream()
                .filter(taskItem -> taskItem.getSk().equals(form.getTaskId()))
                .collect(Collectors.toList()).get(0);

        orderChangedTask.setSortIndex(form.getNewIndex());

        List<TaskItem> reorderedTasks = currentSprintItems.stream()
                .filter(taskItem -> taskItem.getSk().startsWith("task")
                        && taskItem.getBaseStoryId().equals(form.getStoryId())
                        && taskItem.getStatus().equals(orderChangedTask.getStatus()))
                .sorted((a, b) -> {
                    // ユーザーにより変更されたタスクを優先的に前に並べる
                    if(a.getSortIndex() == b.getSortIndex() && a.getSk().equals(form.getTaskId())) {
                        return -1;
                    }

                    // その他の場合は単純に昇順に並べる
                    return a.getSortIndex() - b.getSortIndex();
                })
                .collect(Collectors.toList());

        for (int i = 0; i < reorderedTasks.size(); i++) {
            reorderedTasks.get(i).setSortIndex(i);
        }

        mapper.batchSave(reorderedTasks);

    }

    @RequestMapping(value = "/sprints/tasks", method= RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public AddTasksResponse addTasks(@RequestBody AddTasksForm form) {

        DynamoDBMapper mapper = createMapper();

        DynamoDBQueryExpression<TaskItem> gettingTasksOfCurrentSprintQuery
                = new DynamoDBQueryExpression<TaskItem>()
                .withKeyConditionExpression("PK = :PK and begins_with(SK, :SK)")
                .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {
                    {
                        put(":PK", new AttributeValue().withS(form.getSprintId()));
                        put(":SK", new AttributeValue().withS("task"));
                    }
                });

        List<TaskItem> tasksOfCurrentSprint = mapper.query(TaskItem.class, gettingTasksOfCurrentSprintQuery);

        List<TaskItem> tasksOfCurrentStoryAndStatusNew
                = tasksOfCurrentSprint
                    .stream()
                    .filter(taskItem -> taskItem.getBaseStoryId().equals(form.getStoryId())
                                            && taskItem.getStatus().equals("new"))
                    .collect(Collectors.toList());


        Integer newItemSortIndex = tasksOfCurrentStoryAndStatusNew.size();

        final List<TaskItem> newTasks
                = Arrays.stream(form.getTaskNames())
                        .map(taskName ->
                                {
                                    TaskItem newTask = new TaskItem();
                                    newTask.setPk(form.getSprintId());
                                    newTask.setSk("task" + UUID.randomUUID().toString());
                                    newTask.setData(taskName);
                                    newTask.setStatus("new");
                                    newTask.setBaseStoryId(form.getStoryId());

                                    return newTask;
                                }
                        ).collect(Collectors.toList());

        for (TaskItem item: newTasks) {
            item.setSortIndex(newItemSortIndex++);
        }

        mapper.batchSave(newTasks);

        List<Task> newTasksForResponse = newTasks.stream().map(taskItem -> {
            Task task = new Task();
            task.setTaskId(taskItem.getSk());
            task.setTaskName(taskItem.getData());
            task.setTaskStatus(taskItem.getStatus());
            task.setBaseStoryId(taskItem.getBaseStoryId());
            task.setSortIndex(taskItem.getSortIndex());

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
