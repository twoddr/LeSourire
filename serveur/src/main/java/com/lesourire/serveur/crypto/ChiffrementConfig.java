package com.lesourire.serveur.crypto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Prépare la clé AES utilisée pour chiffrer les données sensibles.
 *
 * <p>Ordre de résolution :</p>
 * <ol>
 *   <li>la propriété {@code lesourire.chiffrement.cle} (ou la variable
 *       d'environnement {@code LESOURIRE_CHIFFREMENT_CLE}) : clé en base64 ;</li>
 *   <li>le fichier indiqué par {@code lesourire.chiffrement.fichier}, s'il est
 *       renseigné ;</li>
 *   <li>le fichier {@code fichiers/cle-chiffrement.key} de l'installation,
 *       cherché de façon déterministe autour du JAR, de la racine transmise par
 *       les lanceurs ({@code -Dlesourire.home}) et du répertoire courant
 *       (voir {@link CheminsInstallation}) ; l'emplacement hérité
 *       {@code serveur/fichiers/cle-chiffrement.key} des paquets Windows
 *       0.1.x reste accepté ;</li>
 *   <li>à défaut de clé existante, une clé est générée et enregistrée dans
 *       {@code <installation>/fichiers/cle-chiffrement.key} — ce cas est
 *       journalisé en <strong>ERREUR</strong> car il rend illisibles les
 *       données déjà chiffrées.</li>
 * </ol>
 *
 * <p><strong>Sauvegardez ce fichier avec la base de données</strong> : sans lui,
 * les données chiffrées (identité et dossier médical des patients) sont
 * définitivement illisibles.</p>
 */
@Component
public class ChiffrementConfig {

    private static final Logger log = LoggerFactory.getLogger(ChiffrementConfig.class);

    /** Origine de la clé active ; reprise par le contrôle de démarrage. */
    private final String origine;

    /** Fichier de la clé active, ou {@code null} si elle vient de la configuration. */
    private final Path cheminFichier;

    /** Empreinte courte de la clé active (jamais la clé elle-même). */
    private final String empreinte;

    /** Vrai si la clé active vient d'être générée faute de clé existante. */
    private final boolean cleGeneree;

    @Autowired
    public ChiffrementConfig(
            @Value("${lesourire.chiffrement.cle:}") String cleBase64,
            @Value("${lesourire.chiffrement.fichier:}") String cheminFichier) {
        this(cleBase64, cheminFichier,
                CheminsInstallation.candidatsFichierCle(),
                CheminsInstallation.emplacementGeneration());
    }

    /** Variante à candidats explicites (tests) : aucun accès à l'état de la JVM. */
    ChiffrementConfig(String cleBase64, String cheminConfigure, List<Path> candidats, Path generation) {
        Resultat resultat = resoudre(cleBase64, cheminConfigure, candidats, generation);
        this.origine = resultat.origine();
        this.cheminFichier = resultat.cheminFichier();
        this.empreinte = resultat.empreinte();
        this.cleGeneree = resultat.cleGeneree();
        Chiffrement.initialiser(resultat.cle());
    }

    /** Origine de la clé active, sous forme lisible (pour les journaux). */
    public String origine() {
        return origine;
    }

    /** Fichier de la clé active, ou {@code null} si elle vient de la configuration. */
    public Path cheminFichier() {
        return cheminFichier;
    }

    /** Empreinte courte de la clé active, ou {@code null} si elle est invalide. */
    public String empreinte() {
        return empreinte;
    }

    /** Vrai si la clé active a été générée automatiquement au démarrage. */
    public boolean cleGeneree() {
        return cleGeneree;
    }

    /** Résumé pour les journaux : origine et empreinte de la clé active. */
    public String resume() {
        return origine + " (empreinte " + empreinte + ")";
    }

    // --------------------------------------------------------------- interne

    /** Clé active et métadonnées associées, produites par la résolution. */
    private record Resultat(SecretKeySpec cle, String origine, Path cheminFichier,
            String empreinte, boolean cleGeneree) {
    }

    private static Resultat resoudre(String cleBase64, String cheminConfigure,
            List<Path> candidats, Path generation) {
        if (cleBase64 != null && !cleBase64.isBlank()) {
            SecretKeySpec cle = Chiffrement.cleDepuisBase64(cleBase64.trim());
            String empreinte = Chiffrement.empreinte(cle);
            log.info("Chiffrement des données sensibles : clé fournie par la configuration "
                    + "(empreinte {}).", empreinte);
            return new Resultat(cle, "configuration (lesourire.chiffrement.cle / "
                    + "LESOURIRE_CHIFFREMENT_CLE)", null, empreinte, false);
        }
        Map<Path, String> trouves = chercherFichierCle(cheminConfigure, candidats);
        signalerAmbiguite(trouves);
        if (!trouves.isEmpty()) {
            Map.Entry<Path, String> retenu = trouves.entrySet().iterator().next();
            SecretKeySpec cle = Chiffrement.cleDepuisBase64(retenu.getValue());
            String empreinte = Chiffrement.empreinte(cle);
            log.info("Chiffrement des données sensibles : clé lue dans {} (empreinte {}).",
                    retenu.getKey(), empreinte);
            return new Resultat(cle, "fichier " + retenu.getKey(), retenu.getKey(), empreinte, false);
        }
        Path cible = cibleGeneration(cheminConfigure, generation);
        String encodee = genererCle(cible);
        SecretKeySpec cle = Chiffrement.cleDepuisBase64(encodee);
        return new Resultat(cle, "fichier généré " + cible, cible, Chiffrement.empreinte(cle), true);
    }

    /**
     * Fichiers de clé existants, dans l'ordre de priorité : chemin configuré,
     * puis candidats de l'installation (racine du paquet, dossier du JAR,
     * répertoire courant et leurs parents).
     */
    private static Map<Path, String> chercherFichierCle(String cheminConfigure, List<Path> candidats) {
        List<Path> aEssayer = new ArrayList<>(cheminsExplicites(cheminConfigure));
        aEssayer.addAll(candidats == null ? List.of() : candidats);
        Map<Path, String> trouves = new LinkedHashMap<>();
        for (Path candidat : dedoublonner(aEssayer)) {
            if (Files.isRegularFile(candidat)) {
                trouves.put(candidat, lireCle(candidat));
            }
        }
        return trouves;
    }

    /**
     * Signale en ERREUR la présence de plusieurs clés différentes : cause typique
     * de données illisibles après une mise à jour ou une ré-extraction du paquet.
     * La clé la plus prioritaire reste utilisée, le démarrage n'est pas modifié.
     */
    private static void signalerAmbiguite(Map<Path, String> trouves) {
        Map<String, List<Path>> parEmpreinte = new LinkedHashMap<>();
        for (Map.Entry<Path, String> entree : trouves.entrySet()) {
            parEmpreinte.computeIfAbsent(Chiffrement.empreinte(entree.getValue()),
                    k -> new ArrayList<>(2)).add(entree.getKey());
        }
        if (parEmpreinte.size() > 1) {
            log.error("{} clés de chiffrement DIFFÉRENTES ont été trouvées : les données écrites avec "
                    + "l'une sont illisibles avec l'autre. Clé utilisée : {}. Inventaire : {}",
                    parEmpreinte.size(), trouves.keySet().iterator().next(), parEmpreinte);
        }
    }

    /** Chemins dérivés d'un chemin configuré (relatif : racine du paquet, puis répertoire courant). */
    private static List<Path> cheminsExplicites(String cheminConfigure) {
        if (cheminConfigure == null || cheminConfigure.isBlank()) {
            return List.of();
        }
        Path chemin = Paths.get(cheminConfigure.trim());
        if (chemin.isAbsolute()) {
            return List.of(chemin.normalize());
        }
        List<Path> chemins = new ArrayList<>(2);
        Path racine = CheminsInstallation.racineExplicite();
        if (racine != null) {
            chemins.add(racine.resolve(chemin).normalize());
        }
        chemins.add(CheminsInstallation.repertoireCourant().resolve(chemin).normalize());
        return chemins;
    }

    /** Fichier d'écriture d'une clé nouvellement générée. */
    private static Path cibleGeneration(String cheminConfigure, Path generation) {
        List<Path> explicites = cheminsExplicites(cheminConfigure);
        if (!explicites.isEmpty()) {
            return explicites.get(0);
        }
        return generation != null ? generation : CheminsInstallation.emplacementGeneration();
    }

    /** Lit et valide le contenu d'un fichier de clé. */
    private static String lireCle(Path fichier) {
        String valeur;
        try {
            valeur = Files.readString(fichier, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new IllegalStateException("Lecture de la clé de chiffrement impossible : " + fichier, e);
        }
        if (valeur.isEmpty()) {
            throw new IllegalStateException("Le fichier de clé de chiffrement est vide : " + fichier
                    + ". Restaurez la clé d'origine (sauvegardée avec la base) puis relancez le serveur.");
        }
        try {
            Chiffrement.cleDepuisBase64(valeur);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(
                    "Clé de chiffrement invalide dans " + fichier + " : " + e.getMessage(), e);
        }
        return valeur;
    }

    /** Génère une clé de 256 bits et l'enregistre ; journalisé en ERREUR (cf. javadoc). */
    private static String genererCle(Path cible) {
        byte[] nouvelle = new byte[Chiffrement.TAILLE_CLE_OCTETS];
        new SecureRandom().nextBytes(nouvelle);
        String encodee = Base64.getEncoder().encodeToString(nouvelle);
        try {
            Path dossier = cible.getParent();
            if (dossier != null) {
                Files.createDirectories(dossier);
            }
            Files.writeString(cible, encodee, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'enregistrer la clé de chiffrement : " + cible, e);
        }
        log.error("AUCUNE clé de chiffrement trouvée : une NOUVELLE clé vient d'être générée dans {}. "
                + "Si cette installation contient déjà des données (patients, rendez-vous, factures…), "
                + "elles deviennent ILLISIBLES : arrêtez le serveur, restaurez la clé d'origine dans {} "
                + "puis relancez (outil de secours : Diagnostic-Chiffrement).", cible, cible);
        return encodee;
    }

    private static List<Path> dedoublonner(List<Path> chemins) {
        Set<Path> uniques = new LinkedHashSet<>();
        for (Path chemin : chemins) {
            if (chemin != null) {
                uniques.add(chemin);
            }
        }
        return List.copyOf(uniques);
    }
}
