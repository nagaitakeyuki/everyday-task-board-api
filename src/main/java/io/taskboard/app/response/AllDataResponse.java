package io.taskboard.app.response;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class AllDataResponse {
    Map<String, Sprint> sprints = new HashMap<>();
    Map<String, BacklogCategory> backlogCategories = new HashMap<>();

    public AllDataResponse putSprint(String sprintId, Sprint sprint) {
        this.sprints.put(sprintId, sprint);
        return this;
    }

    public Sprint getSprint(String sprintId) {
        return this.sprints.get(sprintId);
    }

    public AllDataResponse putBacklogCategory(String backlogCategoryId, BacklogCategory backlogCategory) {
        this.backlogCategories.put(backlogCategoryId, backlogCategory);
        return this;
    }

    public BacklogCategory getBacklogCategory(String backlogCategoryId) {
        return this.backlogCategories.get(backlogCategoryId);
    }

}
