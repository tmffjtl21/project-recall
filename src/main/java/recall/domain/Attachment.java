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

/**
 * 노트에 올린 첨부 파일. 실제 바이트는 디스크에 저장하고, 여기엔 메타데이터만 둔다.
 * 업로드를 노트 저장과 분리하기 위해 memo 는 nullable 이며, 본문에 토큰이 들어가
 * 노트가 저장될 때 해당 노트로 연결된다.
 */
@Entity
@Table(name = "attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소유자(로그인 표시 이름) - 사용자별 분리/접근 검증 기준
    @Column(nullable = false, length = 100)
    private String owner;

    // 정리(cleanup)용 연결. 본문에 토큰이 들어가 노트가 저장될 때 채워진다.
    @ManyToOne
    @JoinColumn(name = "memo_id")
    private Memo memo;

    @Column(nullable = false, length = 255)
    private String originalName;

    // 디스크 저장명(UUID + 확장자) - path traversal 방지로 사용자 파일명을 경로에 쓰지 않는다
    @Column(nullable = false, length = 255)
    private String storedName;

    @Column(nullable = false, length = 150)
    private String contentType;

    @Column(nullable = false)
    private long size;

    // 이미지 여부(본문 인라인 렌더/인라인 서빙 판단)
    @Column(nullable = false)
    private boolean image;

    @Builder
    public Attachment(String owner, String originalName, String storedName,
                      String contentType, long size, boolean image) {
        this.owner = owner;
        this.originalName = originalName;
        this.storedName = storedName;
        this.contentType = contentType;
        this.size = size;
        this.image = image;
    }

    public void linkTo(Memo memo) {
        this.memo = memo;
    }
}
