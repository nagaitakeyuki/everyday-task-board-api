package io.taskboard.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;

@DynamoDBTable(tableName="User")
@Data
public class UserItem {

    private String email;
    private String userName;
    private String password;

    @DynamoDBHashKey(attributeName="Email")
    public String getEmail() {return email;}

    @DynamoDBAttribute(attributeName="UserName")
    public String getUserName() {return userName;}

    @DynamoDBAttribute(attributeName="Password")
    public String getPassword() {return password;}


}
