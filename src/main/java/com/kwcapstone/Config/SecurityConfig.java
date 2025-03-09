package com.kwcapstone.Config;

import com.kwcapstone.GoogleLogin.Auth.Dto.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private String[] possibleAccess = {
            "/api/error", "/api", "/error", "/auth/**", "/api/**",
    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs",
    "/oauth2/**", "/login", "/terms.html", "/auth/agree", "/auth/logout"};

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CustomOAuth2UserService customOAuth2UserService) throws Exception {
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // 세션 사용 X
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(possibleAccess).permitAll()
                        .requestMatchers("/terms.html", "/favicon.ico", "/static/**", "/resources/**", "/public/**").permitAll() // ✅ 정적 리소스 허용
                        .anyRequest().authenticated());

        return http.build();
    }
}
