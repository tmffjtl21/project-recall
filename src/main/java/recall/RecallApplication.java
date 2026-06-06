package recall;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class RecallApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecallApplication.class, args);
    }

    /**
     * 앱 기동 완료 시점에 실행 중인 서버 주소와 로그인 화면 URL을 콘솔에 출력한다.
     * IntelliJ 콘솔/터미널이 URL을 자동 링크 처리하므로 클릭하면 브라우저가 열린다.
     */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> startupInfoPrinter(Environment env) {
        return event -> {
            String port = env.getProperty("local.server.port",
                    env.getProperty("server.port", "8080"));
            String contextPath = env.getProperty("server.servlet.context-path", "");
            String base = "http://localhost:" + port + contextPath;

            // 한국어 Windows + IntelliJ 콘솔에서 한글이 깨지지 않도록 UTF-8 스트림으로 출력
            PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
            out.println();
            out.println("  ┌────────────────────────────────────────────────┐");
            out.println("  │  project-recall 기동 완료");
            out.println("  ├────────────────────────────────────────────────┤");
            out.println("  │  실행 서버 : localhost:" + port);
            out.println("  │  로그인    : " + base + "/login");
            out.println("  │  H2 콘솔   : " + base + "/h2-console");
            out.println("  └────────────────────────────────────────────────┘");
            out.println("  위 로그인 URL을 클릭하면 브라우저가 열립니다 (test / 123)");
            out.println();
        };
    }
}
