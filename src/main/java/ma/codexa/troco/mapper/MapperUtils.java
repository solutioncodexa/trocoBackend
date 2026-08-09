package ma.codexa.troco.mapper;

import ma.codexa.troco.entity.Image;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class MapperUtils {

    /** Date seule (listes produits, etc.). */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Date + heure locale (commandes) — évite le décalage UTC midnight → 01:00. */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static String dateToString(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DATE_FORMATTER);
    }

    public static String dateTimeToString(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    public static String productTypeToLowercase(String productType) {
        return productType != null ? productType.toLowerCase() : null;
    }

    public static String typeToProductType(String type) {
        return type != null ? type.toUpperCase() : null;
    }

    public static String styleToCategory(String style) {
        return style != null && style.equalsIgnoreCase("BELDI") ? "beldi" : "modern";
    }

    public static String categoryToStyle(String category) {
        return category != null && category.equalsIgnoreCase("beldi") ? "BELDI" : "MODERNE";
    }

    public static List<String> imagesToList(List<Image> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        return images.stream()
                .sorted((i1, i2) -> {
                    int order1 = i1.getDisplayOrder() != null ? i1.getDisplayOrder() : Integer.MAX_VALUE;
                    int order2 = i2.getDisplayOrder() != null ? i2.getDisplayOrder() : Integer.MAX_VALUE;
                    if (order1 != order2) return Integer.compare(order1, order2);
                    if (Boolean.TRUE.equals(i1.getIsPrimary())) return -1;
                    if (Boolean.TRUE.equals(i2.getIsPrimary())) return 1;
                    return 0;
                })
                .map(Image::getUrl)
                .collect(Collectors.toList());
    }

    public static List<String> stringToList(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        return List.of(str.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public static String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return String.join(",", list);
    }

    public static List<String> badgesToList(String badges) {
        if (badges == null || badges.isEmpty()) {
            return List.of();
        }
        return List.of(badges.split(","))
                .stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public static String badgesToString(List<String> badges) {
        if (badges == null || badges.isEmpty()) {
            return null;
        }
        return badges.stream()
                .map(String::toUpperCase)
                .collect(Collectors.joining(","));
    }

    public static String firstImageUrl(List<Image> images) {
        List<String> urls = imagesToList(images);
        return urls.isEmpty() ? null : urls.get(0);
    }

    /** Alias explicite pour listes (1ère image triée). */
    public static String firstImageUrlSorted(List<Image> images) {
        return firstImageUrl(images);
    }

    public static String statusToFrontend(String status) {
        if (status == null) return null;
        return switch (status.toUpperCase()) {
            case "PENDING" -> "pending";
            case "IN_REVIEW", "QUOTED", "APPROVED" -> "contacted";
            case "COMPLETED" -> "completed";
            default -> status.toLowerCase();
        };
    }

    public static String statusToLowercase(String status) {
        return status != null ? status.toLowerCase() : null;
    }
}
