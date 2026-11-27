package app.dto;

import app.domain.Model.Player;
import app.domain.Model.PlayerType;

public record PlayerSummary(
        Long id,
        String nickname,
        String phone,
        PlayerType type
) {
    public static PlayerSummary from(Player p) {
        if (p == null) return null;
        return new PlayerSummary(
                p.getId(),
                p.getNickname(),
                p.getPhone(),
                p.getType()
        );
    }
}
