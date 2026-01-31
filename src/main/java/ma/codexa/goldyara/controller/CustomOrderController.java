package ma.codexa.goldyara.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.codexa.goldyara.dto.CustomOrderDTO;
import ma.codexa.goldyara.entity.CustomOrder;
import ma.codexa.goldyara.mapper.CustomOrderMapper;
import ma.codexa.goldyara.service.CustomOrderService;
import ma.codexa.goldyara.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/custom-orders")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class CustomOrderController {

    @Autowired
    private CustomOrderService customOrderService;

    @Autowired
    private CustomOrderMapper customOrderMapper;

    @Autowired
    private FileStorageService fileStorageService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @GetMapping
    public ResponseEntity<List<CustomOrderDTO>> getAllCustomOrders() {
        List<CustomOrder> customOrders = customOrderService.getAllCustomOrders();
        return ResponseEntity.ok(customOrderMapper.toDTOList(customOrders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomOrderDTO> getCustomOrderById(@PathVariable Long id) {
        return customOrderService.getCustomOrderById(id)
                .map(customOrderMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CustomOrderDTO>> getCustomOrdersByStatus(@PathVariable String status) {
        // Convert frontend status to backend status
        String backendStatus = status.toUpperCase();
        List<CustomOrder> customOrders = customOrderService.getCustomOrdersByStatus(backendStatus);
        return ResponseEntity.ok(customOrderMapper.toDTOList(customOrders));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CustomOrderDTO> createCustomOrder(@RequestBody CustomOrderDTO customOrderDTO) {
        CustomOrder customOrder = customOrderService.createCustomOrderFromDTO(customOrderDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(customOrderMapper.toDTO(customOrder));
    }

    @PostMapping(path = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CustomOrderDTO> createCustomOrderWithImages(
            @RequestPart("order") String orderJson,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {
        try {
            CustomOrderDTO dto = OBJECT_MAPPER.readValue(orderJson, CustomOrderDTO.class);
            if (images != null && images.length > 0) {
                List<String> urls = new ArrayList<>();
                for (MultipartFile f : images) {
                    if (f != null && !f.isEmpty()) {
                        try {
                            urls.add(fileStorageService.storeFile(f));
                        } catch (Exception ignored) {
                        }
                        if (urls.size() >= 5) break;
                    }
                }
                if (!urls.isEmpty()) {
                    dto.setImageUrl(urls.get(0));
                    dto.setReferenceImageUrls(urls);
                }
            }
            CustomOrder customOrder = customOrderService.createCustomOrderFromDTO(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(customOrderMapper.toDTO(customOrder));
        } catch (Exception e) {
            throw new RuntimeException("Erreur création commande: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CustomOrderDTO> updateCustomOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {
        try {
            String status = statusUpdate.get("status");
            // Convert frontend status to backend status
            String backendStatus = status.toUpperCase();
            CustomOrder updatedCustomOrder = customOrderService.updateCustomOrderStatus(id, backendStatus);
            return ResponseEntity.ok(customOrderMapper.toDTO(updatedCustomOrder));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/price")
    public ResponseEntity<CustomOrderDTO> updateEstimatedPrice(
            @PathVariable Long id,
            @RequestBody Map<String, Double> priceUpdate) {
        try {
            Double estimatedPrice = priceUpdate.get("estimatedPrice");
            CustomOrder updatedCustomOrder = customOrderService.updateEstimatedPrice(id, estimatedPrice);
            return ResponseEntity.ok(customOrderMapper.toDTO(updatedCustomOrder));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomOrder(@PathVariable Long id) {
        try {
            customOrderService.deleteCustomOrder(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
