package com.lesourire.client.coeur;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Formatage des montants en francs CFA (pas de centimes en pratique). */
public final class Montants {

    private Montants() {
    }

    public static String formater(BigDecimal montant) {
        if (montant == null) {
            return "";
        }
        NumberFormat format = NumberFormat.getNumberInstance(Locale.FRENCH);
        format.setMaximumFractionDigits(montant.stripTrailingZeros().scale() > 0 ? 2 : 0);
        return format.format(montant);
    }

    public static String formaterAvecDevise(BigDecimal montant) {
        return montant == null ? "" : formater(montant) + " XAF";
    }
}
