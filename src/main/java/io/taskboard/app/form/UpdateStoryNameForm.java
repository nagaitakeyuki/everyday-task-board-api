package io.taskboard.app.form;

import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateStoryNameForm implements Serializable {
    private String storyName;
}
