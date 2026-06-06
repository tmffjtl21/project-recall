package recall.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemoDto {
    private final Long id;
    private final String title;
    private final String content;
    // 화면 표기용: 작성자(createdBy), 작성일(createdDate), 수정일(lastModifiedDate)
    private final String writer;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
