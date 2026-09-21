package com.lesourire.serveur.outils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import com.lesourire.serveur.crypto.CheminsInstallation;
import com.lesourire.serveur.service.SauvegardeMysqldump;

/**
 * Sauvegarde de la base <strong>hors de l'application</strong>, pour le
 * double-clic sur {@code Sauvegarder-Base.bat} (ou {@code .sh}) : écrit le dump
 * <em>et</em> la copie de la clé de chiffrement dans un même dossier, car l'un
 * sans l'autre ne permet pas de relire les données.
 *
 * <p>Lancée via {@code PropertiesLauncher}, sans Spring :</p>
 * <pre>
 * java -Dloader.main=com.lesourire.serveur.outils.SauvegardeBase \
 *      -cp lesourire-serveur.jar \
 *      org.springframework.boot.loader.launch.PropertiesLauncher --dossier D:\sauvegardes
 * </pre>
 */
public final class SauvegardeBase {

    private static final String AIDE = """
            Sauvegarde de la base Le Sourire (lecture seule).

            Usage : Sauvegarder-Base [options]

            --dossier <chemin>       où écrire le fichier .sql et la clé
                                     (défaut : <installation>/sauvegardes)
            --mysqldump <chemin>     programme mysqldump à utiliser
                                     (défaut : PATH, puis MariaDB/MySQL installés)
            --bd-url <url jdbc>      base à sauvegarder
            --bd-utilisateur <nom>   utilisateur MariaDB
            --bd-mot-de-passe <mdp>  mot de passe MariaDB
            --aide                   affiche cette aide

            Variables d'environnement reconnues : LESOURIRE_BD_URL,
            LESOURIRE_BD_UTILISATEUR, LESOURIRE_BD_MOT_DE_PASSE, LESOURIRE_MYSQLDUMP.""";

    public static void main(String[] args) {
        int code = executer(args);
        if (code != 0) {
            System.out.println();
            System.out.println("Sauvegarde NON effectuée (code " + code + ").");
        }
        System.exit(code);
    }

    private static int executer(String[] args) {
        Path dossier = null;
        String cheminMysqldump = null;
        String url = null;
        String utilisateur = null;
        String motDePasse = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dossier" -> dossier = Paths.get(valeur(args, ++i, "--dossier"));
                case "--mysqldump" -> cheminMysqldump = valeur(args, ++i, "--mysqldump");
                case "--bd-url" -> url = valeur(args, ++i, "--bd-url");
                case "--bd-utilisateur" -> utilisateur = valeur(args, ++i, "--bd-utilisateur");
                case "--bd-mot-de-passe" -> motDePasse = valeur(args, ++i, "--bd-mot-de-passe");
                case "--aide", "-h", "--help" -> {
                    System.out.println(AIDE);
                    return 0;
                }
                default -> {
                    System.err.println("Option inconnue : " + args[i]);
                    System.err.println(AIDE);
                    return 1;
                }
            }
        }
        url = url != null ? url : parametre("LESOURIRE_BD_URL", "jdbc:mariadb://localhost:3306/lesourire");
        utilisateur = utilisateur != null ? utilisateur : parametre("LESOURIRE_BD_UTILISATEUR", "admin");
        motDePasse = motDePasse != null ? motDePasse : parametre("LESOURIRE_BD_MOT_DE_PASSE", "csa-soft");
        if (cheminMysqldump == null) {
            cheminMysqldump = System.getenv("LESOURIRE_MYSQLDUMP");
        }
        if (dossier == null) {
            dossier = racineInstallation().resolve("sauvegardes");
        }
        dossier = dossier.toAbsolutePath().normalize();

        System.out.println("Sauvegarde de la base Le Sourire");
        System.out.println("  Base        : " + url);
        System.out.println("  Destination : " + dossier);
        System.out.println("  Clé         : " + cleDeChiffrement());
        System.out.println();

        try {
            Files.createDirectories(dossier);
        } catch (IOException e) {
            System.err.println("Impossible de créer le dossier de sauvegarde " + dossier
                    + " : " + e.getMessage());
            return 1;
        }
        SauvegardeMysqldump.Coordonnees coordonnees;
        Path programme;
        try {
            coordonnees = SauvegardeMysqldump.analyserUrl(url);
            programme = SauvegardeMysqldump.trouverMysqldump(cheminMysqldump);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            return 1;
        }
        System.out.println("  Programme   : " + programme);

        Path dump = dossier.resolve(SauvegardeMysqldump.nomFichier(LocalDateTime.now()));
        try {
            SauvegardeMysqldump.executer(programme, coordonnees, utilisateur, motDePasse, dump);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            return 1;
        } catch (IOException e) {
            System.err.println("Échec de la sauvegarde : " + e.getMessage());
            return 1;
        }
        System.out.println();
        System.out.println("  Dump        : " + dump + " (" + taille(dump) + ")");
        try {
            Path copie = SauvegardeMysqldump.copierCle(cleDeChiffrement(), dump);
            if (copie != null) {
                System.out.println("  Clé copiée  : " + copie + " (" + taille(copie) + ")");
            } else {
                System.out.println("  ATTENTION   : clé de chiffrement introuvable — copiez-la"
                        + " manuellement avec le dump, sinon les données chiffrées seront illisibles.");
            }
        } catch (IOException e) {
            System.err.println("  ATTENTION   : copie de la clé impossible (" + e.getMessage() + ")");
        }
        System.out.println();
        System.out.println("Conservez le .sql ET le fichier .cle-chiffrement.key ensemble : l'un sans"
                + " l'autre ne permet pas de relire les données.");
        return 0;
    }

    /** Clé de chiffrement de l'installation : premier emplacement existant. */
    private static Path cleDeChiffrement() {
        for (Path candidat : CheminsInstallation.candidatsFichierCle()) {
            if (Files.isRegularFile(candidat)) {
                return candidat;
            }
        }
        return CheminsInstallation.emplacementGeneration();
    }

    private static Path racineInstallation() {
        Path racine = CheminsInstallation.racineExplicite();
        if (racine != null) {
            return racine;
        }
        Path dossier = CheminsInstallation.dossierJar();
        return dossier != null ? dossier : CheminsInstallation.repertoireCourant();
    }

    private static String taille(Path fichier) {
        try {
            long octets = Files.size(fichier);
            return octets > 1024 * 1024
                    ? (octets / (1024 * 1024)) + " Mo"
                    : Math.max(1, octets / 1024) + " Ko";
        } catch (IOException e) {
            return "taille inconnue";
        }
    }

    private static String parametre(String nom, String defaut) {
        String valeur = System.getenv(nom);
        return valeur == null || valeur.isBlank() ? defaut : valeur;
    }

    private static String valeur(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Valeur attendue après " + option);
        }
        return args[index];
    }
}
