package com.lesourire.serveur.outils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.crypto.spec.SecretKeySpec;

import com.lesourire.serveur.crypto.Chiffrement;

/**
 * Réécriture des données chiffrées sous une clé cible unique.
 *
 * <p>Chaque valeur est déchiffrée avec <em>la clé qui la relit</em> puis
 * réécrite avec la clé cible : une base peut ainsi avoir été écrite
 * successivement avec plusieurs clés (réinstallations) sans que rien ne soit
 * perdu. Les valeurs qu'aucune clé connue ne relit ne sont <strong>jamais
 * modifiées</strong> : elles sont signalées pour traitement manuel.</p>
 *
 * <p>Les écritures sont validées par lots ; en cas d'erreur SQL, le lot en cours
 * est annulé et compté, jamais appliqué à moitié.</p>
 */
final class RechiffreurCle {

    /** Nombre de réécritures validées avant {@code COMMIT}. */
    private static final int TAILLE_LOT = 200;

    /** Bilan du réchiffrement. */
    record Bilan(int converties, int conformes, int orphelines, int echecs) {

        boolean complet() {
            return echecs == 0 && orphelines == 0;
        }
    }

    private RechiffreurCle() {
    }

    /**
     * Réécrit les valeurs analysées avec la clé cible.
     *
     * @param valeurs   valeurs chiffrées à traiter (analyse préalable)
     * @param cles      clés candidates connues (pour relire chaque valeur)
     * @param cible     clé cible en base64 (les valeurs déjà conformes sont ignorées)
     * @param anomalie  signalement d'une valeur en échec : référence et nombre de lignes touchées
     * @param trace     progression (appelée à chaque lot validé)
     */
    static Bilan rechiffrer(Connection connexion, List<MoteurDiagnostic.Valeur> valeurs,
            List<ChercheurCles.CleCandidate> cles, String cible,
            BiConsumer<String, Integer> anomalie, Consumer<String> trace) throws SQLException {
        SecretKeySpec cleCible = Chiffrement.cleDepuisBase64(cible);
        Map<String, SecretKeySpec> preparees = new LinkedHashMap<>();
        for (ChercheurCles.CleCandidate cle : cles) {
            preparees.put(cle.base64(), Chiffrement.cleDepuisBase64(cle.base64()));
        }
        Map<String, PreparedStatement> requetes = new LinkedHashMap<>();
        boolean autoCommitOrigine = connexion.getAutoCommit();
        connexion.setAutoCommit(false);
        int converties = 0;
        int conformes = 0;
        int orphelines = 0;
        int echecs = 0;
        int enAttente = 0;
        try {
            for (MoteurDiagnostic.Valeur valeur : valeurs) {
                SecretKeySpec gagnante = trouverCle(valeur, preparees);
                if (gagnante == null) {
                    orphelines++;
                    anomalie.accept(valeur.reference(), 0);
                    continue;
                }
                if (gagnante.equals(cleCible)) {
                    conformes++;
                    continue;
                }
                try {
                    String clair = Chiffrement.dechiffrerAvec(valeur.stocke(), gagnante);
                    String nouveau = Chiffrement.chiffrerAvec(clair, cleCible);
                    PreparedStatement requete = requete(connexion, requetes, valeur);
                    requete.setString(1, nouveau);
                    requete.setLong(2, valeur.id());
                    if (requete.executeUpdate() != 1) {
                        echecs++;
                        anomalie.accept(valeur.reference(), 0);
                        continue;
                    }
                    converties++;
                    if (++enAttente >= TAILLE_LOT) {
                        connexion.commit();
                        enAttente = 0;
                        trace.accept(converties + " valeur(s) réécrite(s)…");
                    }
                } catch (SQLException e) {
                    connexion.rollback();
                    echecs += enAttente + 1;
                    enAttente = 0;
                    anomalie.accept(valeur.reference() + " : " + e.getMessage(), 0);
                }
            }
            connexion.commit();
        } finally {
            for (PreparedStatement requete : requetes.values()) {
                try {
                    requete.close();
                } catch (SQLException e) {
                    // fermeture best-effort
                }
            }
            connexion.setAutoCommit(autoCommitOrigine);
        }
        return new Bilan(converties, conformes, orphelines, echecs);
    }

    private static SecretKeySpec trouverCle(MoteurDiagnostic.Valeur valeur,
            Map<String, SecretKeySpec> preparees) {
        for (SecretKeySpec cle : preparees.values()) {
            if (Chiffrement.dechiffrableAvec(valeur.stocke(), cle)) {
                return cle;
            }
        }
        return null;
    }

    private static PreparedStatement requete(Connection connexion,
            Map<String, PreparedStatement> requetes, MoteurDiagnostic.Valeur valeur) throws SQLException {
        String clef = valeur.table() + "." + valeur.colonne();
        PreparedStatement requete = requetes.get(clef);
        if (requete == null) {
            requete = connexion.prepareStatement("UPDATE " + valeur.table() + " SET "
                    + valeur.colonne() + " = ? WHERE id = ?");
            requetes.put(clef, requete);
        }
        return requete;
    }
}