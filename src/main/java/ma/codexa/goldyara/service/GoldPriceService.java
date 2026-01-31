package ma.codexa.goldyara.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.dto.GoldPriceDTO;
import ma.codexa.goldyara.dto.GoldPricePointDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class GoldPriceService {

    private static final String OR_FR_URL = "https://or.fr/api/historical-spot-prices?metal=XAU&currency=MAD&weight_unit=oz";
    private static final String GOLDBROKER_URL = "https://goldbroker.com/api/spot-prices?metal=XAU&currency=MAD&weight_unit=oz&boundaries=1";
    private static final RestClient REST_CLIENT = RestClient.create();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Cacheable(value = "goldPriceHistory", unless = "#result == null")
    public GoldPriceDTO getGoldPrices() {
        try {
            return fetchFromOrFr();
        } catch (Exception e) {
            log.warn("or.fr API unavailable: {}. Falling back to goldbroker.com", e.getMessage());
            try {
                return fetchFromGoldbroker();
            } catch (Exception ex) {
                log.error("Both gold price APIs failed. or.fr: {}, goldbroker: {}", e.getMessage(), ex.getMessage());
                throw new RuntimeException("Impossible de récupérer les prix de l'or. Veuillez réessayer plus tard.");
            }
        }
    }

    private GoldPriceDTO fetchFromOrFr() {
        ResponseEntity<String> response = REST_CLIENT.get()
                .uri(OR_FR_URL)
                .retrieve()
                .toEntity(String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("or.fr returned " + response.getStatusCode());
        }

        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Invalid or.fr JSON: " + e.getMessage());
        }
        JsonNode embedded = root.get("_embedded");
        if (embedded == null || !embedded.has("items")) {
            throw new RuntimeException("Invalid or.fr response: missing _embedded.items");
        }

        JsonNode items = embedded.get("items");
        List<GoldPricePointDTO> history = new ArrayList<>();
        double lastClose = 0;

        for (JsonNode item : items) {
            if (item.has("close")) {
                double close = item.get("close").asDouble();
                lastClose = close;
                String date = item.has("date") ? item.get("date").asText() : "";
                if (!date.isEmpty()) {
                    history.add(new GoldPricePointDTO(date.substring(0, Math.min(10, date.length())), close));
                }
            }
        }

        return new GoldPriceDTO(
                lastClose,
                root.path("currency").asText("MAD"),
                root.path("metal").asText("XAU"),
                root.path("weight_unit").asText("oz"),
                "or.fr",
                history
        );
    }

    private GoldPriceDTO fetchFromGoldbroker() {
        ResponseEntity<String> response = REST_CLIENT.get()
                .uri(GOLDBROKER_URL)
                .retrieve()
                .toEntity(String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("goldbroker returned " + response.getStatusCode());
        }

        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Invalid goldbroker JSON: " + e.getMessage());
        }
        JsonNode embedded = root.get("_embedded");
        if (embedded == null || !embedded.has("items")) {
            throw new RuntimeException("Invalid goldbroker response: missing _embedded.items");
        }

        JsonNode items = embedded.get("items");
        List<GoldPricePointDTO> history = new ArrayList<>();
        double lastPrice = 0;

        for (JsonNode item : items) {
            double price = 0;
            if (item.has("mid")) {
                price = item.get("mid").asDouble();
            } else if (item.has("value")) {
                price = item.get("value").asDouble();
            }
            if (price > 0) {
                lastPrice = price;
                String date = item.has("date") ? item.get("date").asText() : "";
                if (!date.isEmpty()) {
                    String dateStr = date.length() >= 10 ? date.substring(0, 10) : date;
                    history.add(new GoldPricePointDTO(dateStr, price));
                }
            }
        }

        return new GoldPriceDTO(
                lastPrice,
                root.path("currency").asText("MAD"),
                root.path("metal").asText("XAU"),
                root.path("weight_unit").asText("oz"),
                "goldbroker.com",
                history
        );
    }
}
