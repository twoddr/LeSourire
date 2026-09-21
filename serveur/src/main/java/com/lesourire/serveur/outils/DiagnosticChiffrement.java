package com.lesourire.serveur.outils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import com.lesourire.serveur.crypto.CheminsInstallation;
import com.lesourire.serveur.crypto.Chiffrement;
import com.lesourire.serveur.crypto.ColonnesChiffrees;

/**
 * Outil de secours « données illisibles » : inventorie les clés de chiffrement
 * présentes sur la machine, vérifie lesquelles relisent réellement la base, et
 * peut réchiffrer l'ensemble sous une clé unique.
 *
 * <p>Cette classe est lancée sans Spring :</p>
 * <pre>
 * java -Dloader.main=com.lesourire.serveur.outils.DiagnosticChiffrement \
 *      -cp lesourire-serveur.jar \
 *      org.springframework.boot.loader.launch.PropertiesLauncher --rapport rapport.txt
 * </pre>
 * <p>Les scripts {@code Diagnostic-Chiffrement.bat} et {@code .sh} des paquets
 * livrés s'en chargent ; ils transmettent les réglages MariaDB du cabinet.</p>
 */
public final class DiagnosticChiffrement {

    /** Données lisibles avec au moins une clé connue. */
    static final int CODE_OK = 0;
    /** Erreur d'exécution (base injoignable, options invalides…). */
    static final int CODE_ERREUR = 1;
    /** Des données chiffrées ne sont relues par aucune clé trouvée. */
    static final int CODE_DONNEES_ILLISIBLES = 2;
    /** Réchiffrement partiel (lignes illisibles ou en échec). */
    static final int CODE_RECHIFFREMENT_PARTIEL = 3;

    /** Port du serveur : aucune écriture tant qu'il répond. */
    private static final int PORT_SERVEUR = 8420;

    private final OptionsDiagnostic options;
    private final StringBuilder rapport = new StringBuilder();
    private final List<String> remarques = new ArrayList<>();
    private List<ChercheurCles.CleCandidate> cles = List.of();
    private MoteurDiagnostic.Analyse analyse =
            new MoteurDiagnostic.Analyse(List.of(), Map.of(), Map.of(), Map.of());

    private DiagnosticChiffrement(OptionsDiagnostic options) {
        this.options = options;
    }

