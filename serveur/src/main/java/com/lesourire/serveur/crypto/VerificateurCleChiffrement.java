package com.lesourire.serveur.crypto;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Contrôle, au démarrage, que la clé de chiffrement active peut
 * <strong>relire</strong> les données déjà chiffrées.
 *
 * <p>Sanction d'un incident réel : un serveur packagé relancé depuis un autre
 * dossier avait généré une nouvelle clé et servait alors des erreurs HTTP 500
 * sur toutes les listes. Plutôt que de continuer à écrire des données
 * illisibles, le serveur refuse maintenant de démarrer.</p>
 *
 * <p>Le contrôle a lieu pendant l'initialisation du contexte ({@code @PostConstruct}),
 * donc <strong>avant</strong> l'ouverture du port HTTP : le message d'erreur n'est
 * jamais masqué par une autre cause de démarrage (port occupé, par exemple), et
 * aucun client ne peut atteindre un serveur dont la clé est incohérente.</p>
 */
@Component
public class VerificateurCleChiffrement {

    private static final Logger log = LoggerFactory.getLogger(VerificateurCleChiffrement.class);

    /** Valeurs relues par colonne : suffisant pour détecter une clé inadaptée. */
    private static final int ECHANTILLON_PAR_COLONNE = 5;

    private final JdbcTemplate jdbc;
    private final ChiffrementConfig configuration;

    public VerificateurCleChiffrement(JdbcTemplate jdbc, ChiffrementConfig configuration) {
        this.jdbc = jdbc;
        this.configuration = configuration;
    }

    /** Relit un échantillon de chaque colonne chiffrée ; interrompt le démarrage si besoin. */
    @PostConstruct
    public void verifier() {
        Set<String> colonnesEnEchec = new LinkedHashSet<>();
        int valeursRelues = 0;
        for (ColonnesChiffrees.ColonneChiffree colonne : ColonnesChiffrees.COLONNES) {
            for (String valeur : echantillon(colonne)) {
                if (Chiffrement.tenterDechiffrer(valeur, Chiffrement.cleCourante()) == null) {
                    colonnesEnEchec.add(colonne.reference());
                    break;
                }
                valeursRelues++;
            }
        }
        if (!colonnesEnEchec.isEmpty()) {
            throw new IllegalStateException(message(colonnesEnEchec));
        }
        log.info("Clé de chiffrement vérifiée : {} valeur(s) chiffrée(s) relue(s) sans erreur ({}).",
                valeursRelues, configuration.resume());
    }

    /** Quelques valeurs chiffrées de la colonne (vide si la colonne est illisible). */
    private List<String> echantillon(ColonnesChiffrees.ColonneChiffree colonne) {
        String sql = "SELECT " + colonne.colonne() + " FROM " + colonne.table()
                + " WHERE " + colonne.colonne() + " LIKE '" + Chiffrement.PREFIXE + "%'"
                + " LIMIT " + ECHANTILLON_PAR_COLONNE;
        try {
            List<String> valeurs = jdbc.queryForList(sql, String.class);
            return valeurs == null ? List.of() : valeurs;
        } catch (DataAccessException e) {
            log.warn("Contrôle de la clé de chiffrement : colonne {} non lue ({}).",
                    colonne.reference(), e.getMostSpecificCause().getMessage());
            return List.of();
        }
    }

    /** Message d'erreur volontairement explicite : il est lu en premier par l'opérateur. */
    private String message(Set<String> colonnesEnEchec) {
        StringBuilder message = new StringBuilder();
        message.append("Clé de chiffrement incompatible avec la base : ")
                .append(colonnesEnEchec.size())
                .append(" colonne(s) contiennent des données chiffrées illisibles (")
                .append(String.join(", ", colonnesEnEchec))
                .append("). Clé utilisée : ").append(configuration.resume()).append(". ");
        if (configuration.cleGeneree()) {
            message.append("Cette clé vient d'être GÉNÉRÉE faute de clé existante : les données ont ")
                    .append("été écrites avec une autre clé. ");
        }
        message.append("Restaurez la clé d'origine dans ")
                .append(CheminsInstallation.emplacementGeneration())
                .append(" (ou renseignez LESOURIRE_CHIFFREMENT_CLE) puis relancez le serveur. ")
                .append("Le démarrage est refusé pour ne pas écrire de données définitivement illisibles. ")
                .append("Diagnostic détaillé : lancez Diagnostic-Chiffrement.bat (ou .sh) — il inventorie ")
                .append("les clés trouvées sur le disque et relit chaque colonne chiffrée.");
        return message.toString();
    }
}