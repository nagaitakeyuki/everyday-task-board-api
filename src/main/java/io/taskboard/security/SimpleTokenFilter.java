package io.taskboard.security;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import io.taskboard.dao.DynamoDBMapperCreator;
import io.taskboard.domain.SessionItem;
import io.taskboard.domain.UserItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

@Slf4j
public class SimpleTokenFilter extends GenericFilterBean {
    private DynamoDBMapper mapper;

    public SimpleTokenFilter(DynamoDBMapperCreator dbMapperCreator) {
        this.mapper = dbMapperCreator.createMapper();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        Cookie sessionIdCookie = getSessionIdCookie(request);

        if (sessionIdCookie == null) {
            filterChain.doFilter(request, response);
            return;
        }

        SessionItem sessionItem = mapper.load(SessionItem.class, sessionIdCookie.getValue());

        if (sessionItem == null) sendUnauthorizedError(response);

        if (sessionItem.getExpirationDateTime() < new Date().getTime()) sendUnauthorizedError(response);

        UserItem userItem = this.mapper.load(UserItem.class, sessionItem.getUserId());

        if (userItem == null) sendUnauthorizedError(response);

        SimpleLoginUser loginUser = new SimpleLoginUser(userItem);

        SecurityContextHolder
                .getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(loginUser,null,
                        loginUser.getAuthorities()));

        filterChain.doFilter(request, response);
    }

    private Cookie getSessionIdCookie(ServletRequest request) {
        HttpServletRequest httpReq = (HttpServletRequest) request;

        if (httpReq.getCookies() == null) return null;

        for (Cookie cookie: httpReq.getCookies()) {
            if ("taskboardsessionid".equals(cookie.getName())) {
                return cookie;
            }
        }

        return null;
    }

    private void sendUnauthorizedError(ServletResponse response) throws IOException {
        ((HttpServletResponse) response).sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase());
    }


}
