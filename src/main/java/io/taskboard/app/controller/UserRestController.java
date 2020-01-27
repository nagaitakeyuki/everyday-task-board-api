package io.taskboard.app.controller;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import io.taskboard.app.form.CreateUserForm;
import io.taskboard.app.response.LoginUser;
import io.taskboard.domain.UserItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserRestController {
    @Autowired
    private Environment appProps;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @RequestMapping("")
    public void createUser(@RequestBody CreateUserForm form) {
        UserItem newUser = new UserItem();
        newUser.setEmail(form.getEmail());
        newUser.setUserName(form.getUserName());
        newUser.setPassword(this.passwordEncoder.encode(form.getPass()));

        createMapper().save(newUser);
    }

    @RequestMapping("/loginUser")
    public LoginUser getSprints(@AuthenticationPrincipal(expression = "user") UserItem user) {

        LoginUser loginUser = new LoginUser();
        loginUser.setEmail(user.getEmail());
        loginUser.setUserName(user.getUserName());

        return loginUser;
    }

    private DynamoDBMapper createMapper() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                this.appProps.getProperty("aws.dynamodb.endpoint"),
                                this.appProps.getProperty("aws.region")))
                .build();

        return new DynamoDBMapper(client);
    }


}
