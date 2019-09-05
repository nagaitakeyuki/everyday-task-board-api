package io.taskboard.security;

import io.taskboard.dao.DynamoDBMapperCreator;
import io.taskboard.domain.SessionItem;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SimpleLogoutHandler implements LogoutHandler {
    DynamoDBMapperCreator mapperCreator;

    public SimpleLogoutHandler(DynamoDBMapperCreator dbMapperCreator) {
        this.mapperCreator = dbMapperCreator;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Cookie sessionIdCookie = getSessionIdCookie(request);

        if (sessionIdCookie == null) {
            return;
        }

        SessionItem sessionItemToDelete = new SessionItem();
        sessionItemToDelete.setSessionId(sessionIdCookie.getValue());

        mapperCreator.createMapper().delete(sessionItemToDelete);
    }

    private Cookie getSessionIdCookie(HttpServletRequest request) {

        if (request.getCookies() == null) return null;

        for (Cookie cookie: request.getCookies()) {
            if ("taskboardsessionid".equals(cookie.getName())) {
                return cookie;
            }
        }

        return null;
    }

}
