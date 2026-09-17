package com.lesourire.serveur.notification;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.lesourire.commun.Rappels;

/**
 * Construit les liens d'envoi assisté (click-to-chat) utilisés par le client
 * lorsque le canal ne peut pas partir tout seul.
 *
 * <p>Ces liens sont calculés côté serveur pour que le client n'ait pas à
 * connaître le format des numéros ni celui des URI.</p>
 */
public final class LiensManuels {

    private LiensManuels() {
    }

    /** Lien {@code https://wa.me/…} pour le canal WhatsApp, sinon {@code null}. */
    public static String lienEnvoi(Rappels.Canal canal, String destinataire,
            String contenu, String indicatif) {
        if (canal == Rappels.Canal.WHATSAPP) {
            return whatsapp(destinataire, contenu, indicatif);
        }
        if (canal == Rappels.Canal.EMAIL) {
            return email(destinataire, "Cabinet Dentaire Le Sourire", contenu);
        }
        return null;
    }

    /** {@code https://wa.me/237699999999?text=…} ou {@code null} si le numéro est invalide. */
    public static String whatsapp(String numero, String message, String indicatif) {
        String chiffres = NormaliseurTelephone.chiffres(numero, indicatif);
        if (chiffres == null) {
            return null;
        }
        String url = "https://wa.me/" + chiffres;
        if (message != null && !message.isBlank()) {
            url += "?text=" + encoderPourUri(message);
        }
        return url;
    }

    /** {@code mailto:…?subject=…&body=…} (sujet/URL encodés à la RFC 6068). */
    public static String email(String adresse, String sujet, String corps) {
        if (adresse == null || adresse.isBlank()) {
            return null;
        }
        StringBuilder url = new StringBuilder("mailto:").append(adresse.trim());
        String params = "";
        if (sujet != null && !sujet.isBlank()) {
            params += "subject=" + encoderPourUri(sujet);
        }
        if (corps != null && !corps.isBlank()) {
            params += (params.isEmpty() ? "" : "&") + "body=" + encoderPourUri(corps);
        }
        return params.isEmpty() ? url.toString() : url.append('?').append(params).toString();
    }

    /**
     * Encodage URI : {@code URLEncoder} écrit les espaces en « + », ce que les
     * clients de messagerie et {@code wa.me} n'interprètent pas. On rétablit
     * le « %20 » attendu.
     */
    private static String encoderPourUri(String valeur) {
        return URLEncoder.encode(valeur, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
