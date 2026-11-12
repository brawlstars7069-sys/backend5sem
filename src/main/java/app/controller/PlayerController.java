package app.controller;

import app.domain.Model.Player;
import app.domain.Model.PlayerType;
import app.dto.PlayerDto;
import app.service.ClubService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/players", "/players"})
public class PlayerController {

    private final ClubService clubService;

    public PlayerController(ClubService clubService) {
        this.clubService = clubService;
    }

    @GetMapping
    public List<Player> getPlayers(
            @RequestParam(required = false) PlayerType type,
            @RequestParam(required = false) Boolean hasActiveComputers,
            @RequestParam(required = false) Boolean hasActiveSession,
            @RequestParam(required = false) String search
    ) {
        Boolean activeFilter = hasActiveComputers != null ? hasActiveComputers : hasActiveSession;
        return clubService.getPlayers(type, activeFilter, search);
    }

    @GetMapping("/{id}")
    public Player getPlayer(@PathVariable Long id) {
        return clubService.getPlayer(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Player createPlayer(@Valid @RequestBody PlayerDto dto) {
        return clubService.createPlayer(dto.nickname(), dto.phone(), dto.type());
    }

    @PutMapping("/{id}")
    public Player updatePlayer(@PathVariable Long id, @Valid @RequestBody PlayerDto dto) {
        return clubService.updatePlayer(id, dto.nickname(), dto.phone(), dto.type());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlayer(@PathVariable Long id) {
        clubService.deletePlayer(id);
    }
}
