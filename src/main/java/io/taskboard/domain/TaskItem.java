package io.taskboard.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;

@DynamoDBTable(tableName="TaskBoard")
@Data
public class TaskItem {

    private String userId;
    private String itemId;

    private String name;
    private String status;
    private Integer sortOrder;
    private String startDate;
    private String endDate;

    private String baseSprintId;
    private String baseStoryId;
    private String backlogCategoryId;

    @DynamoDBHashKey(attributeName="UserId")
    public String getUserId() {return userId;}

    @DynamoDBRangeKey(attributeName="ItemId")
    public String getItemId() {return itemId;}

    @DynamoDBAttribute(attributeName="Name")
    public String getName() {return name; }

    @DynamoDBAttribute(attributeName="Status")
    public String getStatus() {return status;}

    @DynamoDBAttribute(attributeName="SortOrder")
    public Integer getSortOrder() {return sortOrder;}

    @DynamoDBAttribute(attributeName="startDate")
    public String getStartDate() {return startDate;}

    @DynamoDBAttribute(attributeName="endDate")
    public String getEndDate() {return endDate;}

    @DynamoDBAttribute(attributeName="BaseSprintId")
    public String getBaseSprintId() {return baseSprintId;}

    @DynamoDBAttribute(attributeName="BaseStoryId")
    public String getBaseStoryId() {return baseStoryId;}

    @DynamoDBAttribute(attributeName="BacklogCategoryId")
    public String getBacklogCategoryId() {return backlogCategoryId;}

}
