package io.taskboard.app.form;

import lombok.Data;

import java.io.Serializable;

@Data
public class AddBacklogCategoryForm implements Serializable {
    private String backlogCategoryName;
}
