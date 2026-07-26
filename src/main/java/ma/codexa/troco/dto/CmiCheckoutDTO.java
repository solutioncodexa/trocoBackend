package ma.codexa.troco.dto;

import java.util.Map;

/** Payload pour POST auto-submit vers la gateway CMI. */
public record CmiCheckoutDTO(
        String gatewayUrl,
        String oid,
        Map<String, String> fields
) {}
