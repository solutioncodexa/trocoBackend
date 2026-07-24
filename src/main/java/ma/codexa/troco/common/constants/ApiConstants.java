package ma.codexa.troco.common.constants;

public final class ApiConstants {
    
    private ApiConstants() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    // API Version
    public static final String API_VERSION = "/api/v1";
    
    // Messages de succès
    public static final String PRODUCT_CREATED = "Produit créé avec succès";
    public static final String PRODUCT_UPDATED = "Produit mis à jour avec succès";
    public static final String PRODUCT_DELETED = "Produit supprimé avec succès";
    public static final String ORDER_CREATED = "Commande créée avec succès";
    public static final String ORDER_UPDATED = "Commande mise à jour avec succès";
    public static final String CUSTOM_ORDER_CREATED = "Commande personnalisée créée avec succès";
    
    // Messages d'erreur
    public static final String PRODUCT_NOT_FOUND = "Produit non trouvé avec l'id: %s";
    public static final String ORDER_NOT_FOUND = "Commande non trouvée avec l'id: %s";
    public static final String CUSTOM_ORDER_NOT_FOUND = "Commande personnalisée non trouvée avec l'id: %s";
    public static final String CATEGORY_NOT_FOUND = "Catégorie non trouvée avec l'id: %s";
    public static final String CUSTOMER_NOT_FOUND = "Client non trouvé avec l'id: %s";
    
    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";
}
