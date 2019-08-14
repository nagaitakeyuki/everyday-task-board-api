package io.taskboard.app.controller;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import io.taskboard.app.form.*;
import io.taskboard.app.response.*;
import io.taskboard.domain.BacklogCategoryIndexItem;
import io.taskboard.domain.SprintIndexItem;
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
                sprint.setSprintStatus(item.getStatus());
                sprint.setSortOrder(item.getSortOrder());
                response.putSprint(sprint.getSprintId(), sprint);
            });

        all.stream()
            .filter(item -> item.getItemId().startsWith("backlogCategory"))
            .forEach(item -> {
                BacklogCategory backlogCategory = new BacklogCategory();
                backlogCategory.setBacklogCategoryId(item.getItemId());
                backlogCategory.setBacklogCategoryName(item.getName());
                backlogCategory.setSortOrder(item.getSortOrder());
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
                    story.setSortOrder(item.getSortOrder());
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

    @RequestMapping(value = "/sprints/sprint", method= RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Sprint addSprint(@RequestBody AddSprintForm form) {

        DynamoDBMapper mapper = createMapper();

        // 全スプリントのIDを取得する
        DynamoDBQueryExpression<TaskItem> query
                = new DynamoDBQueryExpression<TaskItem>()
                .withKeyConditionExpression("UserId = :userId and begins_with(ItemId, :itemId)")
                .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {
                    {
                        put(":userId", new AttributeValue().withS("user1"));
                        put(":itemId", new AttributeValue().withS("sprint"));
                    }
                });

        int newItemSortOrder = mapper.query(TaskItem.class, query).size();

        TaskItem newSprintItem = new TaskItem();
        newSprintItem.setUserId("user1");
        newSprintItem.setItemId("sprint" + UUID.randomUUID().toString());
        newSprintItem.setName(form.getSprintName());
        newSprintItem.setStatus("new");
        newSprintItem.setSortOrder(newItemSortOrder++);

        mapper.save(newSprintItem);

        Sprint newSprint = new Sprint();
        newSprint.setSprintId(newSprintItem.getItemId());
        newSprint.setSprintName(newSprintItem.getName());
        newSprint.setSprintStatus(newSprintItem.getStatus());
        newSprint.setSortOrder(newSprintItem.getSortOrder());

        return newSprint;
    }

    @RequestMapping(value = "/sprints/backlogCategory", method= RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public BacklogCategory addBacklogCategory(@RequestBody AddBacklogCategoryForm form) {

        DynamoDBMapper mapper = createMapper();

        // 全バックログカテゴリーのIDを取得する
        DynamoDBQueryExpression<TaskItem> query
                = new DynamoDBQueryExpression<TaskItem>()
                .withKeyConditionExpression("UserId = :userId and begins_with(ItemId, :itemId)")
                .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {
                    {
                        put(":userId", new AttributeValue().withS("user1"));
                        put(":itemId", new AttributeValue().withS("backlogCategory"));
                    }
                });

        int newItemSortOrder = mapper.query(TaskItem.class, query).size();

        TaskItem newBacklogCategoryItem = new TaskItem();
        newBacklogCategoryItem.setUserId("user1");
        newBacklogCategoryItem.setItemId("backlogCategory" + UUID.randomUUID().toString());
        newBacklogCategoryItem.setName(form.getBacklogCategoryName());
        newBacklogCategoryItem.setStatus("new");
        newBacklogCategoryItem.setSortOrder(newItemSortOrder);

        mapper.save(newBacklogCategoryItem);

        BacklogCategory newBacklogCategory = new BacklogCategory();
        newBacklogCategory.setBacklogCategoryId(newBacklogCategoryItem.getItemId());
        newBacklogCategory.setBacklogCategoryName(newBacklogCategoryItem.getName());
        newBacklogCategory.setSortOrder(newBacklogCategoryItem.getSortOrder());

        return newBacklogCategory;
    }

    @RequestMapping(value = "/sprints/storyBelongingToSprint", method= RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Story addStoryToSprint(@RequestBody AddStoryForm form) {

        DynamoDBMapper mapper = createMapper();

        // 対象スプリントに属する、全ストーリーのIDを取得する
        DynamoDBQueryExpression<SprintIndexItem> query
                = new DynamoDBQueryExpression<SprintIndexItem>()
                .withIndexName("SprintIndex")
                .withKeyConditionExpression("UserId = :userId and BaseSprintId = :baseSprintId")
                .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {
                    {
                        put(":userId", new AttributeValue().withS("user1"));
                        put(":baseSprintId", new AttributeValue().withS(form.getSprintId()));
                    }
                });

        int newItemSortOrder = mapper.query(SprintIndexItem.class, query).size();

        TaskItem newStoryItem = new TaskItem();
        newStoryItem.setUserId("user1");
        newStoryItem.setItemId("story" + UUID.randomUUID().toString());
        newStoryItem.setName(form.getStoryName());
        newStoryItem.setStatus("new");
        newStoryItem.setBaseSprintId(form.getSprintId());
        newStoryItem.setSortOrder(newItemSortOrder);

        mapper.save(newStoryItem);

        Story newStory = new Story();
        newStory.setStoryId(newStoryItem.getItemId());
        newStory.setStoryName(newStoryItem.getName());
        newStory.setStoryStatus(newStoryItem.getStatus());
        newStory.setBaseSprintId(newStoryItem.getBaseSprintId());
        newStory.setSortOrder(newStoryItem.getSortOrder());

        return newStory;
    }

    @RequestMapping(value = "/sprints/storyBelongingToBacklogCategory", method= RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Story addStoryToBacklogCategory(@RequestBody AddStoryToBacklogCategoryForm form) {

        DynamoDBMapper mapper = createMapper();

        // 対象バックログカテゴリーに属する、全ストーリーのIDを取得する
        DynamoDBQueryExpression<BacklogCategoryIndexItem> query
                = new DynamoDBQueryExpression<BacklogCategoryIndexItem>()
                .withIndexName("BacklogCategoryIndex")
                .withKeyConditionExpression("UserId = :userId and BacklogCategoryId = :backlogCategoryId")
                .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {
                    {
                        put(":userId", new AttributeValue().withS("user1"));
                        put(":backlogCategoryId", new AttributeValue().withS(form.getBacklogCategoryId()));
                    }
                });

        int newItemSortOrder = mapper.query(BacklogCategoryIndexItem.class, query).size();

        TaskItem newStoryItem = new TaskItem();
        newStoryItem.setUserId("user1");
        newStoryItem.setItemId("story" + UUID.randomUUID().toString());
        newStoryItem.setName(form.getStoryName());
        newStoryItem.setStatus("new");
        newStoryItem.setBacklogCategoryId(form.getBacklogCategoryId());
        newStoryItem.setSortOrder(newItemSortOrder++);

        mapper.save(newStoryItem);

        Story newStory = new Story();
        newStory.setStoryId(newStoryItem.getItemId());
        newStory.setStoryName(newStoryItem.getName());
        newStory.setStoryStatus(newStoryItem.getStatus());
        newStory.setBacklogCategoryId(newStoryItem.getBacklogCategoryId());
        newStory.setSortOrder(newStoryItem.getSortOrder());

        return newStory;
    }

    @RequestMapping(value = "/sprints/storyBelonging", method= RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void changeStoryBelonging(@RequestBody ChangeStoryBelongingForm form) {
        // TODO: mapperのインスタンスはどの単位で生成するのが正しい？
        DynamoDBMapper mapper = createMapper();

        Map<String, TaskItem> srcSideStories = form.getSourceId().startsWith("backlogCategory")
                                                ? searchStoriesOfBacklogCategory(form.getSourceId(), mapper)
                                                : searchStoriesOfSprint(form.getSourceId(), mapper);

        Map<String, TaskItem> destSideStories = form.getDestinationId().startsWith("backlogCategory")
                                                    ? searchStoriesOfBacklogCategory(form.getDestinationId(), mapper)
                                                    : searchStoriesOfSprint(form.getDestinationId(), mapper);


        // 対象ストーリーの所属を変更する
        TaskItem changedStory = srcSideStories.get(form.getStoryId());

        if (form.getSourceId().startsWith("backlogCategory")
                && form.getDestinationId().startsWith("backlogCategory")) {

            changedStory.setBacklogCategoryId(form.getDestinationId());

        } else if (form.getSourceId().startsWith("backlogCategory")
                    && form.getDestinationId().startsWith("sprint")) {

            changedStory.setBacklogCategoryId(null);
            changedStory.setBaseSprintId(form.getDestinationId());

        } else if (form.getSourceId().startsWith("sprint")
                && form.getDestinationId().startsWith("backlogCategory")) {

            changedStory.setBaseSprintId(null);
            changedStory.setBacklogCategoryId(form.getDestinationId());

        } else {

            changedStory.setBaseSprintId(form.getDestinationId());
        }
        changedStory.setSortOrder(form.getNewIndex());

        srcSideStories.remove(form.getStoryId());
        destSideStories.put(form.getStoryId(), changedStory);


        // 移動元スプリント or バックログカテゴリーの抜け番を詰める
        List<TaskItem> reOrderedSrcSideStories = new ArrayList<>(srcSideStories.values())
                                                    .stream()
                                                    .sorted(Comparator.comparingInt(TaskItem::getSortOrder))
                                                    .collect(Collectors.toList());

        for (int i = 0; i < reOrderedSrcSideStories.size(); i++) {
            reOrderedSrcSideStories.get(i).setSortOrder(i);
        }


        // 移動先スプリント or バックログカテゴリーの並び順を整える
        List<TaskItem> reOrderedDestSideStories = new ArrayList<TaskItem>(destSideStories.values())
                                                    .stream()
                                                    .sorted((a, b) -> sortItems(a, b, form.getStoryId()))
                                                    .collect(Collectors.toList());

        for (int i = 0; i < reOrderedDestSideStories.size(); i++) {
            reOrderedDestSideStories.get(i).setSortOrder(i);
        }


        mapper.batchSave(Stream.concat(reOrderedSrcSideStories.stream(), reOrderedDestSideStories.stream())
                                .collect(Collectors.toList()));

    }

    @RequestMapping(value = "/sprints/storySortOrder", method= RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void changeStorySortOrder(@RequestBody ChangeStorySortOrderForm form) {
        DynamoDBMapper mapper = createMapper();

        Map<String, TaskItem> allStories = form.getSourceId().startsWith("backlogCategory")
                ? searchStoriesOfBacklogCategory(form.getSourceId(), mapper)
                : searchStoriesOfSprint(form.getSourceId(), mapper);

        TaskItem changedStory = allStories.get(form.getStoryId());
        boolean isUpForward = form.getNewIndex() - changedStory.getSortOrder() > 0;
        changedStory.setSortOrder(form.getNewIndex());

        // スプリント or バックログカテゴリー内の並び順を整える
        List<TaskItem> reorderedStories = new ArrayList<TaskItem>(allStories.values())
                                                .stream()
                                                .sorted((a, b) -> sortItems(a, b, form.getStoryId(), isUpForward))
                                                .collect(Collectors.toList());

        for (int i = 0; i < reorderedStories.size(); i++) {
            reorderedStories.get(i).setSortOrder(i);
        }

        mapper.batchSave(reorderedStories);

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
                                                    .sorted((a, b) -> sortItems(a, b, form.getTaskId()))
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

        boolean isUpForward = form.getNewIndex() - orderChangedTask.getSortOrder() > 0;

        orderChangedTask.setSortOrder(form.getNewIndex());

        List<TaskItem> reorderedTasks = new ArrayList<TaskItem>(tasks.values())
                .stream()
                .filter(taskItem -> taskItem.getStatus().equals(orderChangedTask.getStatus()))
                .sorted((a, b) -> sortItems(a, b, form.getTaskId(), isUpForward))
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

        int newItemSortOrder = 0;

        // 各タスクの詳細を取得する
        List<TaskItem> tasksToGet = taskIds.stream().map(taskId -> {
            TaskItem item = new TaskItem();
            item.setUserId("user1");
            item.setItemId(taskId.getItemId());
            return item;
        }).collect(Collectors.toList());

        List<Object> searchResult = mapper.batchLoad(tasksToGet).get("TaskBoard");
        if (searchResult != null) {
            // ステータス「new」のタスクが１件以上ある場合
            newItemSortOrder = searchResult.stream()
                                            .map(task -> (TaskItem) task)
                                            .filter(task -> task.getStatus().equals("new"))
                                            .collect(Collectors.toList())
                                            .size();
        }

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

    private int sortItems(TaskItem a, TaskItem b, String targetId) {
        // 同じ表示順の場合、変更対象（ストーリー、タスク）を優先的に前に並べる
        if(a.getSortOrder() == b.getSortOrder()){
            if(a.getItemId().equals(targetId)) return -1;
            if(b.getItemId().equals(targetId)) return 1;
        }

        // その他の場合は単純に昇順に並べる
        return a.getSortOrder() - b.getSortOrder();
    }


    private int sortItems(TaskItem a, TaskItem b, String targetId, boolean isUpForward) {
        /* 同じ表示順の場合、
        　　順番の変更方向によって変更対象（ストーリー、タスク）を優先的に前 or 後に並べる */
        if(a.getSortOrder() == b.getSortOrder()){
            if (isUpForward) {
                if(a.getItemId().equals(targetId)) return 1;
                if(b.getItemId().equals(targetId)) return -1;
            } else {
                if(a.getItemId().equals(targetId)) return -1;
                if(b.getItemId().equals(targetId)) return 1;
            }
        }

        // その他の場合は単純に昇順に並べる
        return a.getSortOrder() - b.getSortOrder();
    }

    private Map<String, TaskItem> searchStoriesOfBacklogCategory(String backlogCategoryId, DynamoDBMapper mapper) {
        DynamoDBQueryExpression<BacklogCategoryIndexItem> query
                = new DynamoDBQueryExpression<BacklogCategoryIndexItem>()
                        .withIndexName("BacklogCategoryIndex")
                        .withKeyConditionExpression("UserId = :userId and BacklogCategoryId = :backlogCategoryId")
                        .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {
                            {
                                put(":userId", new AttributeValue().withS("user1"));
                                put(":backlogCategoryId", new AttributeValue().withS(backlogCategoryId));
                            }
                        });

        List<BacklogCategoryIndexItem> storyIds = mapper.query(BacklogCategoryIndexItem.class, query);

        // 各ストーリーの詳細を取得する
        List<TaskItem> storiesToGet = storyIds.stream().map(storyId -> {
                                                            TaskItem item = new TaskItem();
                                                            item.setUserId("user1");
                                                            item.setItemId(storyId.getItemId());
                                                            return item;
                                                        }).collect(Collectors.toList());

        List<Object> searchResult = mapper.batchLoad(storiesToGet).get("TaskBoard");

        if (searchResult == null) {
            return new HashMap<String, TaskItem>();
        } else {
            return searchResult
                    .stream()
                    .map(story -> (TaskItem) story)
                    .collect(Collectors.toMap(story -> story.getItemId(), story -> story));
        }
    }

    private Map<String, TaskItem> searchStoriesOfSprint(String sprintId, DynamoDBMapper mapper) {
        DynamoDBQueryExpression<SprintIndexItem> query
                = new DynamoDBQueryExpression<SprintIndexItem>()
                        .withIndexName("SprintIndex")
                        .withKeyConditionExpression("UserId = :userId and BaseSprintId = :baseSprintId")
                        .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {
                            {
                                put(":userId", new AttributeValue().withS("user1"));
                                put(":baseSprintId", new AttributeValue().withS(sprintId));
                            }
                        });

        List<SprintIndexItem> storyIds = mapper.query(SprintIndexItem.class, query);

        // 各ストーリーの詳細を取得する
        List<TaskItem> storiesToGet = storyIds.stream().map(storyId -> {
                                                            TaskItem item = new TaskItem();
                                                            item.setUserId("user1");
                                                            item.setItemId(storyId.getItemId());
                                                            return item;
                                                        }).collect(Collectors.toList());

        List<Object> searchResult = mapper.batchLoad(storiesToGet).get("TaskBoard");

        if (searchResult == null) {
            return new HashMap<String, TaskItem>();
        } else {
            return searchResult
                    .stream()
                    .map(story -> (TaskItem) story)
                    .collect(Collectors.toMap(story -> story.getItemId(), story -> story));
        }
    }

}
