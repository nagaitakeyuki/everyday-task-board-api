package io.taskboard.security;

import io.taskboard.domain.UserItem;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class SimpleLoginUser extends User {

    // Userエンティティ
    private UserItem userItem;

    public UserItem getUser() {
        return userItem;
    }

    public SimpleLoginUser(UserItem userItem) {
        super(userItem.getEmail(), userItem.getPassword(), USER_ROLES);
        this.userItem = userItem;
    }

    private static final List<GrantedAuthority> USER_ROLES = AuthorityUtils.createAuthorityList("ROLE_USER");
    private static final List<GrantedAuthority> ADMIN_ROLES = AuthorityUtils.createAuthorityList("ROLE_ADMIN", "ROLE_USER");

    private static List<GrantedAuthority> determineRoles(boolean isAdmin) {
        return isAdmin ? ADMIN_ROLES : USER_ROLES;
    }
}
