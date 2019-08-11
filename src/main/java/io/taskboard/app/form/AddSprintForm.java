package io.taskboard.app.form;

import lombok.Data;

import java.io.Serializable;

@Data
public class AddSprintForm implements Serializable {
    private String sprintName;
}
