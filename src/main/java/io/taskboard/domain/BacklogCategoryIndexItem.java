package io.taskboard.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;

@DynamoDBTable(tableName="TaskBoard")
@Data
public class BacklogCategoryIndexItem {

    private String userId;
    private String backlogCategoryId;
    private String itemId;

    @DynamoDBHashKey(attributeName="UserId")
    public String getUserId() {return userId;}

    @DynamoDBIndexRangeKey(localSecondaryIndexName ="BacklogCategoryIndex")
    public String getBacklogCategoryId() {return backlogCategoryId;}

    @DynamoDBAttribute(attributeName="ItemId")
    public String getItemId() {return itemId;}

}
