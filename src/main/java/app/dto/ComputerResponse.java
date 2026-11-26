package app.dto;

import app.domain.Model.Computer;
import app.domain.Model.ComputerStatus;
import app.domain.Model.ComputerType;

public record ComputerResponse(
        Long id,
        String name,
        ComputerType type,
        double hourlyRate,
        ComputerStatus status,
        Long occupiedByPlayerId
) {
    public static ComputerResponse from(Computer pc) {
        return new ComputerResponse(
                pc.getId(),
                pc.getName(),
                pc.getType(),
                pc.getHourlyRate(),
                pc.getStatus(),
                pc.getOccupiedByPlayerId()
        );
    }
}
