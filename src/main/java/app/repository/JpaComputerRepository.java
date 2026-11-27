package app.repository;

import app.domain.Model.Computer;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaComputerRepository implements ComputerRepository {

    private final SpringDataComputerRepository jpaRepo;

    public JpaComputerRepository(SpringDataComputerRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Computer save(Computer computer) {
        return jpaRepo.save(computer);
    }

    @Override
    public Optional<Computer> findById(Long id) {
        if (id == null) return Optional.empty();
        return jpaRepo.findById(id);
    }

    @Override
    public List<Computer> findAll() {
        return jpaRepo.findAll();
    }

    @Override
    public List<Computer> findAllWithPlayer() {
        return jpaRepo.findAllWithPlayer();
    }

    @Override
    public Optional<Computer> findByName(String name) {
        if (name == null) return Optional.empty();
        return jpaRepo.findByNameIgnoreCase(name.trim());
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
