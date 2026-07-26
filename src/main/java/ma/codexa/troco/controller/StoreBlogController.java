package ma.codexa.troco.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.StoreBlogPostDTO;
import ma.codexa.troco.dto.request.UpsertBlogPostRequest;
import ma.codexa.troco.service.StoreBlogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/store-blog")
@RequiredArgsConstructor
public class StoreBlogController {

    private final StoreBlogService storeBlogService;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<StoreBlogPostDTO>>> listPublic(
            @RequestParam(required = false, defaultValue = "fr") String lang) {
        return ResponseEntity.ok(ApiResponse.success(storeBlogService.listPublic(lang)));
    }

    @GetMapping("/public/{slug}")
    public ResponseEntity<ApiResponse<StoreBlogPostDTO>> getPublic(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(storeBlogService.getPublicBySlug(slug)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<StoreBlogPostDTO>>> list() {
        return ResponseEntity.ok(ApiResponse.success(storeBlogService.listAdmin()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<StoreBlogPostDTO>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(storeBlogService.getAdmin(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<StoreBlogPostDTO>> create(@Valid @RequestBody UpsertBlogPostRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storeBlogService.create(request), "Article créé"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<StoreBlogPostDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpsertBlogPostRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storeBlogService.update(id, request), "Article mis à jour"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        storeBlogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
