package app.http;

import app.domain.Exceptions.ConflictException;
import app.domain.Exceptions.NotFoundException;
import app.domain.Exceptions.ValidationException;
import com.sun.net.httpserver.HttpExchange;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpHelper {

    public static final JsonMapper MAPPER = JsonMapper.builder().build();

    public static void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    public static void sendError(HttpExchange exchange, int status, String error, String message) {
        try {
            Map<String, Object> err = Map.of("status", status, "error", error, "message", message);
            sendJson(exchange, status, err);
        } catch (IOException ignored) {}
    }

    public static void sendMethodNotAllowed(HttpExchange exchange, String... allowed) {
        exchange.getResponseHeaders().set("Allow", String.join(", ", allowed));
        sendError(exchange, 405, "Method Not Allowed", "Разрешенные методы: " + String.join(", ", allowed));
    }

    public static void handleException(HttpExchange exchange, Exception e) {
        if (e instanceof ValidationException || e instanceof IllegalArgumentException) {
            sendError(exchange, 400, "Bad Request", e.getMessage());
        } else if (e instanceof NotFoundException) {
            sendError(exchange, 404, "Not Found", e.getMessage());
        } else if (e instanceof ConflictException) {
            sendError(exchange, 409, "Conflict", e.getMessage());
        } else if (e instanceof JacksonException) {
            sendError(exchange, 400, "Bad Request", "Ошибка синтаксиса JSON: " + e.getMessage());
        } else {
            sendError(exchange, 500, "Internal Server Error", "Ошибка сервера: " + e.getMessage());
        }
    }

    public static <T> T readBody(HttpExchange exchange, Class<T> clazz) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] bytes = is.readAllBytes();
            if (bytes.length == 0) throw new ValidationException("Тело запроса пустое");
            return MAPPER.readValue(bytes, clazz);
        }
    }

    public static Map<String, String> parseQuery(HttpExchange exchange) {
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) return Collections.emptyMap();
        Map<String, String> res = new HashMap<>();
        for (String pair : raw.split("&")) {
            if (pair.isBlank()) continue;
            int idx = pair.indexOf('=');
            if (idx > 0) {
                res.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
            } else {
                res.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            }
        }
        return res;
    }

    public static Long parseId(String s) {
        if (s == null || s.isBlank()) {
            throw new ValidationException("ID не может быть пустым");
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("ID должен быть числом: '" + s + "'");
        }
    }

    public static Boolean parseBooleanStrict(String val, String paramName) {
        if (val == null || val.isBlank()) return null;
        String trimmed = val.trim();
        if ("true".equalsIgnoreCase(trimmed)) {
            return Boolean.TRUE;
        } else if ("false".equalsIgnoreCase(trimmed)) {
            return Boolean.FALSE;
        } else {
            throw new ValidationException("Параметр '" + paramName + "' должен быть 'true' или 'false' (получено: '" + val + "')");
        }
    }

    public static Double parseDoubleStrict(String val, String paramName) {
        if (val == null || val.isBlank()) return null;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Параметр '" + paramName + "' должен быть числом: '" + val + "'");
        }
    }

    public static <E extends Enum<E>> E parseEnumStrict(Class<E> enumClass, String val, String paramName) {
        if (val == null || val.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Недопустимое значение параметра '" + paramName + "': '" + val + "'");
        }
    }
}
