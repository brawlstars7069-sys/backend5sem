package app.http;

import app.domain.Model.Computer;
import app.domain.Model.ComputerStatus;
import app.domain.Model.ComputerType;
import app.domain.Model.Player;
import app.domain.Model.PlayerType;
import app.domain.Model.Session;
import app.domain.Model.SessionStatus;
import app.service.ClubService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ClubHandler implements HttpHandler {

    public record ComputerReq(String name, ComputerType type, Double hourlyRate) {}
    public record PlayerReq(String nickname, String phone, PlayerType type) {}
    public record SessionReq(Long playerId, Long computerId, Integer hours) {}

    private final ClubService clubService;

    public ClubHandler(ClubService clubService) {
        this.clubService = clubService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }

            if (path.startsWith("/api/computers")) {
                handleComputers(exchange, path);
            } else if (path.startsWith("/api/players")) {
                handlePlayers(exchange, path);
            } else if (path.startsWith("/api/sessions")) {
                handleSessions(exchange, path);
            } else {
                HttpHelper.sendError(exchange, 404, "Not Found", "Эндпоинт не найден: " + path);
            }
        } catch (Exception e) {
            HttpHelper.handleException(exchange, e);
        }
    }

    private void handleComputers(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        String method = exchange.getRequestMethod();

        if (parts.length == 3) { // /api/computers
            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> q = HttpHelper.parseQuery(exchange);
                ComputerType type = q.containsKey("type") ? ComputerType.valueOf(q.get("type").toUpperCase()) : null;
                ComputerStatus status = q.containsKey("status") ? ComputerStatus.valueOf(q.get("status").toUpperCase()) : null;
                Double maxRate = q.containsKey("maxRate") ? Double.parseDouble(q.get("maxRate")) : null;
                List<Computer> list = clubService.getComputers(type, status, maxRate);
                HttpHelper.sendJson(exchange, 200, list);
            } else if ("POST".equalsIgnoreCase(method)) {
                ComputerReq req = HttpHelper.readBody(exchange, ComputerReq.class);
                Computer pc = clubService.createComputer(req.name(), req.type(), req.hourlyRate());
                HttpHelper.sendJson(exchange, 201, pc);
            } else {
                HttpHelper.sendMethodNotAllowed(exchange, "GET", "POST");
            }
        } else if (parts.length == 4) { // /api/computers/{id}
            Long id = HttpHelper.parseId(parts[3]);
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
        } else if (parts.length == 5) { // /api/computers/{id}/{action}
            Long id = HttpHelper.parseId(parts[3]);
            String action = parts[4];
            if (!"POST".equalsIgnoreCase(method)) {
                HttpHelper.sendMethodNotAllowed(exchange, "POST");
                return;
            }
            switch (action) {
                case "stop" -> HttpHelper.sendJson(exchange, 200, clubService.stopSession(id));
                case "maintenance" -> HttpHelper.sendJson(exchange, 200, clubService.sendToMaintenance(id));
                case "complete-maintenance" -> HttpHelper.sendJson(exchange, 200, clubService.completeMaintenance(id));
                default -> HttpHelper.sendError(exchange, 404, "Not Found", "Неизвестное действие: " + action);
            }
        }
    }

    private void handlePlayers(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        String method = exchange.getRequestMethod();

        if (parts.length == 3) { // /api/players
            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> q = HttpHelper.parseQuery(exchange);
                PlayerType type = q.containsKey("type") ? PlayerType.valueOf(q.get("type").toUpperCase()) : null;
                Boolean hasActive = q.containsKey("hasActiveSession") ? Boolean.parseBoolean(q.get("hasActiveSession")) : null;
                String search = q.get("search");
                List<Player> list = clubService.getPlayers(type, hasActive, search);
                HttpHelper.sendJson(exchange, 200, list);
            } else if ("POST".equalsIgnoreCase(method)) {
                PlayerReq req = HttpHelper.readBody(exchange, PlayerReq.class);
                Player p = clubService.createPlayer(req.nickname(), req.phone(), req.type());
                HttpHelper.sendJson(exchange, 201, p);
            } else {
                HttpHelper.sendMethodNotAllowed(exchange, "GET", "POST");
            }
        } else if (parts.length == 4) { // /api/players/{id}
            Long id = HttpHelper.parseId(parts[3]);
            if ("GET".equalsIgnoreCase(method)) {
                HttpHelper.sendJson(exchange, 200, clubService.getPlayer(id));
            } else if ("PUT".equalsIgnoreCase(method)) {
                PlayerReq req = HttpHelper.readBody(exchange, PlayerReq.class);
                HttpHelper.sendJson(exchange, 200, clubService.updatePlayer(id, req.nickname(), req.phone(), req.type()));
            } else if ("DELETE".equalsIgnoreCase(method)) {
                clubService.deletePlayer(id);
                HttpHelper.sendNoContent(exchange);
            } else {
                HttpHelper.sendMethodNotAllowed(exchange, "GET", "PUT", "DELETE");
            }
        }
    }

    private void handleSessions(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        String method = exchange.getRequestMethod();

        if (parts.length == 3) { // /api/sessions
            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> q = HttpHelper.parseQuery(exchange);
                SessionStatus status = q.containsKey("status") ? SessionStatus.valueOf(q.get("status").toUpperCase()) : null;
                Long playerId = q.containsKey("playerId") ? Long.parseLong(q.get("playerId")) : null;
                Long pcId = q.containsKey("computerId") ? Long.parseLong(q.get("computerId")) : null;
                List<Session> list = clubService.getSessions(status, playerId, pcId);
                HttpHelper.sendJson(exchange, 200, list);
            } else if ("POST".equalsIgnoreCase(method)) {
                SessionReq req = HttpHelper.readBody(exchange, SessionReq.class);
                Session s = clubService.startSession(req.playerId(), req.computerId(), req.hours());
                HttpHelper.sendJson(exchange, 201, s);
            } else {
                HttpHelper.sendMethodNotAllowed(exchange, "GET", "POST");
            }
        } else if (parts.length == 5 && "stop".equalsIgnoreCase(parts[4])) { // /api/sessions/{id}/stop
            Long sessionId = HttpHelper.parseId(parts[3]);
            Session s = clubService.getSessions(null, null, null).stream()
                    .filter(sess -> sess.getId().equals(sessionId))
                    .findFirst()
                    .orElseThrow(() -> new app.domain.Exceptions.NotFoundException("Сессия", sessionId));
            HttpHelper.sendJson(exchange, 200, clubService.stopSession(s.getComputerId()));
        } else {
            HttpHelper.sendError(exchange, 404, "Not Found", "Неизвестный путь сессий: " + path);
        }
    }
}
