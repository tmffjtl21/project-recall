package recall;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * 실제 브라우저(설치된 Chrome, 헤드리스)로 프론트엔드 흐름을 검증하는 E2E 테스트.
 * - 로그인 -> 3분할 화면 진입
 * - 코드 노트 작성 시 CodeMirror 에디터 동작
 * - 보기 화면에서 코드 하이라이트 + HTML 주입 방지(안전)
 * - 다크 모드 토글
 *
 * Chrome 이 설치되어 있지 않으면 테스트를 건너뜁니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoteFrontendTest {

    @LocalServerPort
    int port;

    private static final String[] CHROME_PATHS = {
            "C:/Program Files/Google/Chrome/Application/chrome.exe",
            "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe"
    };

    private static boolean chromeAvailable() {
        for (String p : CHROME_PATHS) {
            if (Files.exists(Path.of(p))) {
                return true;
            }
        }
        return false;
    }

    @Test
    void 코드노트_작성_렌더링_및_다크모드_E2E() {
        Assumptions.assumeTrue(chromeAvailable(), "Chrome 미설치 - E2E 테스트 건너뜀");

        String base = "http://localhost:" + port;
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium()
                    .launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            Page page = browser.newPage();

            // 1) 로그인
            page.navigate(base + "/login");
            page.fill("#username", "test");
            page.fill("#password", "123");
            page.click("button.submit");
            page.waitForSelector("#workspace");

            // 2) 시드된 카테고리가 보인다 (data.sql: 10개)
            int categoryCount = ((Number) page.evaluate(
                    "() => document.querySelectorAll('.cat-item').length")).intValue();
            assertThat(categoryCount).isEqualTo(10);

            // 3) 새 노트 작성 -> CodeMirror 마운트 + 전체 높이
            page.click(".new-note");
            page.waitForSelector(".CodeMirror");
            int editorHeight = ((Number) page.evaluate(
                    "() => document.querySelector('.CodeMirror').offsetHeight")).intValue();
            assertThat(editorHeight).isGreaterThan(300);

            // 4) HTML + 코드가 섞인 내용 입력 후 등록
            String content = "<button type=\"submit\">등록</button>\n"
                    + "public class A {\n    private int x = 1;\n}\n";
            page.fill("input.form-title", "E2E 코드 노트");
            page.evaluate("(c) => document.querySelector('textarea.js-code-editor').cm.setValue(c)", content);
            page.click("button.btn-primary");
            page.waitForSelector(".js-note-content[data-rendered='1']");

            // 5) HTML 주입 안전: 실제 button/section/form 이 생성되지 않음
            int injectedControls = ((Number) page.evaluate(
                    "() => document.querySelectorAll('.js-note-content button, .js-note-content section, .js-note-content form').length"))
                    .intValue();
            assertThat(injectedControls).isZero();

            // HTML 이 텍스트로 그대로 보임
            boolean shownAsText = (Boolean) page.evaluate(
                    "() => document.querySelector('.js-note-content').textContent.includes('<button type=\"submit\">등록</button>')");
            assertThat(shownAsText).isTrue();

            // 6) 코드 하이라이트 적용 (hljs)
            boolean highlighted = (Boolean) page.evaluate(
                    "() => { const c = document.querySelector('.js-note-content pre code');"
                            + " return !!(c && (c.classList.contains('hljs') || c.querySelector('[class^=\"hljs-\"]'))); }");
            assertThat(highlighted).isTrue();

            // 단일 코드블록으로 렌더
            int preCount = ((Number) page.evaluate(
                    "() => document.querySelectorAll('.js-note-content pre').length")).intValue();
            assertThat(preCount).isEqualTo(1);

            // 7) 노트 목록 선택 하이라이트: 방금 만든 노트가 active
            boolean createdNoteActive = (Boolean) page.evaluate(
                    "() => { const a = document.querySelector('.note-item.active .note-title');"
                            + " return !!a && a.textContent.includes('E2E 코드 노트'); }");
            assertThat(createdNoteActive).isTrue();

            // 8) JPA Auditing: 작성자(createdBy)가 상세 정보에 표시됨
            boolean writerShown = (Boolean) page.evaluate(
                    "() => document.querySelector('.detail-meta').textContent.includes('test')");
            assertThat(writerShown).isTrue();

            // 9) 검색: 시드 노트 '이분 탐색' 검색
            page.fill(".search-input", "이분");
            page.waitForFunction(
                    "() => { const h = document.querySelector('.list-col .col-head');"
                            + " return h && h.textContent.includes('검색:'); }");
            boolean searchHit = (Boolean) page.evaluate(
                    "() => [...document.querySelectorAll('.note-title')].some(e => e.textContent.includes('이분 탐색'))");
            assertThat(searchHit).isTrue();

            // 10) 다크 모드 토글
            page.click("#theme-toggle");
            String theme = (String) page.evaluate(
                    "() => document.documentElement.getAttribute('data-theme')");
            assertThat(theme).isEqualTo("dark");
            boolean darkHljsEnabled = (Boolean) page.evaluate(
                    "() => !document.getElementById('hljs-dark').disabled");
            assertThat(darkHljsEnabled).isTrue();

            browser.close();
        }
    }
}
