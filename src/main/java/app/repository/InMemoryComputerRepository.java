package app.repository;

import app.domain.Model.Computer;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryComputerRepository implements ComputerRepository {

    private final Map<Long, Computer> computers = new HashMap<>();
    private long nextId = 1;

    @Override
    public Computer save(Computer computer) {
        if (computer.getId() == null) {
            computer.setId(nextId++);
        }
        computers.put(computer.getId(), computer);
        return computer;
    }

    @Override
    public Optional<Computer> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(computers.get(id));
    }

    @Override
    public List<Computer> findAll() {
        return new ArrayList<>(computers.values());
    }

    @Override
    public Optional<Computer> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String normalized = name.trim();
        return computers.values().stream()
                .filter(c -> normalized.equalsIgnoreCase(c.getName()))
                .findFirst();
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null) {
            return false;
        }
        return computers.remove(id) != null;
    }

    @Override
    public void clear() {
        computers.clear();
        nextId = 1;
    }
}
