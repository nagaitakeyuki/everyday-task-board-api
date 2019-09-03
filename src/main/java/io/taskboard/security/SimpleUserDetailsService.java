package io.taskboard.security;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import io.taskboard.dao.DynamoDBMapperCreator;
import io.taskboard.domain.UserItem;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class SimpleUserDetailsService implements UserDetailsService {
    private DynamoDBMapper mapper;

    public SimpleUserDetailsService(DynamoDBMapperCreator mapperCreator) {
        this.mapper = mapperCreator.createMapper();
    }

    @Override
    public UserDetails loadUserByUsername(final String email) {
        UserItem userItem = this.mapper.load(UserItem.class, email);

        if (userItem == null) throw new UsernameNotFoundException("user not found");


        return new SimpleLoginUser(userItem);
    }

}
