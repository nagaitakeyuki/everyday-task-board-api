package io.taskboard.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;

@DynamoDBTable(tableName="Task")
@Data
public class TaskItem {

    private String pk;
    private String sk;

    private String data;
    private String status;

    private String baseSprintId;
    private String baseStoryId;

    @DynamoDBHashKey(attributeName="PK")
    public String getPk() {return pk;}

    @DynamoDBRangeKey(attributeName="SK")
    public String getSk() {return sk;}

    @DynamoDBAttribute(attributeName="Data")
    public String getData() {return data; }

    @DynamoDBAttribute(attributeName="Status")
    public String getStatus() {return status;}

    @DynamoDBAttribute(attributeName="BaseSprintId")
    public String getBaseSprintId() {return baseSprintId;}

    @DynamoDBAttribute(attributeName="BaseStoryId")
    public String getBaseStoryId() {return baseStoryId;}
}
