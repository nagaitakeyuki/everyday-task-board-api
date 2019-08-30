package io.taskboard.app.form;

import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateSprintForm implements Serializable {
    private String sprintName;
    private String startDate;
    private String endDate;
}
