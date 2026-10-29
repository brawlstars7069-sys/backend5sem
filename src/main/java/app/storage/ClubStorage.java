package app.storage;

import app.domain.Model.Computer;
import app.domain.Model.Player;
import app.domain.Model.Session;
import app.domain.Model.SessionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ClubStorage {

    private final Map<Long, Computer> computers = new ConcurrentHashMap<>();
    private final Map<Long, Player> players = new ConcurrentHashMap<>();
    private final Map<Long, Session> sessions = new ConcurrentHashMap<>();

    private final AtomicLong computerIds = new AtomicLong(1);
    private final AtomicLong playerIds = new AtomicLong(1);
    private final AtomicLong sessionIds = new AtomicLong(1);

    // Компьютеры
    public Computer saveComputer(Computer pc) {
        if (pc.getId() == null) pc.setId(computerIds.getAndIncrement());
        computers.put(pc.getId(), pc);
        return pc;
    }

    public Optional<Computer> findComputerById(Long id) {
        return Optional.ofNullable(computers.get(id));
    }

    public List<Computer> findAllComputers() {
        return new ArrayList<>(computers.values());
    }

    public Optional<Computer> findComputerByName(String name) {
        if (name == null) return Optional.empty();
        return computers.values().stream()
                .filter(c -> name.trim().equalsIgnoreCase(c.getName()))
                .findFirst();
    }

    public boolean deleteComputer(Long id) {
        return computers.remove(id) != null;
    }

    // Игроки
    public Player savePlayer(Player player) {
        if (player.getId() == null) player.setId(playerIds.getAndIncrement());
        players.put(player.getId(), player);
        return player;
    }

    public Optional<Player> findPlayerById(Long id) {
        return Optional.ofNullable(players.get(id));
    }

    public List<Player> findAllPlayers() {
        return new ArrayList<>(players.values());
    }

    public Optional<Player> findPlayerByPhone(String phone) {
        if (phone == null) return Optional.empty();
        return players.values().stream()
                .filter(p -> phone.trim().equalsIgnoreCase(p.getPhone()))
                .findFirst();
    }

    public boolean deletePlayer(Long id) {
        return players.remove(id) != null;
    }

    // Сессии
    public Session saveSession(Session session) {
        if (session.getId() == null) session.setId(sessionIds.getAndIncrement());
        sessions.put(session.getId(), session);
        return session;
    }

    public Optional<Session> findSessionById(Long id) {
        return Optional.ofNullable(sessions.get(id));
    }

    public List<Session> findAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    public Optional<Session> findActiveSessionByComputerId(Long pcId) {
        if (pcId == null) return Optional.empty();
        return sessions.values().stream()
                .filter(s -> pcId.equals(s.getComputerId()) && s.getStatus() == SessionStatus.ACTIVE)
                .findFirst();
    }
}
