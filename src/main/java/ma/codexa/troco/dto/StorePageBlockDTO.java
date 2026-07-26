package ma.codexa.troco.dto;

import java.util.Map;

public record StorePageBlockDTO(
        Long id,
        String type,
        Integer sortOrder,
        Map<String, Object> config,
        Map<String, Object> configAr,
        boolean visibleMobile,
        boolean visibleDesktop
) {}
