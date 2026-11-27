package app.repository;

import app.domain.Model.Computer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataComputerRepository extends JpaRepository<Computer, Long> {
    Optional<Computer> findByNameIgnoreCase(String name);

    @Query("SELECT c FROM Computer c LEFT JOIN FETCH c.occupiedBy")
    List<Computer> findAllWithPlayer();
}
