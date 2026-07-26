package ma.codexa.troco.dto;

import java.util.Map;

public record StoreGlobalSectionDTO(
        Long id,
        String sectionKey,
        boolean enabled,
        Map<String, Object> config
) {}
