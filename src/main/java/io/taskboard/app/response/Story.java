package io.taskboard.app.response;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Story {
    private String storyId;
    private String storyName;
    private String storyStatus;
    private String baseSprintId;
    private Map<String, Task> tasks = new HashMap<>();

    public Story putTask(String taskId, Task task) {
        this.tasks.put(taskId, task);
        return this;
    }
}
