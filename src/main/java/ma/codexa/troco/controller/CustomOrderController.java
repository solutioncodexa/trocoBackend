package ma.codexa.troco.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.common.PageResponse;
import ma.codexa.troco.dto.CustomOrderDTO;
import ma.codexa.troco.dto.CustomOrderListItemDTO;
import ma.codexa.troco.dto.CustomOrderStatsDTO;
import ma.codexa.troco.entity.CustomOrder;
import ma.codexa.troco.mapper.CustomOrderMapper;
import ma.codexa.troco.service.CustomOrderService;
import ma.codexa.troco.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/custom-orders")
@Validated
public class CustomOrderController {

    @Autowired
    private CustomOrderService customOrderService;

    @Autowired
    private CustomOrderMapper customOrderMapper;

    @Autowired
    private FileStorageService fileStorageService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CustomOrderListItemDTO>>> getAllCustomOrders(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Sort sort = "ASC".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Page<CustomOrder> customOrderPage = customOrderService.getCustomOrdersPage(
                status, keyword, PageRequest.of(page, size, sort));
        List<CustomOrderListItemDTO> dtos = customOrderMapper.toListItemDTOList(customOrderPage.getContent());
        PageResponse<CustomOrderListItemDTO> pageResponse = PageResponse.of(
                dtos, customOrderPage.getNumber(), customOrderPage.getSize(), customOrderPage.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<CustomOrderStatsDTO>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(customOrderService.getCustomOrderStats()));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<CustomOrderListItemDTO>>> getCustomOrdersByStatus(
            @PathVariable String status) {
        String backendStatus = status.toUpperCase();
        List<CustomOrder> customOrders = customOrderService.getCustomOrdersByStatus(backendStatus);
        return ResponseEntity.ok(ApiResponse.success(customOrderMapper.toListItemDTOList(customOrders)));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<CustomOrderDTO> getCustomOrderById(@PathVariable Long id) {
        return customOrderService.getCustomOrderById(id)
                .map(customOrderMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CustomOrderDTO> createCustomOrder(@RequestBody CustomOrderDTO customOrderDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customOrderService.createCustomOrderFromDTO(customOrderDTO));
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
            CustomOrderDTO created = customOrderService.createCustomOrderFromDTO(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            throw new RuntimeException("Erreur création commande: " + e.getMessage());
        }
    }

    @PatchMapping("/{id:\\d+}/status")
    public ResponseEntity<CustomOrderDTO> updateCustomOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {
        try {
            String status = statusUpdate.get("status");
            // Convert frontend status to backend status
            String backendStatus = status.toUpperCase();
            CustomOrderDTO updated = customOrderService.updateCustomOrderStatus(id, backendStatus);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id:\\d+}/price")
    public ResponseEntity<CustomOrderDTO> updateEstimatedPrice(
            @PathVariable Long id,
            @RequestBody Map<String, Double> priceUpdate) {
        try {
            Double estimatedPrice = priceUpdate.get("estimatedPrice");
            CustomOrderDTO updated = customOrderService.updateEstimatedPrice(id, estimatedPrice);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteCustomOrder(@PathVariable Long id) {
        try {
            customOrderService.deleteCustomOrder(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
