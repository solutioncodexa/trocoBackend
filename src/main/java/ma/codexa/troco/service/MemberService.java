package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.common.exception.ResourceNotFoundException;
import ma.codexa.troco.dto.MemberDTO;
import ma.codexa.troco.dto.PermissionDTO;
import ma.codexa.troco.dto.request.CreateMemberRequest;
import ma.codexa.troco.dto.request.ResetMemberPasswordRequest;
import ma.codexa.troco.dto.request.UpdateMemberRequest;
import ma.codexa.troco.entity.User;
import ma.codexa.troco.repository.PermissionRepository;
import ma.codexa.troco.repository.RefreshTokenRepository;
import ma.codexa.troco.repository.UserRepository;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.security.service.PermissionCheckService;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionCheckService permissionCheckService;
    private final AuditLogService auditLog;
    private final PlanEntitlementService planEntitlementService;

    @Transactional(readOnly = true)
    public List<MemberDTO> listMembers() {
        // SUPER_ADMIN (bypass) : tous les comptes non-clients.
        if (TenantContext.isBypass()) {
            return userRepository.findAll().stream()
                    .filter(u -> !"CUSTOMER".equalsIgnoreCase(u.getRole()))
                    .map(this::toDto)
                    .toList();
        }
        Long fid = TenantContext.requireFournisseurId();
        return userRepository.findStoreMembers(fid).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionDTO> listPermissions() {
        return permissionRepository.findAllByOrderByCategoryAscLabelAsc().stream()
                .map(p -> new PermissionDTO(p.getCode(), p.getLabel(), p.getCategory(), p.getDescription()))
                .toList();
    }

    public MemberDTO create(CreateMemberRequest request) {
        planEntitlementService.assertCanCreateStaff();
        Long fid = TenantContext.requireFournisseurId();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Un compte existe déjà avec cet email", HttpStatus.CONFLICT);
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setRole("STAFF");
        user.setFournisseurId(fid);
        user.setActive(request.isActive());
        user.setPermissionCodes(sanitizePermissions(request.getPermissions()));
        user = userRepository.save(user);
        auditLog.record(AuditLogService.Action.MEMBER_CREATE, "USER", String.valueOf(user.getId()),
                "Création membre " + user.getEmail());
        return toDto(user);
    }

    public MemberDTO update(Long id, UpdateMemberRequest request) {
        User user = requireMemberInTenant(id);
        if ("ADMIN".equalsIgnoreCase(user.getRole()) && Boolean.FALSE.equals(request.getActive())) {
            assertNotLastActiveAdmin(user);
        }
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }
        if (request.getPermissions() != null && "STAFF".equalsIgnoreCase(user.getRole())) {
            user.setPermissionCodes(sanitizePermissions(request.getPermissions()));
        }
        user = userRepository.save(user);
        auditLog.record(AuditLogService.Action.MEMBER_UPDATE, "USER", String.valueOf(user.getId()),
                "Mise à jour membre " + user.getEmail());
        return toDto(user);
    }

    public MemberDTO activate(Long id) {
        User user = requireMemberInTenant(id);
        user.setActive(true);
        userRepository.save(user);
        auditLog.record(AuditLogService.Action.MEMBER_ACTIVATE, "USER", String.valueOf(id),
                "Activation " + user.getEmail());
        return toDto(user);
    }

    public MemberDTO deactivate(Long id) {
        User user = requireMemberInTenant(id);
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            assertNotLastActiveAdmin(user);
        }
        user.setActive(false);
        userRepository.save(user);
        auditLog.record(AuditLogService.Action.MEMBER_DEACTIVATE, "USER", String.valueOf(id),
                "Désactivation " + user.getEmail());
        return toDto(user);
    }

    public void resetPassword(Long id, ResetMemberPasswordRequest request) {
        User user = requireMemberInTenant(id);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        auditLog.record(AuditLogService.Action.MEMBER_PASSWORD_RESET, "USER", String.valueOf(id),
                "Reset mot de passe " + user.getEmail());
    }

    public void delete(Long id) {
        User user = requireMemberInTenant(id);
        User current = permissionCheckService.currentUser()
                .orElseThrow(() -> new BusinessException("Non authentifié", HttpStatus.UNAUTHORIZED));
        if (current.getId().equals(user.getId())) {
            throw new BusinessException("Vous ne pouvez pas supprimer votre propre compte", HttpStatus.BAD_REQUEST);
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            Long fid = user.getFournisseurId();
            long adminCount = fid == null
                    ? userRepository.findByRole("ADMIN").size()
                    : userRepository.findByFournisseurIdAndRoleIgnoreCase(fid, "ADMIN").size();
            if (adminCount <= 1) {
                throw new BusinessException("Impossible de supprimer le dernier administrateur", HttpStatus.BAD_REQUEST);
            }
        }
        String email = user.getEmail();
        refreshTokenRepository.deleteByUserId(user.getId());
        if (user.getPermissionCodes() != null) {
            user.getPermissionCodes().clear();
        }
        userRepository.delete(user);
        auditLog.record(AuditLogService.Action.MEMBER_DELETE, "USER", String.valueOf(id),
                "Suppression compte " + email);
        log.info("member_deleted id={} email={}", id, email);
    }

    /**
     * Charge un membre et refuse tout accès cross-tenant
     * (sauf SUPER_ADMIN en bypass).
     */
    private User requireMemberInTenant(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membre", id));
        if ("CUSTOMER".equalsIgnoreCase(user.getRole())) {
            throw new BusinessException("Compte client non gérable ici", HttpStatus.BAD_REQUEST);
        }
        if ("SUPER_ADMIN".equalsIgnoreCase(user.getRole()) && !TenantContext.isBypass()) {
            throw new ResourceNotFoundException("Membre", id);
        }
        if (!TenantContext.isBypass()) {
            Long fid = TenantContext.requireFournisseurId();
            if (user.getFournisseurId() == null || !fid.equals(user.getFournisseurId())) {
                throw new ResourceNotFoundException("Membre", id);
            }
        }
        return user;
    }

    private void assertNotLastActiveAdmin(User user) {
        Long fid = user.getFournisseurId();
        long activeAdmins = fid == null
                ? userRepository.findByRole("ADMIN").stream().filter(User::isActive).count()
                : userRepository.countActiveAdmins(fid);
        if (activeAdmins <= 1) {
            throw new BusinessException("Impossible de désactiver le dernier administrateur", HttpStatus.BAD_REQUEST);
        }
    }

    private Set<String> sanitizePermissions(List<String> requested) {
        Set<String> allowed = new HashSet<>(AppPermissions.ALL);
        if (requested == null || requested.isEmpty()) {
            return new HashSet<>();
        }
        Set<String> cleaned = requested.stream()
                .filter(allowed::contains)
                .collect(Collectors.toCollection(HashSet::new));
        return AppPermissions.expand(cleaned);
    }

    private MemberDTO toDto(User user) {
        // ADMIN = toutes les permissions côté UI — ne pas gonfler la liste.
        java.util.List<String> permissions = "STAFF".equalsIgnoreCase(user.getRole())
                ? permissionCheckService.resolvePermissions(user)
                : java.util.List.of();
        return MemberDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .permissions(permissions)
                .build();
    }
}
