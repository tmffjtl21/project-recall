package recall.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import recall.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByOwnerOrderBySortOrderAscIdAsc(String owner);

    long countByOwner(String owner);
}
