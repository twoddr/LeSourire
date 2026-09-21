package com.lesourire.serveur.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Résolution de la clé de chiffrement au démarrage du serveur. */
class ChiffrementConfigTest {

    private static final String CLE_PROJET = "0HBG2wyDnlcxkpXE1bpaTpxeGva4pXTVojUuu4I3oAs=";
    private static final String EMPREINTE_PROJET = "9a09d77d4ec6";
    private static final String CLE_AUTRE = Base64.getEncoder().encodeToString(new byte[32]);

    private final String proprieteOrigine = System.getProperty(CheminsInstallation.PROPRIETE_HOME);

    @AfterEach
    void restaurerPropriete() {
        if (proprieteOrigine == null) {
            System.clearProperty(CheminsInstallation.PROPRIETE_HOME);
        } else {
            System.setProperty(CheminsInstallation.PROPRIETE_HOME, proprieteOrigine);
        }
    }

    private static Path ecrire(Path fichier, String contenu) throws IOException {
        Files.createDirectories(fichier.getParent());
        Files.writeString(fichier, contenu, StandardCharsets.UTF_8);
        return fichier;
    }

    private static Path cleDuPaquet(Path racine) {
        return racine.resolve("fichiers/cle-chiffrement.key");
    }

    @Test
    @DisplayName("La clé du paquet est lue et devient la clé active")
    void cleLueDansLeFichierDuPaquet(@TempDir Path racine) throws IOException {
        Path cle = ecrire(cleDuPaquet(racine), CLE_PROJET);
        ChiffrementConfig config = new ChiffrementConfig("", "", List.of(cle), cle);
        assertFalse(config.cleGeneree());
        assertEquals(EMPREINTE_PROJET, config.empreinte());
        assertEquals(cle, config.cheminFichier());
        assertTrue(config.origine().contains("cle-chiffrement.key"));
        assertEquals("Dupont", Chiffrement.dechiffrer(Chiffrement.chiffrer("Dupont")));
        assertEquals(EMPREINTE_PROJET, Chiffrement.empreinte(Chiffrement.cleCourante()));
    }

    @Test
    @DisplayName("Sans clé existante, une clé est générée dans le paquet et signalée comme telle")
    void cleGenereeEnDernierRecours(@TempDir Path racine) throws IOException {
        Path attendu = cleDuPaquet(racine);
        ChiffrementConfig config = new ChiffrementConfig("", "", List.of(attendu), attendu);
        assertTrue(config.cleGeneree());
        assertEquals("fichier généré " + attendu, config.origine());
        assertTrue(Files.isRegularFile(attendu), "la clé générée doit être écrite sur le disque");
        String contenu = Files.readString(attendu, StandardCharsets.UTF_8).trim();
        assertEquals(Chiffrement.TAILLE_CLE_OCTETS,
                Chiffrement.cleDepuisBase64(contenu).getEncoded().length);
        assertEquals(Chiffrement.empreinte(contenu), Chiffrement.empreinte(Chiffrement.cleCourante()));
    }

    @Test
    @DisplayName("La clé fournie par la configuration prime sur les fichiers")
    void cleDeConfigurationPrioritaire(@TempDir Path racine) throws IOException {
        Path cle = ecrire(cleDuPaquet(racine), CLE_PROJET);
        ChiffrementConfig config = new ChiffrementConfig(CLE_AUTRE, "", List.of(cle), cle);
        assertFalse(config.cleGeneree());
        assertEquals(Chiffrement.empreinte(CLE_AUTRE), config.empreinte());
        assertNull(config.cheminFichier());
        assertTrue(config.origine().contains("configuration"));
    }

    @Test
    @DisplayName("Un chemin de clé relatif est résolu depuis la racine du paquet")
    void cheminRelatifResoluParLaRacine(@TempDir Path racine) throws IOException {
        System.setProperty(CheminsInstallation.PROPRIETE_HOME, racine.toString());
        ecrire(cleDuPaquet(racine), CLE_PROJET);
        ChiffrementConfig config = new ChiffrementConfig("", "fichiers/cle-chiffrement.key",
                List.of(), cleDuPaquet(racine));
        assertEquals(EMPREINTE_PROJET, config.empreinte());
        assertEquals(cleDuPaquet(racine).toAbsolutePath().normalize(), config.cheminFichier());
    }

    @Test
    @DisplayName("Face à plusieurs clés différentes, la plus prioritaire est utilisée")
    void ambiguiteUtiliseLaPremiere(@TempDir Path racine) throws IOException {
        Path ancienne = ecrire(racine.resolve("serveur/fichiers/cle-chiffrement.key"), CLE_AUTRE);
        Path paquet = ecrire(cleDuPaquet(racine), CLE_PROJET);
        ChiffrementConfig config = new ChiffrementConfig("", "", List.of(paquet, ancienne), paquet);
        assertEquals(EMPREINTE_PROJET, config.empreinte());
        assertEquals(paquet, config.cheminFichier());
    }

    @Test
    @DisplayName("Un fichier de clé vide ou invalide arrête le démarrage avec un message clair")
    void cleInvalideRefusee(@TempDir Path racine) throws IOException {
        Path vide = ecrire(cleDuPaquet(racine), "  \n");
        IllegalStateException erreur = assertThrows(IllegalStateException.class,
                () -> new ChiffrementConfig("", "", List.of(vide), vide));
        assertTrue(erreur.getMessage().contains("vide"), erreur.getMessage());

        Path invalide = ecrire(racine.resolve("serveur/fichiers/cle-chiffrement.key"), "pas-une-cle!");
        IllegalStateException erreur2 = assertThrows(IllegalStateException.class,
                () -> new ChiffrementConfig("", "", List.of(invalide), invalide));
        assertTrue(erreur2.getMessage().contains(invalide.toString()), erreur2.getMessage());
    }
}