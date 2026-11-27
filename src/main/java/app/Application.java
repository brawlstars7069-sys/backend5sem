package app;

import app.domain.Model.ComputerType;
import app.domain.Model.PlayerType;
import app.repository.ComputerRepository;
import app.service.ClubService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner initData(ClubService service, ComputerRepository computerRepository) {
        return args -> {
            if (computerRepository.findAll().isEmpty()) {
                service.createComputer("PC-01", ComputerType.STANDARD, 120.0, "192.168.1.101", "RTX 4060, 144Hz, Core i5");
                service.createComputer("PC-02", ComputerType.STANDARD, 120.0, "192.168.1.102", "RTX 4060, 144Hz, Core i5");
                service.createComputer("VIP-BOOTCAMP", ComputerType.VIP, 250.0, "192.168.1.103", "RTX 4090, 240Hz, Core i9");

                service.createPlayer("NoobZombie", "+79991112233", PlayerType.REGULAR);
                service.createPlayer("ProGamer", "+79998887766", PlayerType.VIP);
            }
        };
    }
}
