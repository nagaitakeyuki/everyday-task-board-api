package io.taskboard.app.form;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChangeSortOrderForm implements Serializable {
    private String sprintId;

    private String storyId;

    private String taskId;

    private Integer newIndex;
}
