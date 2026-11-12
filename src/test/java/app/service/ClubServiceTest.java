package app.service;

import app.domain.Exceptions.ConflictException;
import app.domain.Exceptions.NotFoundException;
import app.domain.Exceptions.ValidationException;
import app.domain.Model.Computer;
import app.domain.Model.ComputerStatus;
import app.domain.Model.ComputerType;
import app.domain.Model.Player;
import app.domain.Model.PlayerType;
import app.repository.InMemoryClubRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Юнит-тесты доменной логики компьютерного клуба Zombie Playa (без запуска Spring-контекста)")
class ClubServiceTest {

    private InMemoryClubRepository repository;
    private ClubService service;

    @BeforeEach
    void setUp() {
        // Прямая инициализация без поднятия Spring ApplicationContext
        repository = new InMemoryClubRepository();
        service = new ClubService(repository);
    }

    @Nested
    @DisplayName("1. Успешные сценарии (Happy Path)")
    class SuccessfulScenarios {

        @Test
        @DisplayName("Создание и получение компьютера")
        void createAndGetComputer_shouldSucceed() {
            Computer created = service.createComputer("PC-01", ComputerType.STANDARD, 120.0);
            assertNotNull(created.getId());
            assertEquals("PC-01", created.getName());
            assertEquals(ComputerType.STANDARD, created.getType());
            assertEquals(120.0, created.getHourlyRate());
            assertEquals(ComputerStatus.AVAILABLE, created.getStatus());
            assertNull(created.getOccupiedByPlayerId());

            Computer fetched = service.getComputer(created.getId());
            assertEquals(created.getId(), fetched.getId());
            assertEquals("PC-01", fetched.getName());
        }

        @Test
        @DisplayName("Создание VIP компьютера")
        void createVipComputer_shouldSucceed() {
            Computer vip = service.createComputer("VIP-01", ComputerType.VIP, 300.0);
            assertEquals(ComputerType.VIP, vip.getType());
            assertEquals(300.0, vip.getHourlyRate());
        }

        @Test
        @DisplayName("Фильтрация компьютеров по типу, статусу и тарифу")
        void filterComputers_shouldReturnMatching() {
            service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            service.createComputer("PC-02", ComputerType.STANDARD, 150.0);
            Computer vip = service.createComputer("VIP-01", ComputerType.VIP, 300.0);
            service.sendToMaintenance(vip.getId());

            // Фильтр по типу
            List<Computer> standards = service.getComputers(ComputerType.STANDARD, null, null);
            assertEquals(2, standards.size());

            // Фильтр по статусу
            List<Computer> maintenance = service.getComputers(null, ComputerStatus.MAINTENANCE, null);
            assertEquals(1, maintenance.size());
            assertEquals("VIP-01", maintenance.get(0).getName());

            // Фильтр по максимальному тарифу
            List<Computer> affordable = service.getComputers(null, null, 120.0);
            assertEquals(1, affordable.size());
            assertEquals("PC-01", affordable.get(0).getName());
        }

        @Test
        @DisplayName("Обновление параметров компьютера")
        void updateComputer_shouldUpdateFields() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            Computer updated = service.updateComputer(pc.getId(), "PC-01-UPGRADED", ComputerType.VIP, 200.0);

            assertEquals("PC-01-UPGRADED", updated.getName());
            assertEquals(ComputerType.VIP, updated.getType());
            assertEquals(200.0, updated.getHourlyRate());
        }

        @Test
        @DisplayName("Удаление свободного компьютера")
        void deleteComputer_whenAvailable_shouldSucceed() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            service.deleteComputer(pc.getId());

