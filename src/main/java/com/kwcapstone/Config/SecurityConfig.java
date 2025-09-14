package com.kwcapstone.Config;

import com.kwcapstone.GoogleLogin.Auth.Dto.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private String[] possibleAccess = {
            "/api/error", "/api", "/error", "/auth/**", "/api/**",
    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs",
    "/oauth2/**", "/terms.html", "/auth/agree", "/auth/logout", "/auth/email_duplication",
    "/main/**", "/auth/email_verification", "/auth/login", "/conference/**",
    "/gptTest/**", "/ws/**", "/sockjs-node/**", "/topic/**", "/app/**"};

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(possibleAccess).permitAll()
                        .anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:8080", "https://localhost:8080",
                "https://moaba.vercel.app", "https://www.moaba.site",
                "https://moaba.site",
                "http://3.39.11.168:8080", "https://3.39.11.168:8080",
                "http://localhost:3000", "https://localhost:3000",
                "http://3.35.152.83:80", "http://43.201.65.48",
                "https://jiangxy.github.io" // 테스트 툴이라 나중엔 지워야
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization"));
        config.setExposedHeaders(Arrays.asList("Content-Type", "Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
