package app.domain;

public class Exceptions {

    public static class DomainException extends RuntimeException {
        public DomainException(String message) { super(message); }
    }

    public static class NotFoundException extends DomainException {
        public NotFoundException(String entity, Long id) {
            super(entity + " с ID " + id + " не найден");
        }
    }

    public static class ConflictException extends DomainException {
        public ConflictException(String message) { super(message); }
    }

    public static class ValidationException extends DomainException {
        public ValidationException(String message) { super(message); }
    }
}
