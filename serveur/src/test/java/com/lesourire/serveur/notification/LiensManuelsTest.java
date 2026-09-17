package com.lesourire.serveur.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.lesourire.commun.Rappels;

/** Construction des liens d'envoi assisté (WhatsApp, e-mail). */
class LiensManuelsTest {

    private static final String CAMEROUN = "+237";

    @Test
    @DisplayName("Le lien WhatsApp utilise les chiffres du numéro et encode le texte")
    void lienWhatsapp() {
        String lien = LiensManuels.whatsapp("6 99 11 22 33", "RDV demain à 9 h & 30",
                CAMEROUN);
        assertTrue(lien.startsWith("https://wa.me/237699112233?text="), lien);
        // Les espaces sont encodés en %20 (et non en « + », que wa.me n'interprète pas)
        assertTrue(lien.contains("%20"), lien);
        assertTrue(lien.contains("%26"), lien);
    }

    @Test
    @DisplayName("Un numéro inexploitable ne produit pas de lien WhatsApp")
    void lienWhatsappSansNumero() {
        assertNull(LiensManuels.whatsapp("non renseigné", "Bonjour", CAMEROUN));
    }

    @Test
    @DisplayName("Le lien e-mail encode sujet et corps")
    void lienEmail() {
        String lien = LiensManuels.email("aline@example.cm", "Cabinet Le Sourire",
                "Votre RDV est confirmé.");
        assertTrue(lien.startsWith("mailto:aline@example.cm?"), lien);
        assertTrue(lien.contains("subject=Cabinet%20Le%20Sourire"), lien);
        assertTrue(lien.contains("body=Votre%20RDV%20est%20confirm%C3%A9."), lien);
    }

    @Test
    @DisplayName("Le SMS n'a pas de lien assisté : il part tout seul")
    void pasDeLienPourLeSms() {
        assertNull(LiensManuels.lienEnvoi(Rappels.Canal.SMS, "699112233", "Bonjour", CAMEROUN));
    }

    @Test
    @DisplayName("Chaque canal assisté reçoit son lien")
    void lienParCanal() {
        assertEquals("https://wa.me/237699112233",
                LiensManuels.lienEnvoi(Rappels.Canal.WHATSAPP, "699112233", null, CAMEROUN));
        assertTrue(LiensManuels.lienEnvoi(Rappels.Canal.EMAIL, "a@b.cm", "Bonjour", CAMEROUN)
                .startsWith("mailto:a@b.cm"));
    }
}
