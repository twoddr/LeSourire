package com.lesourire.serveur.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Chiffrement AES-256-GCM des données sensibles. */
class ChiffrementTest {

    /** Clé livrée avec le projet (fichiers/cle-chiffrement.key). */
    private static final String CLE_PROJET = "0HBG2wyDnlcxkpXE1bpaTpxeGva4pXTVojUuu4I3oAs=";

    /** Empreinte courte attendue, telle que journalisée par le serveur. */
    private static final String EMPREINTE_PROJET = "9a09d77d4ec6";

    /** Clé quelconque mais valide (32 octets nuls), pour les tests négatifs. */
    private static final String CLE_BIDON = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    @DisplayName("Une valeur chiffrée est relue à l'identique, accents compris")
    void allerRetour() {
        SecretKeySpec cle = Chiffrement.cleDepuisBase64(CLE_PROJET);
        String clair = "Allergie pénicilline — éruption, Dr Ngo Bell";
        String chiffre = Chiffrement.chiffrerAvec(clair, cle);
        assertTrue(chiffre.startsWith(Chiffrement.PREFIXE));
        assertEquals(clair, Chiffrement.dechiffrerAvec(chiffre, cle));
        assertTrue(Chiffrement.estChiffre(chiffre));
    }

    @Test
    @DisplayName("Deux chiffrements de la même valeur diffèrent (IV aléatoire)")
    void ivAleatoire() {
        SecretKeySpec cle = Chiffrement.cleDepuisBase64(CLE_PROJET);
        assertNotEquals(Chiffrement.chiffrerAvec("Mbarga", cle), Chiffrement.chiffrerAvec("Mbarga", cle));
    }

    @Test
    @DisplayName("Une autre clé ne relit pas la valeur et le dit avec l'empreinte essayée")
    void mauvaiseCle() {
        SecretKeySpec bonne = Chiffrement.cleDepuisBase64(CLE_PROJET);
        SecretKeySpec autre = Chiffrement.cleDepuisBase64(CLE_BIDON);
        String chiffre = Chiffrement.chiffrerAvec("Dupont", bonne);
        assertNull(Chiffrement.tenterDechiffrer(chiffre, autre));
        assertFalse(Chiffrement.dechiffrableAvec(chiffre, autre));
        IllegalStateException erreur = assertThrows(IllegalStateException.class,
                () -> Chiffrement.dechiffrerAvec(chiffre, autre));
        assertTrue(erreur.getMessage().contains(Chiffrement.empreinte(autre)));
    }

    @Test
    @DisplayName("Les valeurs historiques en clair restent lisibles telles quelles")
    void valeursEnClair() {
        SecretKeySpec cle = Chiffrement.cleDepuisBase64(CLE_PROJET);
        assertEquals("Dupont", Chiffrement.dechiffrerAvec("Dupont", cle));
        assertEquals("Dupont", Chiffrement.tenterDechiffrer("Dupont", cle));
        assertFalse(Chiffrement.estChiffre("Dupont"));
    }

    @Test
    @DisplayName("Une valeur nulle reste nulle")
    void valeursNulles() {
        assertNull(Chiffrement.chiffrerAvec(null, Chiffrement.cleDepuisBase64(CLE_PROJET)));
        assertNull(Chiffrement.dechiffrerAvec(null, Chiffrement.cleDepuisBase64(CLE_PROJET)));
        assertNull(Chiffrement.tenterDechiffrer(null, CLE_PROJET));
    }

    @Test
    @DisplayName("L'empreinte d'une clé est stable et ne divulgue pas la clé")
    void empreinte() {
        assertEquals(EMPREINTE_PROJET, Chiffrement.empreinte(CLE_PROJET));
        assertEquals(12, Chiffrement.empreinte(CLE_PROJET).length());
        assertEquals(Chiffrement.empreinte(CLE_PROJET),
                Chiffrement.empreinte(Chiffrement.cleDepuisBase64(CLE_PROJET)));
        assertNotEquals(Chiffrement.empreinte(CLE_PROJET), Chiffrement.empreinte(CLE_BIDON));
        assertNull(Chiffrement.empreinte("pas-une-cle!"));
    }

    @Test
    @DisplayName("Les clés invalides sont refusées avec un message explicite")
    void clesInvalides() {
        assertThrows(IllegalStateException.class, () -> Chiffrement.cleDepuisBase64(null));
        assertThrows(IllegalStateException.class, () -> Chiffrement.cleDepuisBase64("   "));
        assertThrows(IllegalStateException.class, () -> Chiffrement.cleDepuisBase64("pas-une-cle!"));
        assertThrows(IllegalStateException.class,
                () -> Chiffrement.cleDepuisBase64(Base64.getEncoder().encodeToString(new byte[10])));
        assertEquals(16, Chiffrement.cleDepuisBase64(
                Base64.getEncoder().encodeToString(new byte[16])).getEncoded().length);
        assertEquals(24, Chiffrement.cleDepuisBase64(
                Base64.getEncoder().encodeToString(new byte[24])).getEncoded().length);
        assertEquals(Chiffrement.TAILLE_CLE_OCTETS, Chiffrement.cleDepuisBase64(CLE_PROJET)
                .getEncoded().length);
    }

    @Test
    @DisplayName("La clé active est celle installée par initialiser")
    void cleActive() {
        Chiffrement.initialiser(Chiffrement.cleDepuisBase64(CLE_BIDON));
        String chiffre = Chiffrement.chiffrer("Dossier médical");
        assertEquals("Dossier médical", Chiffrement.dechiffrer(chiffre));
        assertEquals(Chiffrement.cleDepuisBase64(CLE_BIDON), Chiffrement.cleCourante());
        assertNull(Chiffrement.tenterDechiffrer(chiffre, CLE_PROJET));
        Chiffrement.initialiser(Chiffrement.cleDepuisBase64(CLE_PROJET));
    }
}