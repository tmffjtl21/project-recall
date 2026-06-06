package recall.config;

import org.springframework.beans.factory.annotation.Value;
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
 * 폼 로그인과 GitHub/카카오/네이버 OAuth2 소셜 로그인 보안 설정.
 * 폼 로그인 계정은 환경변수(APP_LOGIN_USERNAME / APP_LOGIN_PASSWORD)로 바꿀 수 있다.
 * 외부에 배포할 때는 기본값(test/123)을 반드시 변경할 것.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.login.username:test}")
    private String loginUsername;

    @Value("${app.login.password:123}")
    private String loginPassword;

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

    /**
     * 폼 로그인 계정. 기본값은 test/123, 운영에서는 환경변수로 덮어쓴다.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withUsername(loginUsername)
                .password("{noop}" + loginPassword)
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
