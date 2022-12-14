package com.tapcus.portfoliowebsitejava.config;

import com.tapcus.portfoliowebsitejava.security.filter.JwtAuthenticationFilter;
import com.tapcus.portfoliowebsitejava.security.filter.UserAuthenticationFilter;
import com.tapcus.portfoliowebsitejava.security.handler.JwtAccessDeniedHandler;
import com.tapcus.portfoliowebsitejava.security.handler.JwtAuthenticationEntryPoint;
import com.tapcus.portfoliowebsitejava.security.handler.JwtLogoutSuccessHandler;
import com.tapcus.portfoliowebsitejava.security.service.impl.UserDetailServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    UserDetailsService userDetailService;

    @Autowired
    AuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    AuthenticationFailureHandler authenticationFailureHandler;

    @Autowired
    JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Autowired
    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    JwtLogoutSuccessHandler jwtLogoutSuccessHandler;

    // ?????? jwt ?????????
    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter() throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManager());
        return jwtAuthenticationFilter;
    }

    private static final String[] URL_WHITELIST = {
            "/login",
            "/logout"
    };

    private static final String[] URL_USER = {
            "/welcome",
    };

    private static final String[] URL_ADMIN = {
            "/members",
            "/members/**",
            "/article/**/viewable",
            "/articles/simple",
            "/articles/simple/**",
            "/member/{memberId}/auth"
    };

    private static final String[] URL_MEMBER = {
            "/article",
            "/article/**/message",
    };

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // ????????????????????????????????????
        auth.userDetailsService(userDetailService).passwordEncoder(new BCryptPasswordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // ?????????
        http.cors().and().csrf().disable();
        // ?????? session
        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http.formLogin()
                .and()
                .authorizeRequests().antMatchers("/login").permitAll()
                .antMatchers(URL_USER).hasRole("user")
                .antMatchers(URL_ADMIN).hasRole("admin")
                .antMatchers(URL_MEMBER).hasAnyRole("user", "admin")
                .antMatchers(URL_WHITELIST).permitAll()
                .and()
                // ????????????
                .logout()
                .logoutSuccessHandler(jwtLogoutSuccessHandler);
        //.logoutSuccessUrl("/login").permitAll()

        // ????????????
        http.exceptionHandling()
                // ??????
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                // ????????????
                .accessDeniedHandler(jwtAccessDeniedHandler);

        // ??????
        http.addFilterAt(UserAuthenticationFilterBean(), UsernamePasswordAuthenticationFilter.class)
                // ?????? jwt ?????????
                .addFilter(jwtAuthenticationFilter());
    }

    // ??????????????????????????????
    private UserAuthenticationFilter UserAuthenticationFilterBean() throws  Exception {
        UserAuthenticationFilter userAuthenticationFilter = new UserAuthenticationFilter();
        userAuthenticationFilter.setAuthenticationManager(super.authenticationManager());
        // ???????????????????????? Handler
        userAuthenticationFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        userAuthenticationFilter.setAuthenticationFailureHandler(authenticationFailureHandler);
        return userAuthenticationFilter;
    }

}
