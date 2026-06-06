package recall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 폼 로그인(하드코딩 test/123)과 GitHub/카카오/네이버 OAuth2 소셜 로그인 보안 설정.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 인증 정책과 로그인/로그아웃, H2 콘솔 예외를 구성한다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/img/**", "/h2-console/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                // GitHub OAuth2 소셜 로그인 (같은 /login 페이지 사용)
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true))
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                // H2 콘솔(iframe) 사용을 위한 설정
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // 하드코딩 회원: test / 123
        UserDetails user = User.withUsername("test")
                .password("{noop}123")
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
