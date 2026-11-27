package app.dto;

import app.domain.Model.Computer;
import app.domain.Model.ComputerStatus;
import app.domain.Model.ComputerType;

public record ComputerSummary(
        Long id,
        String name,
        ComputerType type,
        double hourlyRate,
        ComputerStatus status,
        String ipAddress,
        String specs
) {
    public static ComputerSummary from(Computer pc) {
        if (pc == null) return null;
        return new ComputerSummary(
                pc.getId(),
                pc.getName(),
                pc.getType(),
                pc.getHourlyRate(),
                pc.getStatus(),
                pc.getIpAddress(),
                pc.getSpecs()
        );
    }
}
