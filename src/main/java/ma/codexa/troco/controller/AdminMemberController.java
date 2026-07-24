package ma.codexa.troco.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.MemberDTO;
import ma.codexa.troco.dto.PermissionDTO;
import ma.codexa.troco.dto.request.CreateMemberRequest;
import ma.codexa.troco.dto.request.ResetMemberPasswordRequest;
import ma.codexa.troco.dto.request.UpdateMemberRequest;
import ma.codexa.troco.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberController {

    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MemberDTO>>> list() {
        return ResponseEntity.ok(ApiResponse.success(memberService.listMembers()));
    }

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<PermissionDTO>>> permissions() {
        return ResponseEntity.ok(ApiResponse.success(memberService.listPermissions()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MemberDTO>> create(@Valid @RequestBody CreateMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(memberService.create(request), "Membre créé"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberRequest request) {
        return ResponseEntity.ok(ApiResponse.success(memberService.update(id, request), "Membre mis à jour"));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<MemberDTO>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(memberService.activate(id), "Compte activé"));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<MemberDTO>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(memberService.deactivate(id), "Compte désactivé"));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetMemberPasswordRequest request) {
        memberService.resetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Mot de passe mis à jour"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Compte supprimé"));
    }
}
