package com.lesourire.serveur.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * La clé doit être trouvée indépendamment du répertoire de travail : c'est la
 * correction de l'incident « données illisibles après réinstallation ».
 */
class CheminsInstallationTest {

    private final String proprieteOrigine = System.getProperty(CheminsInstallation.PROPRIETE_HOME);

    @AfterEach
    void restaurerPropriete() {
        if (proprieteOrigine == null) {
            System.clearProperty(CheminsInstallation.PROPRIETE_HOME);
        } else {
            System.setProperty(CheminsInstallation.PROPRIETE_HOME, proprieteOrigine);
        }
    }

    @Test
    @DisplayName("La racine transmise par le lanceur est lue dans la propriété lesourire.home")
    void racineExplicite(@TempDir Path dossier) {
        System.setProperty(CheminsInstallation.PROPRIETE_HOME, dossier.toString());
        assertEquals(dossier.toAbsolutePath().normalize(), CheminsInstallation.racineExplicite());
    }

    @Test
    @DisplayName("Les emplacements de la clé vont de fichiers/ à l'emplacement hérité serveur/fichiers/")
    void emplacementsCle(@TempDir Path dossier) {
        List<Path> emplacements = CheminsInstallation.emplacementsCle(dossier);
        assertEquals(3, emplacements.size());
        assertEquals(dossier.resolve("fichiers/cle-chiffrement.key"), emplacements.get(0));
        assertEquals(dossier.resolve("serveur/fichiers/cle-chiffrement.key"), emplacements.get(1));
        assertEquals(dossier.resolve("cle-chiffrement.key"), emplacements.get(2));
    }

    @Test
    @DisplayName("Le premier candidat est la clé du paquet, l'emplacement 0.1.x restant accepté")
    void candidats(@TempDir Path dossier) {
        System.setProperty(CheminsInstallation.PROPRIETE_HOME, dossier.toString());
        List<Path> candidats = CheminsInstallation.candidatsFichierCle();
        assertEquals(dossier.toAbsolutePath().normalize().resolve("fichiers/cle-chiffrement.key"),
                candidats.get(0));
        assertTrue(candidats.contains(dossier.toAbsolutePath().normalize()
                .resolve("serveur/fichiers/cle-chiffrement.key")));
        assertEquals(candidats.size(), new HashSet<>(candidats).size(), "candidats dédoublonnés");
    }

    @Test
    @DisplayName("Une clé générée l'est à la racine du paquet, dans fichiers/")
    void emplacementGeneration(@TempDir Path dossier) {
        System.setProperty(CheminsInstallation.PROPRIETE_HOME, dossier.toString());
        assertEquals(dossier.toAbsolutePath().normalize().resolve("fichiers/cle-chiffrement.key"),
                CheminsInstallation.emplacementGeneration());
    }

    @Test
    @DisplayName("Le dossier du programme et la racine du projet sont déterminés")
    void dossierJarEtRacineProjet() {
        assertNotNull(CheminsInstallation.dossierJar());
        Path racineProjet = CheminsInstallation.racineProjet();
        assertNotNull(racineProjet, "pom.xml attendu en développement");
        assertTrue(racineProjet.toFile().isDirectory());
    }

    @Test
    @DisplayName("La liste des parents est bornée et va du plus proche au plus lointain")
    void ancetres() {
        Path depart = Paths.get("/a/b/c/d/e/f/g/h/i");
        List<Path> parents = CheminsInstallation.ancetres(depart);
        assertEquals(5, parents.size());
        assertEquals(depart, parents.get(0));
        assertEquals(Paths.get("/a/b/c/d/e"), parents.get(4));
        Path racine = Paths.get("/").toAbsolutePath().normalize();
        assertEquals(List.of(racine), CheminsInstallation.ancetres(racine));
    }
}