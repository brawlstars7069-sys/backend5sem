package app.service;

import app.domain.Exceptions.ConflictException;
import app.domain.Exceptions.NotFoundException;
import app.domain.Exceptions.ValidationException;
import app.domain.Model.Computer;
import app.domain.Model.ComputerStatus;
import app.domain.Model.ComputerType;
import app.domain.Model.Player;
import app.domain.Model.PlayerType;
import app.dto.ComputerResponse;
import app.dto.PlayerResponse;
import app.repository.ComputerRepository;
import app.repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class ClubService {

    private final ComputerRepository computerRepository;
    private final PlayerRepository playerRepository;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ClubService(ComputerRepository computerRepository, PlayerRepository playerRepository) {
        this.computerRepository = computerRepository;
        this.playerRepository = playerRepository;
    }

    // --- Управление компьютерами ---

    public ComputerResponse createComputer(String name, ComputerType type, Double hourlyRate) {
        validateComputer(name, type, hourlyRate);
        lock.writeLock().lock();
        try {
            if (computerRepository.findByName(name).isPresent()) {
                throw new ConflictException("Компьютер с именем '" + name.trim() + "' уже существует в Zombie Playa");
            }
            Computer saved = computerRepository.save(new Computer(null, name.trim(), type, hourlyRate));
            return ComputerResponse.from(saved);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ComputerResponse getComputer(Long id) {
        if (id == null) {
            throw new ValidationException("Идентификатор компьютера обязателен");
        }
        lock.readLock().lock();
        try {
            return ComputerResponse.from(findComputerOrThrow(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<ComputerResponse> getComputers(ComputerType type, ComputerStatus status, Double maxRate) {
        lock.readLock().lock();
        try {
            return computerRepository.findAll().stream()
                    .filter(c -> type == null || c.getType() == type)
                    .filter(c -> status == null || c.getStatus() == status)
                    .filter(c -> maxRate == null || c.getHourlyRate() <= maxRate)
                    .map(ComputerResponse::from)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public ComputerResponse updateComputer(Long id, String name, ComputerType type, Double hourlyRate) {
        if (id == null) {
            throw new ValidationException("Идентификатор компьютера обязателен");
        }
        validateComputer(name, type, hourlyRate);
        lock.writeLock().lock();
        try {
            Computer pc = findComputerOrThrow(id);

            if (pc.getStatus() == ComputerStatus.OCCUPIED && pc.getType() != type) {
                throw new ConflictException("Нельзя изменить категорию компьютера, пока он занят игроком");
            }

            if (!pc.getName().equalsIgnoreCase(name.trim()) && computerRepository.findByName(name).isPresent()) {
                throw new ConflictException("Компьютер с именем '" + name.trim() + "' уже существует");
            }

            pc.setName(name.trim());
            pc.setType(type);
            pc.setHourlyRate(hourlyRate);
            return ComputerResponse.from(computerRepository.save(pc));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteComputer(Long id) {
        if (id == null) {
            throw new ValidationException("Идентификатор компьютера обязателен");
        }
        lock.writeLock().lock();
        try {
            Computer pc = findComputerOrThrow(id);
            if (pc.getStatus() == ComputerStatus.OCCUPIED) {
                throw new ConflictException("Невозможно удалить компьютер " + id + ": за ним сейчас играет гость");
            }
            computerRepository.deleteById(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // --- Доменные операции: занять и освободить компьютер, техобслуживание ---

    public ComputerResponse occupyComputer(Long computerId, Long playerId) {
        if (computerId == null || playerId == null) {
            throw new ValidationException("Параметры computerId и playerId обязательны");
        }
        lock.writeLock().lock();
        try {
            Computer computer = findComputerOrThrow(computerId);
            Player player = findPlayerOrThrow(playerId);

            if (computer.getStatus() == ComputerStatus.OCCUPIED) {
                throw new ConflictException("Компьютер '" + computer.getName() + "' уже занят другим игроком");
            }
            if (computer.getStatus() == ComputerStatus.MAINTENANCE) {
                throw new ConflictException("Компьютер '" + computer.getName() + "' временно на техобслуживании");
            }

            if (player.getOccupiedComputerIds().size() >= player.getMaxComputers()) {
                throw new ConflictException("Игрок типа " + player.getType() + " исчерпал лимит мест (максимум "
                        + player.getMaxComputers() + ")");
            }

            if (!player.canAccess(computer.getType())) {
                throw new ConflictException("Игрок типа " + player.getType() + " не имеет доступа к компьютерам категории "
                        + computer.getType() + ". Обычным гостям доступны только компьютеры STANDARD");
            }

            computer.setStatus(ComputerStatus.OCCUPIED);
            computer.setOccupiedByPlayerId(playerId);
            computerRepository.save(computer);

            player.addOccupiedComputer(computerId);
            playerRepository.save(player);

            return ComputerResponse.from(computer);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ComputerResponse releaseComputer(Long computerId) {
        if (computerId == null) {
            throw new ValidationException("computerId обязателен");
        }
        lock.writeLock().lock();
        try {
            Computer computer = findComputerOrThrow(computerId);
            if (computer.getStatus() != ComputerStatus.OCCUPIED) {
                throw new ConflictException("Компьютер '" + computer.getName() + "' не занят");
            }

            Long playerId = computer.getOccupiedByPlayerId();
            if (playerId != null) {
                playerRepository.findById(playerId).ifPresent(player -> {
                    player.removeOccupiedComputer(computerId);
                    playerRepository.save(player);
                });
            }

            computer.setStatus(ComputerStatus.AVAILABLE);
            computer.setOccupiedByPlayerId(null);
            return ComputerResponse.from(computerRepository.save(computer));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ComputerResponse sendToMaintenance(Long id) {
        if (id == null) {
            throw new ValidationException("Идентификатор компьютера обязателен");
        }
        lock.writeLock().lock();
        try {
            Computer pc = findComputerOrThrow(id);
            if (pc.getStatus() == ComputerStatus.OCCUPIED) {
                throw new ConflictException("Нельзя отправить компьютер на обслуживание: за ним идет игра");
            }
            if (pc.getStatus() == ComputerStatus.MAINTENANCE) {
                throw new ConflictException("Компьютер уже находится на обслуживании");
            }
            pc.setStatus(ComputerStatus.MAINTENANCE);
            return ComputerResponse.from(computerRepository.save(pc));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ComputerResponse completeMaintenance(Long id) {
        if (id == null) {
            throw new ValidationException("Идентификатор компьютера обязателен");
        }
        lock.writeLock().lock();
        try {
            Computer pc = findComputerOrThrow(id);
            if (pc.getStatus() != ComputerStatus.MAINTENANCE) {
                throw new ConflictException("Компьютер не находится на обслуживании");
            }
            pc.setStatus(ComputerStatus.AVAILABLE);
            return ComputerResponse.from(computerRepository.save(pc));
        } finally {
            lock.writeLock().unlock();
        }
    }

    // --- Управление игроками ---

    public PlayerResponse createPlayer(String nickname, String phone, PlayerType type) {
        validatePlayer(nickname, phone, type);
        lock.writeLock().lock();
        try {
            if (playerRepository.findByPhone(phone).isPresent()) {
                throw new ConflictException("Игрок с номером телефона '" + phone.trim() + "' уже зарегистрирован в клубе");
            }
            Player saved = playerRepository.save(new Player(null, nickname.trim(), phone.trim(), type));
            return PlayerResponse.from(saved);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public PlayerResponse getPlayer(Long id) {
        if (id == null) {
            throw new ValidationException("Идентификатор игрока обязателен");
        }
        lock.readLock().lock();
        try {
            return PlayerResponse.from(findPlayerOrThrow(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<PlayerResponse> getPlayers(PlayerType type, Boolean hasActiveComputers, String search) {
        lock.readLock().lock();
        try {
            return playerRepository.findAll().stream()
                    .filter(p -> type == null || p.getType() == type)
                    .filter(p -> hasActiveComputers == null || p.hasActiveComputers() == hasActiveComputers)
                    .filter(p -> search == null || searchMatches(p, search))
                    .map(PlayerResponse::from)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public PlayerResponse updatePlayer(Long id, String nickname, String phone, PlayerType type) {
        if (id == null) {
            throw new ValidationException("Идентификатор игрока обязателен");
        }
        validatePlayer(nickname, phone, type);
        lock.writeLock().lock();
        try {
            Player player = findPlayerOrThrow(id);

            if (!player.getPhone().equalsIgnoreCase(phone.trim()) && playerRepository.findByPhone(phone).isPresent()) {
                throw new ConflictException("Игрок с телефоном '" + phone.trim() + "' уже зарегистрирован");
            }

            // Проверка при понижении статуса с VIP до REGULAR
            if (player.getType() == PlayerType.VIP && type == PlayerType.REGULAR) {
                if (player.getOccupiedComputerIds().size() > 1) {
                    throw new ConflictException("Нельзя перевести игрока в REGULAR: у него занято "
                            + player.getOccupiedComputerIds().size() + " компьютеров (максимум для REGULAR — 1)");
                }
                for (Long pcId : player.getOccupiedComputerIds()) {
                    Computer pc = computerRepository.findById(pcId).orElse(null);
                    if (pc != null && pc.getType() == ComputerType.VIP) {
                        throw new ConflictException("Нельзя перевести игрока в REGULAR: он сейчас занимает VIP-компьютер");
                    }
                }
            }

            player.setNickname(nickname.trim());
            player.setPhone(phone.trim());
            player.setType(type);
            return PlayerResponse.from(playerRepository.save(player));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deletePlayer(Long id) {
        if (id == null) {
            throw new ValidationException("Идентификатор игрока обязателен");
        }
        lock.writeLock().lock();
        try {
            Player player = findPlayerOrThrow(id);
            if (player.hasActiveComputers()) {
                throw new ConflictException("Нельзя удалить профиль игрока " + id + ": он сейчас занимает компьютеры");
            }
            playerRepository.deleteById(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // --- Вспомогательные методы (вызываются под блокировкой) ---

    private Computer findComputerOrThrow(Long id) {
        return computerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Компьютер", id));
    }

    private Player findPlayerOrThrow(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Игрок", id));
    }

    // --- Валидация данных ---

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
