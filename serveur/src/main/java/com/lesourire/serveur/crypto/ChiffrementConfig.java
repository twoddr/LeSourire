package com.lesourire.serveur.crypto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Prépare la clé AES utilisée pour chiffrer les données sensibles.
 *
 * <p>Ordre de résolution :</p>
 * <ol>
 *   <li>la propriété {@code lesourire.chiffrement.cle} (ou la variable
 *       d'environnement {@code LESOURIRE_CHIFFREMENT_CLE}) : clé en base64 ;</li>
 *   <li>la clé enregistrée dans le fichier {@code lesourire.chiffrement.fichier}
 *       (défaut : {@code fichiers/cle-chiffrement.key}) ;</li>
 *   <li>à défaut, une clé de 256 bits est générée puis enregistrée dans ce
 *       fichier.</li>
 * </ol>
 *
 * <p><strong>Sauvegardez ce fichier avec la base de données</strong> : sans lui,
 * les données chiffrées (identité et dossier médical des patients) sont
 * définitivement illisibles.</p>
 */
@Component
public class ChiffrementConfig {

    private static final Logger log = LoggerFactory.getLogger(ChiffrementConfig.class);
    private static final int TAILLE_CLE_OCTETS = 32;

    public ChiffrementConfig(
            @Value("${lesourire.chiffrement.cle:}") String cleBase64,
            @Value("${lesourire.chiffrement.fichier:fichiers/cle-chiffrement.key}") String cheminFichier) {
        Chiffrement.initialiser(resoudreCle(cleBase64, cheminFichier));
    }

    private SecretKeySpec resoudreCle(String cleBase64, String cheminFichier) {
        if (cleBase64 != null && !cleBase64.isBlank()) {
            log.info("Chiffrement des données sensibles : clé fournie par la configuration.");
            return decoder(cleBase64.trim());
        }
        Path fichier = cheminAbsolu(cheminFichier);
        try {
            if (Files.exists(fichier)) {
                log.info("Chiffrement des données sensibles : clé lue dans {}", fichier);
                return decoder(Files.readString(fichier, StandardCharsets.UTF_8).trim());
            }
            byte[] nouvelle = new byte[TAILLE_CLE_OCTETS];
            new SecureRandom().nextBytes(nouvelle);
            String encodee = Base64.getEncoder().encodeToString(nouvelle);
            if (fichier.getParent() != null) {
                Files.createDirectories(fichier.getParent());
            }
            Files.writeString(fichier, encodee, StandardCharsets.UTF_8);
            log.warn("Chiffrement des données sensibles : nouvelle clé générée dans {}. "
                    + "SAUVEGARDEZ ce fichier avec la base : sans lui, les données "
                    + "patient sont illisibles.", fichier);
            return decoder(encodee);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Impossible de lire ou d'écrire la clé de chiffrement : " + fichier, e);
        }
    }

    private SecretKeySpec decoder(String cleBase64) {
        byte[] octets;
        try {
            octets = Base64.getDecoder().decode(cleBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Clé de chiffrement illisible (base64 attendu).", e);
        }
        if (octets.length != 16 && octets.length != 24 && octets.length != TAILLE_CLE_OCTETS) {
            throw new IllegalStateException("Clé de chiffrement invalide : " + octets.length
                    + " octets (attendu 16, 24 ou 32).");
        }
        return new SecretKeySpec(octets, "AES");
    }

    private Path cheminAbsolu(String chemin) {
        Path p = Paths.get(chemin);
        return p.isAbsolute() ? p : Paths.get(System.getProperty("user.dir")).resolve(p).normalize();
    }
}
