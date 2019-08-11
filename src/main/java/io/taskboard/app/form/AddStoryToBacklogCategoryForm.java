package io.taskboard.app.form;

import lombok.Data;

import java.io.Serializable;

@Data
public class AddStoryToBacklogCategoryForm implements Serializable {
    private String backlogCategoryId;

    private String storyName;

}
