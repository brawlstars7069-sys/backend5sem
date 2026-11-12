package app.service;

import app.domain.Exceptions.ConflictException;
import app.domain.Exceptions.NotFoundException;
import app.domain.Exceptions.ValidationException;
import app.domain.Model.Computer;
import app.domain.Model.ComputerStatus;
import app.domain.Model.ComputerType;
import app.domain.Model.Player;
import app.domain.Model.PlayerType;
import app.repository.ClubRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClubService {

    private final ClubRepository repository;

    public ClubService(ClubRepository repository) {
        this.repository = repository;
    }

    // --- Управление компьютерами ---

    public synchronized Computer createComputer(String name, ComputerType type, Double hourlyRate) {
        validateComputer(name, type, hourlyRate);
        if (repository.findComputerByName(name).isPresent()) {
            throw new ConflictException("Компьютер с именем '" + name.trim() + "' уже существует в Zombie Playa");
        }
        return repository.saveComputer(new Computer(null, name.trim(), type, hourlyRate));
    }

    public synchronized Computer getComputer(Long id) {
        if (id == null) {
            throw new ValidationException("Идентификатор компьютера обязателен");
        }
        return repository.findComputerById(id)
                .orElseThrow(() -> new NotFoundException("Компьютер", id));
    }

    public synchronized List<Computer> getComputers(ComputerType type, ComputerStatus status, Double maxRate) {
        return repository.findAllComputers().stream()
                .filter(c -> type == null || c.getType() == type)
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> maxRate == null || c.getHourlyRate() <= maxRate)
                .toList();
    }

    public synchronized Computer updateComputer(Long id, String name, ComputerType type, Double hourlyRate) {
        Computer pc = getComputer(id);
        validateComputer(name, type, hourlyRate);

        if (pc.getStatus() == ComputerStatus.OCCUPIED && pc.getType() != type) {
            throw new ConflictException("Нельзя изменить категорию компьютера, пока он занят игроком");
        }

        if (!pc.getName().equalsIgnoreCase(name.trim()) && repository.findComputerByName(name).isPresent()) {
            throw new ConflictException("Компьютер с именем '" + name.trim() + "' уже существует");
        }

        pc.setName(name.trim());
        pc.setType(type);
        pc.setHourlyRate(hourlyRate);
        return repository.saveComputer(pc);
    }

    public synchronized void deleteComputer(Long id) {
        Computer pc = getComputer(id);
        if (pc.getStatus() == ComputerStatus.OCCUPIED) {
            throw new ConflictException("Невозможно удалить компьютер " + id + ": за ним сейчас играет гость");
        }
        repository.deleteComputer(id);
    }

    // --- Доменные операции: занять и освободить компьютер, техобслуживание ---

    public synchronized Computer occupyComputer(Long computerId, Long playerId) {
        if (computerId == null || playerId == null) {
            throw new ValidationException("Параметры computerId и playerId обязательны");
        }

        Computer computer = getComputer(computerId);
        Player player = getPlayer(playerId);

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
        repository.saveComputer(computer);

        player.addOccupiedComputer(computerId);
        repository.savePlayer(player);

        return computer;
    }

    public synchronized Computer releaseComputer(Long computerId) {
        if (computerId == null) {
            throw new ValidationException("computerId обязателен");
        }

        Computer computer = getComputer(computerId);
        if (computer.getStatus() != ComputerStatus.OCCUPIED) {
            throw new ConflictException("Компьютер '" + computer.getName() + "' не занят");
        }

        Long playerId = computer.getOccupiedByPlayerId();
        if (playerId != null) {
            Player player = repository.findPlayerById(playerId).orElse(null);
            if (player != null) {
                player.removeOccupiedComputer(computerId);
                repository.savePlayer(player);
            }
        }

        computer.setStatus(ComputerStatus.AVAILABLE);
        computer.setOccupiedByPlayerId(null);
        return repository.saveComputer(computer);
    }

    public synchronized Computer sendToMaintenance(Long id) {
        Computer pc = getComputer(id);
        if (pc.getStatus() == ComputerStatus.OCCUPIED) {
            throw new ConflictException("Нельзя отправить компьютер на обслуживание: за ним идет игра");
        }
        if (pc.getStatus() == ComputerStatus.MAINTENANCE) {
            throw new ConflictException("Компьютер уже находится на обслуживании");
        }
        pc.setStatus(ComputerStatus.MAINTENANCE);
        return repository.saveComputer(pc);
    }

    public synchronized Computer completeMaintenance(Long id) {
        Computer pc = getComputer(id);
        if (pc.getStatus() != ComputerStatus.MAINTENANCE) {
            throw new ConflictException("Компьютер не находится на обслуживании");
        }
        pc.setStatus(ComputerStatus.AVAILABLE);
        return repository.saveComputer(pc);
    }

    // --- Управление игроками ---

    public synchronized Player createPlayer(String nickname, String phone, PlayerType type) {
        validatePlayer(nickname, phone, type);
        if (repository.findPlayerByPhone(phone).isPresent()) {
            throw new ConflictException("Игрок с номером телефона '" + phone.trim() + "' уже зарегистрирован в клубе");
        }
        return repository.savePlayer(new Player(null, nickname.trim(), phone.trim(), type));
    }

    public synchronized Player getPlayer(Long id) {
        if (id == null) {
            throw new ValidationException("Идентификатор игрока обязателен");
        }
        return repository.findPlayerById(id)
                .orElseThrow(() -> new NotFoundException("Игрок", id));
    }

    public synchronized List<Player> getPlayers(PlayerType type, Boolean hasActiveComputers, String search) {
        return repository.findAllPlayers().stream()
                .filter(p -> type == null || p.getType() == type)
                .filter(p -> hasActiveComputers == null || p.hasActiveComputers() == hasActiveComputers)
                .filter(p -> search == null || searchMatches(p, search))
                .toList();
    }

    public synchronized Player updatePlayer(Long id, String nickname, String phone, PlayerType type) {
        Player player = getPlayer(id);
        validatePlayer(nickname, phone, type);

        if (!player.getPhone().equalsIgnoreCase(phone.trim()) && repository.findPlayerByPhone(phone).isPresent()) {
            throw new ConflictException("Игрок с телефоном '" + phone.trim() + "' уже зарегистрирован");
        }

        // Проверка при понижении статуса с VIP до REGULAR
        if (player.getType() == PlayerType.VIP && type == PlayerType.REGULAR) {
            if (player.getOccupiedComputerIds().size() > 1) {
                throw new ConflictException("Нельзя перевести игрока в REGULAR: у него занято "
                        + player.getOccupiedComputerIds().size() + " компьютеров (максимум для REGULAR — 1)");
            }
            for (Long pcId : player.getOccupiedComputerIds()) {
                Computer pc = repository.findComputerById(pcId).orElse(null);
                if (pc != null && pc.getType() == ComputerType.VIP) {
                    throw new ConflictException("Нельзя перевести игрока в REGULAR: он сейчас занимает VIP-компьютер");
                }
            }
        }

        player.setNickname(nickname.trim());
        player.setPhone(phone.trim());
        player.setType(type);
        return repository.savePlayer(player);
    }

    public synchronized void deletePlayer(Long id) {
        Player player = getPlayer(id);
        if (player.hasActiveComputers()) {
            throw new ConflictException("Нельзя удалить профиль игрока " + id + ": он сейчас занимает компьютеры");
        }
        repository.deletePlayer(id);
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
