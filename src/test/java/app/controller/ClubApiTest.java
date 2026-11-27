package app.controller;

import app.domain.Model.ComputerType;
import app.domain.Model.PlayerType;
import app.dto.ComputerDto;
import app.dto.OccupyDto;
import app.dto.PlayerDto;
import app.dto.TransferDto;
import app.repository.ComputerRepository;
import app.repository.PlayerRepository;
import app.service.ClubService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("Сквозное тестирование HTTP API компьютерного клуба Zombie Playa (MockMvc)")
class ClubApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ComputerRepository computerRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.execute(status -> {
            computerRepository.findAll().forEach(c -> {
                c.setOccupiedBy(null);
                computerRepository.save(c);
            });
            computerRepository.clear();
            playerRepository.clear();
            return null;
        });
    }

    @Test
    @DisplayName("1. Проверка служебных эндпоинтов: /ping, 405 Method Not Allowed, 404 Not Found")
    void testSystemEndpoints() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("pong"));

        mockMvc.perform(post("/ping"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.status").value(405));

        mockMvc.perform(get("/unknown-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("2. CRUD компьютеров: создание, валидация 400, дубликат 409, получение и удаление")
    void testComputersCrud() throws Exception {
        // Создание
        ComputerDto dto = new ComputerDto("PC-MOCK-01", ComputerType.VIP, 300.0);
        mockMvc.perform(post("/api/computers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("PC-MOCK-01"))
                .andExpect(jsonPath("$.type").value("VIP"))
                .andExpect(jsonPath("$.hourlyRate").value(300.0))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        // Ошибка 400: отрицательный тариф
        ComputerDto badDto = new ComputerDto("BAD-PC", ComputerType.STANDARD, -10.0);
        mockMvc.perform(post("/api/computers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Ошибка 409: дубликат имени
        mockMvc.perform(post("/api/computers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("3. CRUD игроков: создание, валидация 400, дубликат 409, получение")
    void testPlayersCrud() throws Exception {
        PlayerDto playerDto = new PlayerDto("ShadowGamer", "+79997778899", PlayerType.VIP);
        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(playerDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value("ShadowGamer"))
                .andExpect(jsonPath("$.phone").value("+79997778899"))
                .andExpect(jsonPath("$.type").value("VIP"));

        // Ошибка 409: дубликат телефона
        PlayerDto cloneDto = new PlayerDto("ClonePlayer", "+79997778899", PlayerType.REGULAR);
        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cloneDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("4. Строгая валидация query-параметров и битого JSON (400 Bad Request)")
    void testStrictValidation() throws Exception {
        // Некорректный boolean
        mockMvc.perform(get("/api/players?hasActiveComputers=not-a-boolean"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Некорректный enum
        mockMvc.perform(get("/api/computers?type=INVALID_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Некорректное число
        mockMvc.perform(get("/api/computers?maxRate=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Битый JSON
        mockMvc.perform(post("/api/computers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed:json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Нечисловой ID в пути
        mockMvc.perform(get("/api/computers/xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("5. Связанные ресурсы: занятие ПК, просмотр связанных данных в профиле игрока и ПК")
    void testRelatedResourcesOccupyAndView() throws Exception {
        var pc = clubService.createComputer("PC-PLAY", ComputerType.STANDARD, 100.0, "192.168.1.15", "GTX 1660");
        var player = clubService.createPlayer("GamerOne", "+79991110022", PlayerType.REGULAR);

        // Занимаем ПК
        OccupyDto occupyDto = new OccupyDto(player.id());
        mockMvc.perform(post("/api/computers/" + pc.id() + "/occupy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(occupyDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OCCUPIED"))
                .andExpect(jsonPath("$.occupiedByPlayerId").value(player.id()));

        // Проверяем, что в профиле игрока виден занятый компьютер со спеками и IP
        mockMvc.perform(get("/api/players/" + player.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupiedComputers", hasSize(1)))
                .andExpect(jsonPath("$.occupiedComputers[0].id").value(pc.id()))
                .andExpect(jsonPath("$.occupiedComputers[0].name").value("PC-PLAY"))
                .andExpect(jsonPath("$.occupiedComputers[0].ipAddress").value("192.168.1.15"))
                .andExpect(jsonPath("$.occupiedComputers[0].specs").value("GTX 1660"));

        // Проверяем, что в профиле ПК виден связанный игрок
        mockMvc.perform(get("/api/computers/" + pc.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupiedByPlayer.nickname").value("GamerOne"));
    }

    @Test
    @DisplayName("6. Атомарная пересадка (transfer): успех и откат All-or-Nothing при ошибке")
    void testTransferAtomicOperation() throws Exception {
        var pc1 = clubService.createComputer("PC-FROM", ComputerType.STANDARD, 100.0);
        var pc2 = clubService.createComputer("PC-TO", ComputerType.STANDARD, 120.0);
        var pcVip = clubService.createComputer("PC-VIP-ZONE", ComputerType.VIP, 300.0);
        var player = clubService.createPlayer("TransferGamer", "+79995556677", PlayerType.REGULAR);

        clubService.occupyComputer(pc1.id(), player.id());

        // 6.1 Успешная пересадка с pc1 на pc2
        TransferDto transferDto = new TransferDto(pc2.id(), player.id());
        mockMvc.perform(post("/api/computers/" + pc1.id() + "/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pc2.id()))
                .andExpect(jsonPath("$.status").value("OCCUPIED"))
                .andExpect(jsonPath("$.occupiedByPlayerId").value(player.id()));

        // Проверяем, что pc1 освободился
        mockMvc.perform(get("/api/computers/" + pc1.id()))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.occupiedByPlayerId").doesNotExist());

        // 6.2 Ошибка и откат: REGULAR игрок пытается пересесть на VIP ПК
        TransferDto badTransfer = new TransferDto(pcVip.id(), player.id());
        mockMvc.perform(post("/api/computers/" + pc2.id() + "/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badTransfer)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Проверяем откат: pc2 по-прежнему OCCUPIED за игроком, pcVip по-прежнему AVAILABLE
        mockMvc.perform(get("/api/computers/" + pc2.id()))
                .andExpect(jsonPath("$.status").value("OCCUPIED"))
                .andExpect(jsonPath("$.occupiedByPlayerId").value(player.id()));

        mockMvc.perform(get("/api/computers/" + pcVip.id()))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("7. Целостность данных: запрет удаления занятого ПК и играющего игрока (409 Conflict)")
    void testIntegrityRejectionsOnDelete() throws Exception {
        var pc = clubService.createComputer("LOCKED-PC", ComputerType.STANDARD, 100.0);
        var player = clubService.createPlayer("LockedPlayer", "+79990001188", PlayerType.REGULAR);
        clubService.occupyComputer(pc.id(), player.id());

        // Удаление занятого ПК -> 409
        mockMvc.perform(delete("/api/computers/" + pc.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Удаление играющего игрока -> 409
        mockMvc.perform(delete("/api/players/" + player.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Освобождаем ПК
        mockMvc.perform(post("/api/computers/" + pc.id() + "/release"))
                .andExpect(status().isOk());

        // Теперь удаление свободного ПК успешно -> 204
        mockMvc.perform(delete("/api/computers/" + pc.id()))
                .andExpect(status().isNoContent());

        // Теперь удаление свободного игрока успешно -> 204
        mockMvc.perform(delete("/api/players/" + player.id()))
                .andExpect(status().isNoContent());
    }
}
