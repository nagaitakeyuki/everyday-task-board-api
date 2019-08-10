package io.taskboard.app.form;

import lombok.Data;

import java.io.Serializable;

@Data
public class AddStoryForm implements Serializable {
    private String sprintId;

    private String storyName;

}
