package app.service;

import app.domain.Exceptions.ConflictException;
import app.domain.Exceptions.NotFoundException;
import app.domain.Exceptions.ValidationException;
import app.domain.Model.ComputerStatus;
import app.domain.Model.ComputerType;
import app.domain.Model.PlayerType;
import app.dto.ComputerResponse;
import app.dto.PlayerResponse;
import app.repository.InMemoryComputerRepository;
import app.repository.InMemoryPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Юнит-тесты доменной логики компьютерного клуба Zombie Playa (без запуска Spring-контекста)")
class ClubServiceTest {

    private InMemoryComputerRepository computerRepository;
    private InMemoryPlayerRepository playerRepository;
    private ClubService service;

    @BeforeEach
    void setUp() {
        // Прямая инициализация без поднятия Spring ApplicationContext
        computerRepository = new InMemoryComputerRepository();
        playerRepository = new InMemoryPlayerRepository();
        service = new ClubService(computerRepository, playerRepository);
    }

    @Nested
    @DisplayName("1. Успешные сценарии (Happy Path)")
    class SuccessfulScenarios {

        @Test
        @DisplayName("Создание и получение компьютера")
        void createAndGetComputer_shouldSucceed() {
            ComputerResponse created = service.createComputer("PC-01", ComputerType.STANDARD, 120.0);
            assertNotNull(created.id());
            assertEquals("PC-01", created.name());
            assertEquals(ComputerType.STANDARD, created.type());
            assertEquals(120.0, created.hourlyRate());
            assertEquals(ComputerStatus.AVAILABLE, created.status());
            assertNull(created.occupiedByPlayerId());

            ComputerResponse fetched = service.getComputer(created.id());
            assertEquals(created.id(), fetched.id());
            assertEquals("PC-01", fetched.name());
        }

        @Test
        @DisplayName("Создание VIP компьютера")
        void createVipComputer_shouldSucceed() {
            ComputerResponse vip = service.createComputer("VIP-01", ComputerType.VIP, 300.0);
            assertEquals(ComputerType.VIP, vip.type());
            assertEquals(300.0, vip.hourlyRate());
        }

        @Test
        @DisplayName("Фильтрация компьютеров по типу, статусу и тарифу")
        void filterComputers_shouldReturnMatching() {
            service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            service.createComputer("PC-02", ComputerType.STANDARD, 150.0);
            ComputerResponse vip = service.createComputer("VIP-01", ComputerType.VIP, 300.0);
            service.sendToMaintenance(vip.id());

            // Фильтр по типу
            List<ComputerResponse> standards = service.getComputers(ComputerType.STANDARD, null, null);
            assertEquals(2, standards.size());

            // Фильтр по статусу
            List<ComputerResponse> maintenance = service.getComputers(null, ComputerStatus.MAINTENANCE, null);
            assertEquals(1, maintenance.size());
            assertEquals("VIP-01", maintenance.get(0).name());

            // Фильтр по максимальному тарифу
            List<ComputerResponse> affordable = service.getComputers(null, null, 120.0);
            assertEquals(1, affordable.size());
            assertEquals("PC-01", affordable.get(0).name());
        }

        @Test
        @DisplayName("Обновление параметров компьютера")
        void updateComputer_shouldUpdateFields() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            ComputerResponse updated = service.updateComputer(pc.id(), "PC-01-UPGRADED", ComputerType.VIP, 200.0);

            assertEquals("PC-01-UPGRADED", updated.name());
            assertEquals(ComputerType.VIP, updated.type());
            assertEquals(200.0, updated.hourlyRate());
        }

        @Test
        @DisplayName("Удаление свободного компьютера")
        void deleteComputer_whenAvailable_shouldSucceed() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            service.deleteComputer(pc.id());

