package io.taskboard.app.response;

import lombok.Data;

import java.util.List;

@Data
public class AddTasksResponse {
    List<Task> newTasks;
}