            assertThrows(NotFoundException.class, () -> service.getComputer(pc.getId()));
        }

        @Test
        @DisplayName("Создание, получение и фильтрация игроков")
        void createAndFilterPlayers_shouldSucceed() {
            Player p1 = service.createPlayer("ZombieKing", "+79991112233", PlayerType.REGULAR);
            Player p2 = service.createPlayer("GamerVIP", "+79994445566", PlayerType.VIP);

            assertNotNull(p1.getId());
            assertNotNull(p2.getId());

            // Фильтр по типу
            List<Player> vips = service.getPlayers(PlayerType.VIP, null, null);
            assertEquals(1, vips.size());
            assertEquals("GamerVIP", vips.get(0).getNickname());

            // Фильтр по поисковому запросу (никнейм и телефон)
            List<Player> byNick = service.getPlayers(null, null, "Zombie");
            assertEquals(1, byNick.size());
            assertEquals("ZombieKing", byNick.get(0).getNickname());

            List<Player> byPhone = service.getPlayers(null, null, "44455");
            assertEquals(1, byPhone.size());
            assertEquals("GamerVIP", byPhone.get(0).getNickname());
        }

        @Test
        @DisplayName("Обновление данных игрока")
        void updatePlayer_shouldUpdateFields() {
            Player p = service.createPlayer("OldNick", "+79990001122", PlayerType.REGULAR);
            Player updated = service.updatePlayer(p.getId(), "NewNick", "+79990001133", PlayerType.VIP);

            assertEquals("NewNick", updated.getNickname());
            assertEquals("+79990001133", updated.getPhone());
            assertEquals(PlayerType.VIP, updated.getType());
        }

        @Test
        @DisplayName("Удаление игрока без активных компьютеров")
        void deletePlayer_whenNoActiveComputers_shouldSucceed() {
            Player p = service.createPlayer("Ghost", "+79990001122", PlayerType.REGULAR);
            service.deletePlayer(p.getId());

            assertThrows(NotFoundException.class, () -> service.getPlayer(p.getId()));
        }

        @Test
        @DisplayName("REGULAR игрок успешно занимает STANDARD компьютер")
        void occupyComputer_regularPlayer_shouldOccupyStandardPc() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            Player player = service.createPlayer("RegularGamer", "+79991112233", PlayerType.REGULAR);

            Computer occupied = service.occupyComputer(pc.getId(), player.getId());

            assertEquals(ComputerStatus.OCCUPIED, occupied.getStatus());
            assertEquals(player.getId(), occupied.getOccupiedByPlayerId());

            Player updatedPlayer = service.getPlayer(player.getId());
            assertTrue(updatedPlayer.hasActiveComputers());
            assertTrue(updatedPlayer.getOccupiedComputerIds().contains(pc.getId()));
        }

        @Test
        @DisplayName("Освобождение компьютера успешно снимает занятость")
        void releaseComputer_shouldFreeComputerAndPlayer() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            Player player = service.createPlayer("RegularGamer", "+79991112233", PlayerType.REGULAR);
            service.occupyComputer(pc.getId(), player.getId());

            Computer released = service.releaseComputer(pc.getId());

            assertEquals(ComputerStatus.AVAILABLE, released.getStatus());
            assertNull(released.getOccupiedByPlayerId());

            Player updatedPlayer = service.getPlayer(player.getId());
            assertFalse(updatedPlayer.hasActiveComputers());
            assertEquals(0, updatedPlayer.getOccupiedComputerIds().size());
        }

        @Test
        @DisplayName("Жизненный цикл техобслуживания: отправка и завершение")
        void maintenanceLifecycle_shouldTransitionCorrectly() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);

            Computer onMaint = service.sendToMaintenance(pc.getId());
            assertEquals(ComputerStatus.MAINTENANCE, onMaint.getStatus());

            Computer completed = service.completeMaintenance(pc.getId());
            assertEquals(ComputerStatus.AVAILABLE, completed.getStatus());
        }

        @Test
        @DisplayName("VIP игрок может занять до 2-х компьютеров, включая VIP зону")
        void vipPlayer_canOccupyUpToTwoComputers() {
            Computer pc1 = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            Computer vipPc = service.createComputer("VIP-01", ComputerType.VIP, 250.0);
            Player vip = service.createPlayer("ProVip", "+79997778899", PlayerType.VIP);

            service.occupyComputer(pc1.getId(), vip.getId());
            service.occupyComputer(vipPc.getId(), vip.getId());

            Player updatedVip = service.getPlayer(vip.getId());
            assertEquals(2, updatedVip.getOccupiedComputerIds().size());
            assertTrue(updatedVip.getOccupiedComputerIds().contains(pc1.getId()));
            assertTrue(updatedVip.getOccupiedComputerIds().contains(vipPc.getId()));
        }

        @Test
        @DisplayName("Проверка доменных свойств и скидок типов игроков")
        void playerTypeProperties_shouldMatchRequirements() {
            assertEquals(1, PlayerType.REGULAR.getMaxComputers());
            assertEquals(0.0, PlayerType.REGULAR.getDiscountPercent());
            assertTrue(PlayerType.REGULAR.canAccess(ComputerType.STANDARD));
            assertFalse(PlayerType.REGULAR.canAccess(ComputerType.VIP));

            assertEquals(2, PlayerType.VIP.getMaxComputers());
            assertEquals(20.0, PlayerType.VIP.getDiscountPercent());
            assertTrue(PlayerType.VIP.canAccess(ComputerType.STANDARD));
            assertTrue(PlayerType.VIP.canAccess(ComputerType.VIP));
        }
    }

    @Nested
    @DisplayName("2. Ошибки и конфликты (Error Scenarios)")
    class ErrorScenarios {

        @Test
        @DisplayName("Получение несуществующего компьютера выбрасывает NotFoundException")
        void getComputer_notFound_shouldThrowNotFoundException() {
            NotFoundException ex = assertThrows(NotFoundException.class, () -> service.getComputer(999L));
            assertTrue(ex.getMessage().contains("Компьютер с ID 999 не найден"));
        }

        @Test
        @DisplayName("Получение несуществующего игрока выбрасывает NotFoundException")
        void getPlayer_notFound_shouldThrowNotFoundException() {
            NotFoundException ex = assertThrows(NotFoundException.class, () -> service.getPlayer(999L));
            assertTrue(ex.getMessage().contains("Игрок с ID 999 не найден"));
        }

        @Test
        @DisplayName("Создание компьютера с дублирующимся именем выбрасывает ConflictException")
        void createComputer_duplicateName_shouldThrowConflictException() {
            service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.createComputer("PC-01", ComputerType.VIP, 200.0));
            assertTrue(ex.getMessage().contains("уже существует"));
        }

        @Test
        @DisplayName("Создание игрока с дублирующимся телефоном выбрасывает ConflictException")
        void createPlayer_duplicatePhone_shouldThrowConflictException() {
            service.createPlayer("Gamer1", "+79991112233", PlayerType.REGULAR);
            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.createPlayer("Gamer2", "+79991112233", PlayerType.VIP));
            assertTrue(ex.getMessage().contains("уже зарегистрирован"));
        }

        @Test
        @DisplayName("Попытка занять уже занятый компьютер выбрасывает ConflictException")
        void occupyComputer_alreadyOccupied_shouldThrowConflictException() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            Player p1 = service.createPlayer("Gamer1", "+79991112233", PlayerType.REGULAR);
            Player p2 = service.createPlayer("Gamer2", "+79992223344", PlayerType.REGULAR);

            service.occupyComputer(pc.getId(), p1.getId());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.occupyComputer(pc.getId(), p2.getId()));
            assertTrue(ex.getMessage().contains("уже занят"));
        }

        @Test
        @DisplayName("Попытка занять компьютер на ТО выбрасывает ConflictException")
        void occupyComputer_underMaintenance_shouldThrowConflictException() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            service.sendToMaintenance(pc.getId());
            Player p = service.createPlayer("Gamer1", "+79991112233", PlayerType.REGULAR);

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.occupyComputer(pc.getId(), p.getId()));
            assertTrue(ex.getMessage().contains("техобслуживании"));
        }

        @Test
        @DisplayName("REGULAR игрок пытается занять VIP компьютер выбрасывает ConflictException")
        void occupyComputer_regularAccessingVip_shouldThrowConflictException() {
            Computer vipPc = service.createComputer("VIP-01", ComputerType.VIP, 250.0);
            Player regular = service.createPlayer("RegularGamer", "+79991112233", PlayerType.REGULAR);

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.occupyComputer(vipPc.getId(), regular.getId()));
            assertTrue(ex.getMessage().contains("не имеет доступа к компьютерам категории VIP"));
        }

        @Test
        @DisplayName("REGULAR игрок превышает лимит (максимум 1) выбрасывает ConflictException")
        void occupyComputer_regularExceedingLimit_shouldThrowConflictException() {
            Computer pc1 = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            Computer pc2 = service.createComputer("PC-02", ComputerType.STANDARD, 100.0);
            Player regular = service.createPlayer("RegularGamer", "+79991112233", PlayerType.REGULAR);

            service.occupyComputer(pc1.getId(), regular.getId());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.occupyComputer(pc2.getId(), regular.getId()));
            assertTrue(ex.getMessage().contains("исчерпал лимит мест"));
        }

        @Test
        @DisplayName("VIP игрок превышает лимит (максимум 2) выбрасывает ConflictException")
        void occupyComputer_vipExceedingLimit_shouldThrowConflictException() {
            Computer pc1 = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            Computer pc2 = service.createComputer("PC-02", ComputerType.STANDARD, 100.0);
            Computer pc3 = service.createComputer("PC-03", ComputerType.STANDARD, 100.0);
            Player vip = service.createPlayer("VipGamer", "+79991112233", PlayerType.VIP);

            service.occupyComputer(pc1.getId(), vip.getId());
            service.occupyComputer(pc2.getId(), vip.getId());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.occupyComputer(pc3.getId(), vip.getId()));
            assertTrue(ex.getMessage().contains("исчерпал лимит мест (максимум 2)"));
        }

        @Test
        @DisplayName("Освобождение незанятого компьютера выбрасывает ConflictException")
        void releaseComputer_notOccupied_shouldThrowConflictException() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.releaseComputer(pc.getId()));
            assertTrue(ex.getMessage().contains("не занят"));
        }

        @Test
        @DisplayName("Удаление занятого компьютера выбрасывает ConflictException")
        void deleteComputer_occupied_shouldThrowConflictException() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            Player player = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);
            service.occupyComputer(pc.getId(), player.getId());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.deleteComputer(pc.getId()));
            assertTrue(ex.getMessage().contains("Невозможно удалить компьютер"));
        }

        @Test
        @DisplayName("Удаление игрока с активными компьютерами выбрасывает ConflictException")
        void deletePlayer_withActiveComputers_shouldThrowConflictException() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            Player player = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);
            service.occupyComputer(pc.getId(), player.getId());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.deletePlayer(player.getId()));
            assertTrue(ex.getMessage().contains("Нельзя удалить профиль игрока"));
        }

        @Test
        @DisplayName("Отправка занятого компьютера на ТО выбрасывает ConflictException")
        void sendToMaintenance_whenOccupied_shouldThrowConflictException() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            Player player = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);
            service.occupyComputer(pc.getId(), player.getId());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.sendToMaintenance(pc.getId()));
            assertTrue(ex.getMessage().contains("Нельзя отправить компьютер на обслуживание"));
        }

        @Test
        @DisplayName("Повторная отправка на ТО выбрасывает ConflictException")
        void sendToMaintenance_alreadyUnderMaintenance_shouldThrowConflictException() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            service.sendToMaintenance(pc.getId());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.sendToMaintenance(pc.getId()));
            assertTrue(ex.getMessage().contains("уже находится на обслуживании"));
        }

        @Test
        @DisplayName("Завершение обслуживания компьютера, который не на ТО, выбрасывает ConflictException")
        void completeMaintenance_notUnderMaintenance_shouldThrowConflictException() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.completeMaintenance(pc.getId()));
            assertTrue(ex.getMessage().contains("не находится на обслуживании"));
        }

        @Test
        @DisplayName("Изменение типа компьютера пока он занят выбрасывает ConflictException")
        void updateComputer_changeTypeWhileOccupied_shouldThrowConflictException() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            Player player = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);
            service.occupyComputer(pc.getId(), player.getId());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.updateComputer(pc.getId(), "PC-01", ComputerType.VIP, 200.0));
            assertTrue(ex.getMessage().contains("Нельзя изменить категорию"));
        }

        @Test
        @DisplayName("Понижение VIP до REGULAR при наличии 2-х компьютеров выбрасывает ConflictException")
        void downgradeVipToRegular_withMultipleComputers_shouldThrowConflictException() {
            Computer pc1 = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            Computer pc2 = service.createComputer("PC-02", ComputerType.STANDARD, 100.0);
            Player vip = service.createPlayer("VipGamer", "+79991112233", PlayerType.VIP);

            service.occupyComputer(pc1.getId(), vip.getId());
            service.occupyComputer(pc2.getId(), vip.getId());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.updatePlayer(vip.getId(), "VipGamer", "+79991112233", PlayerType.REGULAR));
            assertTrue(ex.getMessage().contains("Нельзя перевести игрока в REGULAR: у него занято 2 компьютеров"));
        }

        @Test
        @DisplayName("Понижение VIP до REGULAR при занятом VIP месте выбрасывает ConflictException")
        void downgradeVipToRegular_withVipComputer_shouldThrowConflictException() {
            Computer vipPc = service.createComputer("VIP-01", ComputerType.VIP, 250.0);
            Player vip = service.createPlayer("VipGamer", "+79991112233", PlayerType.VIP);

            service.occupyComputer(vipPc.getId(), vip.getId());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.updatePlayer(vip.getId(), "VipGamer", "+79991112233", PlayerType.REGULAR));
            assertTrue(ex.getMessage().contains("он сейчас занимает VIP-компьютер"));
        }
    }

    @Nested
    @DisplayName("3. Граничные случаи и валидация (Boundary Conditions)")
    class BoundaryConditions {

        @Test
        @DisplayName("Пустое или null имя компьютера выбрасывает ValidationException")
        void createComputer_blankOrNullName_shouldThrowValidationException() {
            assertThrows(ValidationException.class,
                    () -> service.createComputer(null, ComputerType.STANDARD, 100.0));
            assertThrows(ValidationException.class,
                    () -> service.createComputer("   ", ComputerType.STANDARD, 100.0));
        }

        @Test
        @DisplayName("Тариф <= 0 выбрасывает ValidationException")
        void createComputer_zeroOrNegativeRate_shouldThrowValidationException() {
            assertThrows(ValidationException.class,
                    () -> service.createComputer("PC-01", ComputerType.STANDARD, 0.0));
            assertThrows(ValidationException.class,
                    () -> service.createComputer("PC-01", ComputerType.STANDARD, -50.0));
            assertThrows(ValidationException.class,
                    () -> service.createComputer("PC-01", ComputerType.STANDARD, null));
        }

        @Test
        @DisplayName("Null тип компьютера выбрасывает ValidationException")
        void createComputer_nullType_shouldThrowValidationException() {
            assertThrows(ValidationException.class,
                    () -> service.createComputer("PC-01", null, 100.0));
        }

        @Test
        @DisplayName("Пустой или null никнейм игрока выбрасывает ValidationException")
        void createPlayer_blankOrNullNickname_shouldThrowValidationException() {
            assertThrows(ValidationException.class,
                    () -> service.createPlayer(null, "+79991112233", PlayerType.REGULAR));
            assertThrows(ValidationException.class,
                    () -> service.createPlayer("   ", "+79991112233", PlayerType.REGULAR));
        }

        @Test
        @DisplayName("Пустой или null телефон игрока выбрасывает ValidationException")
        void createPlayer_blankOrNullPhone_shouldThrowValidationException() {
            assertThrows(ValidationException.class,
                    () -> service.createPlayer("Gamer", null, PlayerType.REGULAR));
            assertThrows(ValidationException.class,
                    () -> service.createPlayer("Gamer", "   ", PlayerType.REGULAR));
        }

        @Test
        @DisplayName("Null тип игрока выбрасывает ValidationException")
        void createPlayer_nullType_shouldThrowValidationException() {
            assertThrows(ValidationException.class,
                    () -> service.createPlayer("Gamer", "+79991112233", null));
        }

        @Test
        @DisplayName("Null ID при занятии или освобождении выбрасывает ValidationException")
        void occupyAndRelease_nullIds_shouldThrowValidationException() {
            assertThrows(ValidationException.class, () -> service.occupyComputer(null, 1L));
            assertThrows(ValidationException.class, () -> service.occupyComputer(1L, null));
            assertThrows(ValidationException.class, () -> service.releaseComputer(null));
            assertThrows(ValidationException.class, () -> service.getComputer(null));
            assertThrows(ValidationException.class, () -> service.getPlayer(null));
        }

        @Test
        @DisplayName("Обновление компьютера с сохранением своего имени не считается дубликатом")
        void updateComputer_withSameName_shouldSucceed() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            Computer updated = service.updateComputer(pc.getId(), "PC-01", ComputerType.STANDARD, 120.0);
            assertEquals(120.0, updated.getHourlyRate());
        }

        @Test
        @DisplayName("Обновление игрока с сохранением своего телефона не считается дубликатом")
        void updatePlayer_withSamePhone_shouldSucceed() {
            Player p = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);
            Player updated = service.updatePlayer(p.getId(), "NewGamer", "+79991112233", PlayerType.REGULAR);
            assertEquals("NewGamer", updated.getNickname());
        }
    }

    @Nested
    @DisplayName("4. Изменения состояния ресурсов (Resource State Transitions)")
    class StateTransitions {

        @Test
        @DisplayName("Полный цикл переходов состояния: AVAILABLE -> OCCUPIED -> AVAILABLE -> MAINTENANCE -> AVAILABLE")
        void fullStateCycle_shouldSucceed() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            Player player = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);

            assertEquals(ComputerStatus.AVAILABLE, pc.getStatus());

            // 1. Занятие: AVAILABLE -> OCCUPIED
            service.occupyComputer(pc.getId(), player.getId());
            Computer occupied = service.getComputer(pc.getId());
            assertEquals(ComputerStatus.OCCUPIED, occupied.getStatus());
            assertEquals(player.getId(), occupied.getOccupiedByPlayerId());

            // 2. Освобождение: OCCUPIED -> AVAILABLE
            service.releaseComputer(pc.getId());
            Computer released = service.getComputer(pc.getId());
            assertEquals(ComputerStatus.AVAILABLE, released.getStatus());
            assertNull(released.getOccupiedByPlayerId());

            // 3. Отправка на ТО: AVAILABLE -> MAINTENANCE
            service.sendToMaintenance(pc.getId());
            Computer onMaint = service.getComputer(pc.getId());
            assertEquals(ComputerStatus.MAINTENANCE, onMaint.getStatus());

            // 4. Завершение ТО: MAINTENANCE -> AVAILABLE
            service.completeMaintenance(pc.getId());
            Computer availableAgain = service.getComputer(pc.getId());
            assertEquals(ComputerStatus.AVAILABLE, availableAgain.getStatus());
        }

        @Test
        @DisplayName("Недопустимые переходы состояния блокируются")
        void invalidStateTransitions_shouldBeRejected() {
            Computer pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            Player player = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);

            // AVAILABLE -> completeMaintenance (нельзя)
            assertThrows(ConflictException.class, () -> service.completeMaintenance(pc.getId()));

            // AVAILABLE -> release (нельзя)
            assertThrows(ConflictException.class, () -> service.releaseComputer(pc.getId()));

            // Занимаем
            service.occupyComputer(pc.getId(), player.getId());

            // OCCUPIED -> sendToMaintenance (нельзя)
            assertThrows(ConflictException.class, () -> service.sendToMaintenance(pc.getId()));

            // OCCUPIED -> delete (нельзя)
            assertThrows(ConflictException.class, () -> service.deleteComputer(pc.getId()));

            // Освобождаем и отправляем на ТО
            service.releaseComputer(pc.getId());
            service.sendToMaintenance(pc.getId());

            // MAINTENANCE -> occupy (нельзя)
            assertThrows(ConflictException.class, () -> service.occupyComputer(pc.getId(), player.getId()));

            // MAINTENANCE -> sendToMaintenance (нельзя)
            assertThrows(ConflictException.class, () -> service.sendToMaintenance(pc.getId()));

            // MAINTENANCE -> release (нельзя)
            assertThrows(ConflictException.class, () -> service.releaseComputer(pc.getId()));
        }
    }
}
