package io.taskboard.app.response;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SprintsResponse {
    Map<String, Sprint> sprints = new HashMap<>();

    public SprintsResponse putSprint(String sprintId, Sprint sprint) {
        this.sprints.put(sprintId, sprint);
        return this;
    }

    public Sprint getSprint(String sprintId) {
        return this.sprints.get(sprintId);
    }
}
