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
        private ComputerType type;
        private double hourlyRate;
        private ComputerStatus status = ComputerStatus.AVAILABLE;
        private Long currentPlayerId;
        private Long currentSessionId;

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
        public Long getCurrentPlayerId() { return currentPlayerId; }
        public void setCurrentPlayerId(Long currentPlayerId) { this.currentPlayerId = currentPlayerId; }
        public Long getCurrentSessionId() { return currentSessionId; }
        public void setCurrentSessionId(Long currentSessionId) { this.currentSessionId = currentSessionId; }
    }

    public enum PlayerType {
        REGULAR {
            @Override public int getMaxActiveComputers() { return 1; }
            @Override public int getMaxHours() { return 4; }
            @Override public boolean canAccess(ComputerType type) { return type == ComputerType.STANDARD; }
            @Override public double calculateCost(double rate, int hours) { return rate * hours; }
            @Override public double getDiscountPercent() { return 0.0; }
        },
        VIP {
            @Override public int getMaxActiveComputers() { return 2; }
            @Override public int getMaxHours() { return 12; }
            @Override public boolean canAccess(ComputerType type) { return true; }
            @Override public double calculateCost(double rate, int hours) {
                return Math.round(rate * hours * 0.8 * 100.0) / 100.0; // Скидка 20%
            }
            @Override public double getDiscountPercent() { return 20.0; }
        };

        public abstract int getMaxActiveComputers();
        public abstract int getMaxHours();
        public abstract boolean canAccess(ComputerType type);
        public abstract double calculateCost(double rate, int hours);
        public abstract double getDiscountPercent();
    }

    public static class Player {
        private Long id;
        private String nickname;
        private String phone;
        private PlayerType type = PlayerType.REGULAR;
        private Set<Long> activeComputerIds = new HashSet<>();

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

        public Set<Long> getActiveComputerIds() { return Collections.unmodifiableSet(activeComputerIds); }
        public void setActiveComputerIds(Set<Long> ids) {
            this.activeComputerIds = (ids != null) ? new HashSet<>(ids) : new HashSet<>();
        }
        public void addActiveComputer(Long pcId) { this.activeComputerIds.add(pcId); }
        public void removeActiveComputer(Long pcId) { this.activeComputerIds.remove(pcId); }
        public boolean hasActiveSession() { return !activeComputerIds.isEmpty(); }

        public int getMaxActiveComputers() { return type.getMaxActiveComputers(); }
        public int getMaxHours() { return type.getMaxHours(); }
        public boolean canAccess(ComputerType computerType) { return type.canAccess(computerType); }
        public double calculateCost(double rate, int hours) { return type.calculateCost(rate, hours); }
        public double getDiscountPercent() { return type.getDiscountPercent(); }
    }

    public enum SessionStatus {
        ACTIVE,
        COMPLETED
    }

    public static class Session {
        private Long id;
        private Long playerId;
        private Long computerId;
        private int hours;
        private double hourlyRate;
        private double discountPercent;
        private double totalCost;
        private SessionStatus status = SessionStatus.ACTIVE;

        public Session() {}

        public Session(Long id, Long playerId, Long computerId, int hours, double hourlyRate, double discountPercent, double totalCost) {
            this.id = id;
            this.playerId = playerId;
            this.computerId = computerId;
            this.hours = hours;
            this.hourlyRate = hourlyRate;
            this.discountPercent = discountPercent;
            this.totalCost = totalCost;
            this.status = SessionStatus.ACTIVE;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getPlayerId() { return playerId; }
        public void setPlayerId(Long playerId) { this.playerId = playerId; }
        public Long getComputerId() { return computerId; }
        public void setComputerId(Long computerId) { this.computerId = computerId; }
        public int getHours() { return hours; }
        public void setHours(int hours) { this.hours = hours; }
        public double getHourlyRate() { return hourlyRate; }
        public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }
        public double getDiscountPercent() { return discountPercent; }
        public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }
        public double getTotalCost() { return totalCost; }
        public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
        public SessionStatus getStatus() { return status; }
        public void setStatus(SessionStatus status) { this.status = status; }
    }
}
