package recall.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import recall.domain.Memo;

public interface MemoRepository extends JpaRepository<Memo, Long> {

    // 표시 순서(sortOrder) 오름차순, 같으면 최신(id) 먼저
    List<Memo> findByCategoryIdOrderBySortOrderAscIdDesc(Long categoryId);

    void deleteByCategoryId(Long categoryId);

    /**
     * 카테고리 안 모든 노트의 순서를 한 칸씩 뒤로 민다.
     * 새 노트를 맨 위(sortOrder=0)에 넣기 위해 사용한다.
     */
    @Modifying
    @Transactional
    @Query("update Memo m set m.sortOrder = m.sortOrder + 1 where m.category.id = :categoryId")
    void shiftSortOrder(@Param("categoryId") Long categoryId);

    /**
     * 제목 또는 내용으로 검색한다(해당 사용자 소유 노트만).
     */
    @Query("select m from Memo m where m.category.owner = :owner and ("
            + "lower(m.title) like lower(concat('%', :q, '%')) "
            + "or lower(m.content) like lower(concat('%', :q, '%'))) "
            + "order by m.id desc")
    List<Memo> search(@Param("owner") String owner, @Param("q") String q);
}
