package io.taskboard.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;

@DynamoDBTable(tableName="TaskBoard")
@Data
public class SprintIndexItem {

    private String userId;
    private String baseSprintId;
    private String itemId;

    @DynamoDBHashKey(attributeName="UserId")
    public String getUserId() {return userId;}

    @DynamoDBIndexRangeKey(localSecondaryIndexName ="SprintIndex")
    public String getBaseSprintId() {return baseSprintId;}

    @DynamoDBAttribute(attributeName="ItemId")
    public String getItemId() {return itemId;}

}
