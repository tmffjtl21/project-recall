package recall.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import recall.domain.Memo;

public interface MemoRepository extends JpaRepository<Memo, Long> {

    List<Memo> findAllByOrderByIdDesc();
}
