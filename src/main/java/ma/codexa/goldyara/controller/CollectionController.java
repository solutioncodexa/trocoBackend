package ma.codexa.goldyara.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.codexa.goldyara.common.ApiResponse;
import ma.codexa.goldyara.dto.CollectionDTO;
import ma.codexa.goldyara.entity.Collection;
import ma.codexa.goldyara.service.CollectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
@Tag(name = "Collections", description = "API des collections")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class CollectionController {

    private final CollectionService collectionService;

    private static CollectionDTO toDTO(Collection c) {
        CollectionDTO dto = new CollectionDTO();
        dto.setId(c.getId() != null ? c.getId().toString() : null);
        dto.setName(c.getName());
        dto.setSlug(c.getSlug());
        dto.setDescription(c.getDescription());
        dto.setIsActive(c.getIsActive());
        dto.setCreatedAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        return dto;
    }

    @Operation(summary = "Liste des collections")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CollectionDTO>>> getAllCollections() {
        List<CollectionDTO> list = collectionService.getAllCollections().stream()
                .map(CollectionController::toDTO)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionDTO> getCollectionById(@PathVariable Long id) {
        return collectionService.getCollectionById(id)
                .map(CollectionController::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<CollectionDTO> getCollectionBySlug(@PathVariable String slug) {
        return collectionService.getCollectionBySlug(slug)
                .map(CollectionController::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Liste des collections actives")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CollectionDTO>>> getActiveCollections() {
        List<CollectionDTO> list = collectionService.getActiveCollections().stream()
                .map(CollectionController::toDTO)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping
    public ResponseEntity<CollectionDTO> createCollection(@RequestBody CollectionDTO dto) {
        Collection entity = new Collection();
        entity.setName(dto.getName());
        entity.setSlug(dto.getSlug() != null ? dto.getSlug() : dto.getName().toLowerCase().replaceAll("\\s+", "-"));
        entity.setDescription(dto.getDescription());
        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        Collection created = collectionService.createCollection(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CollectionDTO> updateCollection(@PathVariable Long id, @RequestBody CollectionDTO dto) {
        Collection entity = new Collection();
        entity.setName(dto.getName());
        entity.setSlug(dto.getSlug());
        entity.setDescription(dto.getDescription());
        entity.setIsActive(dto.getIsActive());
        Collection updated = collectionService.updateCollection(id, entity);
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollection(@PathVariable Long id) {
        collectionService.deleteCollection(id);
        return ResponseEntity.noContent().build();
    }
}
