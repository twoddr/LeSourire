package com.lesourire.serveur.crypto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Garde-fou de démarrage : le serveur refuse de se lancer si la clé active ne
 * peut pas relire les données déjà chiffrées (plutôt que d'écrire des données
 * que plus personne ne saurait lire).
 */
class VerificateurCleChiffrementTest {

    private static final String CLE_PROJET = "0HBG2wyDnlcxkpXE1bpaTpxeGva4pXTVojUuu4I3oAs=";
    private static final String CLE_AUTRE = Base64.getEncoder().encodeToString(new byte[32]);

    /** Faux JdbcTemplate : échantillon par colonne, avec panne SQL simulable. */
    private static JdbcTemplate fauxJdbc(Map<String, List<String>> echantillons,
            Set<String> colonnesEnPanne) {
        return new JdbcTemplate() {
            @Override
            public <T> List<T> queryForList(String sql, Class<T> elementType) {
                for (Map.Entry<String, List<String>> entree : echantillons.entrySet()) {
                    String[] parties = entree.getKey().split("\\.", 2);
                    if (!sql.contains("FROM " + parties[0] + " WHERE " + parties[1])) {
                        continue;
                    }
                    if (colonnesEnPanne.contains(entree.getKey())) {
                        throw new DataAccessException("colonne absente") {
                            private static final long serialVersionUID = 1L;
                        };
                    }
                    return entree.getValue().stream().map(elementType::cast).toList();
                }
                return List.of();
            }
        };
    }

    private static String chiffreAvec(String clair, String cle) {
        return Chiffrement.chiffrerAvec(clair, Chiffrement.cleDepuisBase64(cle));
    }

    private static ChiffrementConfig config(String cle, Path racine) {
        Path fichier = racine.resolve("fichiers/cle-chiffrement.key");
        try {
            java.nio.file.Files.createDirectories(fichier.getParent());
            java.nio.file.Files.writeString(fichier, cle);
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        return new ChiffrementConfig("", "", List.of(fichier), fichier);
    }

    @Test
    @DisplayName("Les données sont relues avec la bonne clé : démarrage autorisé")
    void donneesRelues(@TempDir Path racine) {
        Map<String, List<String>> echantillons = new LinkedHashMap<>();
        echantillons.put("patient.nom", List.of(chiffreAvec("Dupont", CLE_PROJET)));
        echantillons.put("rdv.motif", List.of(chiffreAvec("Détartrage", CLE_PROJET)));
        VerificateurCleChiffrement verificateur =
                new VerificateurCleChiffrement(fauxJdbc(echantillons, Set.of()), config(CLE_PROJET, racine));
        assertDoesNotThrow(() -> verificateur.verifier());
    }

    @Test
    @DisplayName("Une base sans donnée chiffrée est acceptée (installation neuve)")
    void baseNeuve(@TempDir Path racine) {
        VerificateurCleChiffrement verificateur =
                new VerificateurCleChiffrement(fauxJdbc(Map.of(), Set.of()), config(CLE_PROJET, racine));
        assertDoesNotThrow(() -> verificateur.verifier());
    }

    @Test
    @DisplayName("Une clé incompatible interdit le démarrage en nommant la colonne et l'empreinte")
    void cleIncompatible(@TempDir Path racine) {
        Map<String, List<String>> echantillons = new LinkedHashMap<>();
        echantillons.put("patient.nom", List.of(chiffreAvec("Dupont", CLE_PROJET)));
        ChiffrementConfig configuration = config(CLE_AUTRE, racine);
        VerificateurCleChiffrement verificateur =
                new VerificateurCleChiffrement(fauxJdbc(echantillons, Set.of()), configuration);
        IllegalStateException erreur = assertThrows(IllegalStateException.class,
                () -> verificateur.verifier());
        assertTrue(erreur.getMessage().contains("patient.nom"), erreur.getMessage());
        assertTrue(erreur.getMessage().contains(configuration.empreinte()), erreur.getMessage());
        assertTrue(erreur.getMessage().contains("Diagnostic-Chiffrement"), erreur.getMessage());
    }

    @Test
    @DisplayName("Une colonne illisible en SQL n'empêche pas le contrôle des autres")
    void colonneEnPanne(@TempDir Path racine) {
        Map<String, List<String>> echantillons = new LinkedHashMap<>();
        echantillons.put("patient.nom", List.of(chiffreAvec("Dupont", CLE_PROJET)));
        VerificateurCleChiffrement verificateur = new VerificateurCleChiffrement(
                fauxJdbc(echantillons, Set.of("patient.nom")), config(CLE_PROJET, racine));
        assertDoesNotThrow(() -> verificateur.verifier());
    }
}