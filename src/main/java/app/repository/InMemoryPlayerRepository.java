package app.repository;

import app.domain.Model.Player;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryPlayerRepository implements PlayerRepository {

    private final Map<Long, Player> players = new HashMap<>();
    private long nextId = 1;

    @Override
    public Player save(Player player) {
        if (player.getId() == null) {
            player.setId(nextId++);
        }
        players.put(player.getId(), player);
        return player;
    }

    @Override
    public Optional<Player> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(players.get(id));
    }

    @Override
    public List<Player> findAll() {
        return new ArrayList<>(players.values());
    }

    @Override
    public Optional<Player> findByPhone(String phone) {
        if (phone == null) {
            return Optional.empty();
        }
        String normalized = phone.trim();
        return players.values().stream()
                .filter(p -> normalized.equalsIgnoreCase(p.getPhone()))
                .findFirst();
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null) {
            return false;
        }
        return players.remove(id) != null;
    }

    @Override
    public void clear() {
        players.clear();
        nextId = 1;
    }
}
