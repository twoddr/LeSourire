package com.lesourire.serveur.notification;

/**
 * Normalisation des numéros de téléphone (Cameroun par défaut).
 *
 * <p>Les fiches patients sont saisies « à la camerounaise » : {@code 6 99 99 99 99},
 * {@code 0699999999}, {@code 237699999999}, {@code +237 699 999 999}… L'API de
 * l'opérateur, elle, attend un numéro international. Ce normaliseur ramène donc
 * tout numéro à la forme {@code +237XXXXXXXXX}.</p>
 *
 * <p>Un numéro explicitement international ({@code +…} ou {@code 00…}) qui ne
 * commence pas par l'indicatif attendu est laissé tel quel : on ne devine pas à
 * la place du secrétariat.</p>
 */
public final class NormaliseurTelephone {

    /** Indicatif par défaut : Cameroun. */
    public static final String INDICATIF_DEFAUT = "+237";

    private NormaliseurTelephone() {
    }

    /** Numéro au format international {@code +237699999999}, ou {@code null} si vide/invalide. */
    public static String normaliser(String brut, String indicatif) {
        String chiffres = resoudre(brut, indicatif);
        return chiffres == null ? null : "+" + chiffres;
    }

    /** Chiffres seuls ({@code 237699999999}) : format attendu par {@code wa.me}. */
    public static String chiffres(String brut, String indicatif) {
        return resoudre(brut, indicatif);
    }

    private static String resoudre(String brut, String indicatif) {
        if (brut == null) {
            return null;
        }
        String texte = brut.trim();
        if (texte.isEmpty()) {
            return null;
        }
        boolean explicite = texte.startsWith("+") || texte.startsWith("00");

        String chiffres = texte.replaceAll("[^0-9]", "");
        if (chiffres.isEmpty()) {
            return null;
        }
        if (chiffres.startsWith("00")) {
            chiffres = chiffres.substring(2);
        }
        String indic = indicatifNettoye(indicatif);

        if (chiffres.startsWith(indic)) {
            String local = sansZerosInitiaux(chiffres.substring(indic.length()));
            return local.isEmpty() ? null : indic + local;
        }
        if (explicite) {
            // Numéro étranger : conservé tel quel (l'envoi échouera proprement).
            return chiffres;
        }
        String local = sansZerosInitiaux(chiffres);
        if (local.isEmpty()) {
            return null;
        }
        // Numéro local (9 chiffres, ou 8 chiffres « à l'ancienne ») : préfixé.
        if (local.length() <= 9) {
            return indic + local;
        }
        // Déjà complet mais écrit sans « + » (ex. 237699999999) : conservé.
        return chiffres;
    }

    private static String indicatifNettoye(String indicatif) {
        if (indicatif == null) {
            return chiffresIndicatif(INDICATIF_DEFAUT);
        }
        String chiffres = indicatif.replaceAll("[^0-9]", "");
        return chiffres.isEmpty() ? chiffresIndicatif(INDICATIF_DEFAUT) : chiffres;
    }

    private static String chiffresIndicatif(String indicatif) {
        return indicatif.replaceAll("[^0-9]", "");
    }

    private static String sansZerosInitiaux(String valeur) {
        int i = 0;
        while (i < valeur.length() - 1 && valeur.charAt(i) == '0') {
            i++;
        }
        return valeur.substring(i);
    }
}
