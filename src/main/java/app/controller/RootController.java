package app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> index() {
        return Map.of(
                "club", "Zombie Playa Gaming Club API",
                "status", "ONLINE",
                "endpoints", Map.of(
                        "ping", "/ping",
                        "computers", "/api/computers",
                        "players", "/api/players"
                )
        );
    }
}
