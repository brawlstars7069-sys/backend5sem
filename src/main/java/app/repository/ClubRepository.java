package app.repository;

import app.domain.Model.Computer;
import app.domain.Model.Player;

import java.util.List;
import java.util.Optional;

public interface ClubRepository {
    // Компьютеры
    Computer saveComputer(Computer pc);
    Optional<Computer> findComputerById(Long id);
    List<Computer> findAllComputers();
    Optional<Computer> findComputerByName(String name);
    boolean deleteComputer(Long id);

    // Игроки
    Player savePlayer(Player player);
    Optional<Player> findPlayerById(Long id);
    List<Player> findAllPlayers();
    Optional<Player> findPlayerByPhone(String phone);
    boolean deletePlayer(Long id);

    // Очистка данных
    void clear();
}
