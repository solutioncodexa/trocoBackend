package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {

    private Long id;
    private String email;
    private String role;
    private String fullName;
    private Boolean active;
    private Long fournisseurId;
    private List<String> permissions = new ArrayList<>();
    /** true si le guide 1ère utilisation (version courante) a été terminé / ignoré */
    private Boolean adminGuideCompleted;

    public UserInfoDTO(Long id, String email, String role, String fullName,
                       Boolean active, List<String> permissions) {
        this(id, email, role, fullName, active, null, permissions, false);
    }

    public UserInfoDTO(Long id, String email, String role, String fullName,
                       Boolean active, Long fournisseurId, List<String> permissions) {
        this(id, email, role, fullName, active, fournisseurId, permissions, false);
    }
}
