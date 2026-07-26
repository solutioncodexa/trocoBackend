package ma.codexa.troco.service.storage;

public record OptimizedImage(
        byte[] bytes,
        String contentType,
        String extension,
        boolean transformed
) {}
