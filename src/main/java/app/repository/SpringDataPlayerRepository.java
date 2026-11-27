package app.repository;

import app.domain.Model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataPlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByPhone(String phone);

    @Query("SELECT DISTINCT p FROM Player p LEFT JOIN FETCH p.occupiedComputers")
    List<Player> findAllWithComputers();
}
