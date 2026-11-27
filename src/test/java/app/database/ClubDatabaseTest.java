package app.database;

import app.domain.Exceptions.ConflictException;
import app.domain.Exceptions.NotFoundException;
import app.domain.Model.Computer;
import app.domain.Model.ComputerStatus;
import app.domain.Model.ComputerType;
import app.domain.Model.Player;
import app.domain.Model.PlayerType;
import app.dto.ComputerResponse;
import app.dto.PlayerResponse;
import app.repository.ComputerRepository;
import app.repository.PlayerRepository;
import app.service.ClubService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("Интеграционные тесты базы данных, связей, транзакций и отсутствия N+1 (Spring Data JPA + Flyway)")
class ClubDatabaseTest {

    @Autowired
    private ClubService clubService;

    @Autowired
    private ComputerRepository computerRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private SessionFactory sessionFactory;
    private Statistics statistics;

    @BeforeEach
    void cleanUp() {
        sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        // Очищаем зависимые ПК, затем игроков
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.execute(status -> {
            for (Computer c : computerRepository.findAll()) {
                c.setOccupiedBy(null);
                computerRepository.save(c);
            }
            computerRepository.clear();
            playerRepository.clear();
            return null;
        });
    }

    @Test
    @DisplayName("1. Сохранение и извлечение связанных сущностей (Player <-> Computer) через базу данных")
    void testSaveAndRetrieveRelatedEntities() {
        PlayerResponse player = clubService.createPlayer("RelationalGamer", "+79991234567", PlayerType.VIP);
        ComputerResponse pc = clubService.createComputer("DB-PC-01", ComputerType.VIP, 200.0, "192.168.1.50", "RTX 4090");

        // Связываем сущности через операцию occupy
        ComputerResponse occupied = clubService.occupyComputer(pc.id(), player.id());
        assertEquals(ComputerStatus.OCCUPIED, occupied.status());
        assertEquals(player.id(), occupied.occupiedByPlayerId());

        // Извлекаем игрока и проверяем вложенную информацию о связанном компьютере
        PlayerResponse retrievedPlayer = clubService.getPlayer(player.id());
        assertEquals(1, retrievedPlayer.occupiedComputers().size());
        assertEquals("DB-PC-01", retrievedPlayer.occupiedComputers().get(0).name());
        assertEquals("192.168.1.50", retrievedPlayer.occupiedComputers().get(0).ipAddress());
        assertEquals("RTX 4090", retrievedPlayer.occupiedComputers().get(0).specs());

        // Извлекаем компьютер и проверяем связанного игрока
        ComputerResponse retrievedPc = clubService.getComputer(pc.id());
        assertNotNull(retrievedPc.occupiedByPlayer());
        assertEquals("RelationalGamer", retrievedPc.occupiedByPlayer().nickname());
    }

    @Test
    @DisplayName("2. Проверка ссылочной целостности: запрет ссылок на несуществующие ресурсы")
    void testReferentialIntegrity_nonExistentResources() {
        ComputerResponse pc = clubService.createComputer("INTEGRITY-PC", ComputerType.STANDARD, 100.0);

        // Попытка связать с несуществующим игроком
        assertThrows(NotFoundException.class, () -> clubService.occupyComputer(pc.id(), 999999L));

        // Попытка занять несуществующий компьютер
        PlayerResponse player = clubService.createPlayer("Ghost", "+79990000000", PlayerType.REGULAR);
        assertThrows(NotFoundException.class, () -> clubService.occupyComputer(999999L, player.id()));
    }

    @Test
    @DisplayName("3. Целостность данных при удалении: запрет удаления игрока с активными компьютерами")
    void testIntegrityOnDelete_cannotDeleteOccupiedPlayerOrComputer() {
        PlayerResponse player = clubService.createPlayer("ActivePlayer", "+79998881122", PlayerType.REGULAR);
        ComputerResponse pc = clubService.createComputer("ACTIVE-PC", ComputerType.STANDARD, 100.0);
        clubService.occupyComputer(pc.id(), player.id());

        // Удаление занятого ПК должно быть отклонено
        assertThrows(ConflictException.class, () -> clubService.deleteComputer(pc.id()));

        // Удаление игрока с активным ПК должно быть отклонено
        assertThrows(ConflictException.class, () -> clubService.deletePlayer(player.id()));

        // Данные не повреждены
        assertEquals(ComputerStatus.OCCUPIED, clubService.getComputer(pc.id()).status());
        assertNotNull(clubService.getPlayer(player.id()));
    }

