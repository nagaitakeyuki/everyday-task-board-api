package io.taskboard.app.form;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChangeStoryBelongingForm implements Serializable {
    private String sourceId;

    private String destinationId;

    private String storyId;

    private Integer newIndex;
}
