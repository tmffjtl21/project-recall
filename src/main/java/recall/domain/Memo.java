package recall.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "memo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    // 긴 노트/코드 저장 + 검색(LIKE) 가능하도록 대용량 VARCHAR 로 매핑
    @Column(nullable = false, length = 1_000_000)
    private String content;

    // 소속 카테고리 (ManyToOne 기본 EAGER) - 상세/목록에서 즉시 접근
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // 카테고리 안에서의 표시 순서(작을수록 위). 드래그로 변경한다.
    @Column(nullable = false)
    private int sortOrder;

    @Builder
    public Memo(String title, String content, Category category, int sortOrder) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.sortOrder = sortOrder;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
