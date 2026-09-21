package com.lesourire.serveur.crypto;

import java.util.List;

/**
 * Inventaire des colonnes chiffrées au repos (AES-256-GCM via
 * {@link ChiffreurTexte}).
 *
 * <p>Source unique de vérité partagée par le contrôle de démarrage
 * ({@code VerificateurCleChiffrement}) et l'outil de secours
 * ({@code DiagnosticChiffrement}) : toute colonne annotée
 * {@code @Convert(converter = ChiffreurTexte.class)} doit figurer ici.</p>
 */
public final class ColonnesChiffrees {

    /** Une colonne chiffrée d'une table, désignée par son nom SQL. */
    public record ColonneChiffree(String table, String colonne) {

        /** Référence lisible : {@code patient.nom}. */
        public String reference() {
            return table + "." + colonne;
        }
    }

    /**
     * Colonnes chiffrées de la base, table par table (relevé sur les entités
     * {@code @Convert(converter = ChiffreurTexte.class)}).
     */
    public static final List<ColonneChiffree> COLONNES = List.of(
            new ColonneChiffree("patient", "nom"),
            new ColonneChiffree("patient", "prenom"),
            new ColonneChiffree("patient", "telephone"),
            new ColonneChiffree("patient", "telephone_whatsapp"),
            new ColonneChiffree("patient", "email"),
            new ColonneChiffree("patient", "adresse"),
            new ColonneChiffree("patient", "quartier"),
            new ColonneChiffree("patient", "ville"),
            new ColonneChiffree("patient", "profession"),
            new ColonneChiffree("patient", "personne_urgence_nom"),
            new ColonneChiffree("patient", "personne_urgence_tel"),
            new ColonneChiffree("patient", "antecedents"),
            new ColonneChiffree("patient", "allergies"),
            new ColonneChiffree("patient", "notes"),
            new ColonneChiffree("utilisateur", "nom"),
            new ColonneChiffree("utilisateur", "prenom"),
            new ColonneChiffree("utilisateur", "email"),
            new ColonneChiffree("utilisateur", "telephone"),
            new ColonneChiffree("patient_couverture", "numero_assure"),
            new ColonneChiffree("rappel", "destinataire"),
            new ColonneChiffree("rappel", "contenu"),
            new ColonneChiffree("rappel", "message_erreur"),
            new ColonneChiffree("rdv", "motif"),
            new ColonneChiffree("rdv", "notes"),
            new ColonneChiffree("acte", "observations"),
            new ColonneChiffree("facture", "notes"),
            new ColonneChiffree("paiement", "notes"),
            new ColonneChiffree("audit_log", "details"));

    private ColonnesChiffrees() {
    }

    /** Références des colonnes ({@code patient.nom}, {@code rdv.motif}…). */
    public static List<String> references() {
        return COLONNES.stream().map(ColonneChiffree::reference).toList();
    }
}