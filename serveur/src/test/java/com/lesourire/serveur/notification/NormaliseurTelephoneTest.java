package com.lesourire.serveur.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Normalisation des numéros de téléphone saisis au cabinet. */
class NormaliseurTelephoneTest {

    private static final String CAMEROUN = "+237";

    @Test
    @DisplayName("Un numéro mobile local est préfixé de l'indicatif")
    void numeroLocal() {
        assertEquals("+237699112233", NormaliseurTelephone.normaliser("699112233", CAMEROUN));
    }

    @Test
    @DisplayName("Les espaces et tirets de saisie sont ignorés")
    void numeroFormate() {
        assertEquals("+237699112233", NormaliseurTelephone.normaliser("6 99 11 22 33", CAMEROUN));
        assertEquals("+237699112233", NormaliseurTelephone.normaliser("6-99-11-22-33", CAMEROUN));
        assertEquals("+237699112233", NormaliseurTelephone.normaliser("(237) 699 11 22 33", CAMEROUN));
    }

    @Test
    @DisplayName("Un numéro déjà international n'est pas modifié")
    void numeroInternational() {
        assertEquals("+237699112233", NormaliseurTelephone.normaliser("+237699112233", CAMEROUN));
        assertEquals("+237699112233", NormaliseurTelephone.normaliser("+237 699 11 22 33", CAMEROUN));
        assertEquals("+237699112233", NormaliseurTelephone.normaliser("237699112233", CAMEROUN));
    }

    @Test
    @DisplayName("Le préfixe de composition internationale 00 est accepté")
    void prefixeZeroZero() {
        assertEquals("+237699112233", NormaliseurTelephone.normaliser("00237699112233", CAMEROUN));
    }

    @Test
    @DisplayName("Le zéro initial de l'ancien format est retiré")
    void zeroInitial() {
        assertEquals("+237699112233", NormaliseurTelephone.normaliser("0699112233", CAMEROUN));
    }

    @Test
    @DisplayName("Un numéro étranger explicite est conservé tel quel")
    void numeroEtranger() {
        assertEquals("+33612345678", NormaliseurTelephone.normaliser("+33 6 12 34 56 78", CAMEROUN));
    }

    @Test
    @DisplayName("Une saisie vide n'est pas un numéro")
    void saisieVide() {
        assertNull(NormaliseurTelephone.normaliser(null, CAMEROUN));
        assertNull(NormaliseurTelephone.normaliser("   ", CAMEROUN));
        assertNull(NormaliseurTelephone.normaliser("non renseigné", CAMEROUN));
    }

    @Test
    @DisplayName("Le format wa.me n'a ni « + » ni séparateurs")
    void formatWhatsapp() {
        assertEquals("237699112233", NormaliseurTelephone.chiffres("6 99 11 22 33", CAMEROUN));
    }
}
