package ma.codexa.goldyara.repository;

import ma.codexa.goldyara.entity.TopBarMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopBarMessageRepository extends JpaRepository<TopBarMessage, Long> {
    
    List<TopBarMessage> findByIsActiveOrderByDisplayOrderAsc(Boolean isActive);
    
    List<TopBarMessage> findByIsActiveTrueOrderByDisplayOrderAsc();
}
