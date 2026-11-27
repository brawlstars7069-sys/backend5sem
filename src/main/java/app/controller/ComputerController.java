package app.controller;

import app.domain.Model.ComputerStatus;
import app.domain.Model.ComputerType;
import app.dto.ComputerDto;
import app.dto.ComputerResponse;
import app.dto.OccupyDto;
import app.dto.TransferDto;
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
    public List<ComputerResponse> getComputers(
            @RequestParam(required = false) ComputerType type,
            @RequestParam(required = false) ComputerStatus status,
            @RequestParam(required = false) Double maxRate
    ) {
        return clubService.getComputers(type, status, maxRate);
    }

    @GetMapping("/{id}")
    public ComputerResponse getComputer(@PathVariable Long id) {
        return clubService.getComputer(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComputerResponse createComputer(@Valid @RequestBody ComputerDto dto) {
        return clubService.createComputer(dto.name(), dto.type(), dto.hourlyRate());
    }

    @PutMapping("/{id}")
    public ComputerResponse updateComputer(@PathVariable Long id, @Valid @RequestBody ComputerDto dto) {
        return clubService.updateComputer(id, dto.name(), dto.type(), dto.hourlyRate());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComputer(@PathVariable Long id) {
        clubService.deleteComputer(id);
    }

    @PostMapping("/{id}/occupy")
    public ComputerResponse occupyComputer(@PathVariable Long id, @Valid @RequestBody OccupyDto dto) {
        return clubService.occupyComputer(id, dto.playerId());
    }

    @PostMapping("/{id}/transfer")
    public ComputerResponse transferComputer(@PathVariable Long id, @Valid @RequestBody TransferDto dto) {
        return clubService.transferComputer(id, dto.targetComputerId(), dto.playerId());
    }

    @PostMapping("/{id}/release")
    public ComputerResponse releaseComputer(@PathVariable Long id) {
        return clubService.releaseComputer(id);
    }

    @PostMapping("/{id}/maintenance")
    public ComputerResponse sendToMaintenance(@PathVariable Long id) {
        return clubService.sendToMaintenance(id);
    }

    @PostMapping("/{id}/complete-maintenance")
    public ComputerResponse completeMaintenance(@PathVariable Long id) {
        return clubService.completeMaintenance(id);
    }
}
