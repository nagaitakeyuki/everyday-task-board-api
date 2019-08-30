package io.taskboard.app.response;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class BacklogCategory {
    private String backlogCategoryId;
    private String backlogCategoryName;
    private String status;
    private int sortOrder;
    private Map<String, Story> stories = new HashMap<>();

    public BacklogCategory putStory(String storyId, Story story) {
        this.stories.put(storyId, story);
        return this;
    }

    public Story getStory(String storyId) {
        return this.stories.get(storyId);
    }

}
