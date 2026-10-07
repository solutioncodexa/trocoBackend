package ma.codexa.troco.rental;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Règles pures de la location (sans accès base) : unités, durée, bornes. */
public final class RentalRules {

    public static final String DAY = "DAY";
    public static final String WEEK = "WEEK";
    /** Fenêtre maximale interrogeable pour le calendrier de disponibilité. */
    public static final int MAX_AVAILABILITY_DAYS = 120;
    /** Durée maximale d'une réservation. */
    public static final int MAX_BOOKING_DAYS = 365;

    private RentalRules() {}

    /** Unité inconnue ou absente → jour (valeur par défaut). */
    public static String normalizeUnit(String raw) {
        if (raw == null) return DAY;
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return WEEK.equals(u) ? WEEK : DAY;
    }

    /** Nombre de jours d'une période, bornes incluses : du 10 au 10 = 1 jour. */
    public static long days(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    /** Unités facturées : 1 par jour, ou 1 par semaine commencée. */
    public static int units(String unit, LocalDate start, LocalDate end) {
        long days = days(start, end);
        return (int) (WEEK.equals(normalizeUnit(unit)) ? Math.ceil(days / 7.0) : days);
    }
}
