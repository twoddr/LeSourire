package com.lesourire.serveur.outils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.lesourire.serveur.crypto.Chiffrement;

/**
 * Outil de secours : analyse des options, identification des clés et répartition
 * des valeurs entre les clés (sans base de données).
 */
class DiagnosticChiffrementTest {

    private static final String CLE_PROJET = "0HBG2wyDnlcxkpXE1bpaTpxeGva4pXTVojUuu4I3oAs=";
    private static final String CLE_AUTRE =
            java.util.Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                    11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
                    31, 32});

    private static ChercheurCles.CleCandidate candidate(String base64, String origine) {
        return new ChercheurCles.CleCandidate(base64, origine);
    }

    private static String chiffreAvec(String clair, String cle) {
        return Chiffrement.chiffrerAvec(clair, Chiffrement.cleDepuisBase64(cle));
    }

    @Test
    @DisplayName("Les options sont analysées, les valeurs manquantes ou inconnues refusées")
    void options() {
        OptionsDiagnostic options = OptionsDiagnostic.analyser("--rapport", "rapport.txt",
                "--limite", "50", "--profondeur", "8", "--afficher-cles", "--je-confirme");
        assertEquals("rapport.txt", options.rapport().toString());
        assertEquals(50, options.limite());
        assertEquals(8, options.profondeur());
        assertTrue(options.afficherCles());
        assertTrue(options.confirme());
        assertFalse(options.forcer());
        assertTrue(OptionsDiagnostic.analyser("--aide").aide());
        assertThrows(IllegalArgumentException.class, () -> OptionsDiagnostic.analyser("--limite"));
        assertThrows(IllegalArgumentException.class, () -> OptionsDiagnostic.analyser("--limite", "zéro"));
        assertThrows(IllegalArgumentException.class, () -> OptionsDiagnostic.analyser("--inconnue"));
    }

    @Test
    @DisplayName("Sans option d'exploration, les emplacements probables de la machine sont utilisés")
    void racinesParDefaut() {
        OptionsDiagnostic options = OptionsDiagnostic.analyser();
        assertFalse(options.racines().isEmpty());
        assertTrue(OptionsDiagnostic.analyser("--sans-parcours").racines().isEmpty());
    }

    @Test
    @DisplayName("Une clé est acceptée en base64 ou par le chemin de son fichier")
    void resolutionCle(@TempDir Path dossier) throws IOException {
        assertEquals(CLE_PROJET, MoteurDiagnostic.resoudreCle(CLE_PROJET));
        assertEquals(CLE_PROJET, MoteurDiagnostic.resoudreCle("  " + CLE_PROJET + "  "));
        Path fichier = dossier.resolve("cle-chiffrement.key");
        Files.writeString(fichier, CLE_PROJET + "\n", StandardCharsets.UTF_8);
        assertEquals(CLE_PROJET, MoteurDiagnostic.resoudreCle(fichier.toString()));
        assertNull(MoteurDiagnostic.resoudreCle("pas-une-cle!"));
        assertNull(MoteurDiagnostic.resoudreCle(null));
        assertNull(MoteurDiagnostic.resoudreCle(dossier.resolve("introuvable.key").toString()));
    }

    @Test
    @DisplayName("L'analyse ventile chaque valeur vers la clé qui la relit")
    void analyse() {
        List<MoteurDiagnostic.Valeur> valeurs = List.of(
                new MoteurDiagnostic.Valeur(1L, "patient", "nom", chiffreAvec("Dupont", CLE_PROJET)),
                new MoteurDiagnostic.Valeur(2L, "patient", "nom", chiffreAvec("Mbarga", CLE_PROJET)),
                new MoteurDiagnostic.Valeur(3L, "patient", "prenom", chiffreAvec("Nadine", CLE_AUTRE)),
                new MoteurDiagnostic.Valeur(4L, "rdv", "motif", "enc:v1:pasunvraichiffre"),
                new MoteurDiagnostic.Valeur(5L, "rdv", "notes", "Ancienne valeur en clair"));
        MoteurDiagnostic.Analyse analyse = MoteurDiagnostic.analyser(valeurs,
                List.of(candidate(CLE_PROJET, "fichier projet"), candidate(CLE_AUTRE, "ancienne clé")));

        assertEquals(2, analyse.usageParCle().get(CLE_PROJET));
        assertEquals(1, analyse.usageParCle().get(CLE_AUTRE));
        assertFalse(analyse.toutesLisibles());
        assertEquals(1, analyse.orphelines().size());
        assertTrue(analyse.orphelines().keySet().iterator().next().startsWith("rdv.motif"),
                analyse.orphelines().keySet().toString());
        assertEquals(2, analyse.colonnes().get("patient.nom").valeursChiffrees());
        assertEquals(0, analyse.colonnes().get("patient.nom").valeursIlisibles());
        assertEquals(0, analyse.colonnes().get("patient.prenom").valeursIlisibles());
        assertEquals(0, MoteurDiagnostic.colonne(analyse.colonnes(), "acte", "observations")
                .valeursTotales(), "colonne sans donnée chiffrée → colonne vide");
        assertTrue(MoteurDiagnostic.colonne(analyse.colonnes(), "acte", "observations").saine());
        assertEquals(0, MoteurDiagnostic.colonne(analyse.colonnes(), "rdv", "notes")
                .valeursTotales(), "une valeur en clair n'entre pas dans l'analyse");
        assertNotNull(MoteurDiagnostic.candidate(List.of(candidate(CLE_PROJET, "x")), CLE_PROJET));
        assertNull(MoteurDiagnostic.candidate(List.of(), CLE_PROJET));
    }

    @Test
    @DisplayName("Les valeurs en clair ne sont pas comptées comme données chiffrées")
    void valeursEnClairIgnorees() {
        MoteurDiagnostic.Analyse analyse = MoteurDiagnostic.analyser(List.of(), List.of());
        assertTrue(analyse.toutesLisibles());
        assertTrue(analyse.valeurs().isEmpty());
    }
}