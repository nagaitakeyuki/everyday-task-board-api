package io.taskboard.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Data;

@DynamoDBTable(tableName="TaskBoard")
@Data
public class StoryIndexItem {

    private String userId;
    private String baseStoryId;
    private String itemId;

    @DynamoDBHashKey(attributeName="UserId")
    public String getUserId() {return userId;}

    @DynamoDBIndexRangeKey(localSecondaryIndexName ="StoryIndex")
    public String getBaseStoryId() {return baseStoryId;}

    @DynamoDBAttribute(attributeName="ItemId")
    public String getItemId() {return itemId;}

}