    @Test
    @DisplayName("4. Атомарность и полный откат транзакции при ошибке (All-or-Nothing)")
    void testTransactionalRollback_whenErrorOccurs_noChangesPersisted() {
        ComputerResponse fromPc = clubService.createComputer("TX-FROM", ComputerType.STANDARD, 100.0);
        ComputerResponse toPc = clubService.createComputer("TX-TO-VIP", ComputerType.VIP, 300.0);
        PlayerResponse regular = clubService.createPlayer("RegularBob", "+79993334455", PlayerType.REGULAR);

        clubService.occupyComputer(fromPc.id(), regular.id());
        assertEquals(ComputerStatus.OCCUPIED, clubService.getComputer(fromPc.id()).status());

        // REGULAR игрок пытается пересесть на VIP компьютер — ошибка прав доступа
        assertThrows(ConflictException.class, () ->
                clubService.transferComputer(fromPc.id(), toPc.id(), regular.id())
        );

        // Проверяем состояние в БД: транзакция откатилась, fromPc остался занят, toPc остался свободен
        ComputerResponse fromAfter = clubService.getComputer(fromPc.id());
        ComputerResponse toAfter = clubService.getComputer(toPc.id());

        assertEquals(ComputerStatus.OCCUPIED, fromAfter.status(), "Исходный ПК должен остаться OCCUPIED после отката");
        assertEquals(regular.id(), fromAfter.occupiedByPlayerId());
        assertEquals(ComputerStatus.AVAILABLE, toAfter.status(), "Целевой ПК должен остаться AVAILABLE");
        assertNull(toAfter.occupiedByPlayerId());
    }

    @Test
    @DisplayName("5. Отсутствие проблемы N+1 при выборке списка из нескольких десятков объектов (Requirement 5)")
    void testFindAllWithComputers_noNPlusOneQueriesOnDozensOfEntities() {
        // Создаем 50 игроков и 50 компьютеров, связываем их
        final int COUNT = 50;
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.execute(status -> {
            for (int i = 1; i <= COUNT; i++) {
                Player p = new Player(null, "Player_" + i, "+7999000" + String.format("%04d", i), PlayerType.REGULAR);
                playerRepository.save(p);

                Computer c = new Computer(null, "PC_" + i, ComputerType.STANDARD, 100.0, "192.168.1." + i, "Standard Spec");
                c.setStatus(ComputerStatus.OCCUPIED);
                c.setOccupiedBy(p);
                computerRepository.save(c);

                p.addOccupiedComputer(c);
                playerRepository.save(p);
            }
            return null;
        });

        // Сбрасываем статистику запросов
        statistics.clear();

        // Запрашиваем всех 50 игроков со связанными объектами
        List<PlayerResponse> players = clubService.getPlayers(null, null, null);
        assertEquals(COUNT, players.size(), "Должно быть возвращено 50 игроков");

        // Убеждаемся, что связанные данные загружены для каждого
        for (PlayerResponse p : players) {
            assertEquals(1, p.occupiedComputers().size());
            assertNotNull(p.occupiedComputers().get(0).name());
        }

        // Проверяем количество выполненных SQL-запросов
        long statementCount = statistics.getPrepareStatementCount();
        System.out.println("Выполнено SQL-запросов для " + COUNT + " связанных объектов: " + statementCount);

        // При проблеме N+1 было бы 51 запрос (1 + 50). С LEFT JOIN FETCH выполняется ровно 1 запрос.
        assertTrue(statementCount <= 1,
                "Запрос списка из " + COUNT + " связанных объектов не должен выполнять N запросов! Выполнено: " + statementCount);
    }
}
