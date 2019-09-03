package io.taskboard.security;

import io.taskboard.dao.DynamoDBMapperCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private DynamoDBMapperCreator dbMapperCreator;

    // @Value("${security.secret-key:secret}")
    private String secretKey = "secret";

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
            // AUTHORIZE
            .authorizeRequests()
                .mvcMatchers("/sprints/**")
                    .hasRole("USER")
                .anyRequest()
                    .authenticated()
            .and()
            // EXCEPTION
            .exceptionHandling()
                .authenticationEntryPoint(new SimpleAuthenticationEntryPoint())
            .and()
            // LOGIN
            .formLogin()
                .loginProcessingUrl("/authenticate").permitAll()
                    .usernameParameter("email")
                    .passwordParameter("pass")
                .successHandler(new SimpleAuthenticationSuccessHandler(secretKey))
                .failureHandler(new SimpleAuthenticationFailureHandler())
            .and()
            // LOGOUT
            .logout()
                .logoutUrl("/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
            .and()
            // CSRF
            .csrf()
                .disable()
            // AUTHORIZE
            .addFilterBefore(new SimpleTokenFilter(secretKey, dbMapperCreator), UsernamePasswordAuthenticationFilter.class)
            // SESSION
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            ;
        // @formatter:on
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.eraseCredentials(true)
            .userDetailsService(new SimpleUserDetailsService(dbMapperCreator))
            .passwordEncoder(passwordEncoder());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}
