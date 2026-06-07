-- ============================================================
-- 앱 시작 시 자동 실행되는 테스트 데이터 (owner = 'test')
-- 폼 로그인 test / 123 으로 접속하면 보입니다.
-- (create-drop + defer-datasource-initialization=true 라 매 실행마다 새로 주입)
-- ============================================================

-- 카테고리 10개 (id 1~10 순서대로)
INSERT INTO category (name, owner, sort_order) VALUES ('자바', 'test', 0);
INSERT INTO category (name, owner, sort_order) VALUES ('스프링', 'test', 1);
INSERT INTO category (name, owner, sort_order) VALUES ('JPA', 'test', 2);
INSERT INTO category (name, owner, sort_order) VALUES ('알고리즘', 'test', 3);
INSERT INTO category (name, owner, sort_order) VALUES ('SQL', 'test', 4);
INSERT INTO category (name, owner, sort_order) VALUES ('프론트엔드', 'test', 5);
INSERT INTO category (name, owner, sort_order) VALUES ('네트워크', 'test', 6);
INSERT INTO category (name, owner, sort_order) VALUES ('운영체제', 'test', 7);
INSERT INTO category (name, owner, sort_order) VALUES ('면접준비', 'test', 8);
INSERT INTO category (name, owner, sort_order) VALUES ('회고', 'test', 9);

-- 노트 20개 (sort_order = 카테고리 안 표시 순서, 0이 맨 위 = 최신순)
INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('스트림 기본', 'List<String> names = users.stream()
    .filter(u -> u.getAge() >= 20)
    .map(User::getName)
    .toList();', 1, 2, 'test', '2026-05-21 09:10:00', 'test', '2026-05-21 09:10:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('Optional 사용', 'String name = Optional.ofNullable(user)
    .map(User::getName)
    .orElse("이름없음");', 1, 1, 'test', '2026-05-21 10:25:00', 'test', '2026-05-21 10:25:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('record 클래스', 'public record Point(int x, int y) {
    int distance() {
        return Math.abs(x) + Math.abs(y);
    }
}', 1, 0, 'test', '2026-05-22 11:00:00', 'test', '2026-05-22 11:00:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('REST 컨트롤러', '@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/{id}")
    public UserDto find(@PathVariable Long id) {
        return userService.find(id);
    }
}', 2, 2, 'test', '2026-05-23 09:30:00', 'test', '2026-05-23 09:30:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('의존성 주입', '@Service
public class OrderService {
    private final OrderRepository repository;
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}', 2, 1, 'test', '2026-05-23 14:05:00', 'test', '2026-05-23 14:05:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('시큐리티 필터체인', '@Bean
SecurityFilterChain chain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(a -> a.anyRequest().authenticated())
        .formLogin(Customizer.withDefaults());
    return http.build();
}', 2, 0, 'test', '2026-05-24 16:40:00', 'test', '2026-05-24 16:40:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('연관관계 매핑', '@Entity
public class Memo {
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}', 3, 1, 'test', '2026-05-25 09:15:00', 'test', '2026-05-25 09:15:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('N+1 문제', '-- fetch join 으로 해결
@Query("select m from Memo m join fetch m.category")
List<Memo> findAllWithCategory();', 3, 0, 'test', '2026-05-25 13:20:00', 'test', '2026-05-25 13:20:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('이분 탐색', 'int binarySearch(int[] a, int key) {
    int lo = 0, hi = a.length - 1;
    while (lo <= hi) {
        int mid = (lo + hi) / 2;
        if (a[mid] == key) return mid;
        if (a[mid] < key) lo = mid + 1;
        else hi = mid - 1;
    }
    return -1;
}', 4, 2, 'test', '2026-05-26 10:00:00', 'test', '2026-05-26 10:00:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('DFS 템플릿', 'void dfs(int node, boolean[] visited, List<List<Integer>> graph) {
    visited[node] = true;
    for (int next : graph.get(node)) {
        if (!visited[next]) dfs(next, visited, graph);
    }
}', 4, 1, 'test', '2026-05-26 15:45:00', 'test', '2026-05-26 15:45:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('정렬 비교', 'people.sort(Comparator
    .comparingInt(Person::age)
    .thenComparing(Person::name));', 4, 0, 'test', '2026-05-27 09:05:00', 'test', '2026-05-27 09:05:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('조인 예제', 'SELECT m.title, c.name
FROM memo m
JOIN category c ON m.category_id = c.id
WHERE c.owner = ''test''
ORDER BY m.id DESC;', 5, 2, 'test', '2026-05-28 11:30:00', 'test', '2026-05-28 11:30:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('그룹 집계', 'SELECT category_id, COUNT(*) AS cnt
FROM memo
GROUP BY category_id
HAVING COUNT(*) >= 2;', 5, 1, 'test', '2026-05-28 17:10:00', 'test', '2026-05-28 17:10:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('인덱스 메모', '-- 자주 조회하는 컬럼에 인덱스
CREATE INDEX idx_memo_category ON memo(category_id);', 5, 0, 'test', '2026-05-29 09:50:00', 'test', '2026-05-29 09:50:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('fetch API', 'const res = await fetch("/api/notes");
const notes = await res.json();
notes.forEach(n => console.log(n.title));', 6, 1, 'test', '2026-05-30 10:20:00', 'test', '2026-05-30 10:20:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('CSS 그리드', '.workspace {
    display: grid;
    grid-template-columns: 200px 320px 1fr;
    height: 100vh;
}', 6, 0, 'test', '2026-05-30 16:00:00', 'test', '2026-05-30 16:00:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('TCP 3-way', '1) Client -> SYN
2) Server -> SYN + ACK
3) Client -> ACK
연결 수립 완료', 7, 1, 'test', '2026-05-31 09:00:00', 'test', '2026-05-31 09:00:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('HTTP 상태코드', '200 OK
301 Moved Permanently
401 Unauthorized
403 Forbidden
404 Not Found
500 Internal Server Error', 7, 0, 'test', '2026-05-31 14:30:00', 'test', '2026-05-31 14:30:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('프로세스 vs 스레드', '프로세스: 독립된 메모리 공간
스레드: 프로세스 내 자원 공유
컨텍스트 스위칭 비용은 스레드가 더 작다.', 8, 0, 'test', '2026-06-01 10:40:00', 'test', '2026-06-01 10:40:00');

INSERT INTO memo (title, content, category_id, sort_order, created_by, created_date, last_modified_by, last_modified_date) VALUES
('자기소개', '안녕하세요. 백엔드 개발자를 준비하는 지원자입니다.
Spring/JPA 기반 프로젝트 경험이 있습니다.', 9, 0, 'test', '2026-06-02 09:25:00', 'test', '2026-06-02 09:25:00');
