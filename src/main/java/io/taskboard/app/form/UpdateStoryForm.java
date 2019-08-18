package io.taskboard.app.form;

import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateStoryForm implements Serializable {
    private String storyId;

    private String storyName;

    private String storyStatus;
}
