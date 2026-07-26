package ma.codexa.troco.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * Mixin multi-tenant : colonne {@code fournisseur_id} + filtre Hibernate.
 * Le {@code @FilterDef} est déclaré sur {@link ma.codexa.troco.entity.Fournisseur}.
 */
@MappedSuperclass
@Getter
@Setter
@Filter(name = TenantScoped.FILTER_NAME, condition = "fournisseur_id = :" + TenantScoped.PARAM_NAME)
public abstract class TenantScoped {

    public static final String FILTER_NAME = "fournisseurFilter";
    public static final String PARAM_NAME = "fournisseurId";

    @Column(name = "fournisseur_id")
    private Long fournisseurId;
}
