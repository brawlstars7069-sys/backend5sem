package app.dto;

import app.domain.Model.Player;
import app.domain.Model.PlayerType;

import java.util.Set;

public record PlayerResponse(
        Long id,
        String nickname,
        String phone,
        PlayerType type,
        Set<Long> occupiedComputerIds,
        int maxComputers,
        double discountPercent
) {
    public static PlayerResponse from(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getNickname(),
                player.getPhone(),
                player.getType(),
                Set.copyOf(player.getOccupiedComputerIds()),
                player.getMaxComputers(),
                player.getDiscountPercent()
        );
    }
}
