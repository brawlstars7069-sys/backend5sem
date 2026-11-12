package app;

import app.domain.Model.ComputerType;
import app.domain.Model.PlayerType;
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
    public CommandLineRunner initData(ClubService service) {
        return args -> {
            service.createComputer("PC-01", ComputerType.STANDARD, 120.0);
            service.createComputer("PC-02", ComputerType.STANDARD, 120.0);
            service.createComputer("VIP-BOOTCAMP", ComputerType.VIP, 250.0);

            service.createPlayer("NoobZombie", "+79991112233", PlayerType.REGULAR);
            service.createPlayer("ProGamer", "+79998887766", PlayerType.VIP);
        };
    }
}
