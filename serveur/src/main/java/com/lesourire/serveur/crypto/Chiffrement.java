package com.lesourire.serveur.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
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
        if (clair == null) {
            return null;
        }
        try {
            byte[] iv = new byte[TAILLE_IV];
            ALEA.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHME);
            cipher.init(Cipher.ENCRYPT_MODE, cle(), new GCMParameterSpec(TAILLE_TAG_BITS, iv));
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
        try {
            byte[] paquet = Base64.getDecoder().decode(stocke.substring(PREFIXE.length()));
            byte[] iv = Arrays.copyOfRange(paquet, 0, TAILLE_IV);
            Cipher cipher = Cipher.getInstance(ALGORITHME);
            cipher.init(Cipher.DECRYPT_MODE, cle(), new GCMParameterSpec(TAILLE_TAG_BITS, iv));
            byte[] clair = cipher.doFinal(paquet, TAILLE_IV, paquet.length - TAILLE_IV);
            return new String(clair, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Déchiffrement impossible : clé de chiffrement absente ou différente "
                            + "de celle utilisée à l'écriture.",
                    e);
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
