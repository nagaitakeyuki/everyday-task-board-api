package io.taskboard.security;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.taskboard.app.response.LoginUser;
import io.taskboard.dao.DynamoDBMapperCreator;
import io.taskboard.domain.SessionItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 認証が成功した時の処理
 */
@Slf4j
public class SimpleAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private DynamoDBMapper mapper;

    private static final Long EXPIRATION_TIME = TimeUnit.MINUTES.toMillis(60L);
    private static final Long EXPIRATION_TIME_IN_SECONDS = TimeUnit.MINUTES.toSeconds(60L);

    public SimpleAuthenticationSuccessHandler(DynamoDBMapperCreator mapperCreator) {
        this.mapper = mapperCreator.createMapper();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication auth) {
        if (response.isCommitted()) {
            log.info("Response has already been committed.");
            return;
        }

        SimpleLoginUser loginUser = (SimpleLoginUser) auth.getPrincipal();

        String sessionId = UUID.randomUUID().toString();

        SessionItem sessionItem = new SessionItem();
        sessionItem.setSessionId(sessionId);
        sessionItem.setUserId(loginUser.getUser().getEmail());
        sessionItem.setExpirationDateTime(new Date().getTime() + EXPIRATION_TIME);

        mapper.save(sessionItem);

        Cookie sessionIdCookie = new Cookie("taskboardsessionid", sessionId);
        sessionIdCookie.setHttpOnly(true);
        // TODO: デベロッパーツールをみると、ExpiresがUTCになってる。
        sessionIdCookie.setMaxAge(EXPIRATION_TIME_IN_SECONDS.intValue());

        response.addCookie(sessionIdCookie);
        response.setStatus(HttpStatus.OK.value());

        response.setContentType("application/json;charset=UTF-8");

        try (PrintWriter pw = response.getWriter()) {
            LoginUser userResponse = new LoginUser();
            userResponse.setEmail(loginUser.getUser().getEmail());
            userResponse.setUserName(loginUser.getUser().getUserName());

            pw.println(new ObjectMapper().writeValueAsString(userResponse));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        clearAuthenticationAttributes(request);
    }

    private void setToken(HttpServletResponse response, String token) {
      response.setHeader("Authorization", String.format("Bearer %s", token));
    }

    /**
    * Removes temporary authentication-related data which may have been stored in the
    * session during the authentication process.
    */
    private void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
          return;
        }
        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }

}
