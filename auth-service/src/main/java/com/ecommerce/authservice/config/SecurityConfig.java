package com.ecommerce.authservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
	
	@Autowired
	JwtFilter jwtFilter;

	
	@Bean
	public PasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder();
	}
	
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
	    CorsConfiguration configuration = new CorsConfiguration();
	    configuration.setAllowedOrigins(java.util.Arrays.asList(
	    	"http://localhost:3000",
	    	"http://localhost:5173",
	    	"http://localhost:3001",
	    	"http://localhost:3002",
	    	"http://127.0.0.1:3000",
	    	"http://127.0.0.1:3001",
	    	"http://127.0.0.1:3002",
	    	"http://127.0.0.1:5173",
	    	"http://advancepaymentui.s3-website.ap-south-1.amazonaws.com",
	    	"https://advancepaymentui.s3-website.ap-south-1.amazonaws.com"
	    ));
	    configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
	    configuration.setAllowedHeaders(java.util.Arrays.asList("*"));
	    configuration.setAllowCredentials(true);
	    
	    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    source.registerCorsConfiguration("/**", configuration);
	    return source;
	}
	
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/auth/login","/auth/signup","/auth/loginRefreshToken").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/users/*/advance-payments").authenticated()
                .requestMatchers(HttpMethod.GET, "/auth/users/*/advance-payments").authenticated()
                .requestMatchers(HttpMethod.POST, "/auth/exports/advance-payments").authenticated()
                .requestMatchers(HttpMethod.POST, "/auth/users/*/notes").authenticated()
                .requestMatchers(HttpMethod.GET, "/auth/users/*/notes").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/auth/users/*/notes/*").authenticated()
                .requestMatchers(HttpMethod.PUT, "/auth/users/*/notes/*").authenticated()
                .requestMatchers(HttpMethod.PUT, "/auth/users/*/notes/*/star").authenticated()
                .anyRequest().authenticated()
            ).addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
