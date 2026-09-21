package com.lesourire.serveur.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Sauvegarde de la base : décomposition de l'URL, noms des fichiers et
 * recherche de {@code mysqldump} (le point qui provoquait « Erreur 500 » sur
 * les postes où MariaDB n'est pas dans le PATH de Windows).
 */
class SauvegardeMysqldumpTest {

    @Test
    @DisplayName("L'URL JDBC est décomposée en hôte, port et base")
    void analyserUrl() {
        SauvegardeMysqldump.Coordonnees avecPort =
                SauvegardeMysqldump.analyserUrl("jdbc:mariadb://serveur:3307/lesourire");
        assertEquals("serveur", avecPort.hote());
        assertEquals("3307", avecPort.port());
        assertEquals("lesourire", avecPort.base());

        SauvegardeMysqldump.Coordonnees sansPort =
                SauvegardeMysqldump.analyserUrl("jdbc:mariadb://localhost/lesourire");
        assertEquals("localhost", sansPort.hote());
        assertEquals("3306", sansPort.port());
        assertEquals("lesourire", sansPort.base());

        assertEquals("lesourire", SauvegardeMysqldump
                .analyserUrl("jdbc:mariadb://localhost:3306/lesourire?useUnicode=true").base());
    }

    @Test
    @DisplayName("Une URL non reconnue est refusée avec un message explicite")
    void urlInvalide() {
        IllegalStateException erreur = assertThrows(IllegalStateException.class,
                () -> SauvegardeMysqldump.analyserUrl("jdbc:mysql://localhost/lesourire"));
        assertTrue(erreur.getMessage().contains("URL JDBC"), erreur.getMessage());
        assertThrows(IllegalStateException.class, () -> SauvegardeMysqldump.analyserUrl(null));
    }

    @Test
    @DisplayName("Le dump et la copie de la clé portent des noms associés")
    void nomsFichiers() {
        String nom = SauvegardeMysqldump.nomFichier(LocalDateTime.of(2026, 9, 21, 7, 30, 0));
        assertEquals("lesourire-20260921-073000.sql", nom);
        assertEquals("lesourire-20260921-073000.cle-chiffrement.key", SauvegardeMysqldump.nomCle(nom));
        assertEquals("lesourire-20260921-073000.cle-chiffrement.key",
                SauvegardeMysqldump.nomCle("lesourire-20260921-073000.sql.gz"));
    }

    @Test
    @DisplayName("Un chemin explicite de mysqldump est respecté, un chemin faux est refusé")
    void cheminExplicite(@TempDir Path dossier) throws IOException {
        Path faux = dossier.resolve("mysqldump");
        Files.writeString(faux, "#!/bin/sh\n", StandardCharsets.UTF_8);
        assertEquals(faux, SauvegardeMysqldump.trouverMysqldump(faux.toString()));

        Path absent = dossier.resolve("absent").resolve("mysqldump");
        IllegalStateException erreur = assertThrows(IllegalStateException.class,
                () -> SauvegardeMysqldump.trouverMysqldump(absent.toString()));
        assertTrue(erreur.getMessage().contains("emplacement indiqué"), erreur.getMessage());
    }

    @Test
    @DisplayName("Sans chemin configuré : le programme est trouvé, sinon le message dit quoi faire")
    void rechercheAutomatique() {
        try {
            Path trouve = SauvegardeMysqldump.trouverMysqldump(null);
            assertNotNull(trouve);
            assertTrue(trouve.getFileName().toString().startsWith("mysqldump"),
                    "programme inattendu : " + trouve);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("PATH"), e.getMessage());
            assertTrue(e.getMessage().contains("LESOURIRE_MYSQLDUMP"), e.getMessage());
        }
    }

    @Test
    @DisplayName("La clé est copiée à côté du dump ; son absence est signalée sans échec")
    void copieCle(@TempDir Path dossier) throws IOException {
        Path cle = dossier.resolve("fichiers/cle-chiffrement.key");
        Files.createDirectories(cle.getParent());
        Files.writeString(cle, "0HBG2wyDnlcxkpXE1bpaTpxeGva4pXTVojUuu4I3oAs=", StandardCharsets.UTF_8);
        Path dump = dossier.resolve("lesourire-20260921-073000.sql");
        Files.writeString(dump, "-- dump", StandardCharsets.UTF_8);

        Path copie = SauvegardeMysqldump.copierCle(cle, dump);
        assertNotNull(copie);
        assertEquals("lesourire-20260921-073000.cle-chiffrement.key", copie.getFileName().toString());
        assertEquals(Files.readString(cle), Files.readString(copie));

        assertNull(SauvegardeMysqldump.copierCle(dossier.resolve("absente.key"), dump));
    }
}