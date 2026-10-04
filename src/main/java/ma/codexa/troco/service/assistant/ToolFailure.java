package ma.codexa.troco.service.assistant;

/** Arguments invalides ou action impossible : le message est renvoyé au modèle et au commerçant. */
public class ToolFailure extends RuntimeException {

    public ToolFailure(String message) {
        super(message);
    }
}
