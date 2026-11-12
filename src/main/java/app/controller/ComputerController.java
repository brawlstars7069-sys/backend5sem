package app.controller;

import app.domain.Model.Computer;
import app.domain.Model.ComputerStatus;
import app.domain.Model.ComputerType;
import app.dto.ComputerDto;
import app.dto.OccupyDto;
import app.service.ClubService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/computers", "/computers"})
public class ComputerController {

    private final ClubService clubService;

    public ComputerController(ClubService clubService) {
        this.clubService = clubService;
    }

    @GetMapping
    public List<Computer> getComputers(
            @RequestParam(required = false) ComputerType type,
            @RequestParam(required = false) ComputerStatus status,
            @RequestParam(required = false) Double maxRate
    ) {
        return clubService.getComputers(type, status, maxRate);
    }

    @GetMapping("/{id}")
    public Computer getComputer(@PathVariable Long id) {
        return clubService.getComputer(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Computer createComputer(@Valid @RequestBody ComputerDto dto) {
        return clubService.createComputer(dto.name(), dto.type(), dto.hourlyRate());
    }

    @PutMapping("/{id}")
    public Computer updateComputer(@PathVariable Long id, @Valid @RequestBody ComputerDto dto) {
        return clubService.updateComputer(id, dto.name(), dto.type(), dto.hourlyRate());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComputer(@PathVariable Long id) {
        clubService.deleteComputer(id);
    }

    @PostMapping("/{id}/occupy")
    public Computer occupyComputer(@PathVariable Long id, @Valid @RequestBody OccupyDto dto) {
        return clubService.occupyComputer(id, dto.playerId());
    }

    @PostMapping("/{id}/release")
    public Computer releaseComputer(@PathVariable Long id) {
        return clubService.releaseComputer(id);
    }

    @PostMapping("/{id}/maintenance")
    public Computer sendToMaintenance(@PathVariable Long id) {
        return clubService.sendToMaintenance(id);
    }

    @PostMapping("/{id}/complete-maintenance")
    public Computer completeMaintenance(@PathVariable Long id) {
        return clubService.completeMaintenance(id);
    }
}
