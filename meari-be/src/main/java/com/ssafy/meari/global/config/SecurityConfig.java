package com.ssafy.meari.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meari.global.auth.DefaultAuthenticationFilter;
import com.ssafy.meari.global.auth.UserDetailsServiceImpl;
import com.ssafy.meari.global.auth.jwt.JwtAuthenticationFilter;
import com.ssafy.meari.global.auth.jwt.JwtExceptionFilter;
import com.ssafy.meari.global.auth.jwt.JwtUtil;
import com.ssafy.meari.global.auth.service.RefreshTokenService;
import com.ssafy.meari.global.auth.service.TokenBlacklistService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfig corsConfig;
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final ObjectMapper objectMapper;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    @Bean
    public AuthenticationManager authenticationManager() {

        // 데이터베이스에서 사용자 정보를 조회하여 인증하는 방식
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        provider.setPasswordEncoder(passwordEncoder());

        // loadUserByUsername(String username) 메서드로 사용자 조회
        provider.setUserDetailsService(userDetailsService);

        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS 설정
            .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))

            // CSRF 비활성화 (REST API)
            .csrf(csrf -> csrf.disable())

            // 세션 사용 안함 (JWT 사용)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Form 로그인 비활성화
            .formLogin(form -> form.disable())

            // HTTP Basic 인증 비활성화
            .httpBasic(basic -> basic.disable())

            .authorizeHttpRequests(authz -> authz
                // 인증 없이 접근 가능
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/error").permitAll()

                // 나머지는 인증 필요
                .anyRequest().authenticated()
            );

        // JWT 필터 등록
        http // JwtExceptionFilter -> JwtAuthenticationFilter -> DefaultAuthenticationFilter 순서로 작동

            // 토큰이 없다면 AuthenticationManager에게 DB조회를 통한 인증을 위임하여
            // 성공하면 토큰을 반환하고(로그인 처리) 실패하면 예외 발생
            .addFilterBefore(new DefaultAuthenticationFilter(authenticationManager(), jwtUtil, refreshTokenService, objectMapper), UsernamePasswordAuthenticationFilter.class)

            // 토큰이 있으면 검증하고 없으면 DefaultAuthenticationFilter로 전달
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService, tokenBlacklistService), DefaultAuthenticationFilter.class)

            // Spring Security 필터에서 발생한 예외를 처리
            .addFilterBefore(new JwtExceptionFilter(objectMapper), JwtAuthenticationFilter.class);


        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
