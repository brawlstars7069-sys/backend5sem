package app.domain;

import jakarta.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Entity(name = "Computer")
    @Table(name = "computers")
    public static class Computer {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, unique = true)
        private String name;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private ComputerType type = ComputerType.STANDARD;

        @Column(name = "hourly_rate", nullable = false)
        private double hourlyRate;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private ComputerStatus status = ComputerStatus.AVAILABLE;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "occupied_by_player_id")
        private Player occupiedBy;

        @Column(name = "ip_address")
        private String ipAddress;

        @Column(name = "specs")
        private String specs;

        public Computer() {}

        public Computer(Long id, String name, ComputerType type, double hourlyRate) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.hourlyRate = hourlyRate;
            this.status = ComputerStatus.AVAILABLE;
        }

        public Computer(Long id, String name, ComputerType type, double hourlyRate, String ipAddress, String specs) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.hourlyRate = hourlyRate;
            this.status = ComputerStatus.AVAILABLE;
            this.ipAddress = ipAddress;
            this.specs = specs;
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

        public Player getOccupiedBy() { return occupiedBy; }
        public void setOccupiedBy(Player occupiedBy) { this.occupiedBy = occupiedBy; }

        public Long getOccupiedByPlayerId() {
            return occupiedBy != null ? occupiedBy.getId() : null;
        }

        public void setOccupiedByPlayerId(Long playerId) {
            if (playerId == null) {
                this.occupiedBy = null;
            } else if (this.occupiedBy == null || !playerId.equals(this.occupiedBy.getId())) {
                Player p = new Player();
                p.setId(playerId);
                this.occupiedBy = p;
            }
        }

        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public String getSpecs() { return specs; }
        public void setSpecs(String specs) { this.specs = specs; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Computer computer = (Computer) o;
            return id != null && Objects.equals(id, computer.id);
        }

        @Override
        public int hashCode() {
            return id != null ? Objects.hash(id) : Objects.hash(name);
        }
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

    @Entity(name = "Player")
    @Table(name = "players")
    public static class Player {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, unique = true)
        private String nickname;

        @Column(nullable = false, unique = true)
        private String phone;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private PlayerType type = PlayerType.REGULAR;

        @Column(nullable = false)
        private double balance = 0.0;

        @OneToMany(mappedBy = "occupiedBy", fetch = FetchType.LAZY)
        private Set<Computer> occupiedComputers = new HashSet<>();

        public Player() {}

        public Player(Long id, String nickname, String phone, PlayerType type) {
            this.id = id;
            this.nickname = nickname;
            this.phone = phone;
            this.type = type;
            this.balance = 0.0;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public PlayerType getType() { return type; }
        public void setType(PlayerType type) { this.type = type; }
        public double getBalance() { return balance; }
        public void setBalance(double balance) { this.balance = balance; }

        public Set<Computer> getOccupiedComputers() {
            return occupiedComputers;
        }

        public void setOccupiedComputers(Set<Computer> computers) {
            this.occupiedComputers = (computers != null) ? new HashSet<>(computers) : new HashSet<>();
        }

        public Set<Long> getOccupiedComputerIds() {
            if (occupiedComputers == null) return Collections.emptySet();
            return occupiedComputers.stream()
                    .map(Computer::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
        }

        public void setOccupiedComputerIds(Set<Long> ids) {
            if (ids == null) {
                this.occupiedComputers.clear();
            } else {
                this.occupiedComputers = ids.stream().map(cid -> {
                    Computer c = new Computer();
                    c.setId(cid);
                    return c;
                }).collect(Collectors.toSet());
            }
        }

        public void addOccupiedComputer(Computer pc) {
            if (pc != null) {
                if (this.occupiedComputers == null) {
                    this.occupiedComputers = new HashSet<>();
                }
                this.occupiedComputers.add(pc);
                pc.setOccupiedBy(this);
            }
        }

        public void removeOccupiedComputer(Computer pc) {
            if (pc != null && this.occupiedComputers != null) {
                this.occupiedComputers.remove(pc);
                if (this.equals(pc.getOccupiedBy())) {
                    pc.setOccupiedBy(null);
                }
            }
        }

        public void addOccupiedComputerId(Long pcId) {
            if (pcId != null) {
                Computer c = new Computer();
                c.setId(pcId);
                addOccupiedComputer(c);
            }
        }

        public void removeOccupiedComputerId(Long pcId) {
            if (pcId != null && this.occupiedComputers != null) {
                this.occupiedComputers.removeIf(c -> pcId.equals(c.getId()));
            }
        }

        public boolean hasActiveComputers() {
            return occupiedComputers != null && !occupiedComputers.isEmpty();
        }

        public int getMaxComputers() { return type.getMaxComputers(); }
        public boolean canAccess(ComputerType computerType) { return type.canAccess(computerType); }
        public double getDiscountPercent() { return type.getDiscountPercent(); }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Player player = (Player) o;
            return id != null && Objects.equals(id, player.id);
        }

        @Override
        public int hashCode() {
            return id != null ? Objects.hash(id) : Objects.hash(phone);
        }
    }
}
