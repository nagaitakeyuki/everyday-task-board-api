package io.taskboard.app.response;

import lombok.Data;

@Data
public class Task {
    private String taskId;
    private String taskName;
    private String taskStatus;
    private String baseStoryId;
    private int sortOrder;
}
