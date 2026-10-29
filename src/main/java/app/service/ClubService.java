package app.service;

import app.domain.Exceptions.ConflictException;
import app.domain.Exceptions.NotFoundException;
import app.domain.Exceptions.ValidationException;
import app.domain.Model.Computer;
import app.domain.Model.ComputerStatus;
import app.domain.Model.ComputerType;
import app.domain.Model.Player;
import app.domain.Model.PlayerType;
import app.domain.Model.Session;
import app.domain.Model.SessionStatus;
import app.storage.ClubStorage;

import java.util.List;

public class ClubService {

    private final ClubStorage storage;

    public ClubService(ClubStorage storage) {
        this.storage = storage;
    }

    // --- Управление компьютерами ---

    public Computer createComputer(String name, ComputerType type, Double hourlyRate) {
        validateComputer(name, type, hourlyRate);
        if (storage.findComputerByName(name).isPresent()) {
            throw new ConflictException("Компьютер с именем '" + name.trim() + "' уже существует в Zombie Playa");
        }
        return storage.saveComputer(new Computer(null, name.trim(), type, hourlyRate));
    }

    public Computer getComputer(Long id) {
        return storage.findComputerById(id)
                .orElseThrow(() -> new NotFoundException("Компьютер", id));
    }

    public List<Computer> getComputers(ComputerType type, ComputerStatus status, Double maxRate) {
        return storage.findAllComputers().stream()
                .filter(c -> type == null || c.getType() == type)
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> maxRate == null || c.getHourlyRate() <= maxRate)
                .toList();
    }

    public Computer updateComputer(Long id, String name, ComputerType type, Double hourlyRate) {
        Computer pc = getComputer(id);
        validateComputer(name, type, hourlyRate);

        if (pc.getStatus() == ComputerStatus.OCCUPIED && pc.getType() != type) {
            throw new ConflictException("Нельзя изменить категорию компьютера во время активной игровой сессии");
        }

        if (!pc.getName().equalsIgnoreCase(name.trim()) && storage.findComputerByName(name).isPresent()) {
            throw new ConflictException("Компьютер с именем '" + name.trim() + "' уже существует");
        }

        pc.setName(name.trim());
        pc.setType(type);
        pc.setHourlyRate(hourlyRate);
        return storage.saveComputer(pc);
    }

    public void deleteComputer(Long id) {
        Computer pc = getComputer(id);
        if (pc.getStatus() == ComputerStatus.OCCUPIED) {
            throw new ConflictException("Невозможно списать/удалить компьютер " + id + ": за ним сейчас играет гость");
        }
        storage.deleteComputer(id);
    }

    public Computer sendToMaintenance(Long id) {
        Computer pc = getComputer(id);
        if (pc.getStatus() == ComputerStatus.OCCUPIED) {
            throw new ConflictException("Нельзя отправить компьютер на обслуживание: за ним идет игровая сессия");
        }
        if (pc.getStatus() == ComputerStatus.MAINTENANCE) {
            throw new ConflictException("Компьютер уже находится на обслуживании");
        }
        pc.setStatus(ComputerStatus.MAINTENANCE);
        return storage.saveComputer(pc);
    }

    public Computer completeMaintenance(Long id) {
        Computer pc = getComputer(id);
        if (pc.getStatus() != ComputerStatus.MAINTENANCE) {
            throw new ConflictException("Компьютер не находится на обслуживании");
        }
        pc.setStatus(ComputerStatus.AVAILABLE);
        return storage.saveComputer(pc);
    }

    // --- Управление игроками ---

    public Player createPlayer(String nickname, String phone, PlayerType type) {
        validatePlayer(nickname, phone, type);
        if (storage.findPlayerByPhone(phone).isPresent()) {
            throw new ConflictException("Игрок с номером телефона '" + phone.trim() + "' уже зарегистрирован в клубе");
        }
        return storage.savePlayer(new Player(null, nickname.trim(), phone.trim(), type));
    }

    public Player getPlayer(Long id) {
        return storage.findPlayerById(id)
                .orElseThrow(() -> new NotFoundException("Игрок", id));
    }

    public List<Player> getPlayers(PlayerType type, Boolean hasActiveSession, String search) {
        return storage.findAllPlayers().stream()
                .filter(p -> type == null || p.getType() == type)
                .filter(p -> hasActiveSession == null || p.hasActiveSession() == hasActiveSession)
                .filter(p -> search == null || searchMatches(p, search))
                .toList();
    }

    public Player updatePlayer(Long id, String nickname, String phone, PlayerType type) {
        Player player = getPlayer(id);
        validatePlayer(nickname, phone, type);

        if (!player.getPhone().equalsIgnoreCase(phone.trim()) && storage.findPlayerByPhone(phone).isPresent()) {
            throw new ConflictException("Игрок с телефоном '" + phone.trim() + "' уже зарегистрирован");
        }

        // Правило предметной области: проверка при понижении VIP до REGULAR
        if (player.getType() == PlayerType.VIP && type == PlayerType.REGULAR) {
            if (player.getActiveComputerIds().size() > 1) {
                throw new ConflictException("Нельзя перевести игрока в REGULAR: у него запущено "
                        + player.getActiveComputerIds().size() + " сессий (максимум для REGULAR — 1)");
            }
            for (Long pcId : player.getActiveComputerIds()) {
                Computer pc = storage.findComputerById(pcId).orElse(null);
                if (pc != null && pc.getType() == ComputerType.VIP) {
                    throw new ConflictException("Нельзя перевести игрока в REGULAR: он сейчас играет за VIP-компьютером");
                }
            }
        }

        player.setNickname(nickname.trim());
        player.setPhone(phone.trim());
        player.setType(type);
        return storage.savePlayer(player);
    }

    public void deletePlayer(Long id) {
        Player player = getPlayer(id);
        if (player.hasActiveSession()) {
            throw new ConflictException("Нельзя удалить профиль игрока " + id + ": у него есть активная сессия");
        }
        storage.deletePlayer(id);
    }

    // --- Доменные операции: запуск и завершение игровой сессии ---

    public synchronized Session startSession(Long playerId, Long computerId, Integer hours) {
        if (playerId == null || computerId == null || hours == null || hours <= 0) {
            throw new ValidationException("Необходимо указать playerId, computerId и положительное число hours");
        }

        Player player = getPlayer(playerId);
        Computer computer = getComputer(computerId);

        // 1. Проверка доступности компьютера
        if (computer.getStatus() == ComputerStatus.OCCUPIED) {
            throw new ConflictException("Компьютер '" + computer.getName() + "' уже занят другим игроком");
        }
        if (computer.getStatus() == ComputerStatus.MAINTENANCE) {
            throw new ConflictException("Компьютер '" + computer.getName() + "' временно на техобслуживании");
        }

        // 2. Проверка лимита одновременных ПК для типа игрока
        if (player.getActiveComputerIds().size() >= player.getMaxActiveComputers()) {
            throw new ConflictException("Игрок типа " + player.getType() + " исчерпал лимит активных мест (максимум "
                    + player.getMaxActiveComputers() + ")");
        }

        // 3. Проверка длительности сессии
        if (hours > player.getMaxHours()) {
            throw new ConflictException("Игрок типа " + player.getType() + " не может забронировать более "
                    + player.getMaxHours() + " часов подряд (запрошено: " + hours + ")");
        }

        // 4. Проверка доступа к категории компьютера
        if (!player.canAccess(computer.getType())) {
            throw new ConflictException("Игрок типа " + player.getType() + " не имеет доступа к компьютерам категории "
                    + computer.getType() + ". Обычным гостям доступны только компьютеры STANDARD");
        }

        // 5. Расчет стоимости со скидкой игрока
        double totalCost = player.calculateCost(computer.getHourlyRate(), hours);

        // 6. Оформление сессии и смена состояний
        Session session = storage.saveSession(new Session(
                null, playerId, computerId, hours, computer.getHourlyRate(), player.getDiscountPercent(), totalCost
        ));

        computer.setStatus(ComputerStatus.OCCUPIED);
        computer.setCurrentPlayerId(playerId);
        computer.setCurrentSessionId(session.getId());
        storage.saveComputer(computer);

        player.addActiveComputer(computerId);
        storage.savePlayer(player);

        return session;
    }

    public synchronized Session stopSession(Long computerId) {
        if (computerId == null) throw new ValidationException("computerId обязателен");
        Computer computer = getComputer(computerId);
        if (computer.getStatus() != ComputerStatus.OCCUPIED) {
            throw new ConflictException("Компьютер '" + computer.getName() + "' не занят");
        }

        Session session = storage.findActiveSessionByComputerId(computerId)
                .orElseThrow(() -> new ConflictException("Активная сессия для ПК " + computerId + " не найдена"));

        Player player = storage.findPlayerById(session.getPlayerId()).orElse(null);
        if (player != null) {
            player.removeActiveComputer(computerId);
            storage.savePlayer(player);
        }

        computer.setStatus(ComputerStatus.AVAILABLE);
        computer.setCurrentPlayerId(null);
        computer.setCurrentSessionId(null);
        storage.saveComputer(computer);

        session.setStatus(SessionStatus.COMPLETED);
        return storage.saveSession(session);
    }

    public List<Session> getSessions(SessionStatus status, Long playerId, Long computerId) {
        return storage.findAllSessions().stream()
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> playerId == null || s.getPlayerId().equals(playerId))
                .filter(s -> computerId == null || s.getComputerId().equals(computerId))
                .toList();
    }

    // --- Валидация ---

    private void validateComputer(String name, ComputerType type, Double rate) {
        if (name == null || name.isBlank()) throw new ValidationException("Имя компьютера обязательно");
        if (type == null) throw new ValidationException("Тип компьютера обязателен (STANDARD, VIP)");
        if (rate == null || rate <= 0) throw new ValidationException("Почасовой тариф должен быть > 0");
    }

    private void validatePlayer(String nick, String phone, PlayerType type) {
        if (nick == null || nick.isBlank()) throw new ValidationException("Никнейм игрока обязателен");
        if (phone == null || phone.isBlank()) throw new ValidationException("Телефон игрока обязателен");
        if (type == null) throw new ValidationException("Тип игрока обязателен (REGULAR, VIP)");
    }

    private boolean searchMatches(Player p, String query) {
        String q = query.trim().toLowerCase();
        return p.getNickname().toLowerCase().contains(q) || p.getPhone().contains(q);
    }
}
