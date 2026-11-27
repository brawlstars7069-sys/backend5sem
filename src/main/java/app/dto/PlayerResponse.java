package app.dto;

import app.domain.Model.Player;
import app.domain.Model.PlayerType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public record PlayerResponse(
        Long id,
        String nickname,
        String phone,
        PlayerType type,
        Set<Long> occupiedComputerIds,
        List<ComputerSummary> occupiedComputers,
        int maxComputers,
        double discountPercent,
        double balance
) {
    public static PlayerResponse from(Player player) {
        List<ComputerSummary> comps = player.getOccupiedComputers() != null
                ? player.getOccupiedComputers().stream().map(ComputerSummary::from).toList()
                : Collections.emptyList();

        return new PlayerResponse(
                player.getId(),
                player.getNickname(),
                player.getPhone(),
                player.getType(),
                Set.copyOf(player.getOccupiedComputerIds()),
                comps,
                player.getMaxComputers(),
                player.getDiscountPercent(),
                player.getBalance()
        );
    }
}
