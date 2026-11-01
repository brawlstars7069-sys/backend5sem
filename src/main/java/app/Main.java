package app;

import app.domain.Model.ComputerType;
import app.domain.Model.PlayerType;
import app.http.ComputerHandler;
import app.http.HttpHelper;
import app.http.PingHandler;
import app.http.PlayerHandler;
import app.service.ClubService;
import app.storage.ClubStorage;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        ClubStorage storage = new ClubStorage();
        ClubService service = new ClubService(storage);

        // Начальные данные компьютерного клуба "Zombie Playa"
        seedInitialData(service);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        server.createContext("/ping", new PingHandler());

        ComputerHandler computerHandler = new ComputerHandler(service);
        server.createContext("/api/computers", computerHandler);
        server.createContext("/computers", computerHandler);

        PlayerHandler playerHandler = new PlayerHandler(service);
        server.createContext("/api/players", playerHandler);
        server.createContext("/players", playerHandler);

        // Главная страница / и fallback для неизвестных маршрутов
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isBlank()) {
                Map<String, Object> welcome = Map.of(
                        "club", "Zombie Playa Gaming Club API",
                        "status", "ONLINE",
                        "endpoints", Map.of(
                                "ping", "http://localhost:" + PORT + "/ping",
                                "computers", "http://localhost:" + PORT + "/api/computers",
                                "players", "http://localhost:" + PORT + "/api/players"
                        )
                );
                HttpHelper.sendJson(exchange, 200, welcome);
            } else {
                HttpHelper.sendError(exchange, 404, "Not Found", "Маршрут не найден: " + path);
            }
        });

        server.start();
        System.out.println("Компьютерный клуб 'Zombie Playa' запущен: http://localhost:" + PORT);
    }

    private static void seedInitialData(ClubService service) {
        service.createComputer("PC-01", ComputerType.STANDARD, 120.0);
        service.createComputer("PC-02", ComputerType.STANDARD, 120.0);
        service.createComputer("VIP-BOOTCAMP", ComputerType.VIP, 250.0);

        service.createPlayer("NoobZombie", "+79991112233", PlayerType.REGULAR);
        service.createPlayer("ProGamer", "+79998887766", PlayerType.VIP);
    }
}
