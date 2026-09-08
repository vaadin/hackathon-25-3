package com.example;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/** The ordinary Vaadin plus Spring Security setup, with one user in memory. */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // The entry point is permitted, and nothing else. This is the
                // state most applications reach: somebody notices the declared
                // stylesheet redirecting and adds a matcher for it. Comment this
                // line out and the declared sheet fails the same way.
                .authorizeHttpRequests(auth -> auth.requestMatchers("/styles/main.css").permitAll())
                .with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin.loginView(LoginView.class))
                .build();
    }

    @Bean
    public InMemoryUserDetailsManager users() {
        return new InMemoryUserDetailsManager(User.withDefaultPasswordEncoder()
                .username("user").password("password").roles("USER").build());
    }
}
