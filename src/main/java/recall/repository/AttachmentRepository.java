package recall.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import recall.domain.Attachment;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByMemoId(Long memoId);
}
