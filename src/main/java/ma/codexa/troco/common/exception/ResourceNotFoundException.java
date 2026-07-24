package ma.codexa.troco.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
    
    public ResourceNotFoundException(String resourceName, Object id) {
        super(String.format("%s non trouvé(e) avec l'id: %s", resourceName, id), 
              HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
