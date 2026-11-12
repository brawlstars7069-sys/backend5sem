package app.repository;

import app.domain.Model.Computer;
import app.domain.Model.Player;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryClubRepository implements ClubRepository {

    private final Map<Long, Computer> computers = new ConcurrentHashMap<>();
    private final Map<Long, Player> players = new ConcurrentHashMap<>();

    private final AtomicLong computerIds = new AtomicLong(1);
    private final AtomicLong playerIds = new AtomicLong(1);

    // --- Компьютеры ---

    @Override
    public Computer saveComputer(Computer pc) {
        if (pc.getId() == null) {
            pc.setId(computerIds.getAndIncrement());
        }
        computers.put(pc.getId(), pc);
        return pc;
    }

    @Override
    public Optional<Computer> findComputerById(Long id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(computers.get(id));
    }

    @Override
    public List<Computer> findAllComputers() {
        return new ArrayList<>(computers.values());
    }

    @Override
    public Optional<Computer> findComputerByName(String name) {
        if (name == null) return Optional.empty();
        return computers.values().stream()
                .filter(c -> name.trim().equalsIgnoreCase(c.getName()))
                .findFirst();
    }

    @Override
    public boolean deleteComputer(Long id) {
        if (id == null) return false;
        return computers.remove(id) != null;
    }

    // --- Игроки ---

    @Override
    public Player savePlayer(Player player) {
        if (player.getId() == null) {
            player.setId(playerIds.getAndIncrement());
        }
        players.put(player.getId(), player);
        return player;
    }

    @Override
    public Optional<Player> findPlayerById(Long id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(players.get(id));
    }

    @Override
    public List<Player> findAllPlayers() {
        return new ArrayList<>(players.values());
    }

    @Override
    public Optional<Player> findPlayerByPhone(String phone) {
        if (phone == null) return Optional.empty();
        return players.values().stream()
                .filter(p -> phone.trim().equalsIgnoreCase(p.getPhone()))
                .findFirst();
    }

    @Override
    public boolean deletePlayer(Long id) {
        if (id == null) return false;
        return players.remove(id) != null;
    }

    @Override
    public void clear() {
        computers.clear();
        players.clear();
        computerIds.set(1);
        playerIds.set(1);
    }
}
