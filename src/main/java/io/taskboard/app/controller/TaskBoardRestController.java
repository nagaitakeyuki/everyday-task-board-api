package io.taskboard.app.controller;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import io.taskboard.app.form.AddTasksForm;
import io.taskboard.app.form.ChangeTaskStatusForm;
import io.taskboard.app.response.*;
import io.taskboard.domain.TaskItem;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
                response.getSprint(item.getPk()).getStory(item.getBaseStoryId()).putTask(item.getSk(), task);
            }

        });

        return response;

    }

    @RequestMapping(value = "/sprints/taskStatus", method= RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void changeTaskStatus(@RequestBody ChangeTaskStatusForm form) {

        DynamoDBMapper mapper = createMapper();

        TaskItem targetTaskItem = mapper.load(TaskItem.class, form.getSprintId(), form.getTaskId());

        targetTaskItem.setStatus(form.getNewStatus());

        mapper.save(targetTaskItem);
    }

    @RequestMapping(value = "/sprints/tasks", method= RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public AddTasksResponse addTasks(@RequestBody AddTasksForm form) {

        DynamoDBMapper mapper = createMapper();

        final List<TaskItem> newTasks = Arrays.stream(form.getTaskNames()).map(taskName -> {
            TaskItem newTask = new TaskItem();
            newTask.setPk(form.getSprintId());
            newTask.setSk("task" + UUID.randomUUID().toString());
            newTask.setData(taskName);
            newTask.setStatus("new");
            newTask.setBaseStoryId(form.getStoryId());

            return newTask;
        }).collect(Collectors.toList());

        mapper.batchSave(newTasks);

        List<Task> newTasksForResponse = newTasks.stream().map(taskItem -> {
            Task task = new Task();
            task.setTaskId(taskItem.getSk());
            task.setTaskName(taskItem.getData());
            task.setTaskStatus(taskItem.getStatus());
            task.setBaseStoryId(taskItem.getBaseStoryId());

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
