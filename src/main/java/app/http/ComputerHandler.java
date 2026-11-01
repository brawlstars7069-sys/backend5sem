package app.http;

import app.domain.Model.Computer;
import app.domain.Model.ComputerStatus;
import app.domain.Model.ComputerType;
import app.service.ClubService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ComputerHandler implements HttpHandler {

    public record ComputerReq(String name, ComputerType type, Double hourlyRate) {}
    public record OccupyReq(Long playerId) {}

    private final ClubService clubService;

    public ComputerHandler(ClubService clubService) {
        this.clubService = clubService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }

            String sub = path;
            if (sub.startsWith("/api")) {
                sub = sub.substring(4);
            }

            String[] parts = sub.split("/");
            String method = exchange.getRequestMethod();

            if (parts.length == 2) { // /computers или /api/computers
                if ("GET".equalsIgnoreCase(method)) {
                    Map<String, String> q = HttpHelper.parseQuery(exchange);
                    ComputerType type = HttpHelper.parseEnumStrict(ComputerType.class, q.get("type"), "type");
                    ComputerStatus status = HttpHelper.parseEnumStrict(ComputerStatus.class, q.get("status"), "status");
                    Double maxRate = HttpHelper.parseDoubleStrict(q.get("maxRate"), "maxRate");

                    List<Computer> list = clubService.getComputers(type, status, maxRate);
                    HttpHelper.sendJson(exchange, 200, list);
                } else if ("POST".equalsIgnoreCase(method)) {
                    ComputerReq req = HttpHelper.readBody(exchange, ComputerReq.class);
                    Computer pc = clubService.createComputer(req.name(), req.type(), req.hourlyRate());
                    HttpHelper.sendJson(exchange, 201, pc);
                } else {
                    HttpHelper.sendMethodNotAllowed(exchange, "GET", "POST");
                }
            } else if (parts.length == 3) { // /computers/{id} или /api/computers/{id}
                Long id = HttpHelper.parseId(parts[2]);
                if ("GET".equalsIgnoreCase(method)) {
                    HttpHelper.sendJson(exchange, 200, clubService.getComputer(id));
                } else if ("PUT".equalsIgnoreCase(method)) {
                    ComputerReq req = HttpHelper.readBody(exchange, ComputerReq.class);
                    HttpHelper.sendJson(exchange, 200, clubService.updateComputer(id, req.name(), req.type(), req.hourlyRate()));
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    clubService.deleteComputer(id);
                    HttpHelper.sendNoContent(exchange);
                } else {
                    HttpHelper.sendMethodNotAllowed(exchange, "GET", "PUT", "DELETE");
                }
            } else if (parts.length == 4) { // /computers/{id}/{action} или /api/computers/{id}/{action}
                Long id = HttpHelper.parseId(parts[2]);
                String action = parts[3];

                switch (action) {
                    case "occupy" -> {
                        if (!"POST".equalsIgnoreCase(method)) {
                            HttpHelper.sendMethodNotAllowed(exchange, "POST");
                            return;
                        }
                        OccupyReq req = HttpHelper.readBody(exchange, OccupyReq.class);
                        HttpHelper.sendJson(exchange, 200, clubService.occupyComputer(id, req.playerId()));
                    }
                    case "release" -> {
                        if (!"POST".equalsIgnoreCase(method)) {
                            HttpHelper.sendMethodNotAllowed(exchange, "POST");
                            return;
                        }
                        HttpHelper.sendJson(exchange, 200, clubService.releaseComputer(id));
                    }
                    case "maintenance" -> {
                        if (!"POST".equalsIgnoreCase(method)) {
                            HttpHelper.sendMethodNotAllowed(exchange, "POST");
                            return;
                        }
                        HttpHelper.sendJson(exchange, 200, clubService.sendToMaintenance(id));
                    }
                    case "complete-maintenance" -> {
                        if (!"POST".equalsIgnoreCase(method)) {
                            HttpHelper.sendMethodNotAllowed(exchange, "POST");
                            return;
                        }
                        HttpHelper.sendJson(exchange, 200, clubService.completeMaintenance(id));
                    }
                    default -> HttpHelper.sendError(exchange, 404, "Not Found", "Неизвестное действие: " + action);
                }
            } else {
                HttpHelper.sendError(exchange, 404, "Not Found", "Неизвестный путь: " + path);
            }
        } catch (Exception e) {
            HttpHelper.handleException(exchange, e);
        }
    }
}
