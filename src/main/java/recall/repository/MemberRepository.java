package recall.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import recall.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);
}
