package recall.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import recall.domain.Memo;

public interface MemoRepository extends JpaRepository<Memo, Long> {

    List<Memo> findByCategoryIdOrderByIdDesc(Long categoryId);

    void deleteByCategoryId(Long categoryId);

    /**
     * 제목 또는 내용으로 검색한다(해당 사용자 소유 노트만).
     */
    @Query("select m from Memo m where m.category.owner = :owner and ("
            + "lower(m.title) like lower(concat('%', :q, '%')) "
            + "or lower(m.content) like lower(concat('%', :q, '%'))) "
            + "order by m.id desc")
    List<Memo> search(@Param("owner") String owner, @Param("q") String q);
}
