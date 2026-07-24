package ma.codexa.troco.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.entity.Permission;
import ma.codexa.troco.repository.PermissionRepository;
import ma.codexa.troco.security.AppPermissions;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PermissionInitializer {

    private final PermissionRepository permissionRepository;

    private static final Map<String, String[]> DEFS = new LinkedHashMap<>();

    static {
        DEFS.put(AppPermissions.PRODUCTS_VIEW, new String[]{"Voir les produits", "PRODUITS", "Consulter le catalogue produits"});
        DEFS.put(AppPermissions.PRODUCTS_CREATE, new String[]{"Créer des produits", "PRODUITS", "Ajouter de nouveaux produits"});
        DEFS.put(AppPermissions.PRODUCTS_UPDATE, new String[]{"Modifier des produits", "PRODUITS", "Modifier les produits existants"});
        DEFS.put(AppPermissions.PRODUCTS_DELETE, new String[]{"Supprimer des produits", "PRODUITS", "Supprimer des produits"});
        DEFS.put(AppPermissions.ORDERS_VIEW, new String[]{"Voir les commandes", "COMMANDES", "Consulter les commandes site"});
        DEFS.put(AppPermissions.ORDERS_UPDATE, new String[]{"Gérer les commandes", "COMMANDES", "Changer le statut / supprimer des commandes"});
        DEFS.put(AppPermissions.STOCK_VIEW, new String[]{"Voir le stock", "STOCK", "Consulter stock et historique"});
        DEFS.put(AppPermissions.STOCK_ADJUST, new String[]{"Ajuster le stock", "STOCK", "Achat, vente directe, inventaire"});
        DEFS.put(AppPermissions.CUSTOM_ORDERS_VIEW, new String[]{"Voir devis / sur-mesure", "DEVIS", "Consulter les demandes"});
        DEFS.put(AppPermissions.CUSTOM_ORDERS_UPDATE, new String[]{"Gérer devis / sur-mesure", "DEVIS", "Mettre à jour les demandes"});
        DEFS.put(AppPermissions.CATALOG_MANAGE, new String[]{"Gérer le catalogue", "CATALOGUE", "Catégories, sélectionnés, hero"});
        DEFS.put(AppPermissions.CONTENT_MANAGE, new String[]{"Gérer le contenu", "CONTENU", "Top-bar, promos, réseaux, codes promo"});
        DEFS.put(AppPermissions.STATS_VIEW, new String[]{"Voir les statistiques", "STATS", "Tableau de bord et revenus"});
        DEFS.put(AppPermissions.MEMBERS_MANAGE, new String[]{"Gérer les membres", "MEMBRES", "Créer et administrer les comptes équipe"});
        DEFS.put(AppPermissions.AUDIT_VIEW, new String[]{"Voir l'audit", "AUDIT", "Consulter l'historique des actions"});
    }

    @Bean
    @Order(1)
    public ApplicationRunner seedPermissions() {
        return args -> {
            int created = 0;
            for (Map.Entry<String, String[]> e : DEFS.entrySet()) {
                if (!permissionRepository.existsByCode(e.getKey())) {
                    Permission p = new Permission();
                    p.setCode(e.getKey());
                    p.setLabel(e.getValue()[0]);
                    p.setCategory(e.getValue()[1]);
                    p.setDescription(e.getValue()[2]);
                    permissionRepository.save(p);
                    created++;
                }
            }
            if (created > 0) {
                log.info("Seeded {} permissions", created);
            }
        };
    }
}
