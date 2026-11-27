package app.repository;

import app.domain.Model.Computer;

import java.util.List;
import java.util.Optional;

public interface ComputerRepository {
    Computer save(Computer computer);
    Optional<Computer> findById(Long id);
    List<Computer> findAll();
    List<Computer> findAllWithPlayer();
    Optional<Computer> findByName(String name);
    boolean deleteById(Long id);
    void clear();
}