            assertThrows(NotFoundException.class, () -> service.getComputer(pc.id()));
        }

        @Test
        @DisplayName("Создание, получение и фильтрация игроков")
        void createAndFilterPlayers_shouldSucceed() {
            PlayerResponse p1 = service.createPlayer("ZombieKing", "+79991112233", PlayerType.REGULAR);
            PlayerResponse p2 = service.createPlayer("GamerVIP", "+79994445566", PlayerType.VIP);

            assertNotNull(p1.id());
            assertNotNull(p2.id());

            // Фильтр по типу
            List<PlayerResponse> vips = service.getPlayers(PlayerType.VIP, null, null);
            assertEquals(1, vips.size());
            assertEquals("GamerVIP", vips.get(0).nickname());

            // Фильтр по поисковому запросу (никнейм и телефон)
            List<PlayerResponse> byNick = service.getPlayers(null, null, "Zombie");
            assertEquals(1, byNick.size());
            assertEquals("ZombieKing", byNick.get(0).nickname());

            List<PlayerResponse> byPhone = service.getPlayers(null, null, "44455");
            assertEquals(1, byPhone.size());
            assertEquals("GamerVIP", byPhone.get(0).nickname());
        }

        @Test
        @DisplayName("Обновление данных игрока")
        void updatePlayer_shouldUpdateFields() {
            PlayerResponse p = service.createPlayer("OldNick", "+79990001122", PlayerType.REGULAR);
            PlayerResponse updated = service.updatePlayer(p.id(), "NewNick", "+79990001133", PlayerType.VIP);

            assertEquals("NewNick", updated.nickname());
            assertEquals("+79990001133", updated.phone());
            assertEquals(PlayerType.VIP, updated.type());
        }

        @Test
        @DisplayName("Удаление игрока без активных компьютеров")
        void deletePlayer_whenNoActiveComputers_shouldSucceed() {
            PlayerResponse p = service.createPlayer("Ghost", "+79990001122", PlayerType.REGULAR);
            service.deletePlayer(p.id());

            assertThrows(NotFoundException.class, () -> service.getPlayer(p.id()));
        }

        @Test
        @DisplayName("REGULAR игрок успешно занимает STANDARD компьютер")
        void occupyComputer_regularPlayer_shouldOccupyStandardPc() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            PlayerResponse player = service.createPlayer("RegularGamer", "+79991112233", PlayerType.REGULAR);

            ComputerResponse occupied = service.occupyComputer(pc.id(), player.id());

            assertEquals(ComputerStatus.OCCUPIED, occupied.status());
            assertEquals(player.id(), occupied.occupiedByPlayerId());

            PlayerResponse updatedPlayer = service.getPlayer(player.id());
            assertFalse(updatedPlayer.occupiedComputerIds().isEmpty());
            assertTrue(updatedPlayer.occupiedComputerIds().contains(pc.id()));
        }

        @Test
        @DisplayName("Освобождение компьютера успешно снимает занятость")
        void releaseComputer_shouldFreeComputerAndPlayer() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            PlayerResponse player = service.createPlayer("RegularGamer", "+79991112233", PlayerType.REGULAR);
            service.occupyComputer(pc.id(), player.id());

            ComputerResponse released = service.releaseComputer(pc.id());

            assertEquals(ComputerStatus.AVAILABLE, released.status());
            assertNull(released.occupiedByPlayerId());

            PlayerResponse updatedPlayer = service.getPlayer(player.id());
            assertTrue(updatedPlayer.occupiedComputerIds().isEmpty());
        }

        @Test
        @DisplayName("Жизненный цикл техобслуживания: отправка и завершение")
        void maintenanceLifecycle_shouldTransitionCorrectly() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);

            ComputerResponse onMaint = service.sendToMaintenance(pc.id());
            assertEquals(ComputerStatus.MAINTENANCE, onMaint.status());

            ComputerResponse completed = service.completeMaintenance(pc.id());
            assertEquals(ComputerStatus.AVAILABLE, completed.status());
        }

        @Test
        @DisplayName("VIP игрок может занять до 2-х компьютеров, включая VIP зону")
        void vipPlayer_canOccupyUpToTwoComputers() {
            ComputerResponse pc1 = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            ComputerResponse vipPc = service.createComputer("VIP-01", ComputerType.VIP, 250.0);
            PlayerResponse vip = service.createPlayer("ProVip", "+79997778899", PlayerType.VIP);

            service.occupyComputer(pc1.id(), vip.id());
            service.occupyComputer(vipPc.id(), vip.id());

            PlayerResponse updatedVip = service.getPlayer(vip.id());
            assertEquals(2, updatedVip.occupiedComputerIds().size());
            assertTrue(updatedVip.occupiedComputerIds().contains(pc1.id()));
            assertTrue(updatedVip.occupiedComputerIds().contains(vipPc.id()));
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

        @Test
        @DisplayName("Возвращаемые снимки состояния (DTO) неизменяемы и изолированы от последующих операций")
        void returnedSnapshots_shouldBeImmutableAndIsolated() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            PlayerResponse player = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);

            ComputerResponse beforeOccupy = service.getComputer(pc.id());
            PlayerResponse playerBeforeOccupy = service.getPlayer(player.id());

            // Пытаемся изменить множество в снимке — должно выбросить UnsupportedOperationException
            assertThrows(UnsupportedOperationException.class,
                    () -> playerBeforeOccupy.occupiedComputerIds().add(999L));

            // Занимаем компьютер после получения первого снимка
            service.occupyComputer(pc.id(), player.id());

            // Старый снимок не должен измениться
            assertEquals(ComputerStatus.AVAILABLE, beforeOccupy.status());
            assertNull(beforeOccupy.occupiedByPlayerId());
            assertTrue(playerBeforeOccupy.occupiedComputerIds().isEmpty());

            // Новый снимок отражает актуальное состояние
            ComputerResponse afterOccupy = service.getComputer(pc.id());
            PlayerResponse playerAfterOccupy = service.getPlayer(player.id());
            assertEquals(ComputerStatus.OCCUPIED, afterOccupy.status());
            assertEquals(1, playerAfterOccupy.occupiedComputerIds().size());
        }

        @Test
        @DisplayName("Атомарная пересадка игрока на другой компьютер (transferComputer)")
        void transferComputer_shouldAtomicallyMovePlayerToTargetPc() {
            ComputerResponse pc1 = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            ComputerResponse pc2 = service.createComputer("PC-02", ComputerType.STANDARD, 120.0);
            PlayerResponse player = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);

            service.occupyComputer(pc1.id(), player.id());
            assertEquals(ComputerStatus.OCCUPIED, service.getComputer(pc1.id()).status());

            // Пересаживаем игрока с PC-01 на PC-02
            ComputerResponse transferred = service.transferComputer(pc1.id(), pc2.id(), player.id());

            assertEquals(pc2.id(), transferred.id());
            assertEquals(ComputerStatus.OCCUPIED, transferred.status());
            assertEquals(player.id(), transferred.occupiedByPlayerId());

            // Проверяем, что исходный ПК освободился
            ComputerResponse pc1After = service.getComputer(pc1.id());
            assertEquals(ComputerStatus.AVAILABLE, pc1After.status());
            assertNull(pc1After.occupiedByPlayerId());

            // Проверяем, что у игрока активен именно целевой ПК
            PlayerResponse playerAfter = service.getPlayer(player.id());
            assertEquals(1, playerAfter.occupiedComputerIds().size());
            assertTrue(playerAfter.occupiedComputerIds().contains(pc2.id()));
            assertFalse(playerAfter.occupiedComputerIds().contains(pc1.id()));
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
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            PlayerResponse p1 = service.createPlayer("Gamer1", "+79991112233", PlayerType.REGULAR);
            PlayerResponse p2 = service.createPlayer("Gamer2", "+79992223344", PlayerType.REGULAR);

            service.occupyComputer(pc.id(), p1.id());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.occupyComputer(pc.id(), p2.id()));
            assertTrue(ex.getMessage().contains("уже занят"));
        }

        @Test
        @DisplayName("Попытка занять компьютер на ТО выбрасывает ConflictException")
        void occupyComputer_underMaintenance_shouldThrowConflictException() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            service.sendToMaintenance(pc.id());
            PlayerResponse p = service.createPlayer("Gamer1", "+79991112233", PlayerType.REGULAR);

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.occupyComputer(pc.id(), p.id()));
            assertTrue(ex.getMessage().contains("техобслуживании"));
        }

        @Test
        @DisplayName("REGULAR игрок пытается занять VIP компьютер выбрасывает ConflictException")
        void occupyComputer_regularAccessingVip_shouldThrowConflictException() {
            ComputerResponse vipPc = service.createComputer("VIP-01", ComputerType.VIP, 250.0);
            PlayerResponse regular = service.createPlayer("RegularGamer", "+79991112233", PlayerType.REGULAR);

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.occupyComputer(vipPc.id(), regular.id()));
            assertTrue(ex.getMessage().contains("не имеет доступа к компьютерам категории VIP"));
        }

        @Test
        @DisplayName("REGULAR игрок превышает лимит (максимум 1) выбрасывает ConflictException")
        void occupyComputer_regularExceedingLimit_shouldThrowConflictException() {
            ComputerResponse pc1 = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            ComputerResponse pc2 = service.createComputer("PC-02", ComputerType.STANDARD, 100.0);
            PlayerResponse regular = service.createPlayer("RegularGamer", "+79991112233", PlayerType.REGULAR);

            service.occupyComputer(pc1.id(), regular.id());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.occupyComputer(pc2.id(), regular.id()));
            assertTrue(ex.getMessage().contains("исчерпал лимит мест"));
        }

        @Test
        @DisplayName("VIP игрок превышает лимит (максимум 2) выбрасывает ConflictException")
        void occupyComputer_vipExceedingLimit_shouldThrowConflictException() {
            ComputerResponse pc1 = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            ComputerResponse pc2 = service.createComputer("PC-02", ComputerType.STANDARD, 100.0);
            ComputerResponse pc3 = service.createComputer("PC-03", ComputerType.STANDARD, 100.0);
            PlayerResponse vip = service.createPlayer("VipGamer", "+79991112233", PlayerType.VIP);

            service.occupyComputer(pc1.id(), vip.id());
            service.occupyComputer(pc2.id(), vip.id());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.occupyComputer(pc3.id(), vip.id()));
            assertTrue(ex.getMessage().contains("исчерпал лимит мест (максимум 2)"));
        }

        @Test
        @DisplayName("Освобождение незанятого компьютера выбрасывает ConflictException")
        void releaseComputer_notOccupied_shouldThrowConflictException() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.releaseComputer(pc.id()));
            assertTrue(ex.getMessage().contains("не занят"));
        }

        @Test
        @DisplayName("Удаление занятого компьютера выбрасывает ConflictException")
        void deleteComputer_occupied_shouldThrowConflictException() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            PlayerResponse player = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);
            service.occupyComputer(pc.id(), player.id());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.deleteComputer(pc.id()));
            assertTrue(ex.getMessage().contains("Невозможно удалить компьютер"));
        }

        @Test
        @DisplayName("Удаление игрока с активными компьютерами выбрасывает ConflictException")
        void deletePlayer_withActiveComputers_shouldThrowConflictException() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            PlayerResponse player = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);
            service.occupyComputer(pc.id(), player.id());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.deletePlayer(player.id()));
            assertTrue(ex.getMessage().contains("Нельзя удалить профиль игрока"));
        }

        @Test
        @DisplayName("Отправка занятого компьютера на ТО выбрасывает ConflictException")
        void sendToMaintenance_whenOccupied_shouldThrowConflictException() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            PlayerResponse player = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);
            service.occupyComputer(pc.id(), player.id());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.sendToMaintenance(pc.id()));
            assertTrue(ex.getMessage().contains("Нельзя отправить компьютер на обслуживание"));
        }

        @Test
        @DisplayName("Повторная отправка на ТО выбрасывает ConflictException")
        void sendToMaintenance_alreadyUnderMaintenance_shouldThrowConflictException() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            service.sendToMaintenance(pc.id());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.sendToMaintenance(pc.id()));
            assertTrue(ex.getMessage().contains("уже находится на обслуживании"));
        }

        @Test
        @DisplayName("Завершение обслуживания компьютера, который не на ТО, выбрасывает ConflictException")
        void completeMaintenance_notUnderMaintenance_shouldThrowConflictException() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.completeMaintenance(pc.id()));
            assertTrue(ex.getMessage().contains("не находится на обслуживании"));
        }

        @Test
        @DisplayName("Изменение типа компьютера пока он занят выбрасывает ConflictException")
        void updateComputer_changeTypeWhileOccupied_shouldThrowConflictException() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            PlayerResponse player = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);
            service.occupyComputer(pc.id(), player.id());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.updateComputer(pc.id(), "PC-01", ComputerType.VIP, 200.0));
            assertTrue(ex.getMessage().contains("Нельзя изменить категорию"));
        }

        @Test
        @DisplayName("Понижение VIP до REGULAR при наличии 2-х компьютеров выбрасывает ConflictException")
        void downgradeVipToRegular_withMultipleComputers_shouldThrowConflictException() {
            ComputerResponse pc1 = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            ComputerResponse pc2 = service.createComputer("PC-02", ComputerType.STANDARD, 100.0);
            PlayerResponse vip = service.createPlayer("VipGamer", "+79991112233", PlayerType.VIP);

            service.occupyComputer(pc1.id(), vip.id());
            service.occupyComputer(pc2.id(), vip.id());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.updatePlayer(vip.id(), "VipGamer", "+79991112233", PlayerType.REGULAR));
            assertTrue(ex.getMessage().contains("Нельзя перевести игрока в REGULAR: у него занято 2 компьютеров"));
        }

        @Test
        @DisplayName("Понижение VIP до REGULAR при занятом VIP месте выбрасывает ConflictException")
        void downgradeVipToRegular_withVipComputer_shouldThrowConflictException() {
            ComputerResponse vipPc = service.createComputer("VIP-01", ComputerType.VIP, 250.0);
            PlayerResponse vip = service.createPlayer("VipGamer", "+79991112233", PlayerType.VIP);

            service.occupyComputer(vipPc.id(), vip.id());

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> service.updatePlayer(vip.id(), "VipGamer", "+79991112233", PlayerType.REGULAR));
            assertTrue(ex.getMessage().contains("он сейчас занимает VIP-компьютер"));
        }

        @Test
        @DisplayName("Пересадка на уже занятый ПК выбрасывает ConflictException и сохраняет исходный ПК занятым")
        void transferComputer_targetOccupied_shouldThrowConflictExceptionAndPreserveState() {
            ComputerResponse pc1 = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            ComputerResponse pc2 = service.createComputer("PC-02", ComputerType.STANDARD, 120.0);
            PlayerResponse p1 = service.createPlayer("Gamer1", "+79991112233", PlayerType.REGULAR);
            PlayerResponse p2 = service.createPlayer("Gamer2", "+79992223344", PlayerType.REGULAR);

            service.occupyComputer(pc1.id(), p1.id());
            service.occupyComputer(pc2.id(), p2.id());

            // Попытка пересадить p1 на уже занятый pc2
            assertThrows(ConflictException.class,
                    () -> service.transferComputer(pc1.id(), pc2.id(), p1.id()));

            // Исходное состояние должно быть сохранено (All-or-Nothing)
            assertEquals(ComputerStatus.OCCUPIED, service.getComputer(pc1.id()).status());
            assertEquals(p1.id(), service.getComputer(pc1.id()).occupiedByPlayerId());
            assertEquals(p2.id(), service.getComputer(pc2.id()).occupiedByPlayerId());
        }

        @Test
        @DisplayName("REGULAR игрок не может пересаживаться на VIP ПК")
        void transferComputer_regularToVip_shouldThrowConflictException() {
            ComputerResponse standard = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            ComputerResponse vip = service.createComputer("VIP-01", ComputerType.VIP, 250.0);
            PlayerResponse regular = service.createPlayer("Regular", "+79991112233", PlayerType.REGULAR);

            service.occupyComputer(standard.id(), regular.id());

            assertThrows(ConflictException.class,
                    () -> service.transferComputer(standard.id(), vip.id(), regular.id()));

            // Исходный ПК по-прежнему занят
            assertEquals(ComputerStatus.OCCUPIED, service.getComputer(standard.id()).status());
        }

        @Test
        @DisplayName("Пересадка с ПК, который не занят данным игроком, выбрасывает ConflictException")
        void transferComputer_fromPcNotOccupiedByPlayer_shouldThrowConflictException() {
            ComputerResponse pc1 = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            ComputerResponse pc2 = service.createComputer("PC-02", ComputerType.STANDARD, 120.0);
            PlayerResponse p1 = service.createPlayer("Gamer1", "+79991112233", PlayerType.REGULAR);
            PlayerResponse p2 = service.createPlayer("Gamer2", "+79992223344", PlayerType.REGULAR);

            service.occupyComputer(pc1.id(), p1.id());

            // Игрок p2 пытается "пересесть" с ПК 1, за которым сидит p1
            assertThrows(ConflictException.class,
                    () -> service.transferComputer(pc1.id(), pc2.id(), p2.id()));
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
            assertThrows(ValidationException.class, () -> service.transferComputer(null, 2L, 1L));
            assertThrows(ValidationException.class, () -> service.transferComputer(1L, null, 1L));
            assertThrows(ValidationException.class, () -> service.transferComputer(1L, 2L, null));
            assertThrows(ValidationException.class, () -> service.transferComputer(1L, 1L, 1L));
        }

        @Test
        @DisplayName("Обновление компьютера с сохранением своего имени не считается дубликатом")
        void updateComputer_withSameName_shouldSucceed() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            ComputerResponse updated = service.updateComputer(pc.id(), "PC-01", ComputerType.STANDARD, 120.0);
            assertEquals(120.0, updated.hourlyRate());
        }

        @Test
        @DisplayName("Обновление игрока с сохранением своего телефона не считается дубликатом")
        void updatePlayer_withSamePhone_shouldSucceed() {
            PlayerResponse p = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);
            PlayerResponse updated = service.updatePlayer(p.id(), "NewGamer", "+79991112233", PlayerType.REGULAR);
            assertEquals("NewGamer", updated.nickname());
        }
    }

    @Nested
    @DisplayName("4. Изменения состояния ресурсов (Resource State Transitions)")
    class StateTransitions {

        @Test
        @DisplayName("Полный цикл переходов состояния: AVAILABLE -> OCCUPIED -> AVAILABLE -> MAINTENANCE -> AVAILABLE")
        void fullStateCycle_shouldSucceed() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            PlayerResponse player = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);

            assertEquals(ComputerStatus.AVAILABLE, pc.status());

            // 1. Занятие: AVAILABLE -> OCCUPIED
            service.occupyComputer(pc.id(), player.id());
            ComputerResponse occupied = service.getComputer(pc.id());
            assertEquals(ComputerStatus.OCCUPIED, occupied.status());
            assertEquals(player.id(), occupied.occupiedByPlayerId());

            // 2. Освобождение: OCCUPIED -> AVAILABLE
            service.releaseComputer(pc.id());
            ComputerResponse released = service.getComputer(pc.id());
            assertEquals(ComputerStatus.AVAILABLE, released.status());
            assertNull(released.occupiedByPlayerId());

            // 3. Отправка на ТО: AVAILABLE -> MAINTENANCE
            service.sendToMaintenance(pc.id());
            ComputerResponse onMaint = service.getComputer(pc.id());
            assertEquals(ComputerStatus.MAINTENANCE, onMaint.status());

            // 4. Завершение ТО: MAINTENANCE -> AVAILABLE
            service.completeMaintenance(pc.id());
            ComputerResponse availableAgain = service.getComputer(pc.id());
            assertEquals(ComputerStatus.AVAILABLE, availableAgain.status());
        }

        @Test
        @DisplayName("Недопустимые переходы состояния блокируются")
        void invalidStateTransitions_shouldBeRejected() {
            ComputerResponse pc = service.createComputer("PC-01", ComputerType.STANDARD, 100.0);
            PlayerResponse player = service.createPlayer("Gamer", "+79991112233", PlayerType.REGULAR);

            // AVAILABLE -> completeMaintenance (нельзя)
            assertThrows(ConflictException.class, () -> service.completeMaintenance(pc.id()));

            // AVAILABLE -> release (нельзя)
            assertThrows(ConflictException.class, () -> service.releaseComputer(pc.id()));

            // Занимаем
            service.occupyComputer(pc.id(), player.id());

            // OCCUPIED -> sendToMaintenance (нельзя)
            assertThrows(ConflictException.class, () -> service.sendToMaintenance(pc.id()));

            // OCCUPIED -> delete (нельзя)
            assertThrows(ConflictException.class, () -> service.deleteComputer(pc.id()));

            // Освобождаем и отправляем на ТО
            service.releaseComputer(pc.id());
            service.sendToMaintenance(pc.id());

            // MAINTENANCE -> occupy (нельзя)
            assertThrows(ConflictException.class, () -> service.occupyComputer(pc.id(), player.id()));

            // MAINTENANCE -> sendToMaintenance (нельзя)
            assertThrows(ConflictException.class, () -> service.sendToMaintenance(pc.id()));

            // MAINTENANCE -> release (нельзя)
            assertThrows(ConflictException.class, () -> service.releaseComputer(pc.id()));
        }
    }
}
