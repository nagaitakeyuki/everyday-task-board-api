package io.taskboard.app.controller;

import io.taskboard.app.response.LoginUser;
import io.taskboard.domain.UserItem;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserRestController {

    @RequestMapping("/loginUser")
    public LoginUser getSprints(@AuthenticationPrincipal(expression = "user") UserItem user) {

        LoginUser loginUser = new LoginUser();
        loginUser.setEmail(user.getEmail());
        loginUser.setUserName(user.getUserName());

        return loginUser;
    }
}