    public static void main(String[] args) {
        OptionsDiagnostic options;
        try {
            options = OptionsDiagnostic.analyser(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            OptionsDiagnostic.aide(System.err);
            System.exit(CODE_ERREUR);
            return;
        }
        if (options.aide()) {
            OptionsDiagnostic.aide(System.out);
            return;
        }
        int code = new DiagnosticChiffrement(options).executer();
        System.out.println();
        System.out.println("Code de sortie " + code + " — " + libelle(code));
        System.out.println("Rapport : " + options.rapport().toAbsolutePath());
        System.exit(code);
    }

    private static String libelle(int code) {
        return switch (code) {
            case CODE_OK -> "données lisibles";
            case CODE_DONNEES_ILLISIBLES -> "des données chiffrées ne sont relues par aucune clé";
            case CODE_RECHIFFREMENT_PARTIEL -> "réchiffrement partiel";
            default -> "erreur d'exécution";
        };
    }

    private int executer() {
        entete();
        contexte();
        cles = ChercheurCles.chercher(options.racines(), options.profondeur(), remarques::add);
        inventaireCles();
        Connection connexion;
        try {
            connexion = MoteurDiagnostic.ouvrir(options);
        } catch (SQLException e) {
            section("Base de données");
            ligne("Base injoignable : %s", e.getMessage());
            ligne("Vérifiez que MariaDB est démarré et que les réglages du cabinet sont exacts.");
            remarquesPourRapport();
            return CODE_ERREUR;
        }
        try {
            int limite = options.rechiffrer() == null ? options.limite() : 0;
            analyse = MoteurDiagnostic.analyser(MoteurDiagnostic.lireValeurs(connexion, limite), cles);
            compatibilite();
            synthese();
            if (options.exporterCle() != null) {
                exporterCle();
            }
            if (options.verifierCle() != null) {
                return verifier();
            }
            if (options.rechiffrer() != null) {
                return rechiffrer(connexion);
            }
            conclusion();
            return analyse.toutesLisibles() ? CODE_OK : CODE_DONNEES_ILLISIBLES;
        } catch (SQLException e) {
            section("Base de données");
            ligne("Lecture impossible : %s", e.getMessage());
            return CODE_ERREUR;
        } finally {
            fermer(connexion);
            remarquesPourRapport();
            ecrireRapport();
        }
    }

    // ------------------------------------------------------------- rapport

    private void entete() {
        ligne("=".repeat(78));
        ligne("DIAGNOSTIC DU CHIFFREMENT — LE SOURIRE");
        ligne("Rapport généré le %s", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm:ss")));
        ligne("=".repeat(78));
    }

    private void contexte() {
        section("Contexte");
        Path racine = CheminsInstallation.racineExplicite();
        Path attendu = CheminsInstallation.emplacementGeneration();
        ligne("Répertoire courant ..................... %s", CheminsInstallation.repertoireCourant());
        ligne("Dossier du programme ................... %s", texte(CheminsInstallation.dossierJar()));
        ligne("Racine du paquet (-Dlesourire.home) .... %s",
                racine == null ? "non transmise" : racine.toString());
        ligne("Racine du projet (pom.xml) ............. %s", texte(CheminsInstallation.racineProjet()));
        ligne("Clé attendue à ......................... %s [%s]", attendu,
                Files.isRegularFile(attendu) ? "présente" : "ABSENTE");
        ligne("Emplacements connus testés ............. %d",
                CheminsInstallation.candidatsFichierCle().size());
        ligne("Dossiers explorés ...................... %d", options.racines().size());
        for (Path dossier : options.racines()) {
            ligne("    - %s", dossier);
        }
        ligne("Base de données ........................ %s", options.url());
        ligne("Utilisateur ............................ %s", options.utilisateur());
        ligne("Valeurs examinées par colonne .......... %s",
                options.limite() == 0 ? "toutes" : String.valueOf(options.limite()));
        ligne("Profondeur d'exploration ............... %d", options.profondeur());
    }

    private void inventaireCles() {
        section("Clés de chiffrement trouvées (%d)", cles.size());
        if (cles.isEmpty()) {
            ligne("Aucune clé de chiffrement exploitable n'a été trouvée sur cette machine.");
            ligne("Si la base contient des données « enc:v1: », restaurez la clé d'origine");
            ligne("(fichier cle-chiffrement.key) dans le dossier fichiers\\ du paquet, puis");
            ligne("relancez ce diagnostic avec --racine <dossier de l'ancienne installation>.");
        }
        int rang = 1;
        for (ChercheurCles.CleCandidate cle : cles) {
            ligne("%2d. empreinte %s  ← %s", rang++, cle.empreinte(), cle.origine());
        }
        if (options.afficherCles()) {
            ligne("");
            ligne("Clés en clair (confidentiel — ne pas diffuser) :");
            for (ChercheurCles.CleCandidate cle : cles) {
                ligne("    %s  %s", cle.empreinte(), cle.base64());
            }
        }
    }

    private void remarquesPourRapport() {
        if (remarques.isEmpty()) {
            return;
        }
        section("Éléments ignorés / remarques (%d)", remarques.size());
        for (String remarque : remarques) {
            ligne("    - %s", remarque);
        }
    }

    private void ecrireRapport() {
        try {
            Path cible = options.rapport().toAbsolutePath();
            Path dossier = cible.getParent();
            if (dossier != null) {
                Files.createDirectories(dossier);
            }
            Files.writeString(cible, rapport.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Impossible d'écrire le rapport : " + e.getMessage());
        }
    }

    private void fermer(Connection connexion) {
        if (connexion != null) {
            try {
                connexion.close();
            } catch (SQLException e) {
                // fermeture best-effort
            }
        }
    }

    private void section(String titre, Object... arguments) {
        ligne("");
        ligne(String.format(Locale.ROOT, titre, arguments));
        ligne("-".repeat(Math.max(titre.length(), 12)));
    }

    private void ligne(String format, Object... arguments) {
        String texte = String.format(Locale.ROOT, format, arguments);
        rapport.append(texte).append('\n');
        System.out.println(texte);
    }

    private static String texte(Path chemin) {
        return chemin == null ? "indéterminé" : chemin.toString();
    }

    // ------------------------------------------------------------- analyse

    private void compatibilite() {
        section("Compatibilité des données (échantillon par colonne)");
        int colonnesAvecDonnees = 0;
        for (ColonnesChiffrees.ColonneChiffree colonne : ColonnesChiffrees.COLONNES) {
            List<MoteurDiagnostic.Valeur> valeurs = valeursDe(colonne);
            if (valeurs.isEmpty()) {
                ligne("%-32s aucune valeur chiffrée", colonne.reference());
                continue;
            }
            colonnesAvecDonnees++;
            Map<String, Integer> compte = new LinkedHashMap<>();
            int illisibles = 0;
            for (MoteurDiagnostic.Valeur valeur : valeurs) {
                String empreinte = empreinteGagnante(valeur);
                if (empreinte == null) {
                    illisibles++;
                } else {
                    compte.merge(empreinte, 1, Integer::sum);
                }
            }
            StringBuilder detail = new StringBuilder();
            for (Map.Entry<String, Integer> entree : compte.entrySet()) {
                detail.append(entree.getKey()).append('=').append(entree.getValue()).append("  ");
            }
            if (illisibles > 0) {
                detail.append("ILLISIBLES=").append(illisibles).append(" (clé inconnue)");
            }
            ligne("%-32s %5d valeurs | %s", colonne.reference(), valeurs.size(), detail);
        }
        if (colonnesAvecDonnees == 0) {
            ligne("");
            ligne("Aucune donnée chiffrée en base : rien à récupérer.");
            ligne("La clé de fichiers\\cle-chiffrement.key servira aux prochaines écritures.");
        }
    }

    private List<MoteurDiagnostic.Valeur> valeursDe(ColonnesChiffrees.ColonneChiffree colonne) {
        List<MoteurDiagnostic.Valeur> valeurs = new ArrayList<>();
        for (MoteurDiagnostic.Valeur valeur : analyse.valeurs()) {
            if (valeur.table().equals(colonne.table()) && valeur.colonne().equals(colonne.colonne())) {
                valeurs.add(valeur);
            }
        }
        return valeurs;
    }

    /** Empreinte de la clé qui relit la valeur, ou {@code null} si aucune ne la relit. */
    private String empreinteGagnante(MoteurDiagnostic.Valeur valeur) {
        for (ChercheurCles.CleCandidate cle : cles) {
            if (Chiffrement.dechiffrableAvec(valeur.stocke(), Chiffrement.cleDepuisBase64(cle.base64()))) {
                return cle.empreinte();
            }
        }
        return null;
    }

    private void synthese() {
        section("Synthèse");
        int total = analyse.valeurs().size();
        int illisibles = total - analyse.usageParCle().values().stream()
                .mapToInt(Integer::intValue).sum();
        ligne("Valeurs chiffrées examinées ............. %d", total);
        ligne("    relues sans erreur .................. %d", total - illisibles);
        ligne("    illisibles (aucune clé connue) ...... %d", illisibles);
        if (!analyse.usageParCle().isEmpty()) {
            ligne("");
            ligne("Clés utilisées à l'écriture (clé la plus fréquente en premier) :");
            analyse.usageParCle().entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entree -> ligne("    %6d valeur(s)  empreinte %s  ← %s", entree.getValue(),
                            Chiffrement.empreinte(entree.getKey()),
                            origine(entree.getKey())));
        }
        List<MoteurDiagnostic.Valeur> orphelines = new ArrayList<>();
        analyse.orphelines().values().forEach(orphelines::addAll);
        if (!orphelines.isEmpty()) {
            ligne("");
            ligne("Valeurs illisibles (%d) — lignes à traiter manuellement :", orphelines.size());
            int affichees = 0;
            for (MoteurDiagnostic.Valeur valeur : orphelines) {
                if (affichees++ >= 20) {
                    ligne("    … %d autre(s)", orphelines.size() - 20);
                    break;
                }
                ligne("    - %s", valeur.reference());
            }
        }
    }

    private String origine(String base64) {
        for (ChercheurCles.CleCandidate cle : cles) {
            if (cle.base64().equals(base64)) {
                return cle.origine();
            }
        }
        return "origine inconnue";
    }

    /** Clé ayant écrit le plus de valeurs (celle à conserver), ou {@code null}. */
    private String cleMajoritaire() {
        return analyse.usageParCle().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // --------------------------------------------------------------- modes

    private void conclusion() {
        section("Conclusion");
        if (analyse.valeurs().isEmpty()) {
            ligne("Aucune donnée chiffrée n'a été trouvée : l'installation peut démarrer avec la clé");
            ligne("livrée dans le paquet (fichiers\\cle-chiffrement.key).");
            ligne("Vérifiez que ce fichier est présent, puis que le journal du serveur affiche");
            ligne("« Clé de chiffrement vérifiée ».");
            return;
        }
        if (analyse.toutesLisibles()) {
            ligne("Toutes les valeurs examinées sont relues par une clé connue.");
            String majoritaire = cleMajoritaire();
            if (majoritaire != null) {
                ligne("");
                ligne("Clé à conserver (celle qui a écrit le plus de données) :");
                ligne("    empreinte %s  ← %s", Chiffrement.empreinte(majoritaire), origine(majoritaire));
            }
            if (analyse.usageParCle().size() > 1) {
                ligne("");
                ligne("Attention : %d clés différentes ont servi à écrire les données.",
                        analyse.usageParCle().size());
                ligne("Un réchiffrement unifie la base sous une seule clé :");
                ligne("    Diagnostic-Chiffrement --rechiffrer <fichier cle-chiffrement.key> --je-confirme");
            }
            ligne("");
            ligne("Copiez la clé à conserver dans : %s", CheminsInstallation.emplacementGeneration());
            return;
        }
        ligne("%d valeur(s) ne sont relues par AUCUNE des %d clés trouvées.",
                analyse.orphelines().values().stream().mapToInt(List::size).sum(), cles.size());
        ligne("Ces valeurs n'ont pas été modifiées : elles ont été écrites avec une clé absente du poste.");
        ligne("");
        ligne("Que faire, dans cet ordre :");
        ligne("  1. relancer le diagnostic en pointant l'ancienne installation ou les archives :");
        ligne("     Diagnostic-Chiffrement --racine \"D:\\chemin\\de\\l'ancien\\dossier\" --profondeur 8");
        ligne("  2. restaurer la clé retrouvée dans %s", CheminsInstallation.emplacementGeneration());
        ligne("  3. ne rien saisir dans le client tant que la clé n'est pas retrouvée : les nouvelles");
        ligne("     écritures seraient, elles, chiffrées avec la clé actuelle.");
    }

    private void exporterCle() {
        section("Export de la clé");
        Path cible = options.exporterCle().toAbsolutePath();
        String majoritaire = cleMajoritaire();
        String aExporter = majoritaire != null ? majoritaire
                : (cles.size() == 1 ? cles.get(0).base64() : null);
        if (aExporter == null) {
            ligne("Aucune clé d'écriture identifiée : export annulé.");
            ligne("Précisez la clé à exporter (--rechiffrer) ou copiez le fichier cle-chiffrement.key.");
            return;
        }
        String existante = null;
        if (Files.isRegularFile(cible)) {
            try {
                existante = Files.readString(cible, StandardCharsets.UTF_8).trim();
            } catch (IOException e) {
                existante = null;
            }
            if (existante != null && !existante.equals(aExporter) && !options.forcer()) {
                ligne("%s contient déjà une AUTRE clé (empreinte %s) : export refusé.",
                        cible, Chiffrement.empreinte(existante));
                ligne("Relancez avec --forcer pour l'écraser, après avoir conservé l'ancienne.");
                return;
            }
        }
        try {
            Path dossier = cible.getParent();
            if (dossier != null) {
                Files.createDirectories(dossier);
            }
            Files.writeString(cible, aExporter, StandardCharsets.UTF_8);
            ligne("Clé écrite dans %s (empreinte %s)", cible, Chiffrement.empreinte(aExporter));
            ligne("Origine ......................... %s", origine(aExporter));
        } catch (IOException e) {
            ligne("Écriture impossible : %s", e.getMessage());
        }
    }

    /** Mode {@code --verifier-cle} : teste une clé sur l'échantillon lu. */
    private int verifier() {
        section("Vérification d'une clé");
        String fournie = MoteurDiagnostic.resoudreCle(options.verifierCle());
        if (fournie == null) {
            ligne("Clé illisible : %s", options.verifierCle());
            ligne("Attendu : la clé en base64, ou le chemin d'un fichier cle-chiffrement.key.");
            return CODE_ERREUR;
        }
        SecretKeySpec cle = Chiffrement.cleDepuisBase64(fournie);
        int total = 0;
        int echecs = 0;
        Map<String, Integer> parColonne = new LinkedHashMap<>();
        for (MoteurDiagnostic.Valeur valeur : analyse.valeurs()) {
            total++;
            if (!Chiffrement.dechiffrableAvec(valeur.stocke(), cle)) {
                echecs++;
                parColonne.merge(valeur.table() + "." + valeur.colonne(), 1, Integer::sum);
            }
        }
        ligne("Clé testée ..................... empreinte %s", Chiffrement.empreinte(cle));
        ligne("Valeurs examinées .............. %d", total);
        ligne("Valeurs illisibles ............. %d", echecs);
        parColonne.forEach((colonne, nombre) -> ligne("    - %-30s %d", colonne, nombre));
        if (echecs == 0) {
            ligne("");
            ligne("Cette clé relit l'intégralité des données examinées : elle est utilisable.");
            return CODE_OK;
        }
        ligne("");
        ligne("Cette clé ne relit pas toutes les données : ne l'installez pas en l'état.");
        ligne("Restaurez la clé d'origine, ou réchiffrez la base vers cette clé (--rechiffrer).");
        return CODE_DONNEES_ILLISIBLES;
    }

    /** Mode {@code --rechiffrer} : sauvegarde obligatoire, puis réécriture sous une clé unique. */
    private int rechiffrer(Connection connexion) throws SQLException {
        section("Réchiffrement vers une clé unique");
        if (!options.confirme()) {
            ligne("Écriture refusée : ajoutez --je-confirme pour réchiffrer la base.");
            ligne("Une sauvegarde de la base est alors réalisée automatiquement avant toute écriture.");
            return CODE_ERREUR;
        }
        if (MoteurDiagnostic.serveurActif(PORT_SERVEUR) && !options.forcer()) {
            ligne("Un serveur répond déjà sur le port %d : arrêtez-le (Arreter-Serveur.bat) avant de",
                    PORT_SERVEUR);
            ligne("réchiffrer, afin qu'aucune écriture concurrente n'ait lieu.");
            ligne("(--forcer contourne ce contrôle si le port est occupé par une autre application.)");
            return CODE_ERREUR;
        }
        String cible = MoteurDiagnostic.resoudreCle(options.rechiffrer());
        if (cible == null) {
            ligne("Clé cible illisible : %s", options.rechiffrer());
            return CODE_ERREUR;
        }
        int orphelines = analyse.orphelines().values().stream().mapToInt(List::size).sum();
        if (orphelines > 0 && !options.forcer()) {
            ligne("%d valeur(s) sont illisibles avec toutes les clés connues : réchiffrement refusé", orphelines);
            ligne("pour ne pas mélanger des données relisibles et des données définitivement perdues.");
            ligne("Retrouvez la clé d'origine, ou relancez avec --forcer : ces lignes resteront intactes.");
            return CODE_ERREUR;
        }
        Path parent = options.rapport().toAbsolutePath().getParent();
        try {
            Path sauvegarde = MoteurDiagnostic.sauvegarderBase(options,
                    parent != null ? parent : Path.of("."));
            ligne("Sauvegarde de la base .......... %s", sauvegarde);
        } catch (IOException e) {
            ligne("Sauvegarde impossible : %s", e.getMessage());
            if (!options.forcer()) {
                ligne("Réchiffrement interrompu : sauvegardez la base, puis relancez avec --forcer.");
                return CODE_ERREUR;
            }
            ligne("Poursuite forcée SANS sauvegarde automatique.");
        }
        ligne("");
        ligne("Clé cible ...................... empreinte %s", Chiffrement.empreinte(cible));
        ligne("Valeurs à traiter .............. %d", analyse.valeurs().size());
        RechiffreurCle.Bilan bilan = RechiffreurCle.rechiffrer(connexion, analyse.valeurs(), cles, cible,
                (reference, lignes) -> ligne("    ! %s", reference),
                avancement -> ligne("    %s", avancement));
        ligne("");
        ligne("Valeurs réécrites .............. %d", bilan.converties());
        ligne("Déjà conformes ................. %d", bilan.conformes());
        ligne("Illisibles (laissées intactes) .. %d", bilan.orphelines());
        ligne("Échecs ......................... %d", bilan.echecs());
        if (bilan.complet()) {
            ligne("");
            ligne("Base entièrement réécrite sous la clé cible : installez cette clé dans");
            ligne("%s puis redémarrez le serveur.", CheminsInstallation.emplacementGeneration());
            return CODE_OK;
        }
        ligne("");
        ligne("Réchiffrement partiel : les lignes signalées n'ont pas été modifiées.");
        ligne("Conservez la sauvegarde et les clés trouvées jusqu'à la résolution complète.");
        return CODE_RECHIFFREMENT_PARTIEL;
    }
}
