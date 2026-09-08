package com.vaadin.bakery.base.security;

import com.vaadin.bakery.base.ui.LoginView;
import com.vaadin.bakery.people.Role;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration {

    /** The realm a browser shows in its prompt, and the reason it prompts. */
    private static BasicAuthenticationEntryPoint entryPoint() {
        var basic = new BasicAuthenticationEntryPoint();
        basic.setRealmName("Bakery metrics");
        return basic;
    }

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
                // Basic, and its own entry point, so an unauthenticated
                // request is answered 401 with a challenge rather than 302 to
                // the login view. Both readers need that. A scraper is
                // configured with credentials and sends them anyway; a person
                // following the link from the diagnostics screen gets the
                // browser's own password prompt, which is the only way in,
                // because this chain is stateless and the screen's session
                // does not reach it.
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint()))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(entryPoint()))
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
                        // The container's error dispatch goes through the chain
                        // again, and this chain matches everything, so a 401
                        // raised by the actuator chain came back here and was
                        // turned into a 302 to the login view: the browser
                        // followed the redirect instead of asking for the
                        // credentials, and a scraper read a login form.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
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
