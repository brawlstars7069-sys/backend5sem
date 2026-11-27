package app.repository;

import app.domain.Model.Player;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaPlayerRepository implements PlayerRepository {

    private final SpringDataPlayerRepository jpaRepo;

    public JpaPlayerRepository(SpringDataPlayerRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Player save(Player player) {
        return jpaRepo.save(player);
    }

    @Override
    public Optional<Player> findById(Long id) {
        if (id == null) return Optional.empty();
        return jpaRepo.findById(id);
    }

    @Override
    public List<Player> findAll() {
        return jpaRepo.findAll();
    }

    @Override
    public List<Player> findAllWithComputers() {
        return jpaRepo.findAllWithComputers();
    }

    @Override
    public Optional<Player> findByPhone(String phone) {
        if (phone == null) return Optional.empty();
        return jpaRepo.findByPhone(phone.trim());
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null || !jpaRepo.existsById(id)) return false;
        jpaRepo.deleteById(id);
        return true;
    }

    @Override
    public void clear() {
        jpaRepo.deleteAll();
    }
}
