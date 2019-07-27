package io.taskboard.app.response;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Sprint {
    private String sprintId;
    private String sprintName;
    private String springStatus;
    private Map<String, Story> stories = new HashMap<>();

    public Sprint putStory(String storyId, Story story) {
        this.stories.put(storyId, story);
        return this;
    }

    public Story getStory(String storyId) {
        return this.stories.get(storyId);
    }

}
