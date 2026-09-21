package com.lesourire.serveur.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Chiffrement symétrique AES-256-GCM des données sensibles stockées en base
 * (identité et données médicales des patients, coordonnées des utilisateurs,
 * numéro d'assuré).
 *
 * <p>Format stocké : {@code enc:v1:<base64(iv || cryptogramme || tag)>}.
 * Les valeurs historiques non préfixées (données en clair antérieures) sont
 * relues telles quelles, puis réécrites chiffrées à la première modification :
 * la bascule est donc progressive et sans rupture.</p>
 *
 * <p>La clé est injectée au démarrage par {@link ChiffrementConfig}. Elle est
 * indispensable pour relire les données chiffrées : sauvegardez-la avec la base.</p>
 */
public final class Chiffrement {

    /** Marqueur identifiant une valeur chiffrée et sa version de format. */
    public static final String PREFIXE = "enc:v1:";

    private static final String ALGORITHME = "AES/GCM/NoPadding";
    private static final int TAILLE_IV = 12;
    private static final int TAILLE_TAG_BITS = 128;
    /** Taille attendue d'une clé AES-256 (les tailles 16 et 24 restent acceptées). */
    public static final int TAILLE_CLE_OCTETS = 32;
    private static final SecureRandom ALEA = new SecureRandom();

    private static volatile SecretKeySpec cle;

    private Chiffrement() {
    }

    /** Définit la clé AES utilisée par toutes les lectures/écritures chiffrées. */
    public static void initialiser(SecretKeySpec cleAes) {
        cle = cleAes;
    }

    /** Indique si la valeur stockée est déjà au format chiffré. */
    public static boolean estChiffre(String valeur) {
        return valeur != null && valeur.startsWith(PREFIXE);
    }

    /** Chiffre une valeur en clair ; {@code null} reste {@code null}. */
    public static String chiffrer(String clair) {
        return chiffrerAvec(clair, cle());
    }

    /**
     * Chiffre une valeur avec une clé explicite, sans modifier la clé active de
     * l'application (utilisé par l'outil de secours pour réchiffrer les données
     * sous une clé cible unique).
     */
    public static String chiffrerAvec(String clair, SecretKeySpec cleAes) {
        if (clair == null) {
            return null;
        }
        try {
            byte[] iv = new byte[TAILLE_IV];
            ALEA.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHME);
            cipher.init(Cipher.ENCRYPT_MODE, cleAes, new GCMParameterSpec(TAILLE_TAG_BITS, iv));
            byte[] cryptogramme = cipher.doFinal(clair.getBytes(StandardCharsets.UTF_8));
            ByteBuffer paquet = ByteBuffer.allocate(iv.length + cryptogramme.length);
            paquet.put(iv).put(cryptogramme);
            return PREFIXE + Base64.getEncoder().encodeToString(paquet.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Chiffrement d'une donnée sensible impossible.", e);
        }
    }

    /**
     * Déchiffre une valeur stockée. Les valeurs non préfixées (données en clair
     * antérieures au chiffrement) sont renvoyées telles quelles.
     */
    public static String dechiffrer(String stocke) {
        if (stocke == null || !stocke.startsWith(PREFIXE)) {
            return stocke;
        }
        return dechiffrerAvec(stocke, cle());
    }

    /**
     * Déchiffre une valeur avec une clé explicite, sans modifier la clé active.
     * Les valeurs non préfixées (données en clair antérieures au chiffrement)
     * sont renvoyées telles quelles.
     *
     * @throws IllegalStateException si la valeur est chiffrée et que la clé
     *         fournie n'est pas celle utilisée à l'écriture
     */
    public static String dechiffrerAvec(String stocke, SecretKeySpec cleAes) {
        if (stocke == null || !stocke.startsWith(PREFIXE)) {
            return stocke;
        }
        try {
            byte[] paquet = Base64.getDecoder().decode(stocke.substring(PREFIXE.length()));
            byte[] iv = Arrays.copyOfRange(paquet, 0, TAILLE_IV);
            Cipher cipher = Cipher.getInstance(ALGORITHME);
            cipher.init(Cipher.DECRYPT_MODE, cleAes, new GCMParameterSpec(TAILLE_TAG_BITS, iv));
            byte[] clair = cipher.doFinal(paquet, TAILLE_IV, paquet.length - TAILLE_IV);
            return new String(clair, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Déchiffrement impossible : clé de chiffrement absente ou différente "
                            + "de celle utilisée à l'écriture (clé essayée : empreinte "
                            + empreinte(cleAes) + ").",
                    e);
        }
    }

    /** Clé active, ou {@code null} tant que {@link #initialiser} n'a pas été appelée. */
    public static SecretKeySpec cleCourante() {
        return cle;
    }

    /**
     * Variante tolérante de {@link #dechiffrerAvec} : renvoie {@code null} si la
     * valeur est chiffrée et illisible avec cette clé au lieu de lever une
     * exception ; une valeur non chiffrée est renvoyée telle quelle.
     */
    public static String tenterDechiffrer(String stocke, SecretKeySpec cleAes) {
        if (stocke == null || !stocke.startsWith(PREFIXE)) {
            return stocke;
        }
        try {
            return dechiffrerAvec(stocke, cleAes);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * Variante de {@link #tenterDechiffrer(String, SecretKeySpec)} prenant la clé
     * en base64 ; renvoie {@code null} si la clé est mal formée ou inadaptée.
     */
    public static String tenterDechiffrer(String stocke, String cleBase64) {
        try {
            return tenterDechiffrer(stocke, cleDepuisBase64(cleBase64));
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /** Indique si la valeur peut être relue avec la clé fournie. */
    public static boolean dechiffrableAvec(String stocke, SecretKeySpec cleAes) {
        return tenterDechiffrer(stocke, cleAes) != null;
    }

    /**
     * Valide une clé fournie en base64 (16, 24 ou 32 octets) et la convertit en
     * clé AES, sans toucher à la clé active.
     *
     * @throws IllegalStateException si la clé est vide, non base64 ou de taille invalide
     */
    public static SecretKeySpec cleDepuisBase64(String cleBase64) {
        if (cleBase64 == null || cleBase64.isBlank()) {
            throw new IllegalStateException("Clé de chiffrement vide.");
        }
        byte[] octets;
        try {
            octets = Base64.getDecoder().decode(cleBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Clé de chiffrement illisible (base64 attendu).", e);
        }
        if (octets.length != 16 && octets.length != 24 && octets.length != TAILLE_CLE_OCTETS) {
            throw new IllegalStateException("Clé de chiffrement invalide : " + octets.length
                    + " octets (attendu 16, 24 ou 32).");
        }
        return new SecretKeySpec(octets, "AES");
    }

    /**
     * Empreinte courte (SHA-256 tronqué) d'une clé : permet de comparer deux
     * clés et d'en journaliser l'identité sans jamais divulguer la clé.
     */
    public static String empreinte(SecretKeySpec cleAes) {
        try {
            byte[] hache = MessageDigest.getInstance("SHA-256").digest(cleAes.getEncoded());
            StringBuilder empreinte = new StringBuilder(12);
            for (int i = 0; i < 6; i++) {
                empreinte.append(String.format("%02x", hache[i]));
            }
            return empreinte.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Calcul de l'empreinte de clé impossible.", e);
        }
    }

    /** Empreinte courte d'une clé en base64, ou {@code null} si la clé est invalide. */
    public static String empreinte(String cleBase64) {
        try {
            return empreinte(cleDepuisBase64(cleBase64));
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private static SecretKeySpec cle() {
        SecretKeySpec courante = cle;
        if (courante == null) {
            throw new IllegalStateException(
                    "Clé de chiffrement non initialisée (voir ChiffrementConfig).");
        }
        return courante;
    }
}
