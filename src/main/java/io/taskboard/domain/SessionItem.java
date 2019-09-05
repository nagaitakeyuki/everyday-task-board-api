package io.taskboard.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;

@DynamoDBTable(tableName="Session")
@Data
public class SessionItem {

    private String sessionId;
    private String userId;
    private Long expirationDateTime;

    @DynamoDBHashKey(attributeName="SessionId")
    public String getSessionId() {return sessionId;}

    @DynamoDBAttribute(attributeName="UserId")
    public String getUserId() {return userId;}

    @DynamoDBAttribute(attributeName="ExpirationDateTime")
    public Long getExpirationDateTime() {return expirationDateTime;}

}
