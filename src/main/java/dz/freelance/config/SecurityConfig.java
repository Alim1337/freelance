package dz.freelance.config;

import dz.freelance.modules.user.repository.UserRepository;
import dz.freelance.shared.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public routes
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/categories/tree", "/api/v1/categories/flat").permitAll()
                .requestMatchers("/api/v1/categories/*/sub").permitAll()
                .requestMatchers("/api/v1/services/**").permitAll()
                .requestMatchers("/api/v1/providers/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Admin routes
                .requestMatchers("/api/v1/categories/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // All others require auth
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public OncePerRequestFilter jwtAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {

                String authHeader = req.getHeader("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    chain.doFilter(req, res);
                    return;
                }

                String token = authHeader.substring(7);
                try {
                    String email = jwtUtil.extractUsername(token);
                    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailsService().loadUserByUsername(email);
                        if (jwtUtil.isTokenValid(token, userDetails)) {
                            var auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                            );
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                } catch (Exception ignored) {}

                chain.doFilter(req, res);
            }
        };
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email)
            .map(user -> User.withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable: " + email));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
