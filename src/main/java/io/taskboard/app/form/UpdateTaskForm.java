package io.taskboard.app.form;

import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateTaskForm implements Serializable {
    private String taskId;

    private String taskName;

}
