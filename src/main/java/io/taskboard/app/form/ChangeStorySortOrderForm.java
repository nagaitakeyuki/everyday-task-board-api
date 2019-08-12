package io.taskboard.app.form;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChangeStorySortOrderForm implements Serializable {
    private String sourceId;

    private String storyId;

    private Integer newIndex;
}
