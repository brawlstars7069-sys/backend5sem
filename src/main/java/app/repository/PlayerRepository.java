package app.repository;

import app.domain.Model.Player;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository {
    Player save(Player player);
    Optional<Player> findById(Long id);
    List<Player> findAll();
    Optional<Player> findByPhone(String phone);
    boolean deleteById(Long id);
    void clear();
}
