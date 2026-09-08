package com.vaadin.bakery.base.security;

import com.vaadin.bakery.base.ui.LoginView;
import com.vaadin.bakery.people.Role;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * The monitoring chain, ahead of the application's own.
     *
     * A scraper is not a person and cannot use a login form. With one chain for
     * everything, `/actuator/prometheus` answered a scrape with a 302 to the
     * login page, so the committed Prometheus job collected a login form every
     * five seconds and the Grafana dashboard stayed empty with nothing saying
     * why. This chain covers the actuator only, authenticates with HTTP Basic
     * against the same users, and still requires an admin: metrics say a great
     * deal about a business, and health is the only part that is public.
     *
     * Ordered first, because Spring Security uses the first chain whose matcher
     * accepts the request and the Vaadin chain accepts everything.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().hasRole(Role.ADMIN.name()))
                .httpBasic(basic -> {
                })
                // No session for a scraper, and no CSRF token it could not
                // obtain: every request carries its own credentials.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain bakerySecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/robots.txt", "/icons/**", "/images/**", "/styles.css", "/styles/**",
                                "/offline.html", "/manifest.webmanifest", "/sw.js", "/attachments/**")
                        .permitAll()
                        // The actuator is not here any more: it has a chain of
                        // its own above, because a scraper cannot log in.
                        )
                .with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin.loginView(LoginView.class))
                .build();
    }
}
