package app.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Model {

    public enum ComputerType {
        STANDARD,
        VIP
    }

    public enum ComputerStatus {
        AVAILABLE,
        OCCUPIED,
        MAINTENANCE
    }

    public static class Computer {
        private Long id;
        private String name;
        private ComputerType type = ComputerType.STANDARD;
        private double hourlyRate;
        private ComputerStatus status = ComputerStatus.AVAILABLE;
        private Long occupiedByPlayerId;

        public Computer() {}

        public Computer(Long id, String name, ComputerType type, double hourlyRate) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.hourlyRate = hourlyRate;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public ComputerType getType() { return type; }
        public void setType(ComputerType type) { this.type = type; }
        public double getHourlyRate() { return hourlyRate; }
        public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }
        public ComputerStatus getStatus() { return status; }
        public void setStatus(ComputerStatus status) { this.status = status; }
        public Long getOccupiedByPlayerId() { return occupiedByPlayerId; }
        public void setOccupiedByPlayerId(Long occupiedByPlayerId) { this.occupiedByPlayerId = occupiedByPlayerId; }
    }

    public enum PlayerType {
        REGULAR {
            @Override public int getMaxComputers() { return 1; }
            @Override public boolean canAccess(ComputerType type) { return type == ComputerType.STANDARD; }
            @Override public double getDiscountPercent() { return 0.0; }
        },
        VIP {
            @Override public int getMaxComputers() { return 2; }
            @Override public boolean canAccess(ComputerType type) { return true; }
            @Override public double getDiscountPercent() { return 20.0; }
        };

        public abstract int getMaxComputers();
        public abstract boolean canAccess(ComputerType type);
        public abstract double getDiscountPercent();
    }

    public static class Player {
        private Long id;
        private String nickname;
        private String phone;
        private PlayerType type = PlayerType.REGULAR;
        private Set<Long> occupiedComputerIds = new HashSet<>();

        public Player() {}

        public Player(Long id, String nickname, String phone, PlayerType type) {
            this.id = id;
            this.nickname = nickname;
            this.phone = phone;
            this.type = type;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public PlayerType getType() { return type; }
        public void setType(PlayerType type) { this.type = type; }

        public Set<Long> getOccupiedComputerIds() { return Collections.unmodifiableSet(occupiedComputerIds); }
        public void setOccupiedComputerIds(Set<Long> ids) {
            this.occupiedComputerIds = (ids != null) ? new HashSet<>(ids) : new HashSet<>();
        }
        public void addOccupiedComputer(Long pcId) { this.occupiedComputerIds.add(pcId); }
        public void removeOccupiedComputer(Long pcId) { this.occupiedComputerIds.remove(pcId); }
        public boolean hasActiveComputers() { return !occupiedComputerIds.isEmpty(); }

        public int getMaxComputers() { return type.getMaxComputers(); }
        public boolean canAccess(ComputerType computerType) { return type.canAccess(computerType); }
        public double getDiscountPercent() { return type.getDiscountPercent(); }
    }
}
