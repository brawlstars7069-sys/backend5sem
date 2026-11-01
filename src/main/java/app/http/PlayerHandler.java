package app.http;

import app.domain.Model.Player;
import app.domain.Model.PlayerType;
import app.service.ClubService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PlayerHandler implements HttpHandler {

    public record PlayerReq(String nickname, String phone, PlayerType type) {}

    private final ClubService clubService;

    public PlayerHandler(ClubService clubService) {
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

            if (parts.length == 2) { // /players или /api/players
                if ("GET".equalsIgnoreCase(method)) {
                    Map<String, String> q = HttpHelper.parseQuery(exchange);
                    PlayerType type = HttpHelper.parseEnumStrict(PlayerType.class, q.get("type"), "type");

                    String activeParam = q.get("hasActiveComputers");
                    if (activeParam == null) {
                        activeParam = q.get("hasActiveSession");
                    }
                    Boolean hasActive = HttpHelper.parseBooleanStrict(activeParam, "hasActiveComputers");
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
            } else if (parts.length == 3) { // /players/{id} или /api/players/{id}
                Long id = HttpHelper.parseId(parts[2]);
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
            } else {
                HttpHelper.sendError(exchange, 404, "Not Found", "Неизвестный путь: " + path);
            }
        } catch (Exception e) {
            HttpHelper.handleException(exchange, e);
        }
    }
}
