package io.taskboard.dao;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DynamoDBMapperCreator {
    @Autowired
    private Environment appProps;

    public DynamoDBMapper createMapper() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                this.appProps.getProperty("aws.dynamodb.endpoint"),
                                this.appProps.getProperty("aws.region")))
                .build();

        return new DynamoDBMapper(client);
    }

}
