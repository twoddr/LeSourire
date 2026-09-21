package com.lesourire.serveur.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Sauvegarde de la base par {@code mysqldump} : lecture de l'URL JDBC, recherche
 * du programme et lancement du dump.
 *
 * <p>Classe utilitaire partagée par le module Sauvegardes du client
 * ({@link SauvegardeService}) et par l'outil en ligne de commande
 * {@code Sauvegarder-Base} livré dans les paquets : une seule implémentation,
 * donc un seul comportement (et un seul jeu de tests).</p>
 *
 * <p>Sous Windows, {@code mysqldump} n'est pas toujours dans le « PATH » : on
 * cherche donc également dans les dossiers d'installation habituels
 * ({@code C:\Program Files\MariaDB *\bin\mysqldump.exe}).</p>
 */
public final class SauvegardeMysqldump {

    /** Coordonnées de connexion extraites de l'URL JDBC. */
    public record Coordonnees(String hote, String port, String base) {
    }

    /** Nom de la clé de chiffrement à copier avec chaque sauvegarde. */
    public static final String NOM_CLE = "cle-chiffrement.key";

    private static final Pattern JDBC = Pattern.compile("jdbc:mariadb://([^:/]+)(?::(\\d+))?/([^?]+)");
    private static final DateTimeFormatter FORMAT_FICHIER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String PROGRAMME = "mysqldump";

    private SauvegardeMysqldump() {
    }

    /** Coordonnées (hôte, port, base) lues dans l'URL JDBC du serveur. */
    public static Coordonnees analyserUrl(String jdbcUrl) {
        Matcher analyse = JDBC.matcher(jdbcUrl == null ? "" : jdbcUrl);
        if (!analyse.find()) {
            throw new IllegalStateException("URL JDBC non reconnue : " + jdbcUrl);
        }
        return new Coordonnees(analyse.group(1),
                analyse.group(2) != null ? analyse.group(2) : "3306",
                analyse.group(3));
    }

    /** Nom du fichier de sauvegarde, par exemple {@code lesourire-20260921-073000.sql}. */
    public static String nomFichier(LocalDateTime moment) {
        return "lesourire-" + moment.format(FORMAT_FICHIER) + ".sql";
    }

    /** Nom de la copie de la clé associée à une sauvegarde. */
    public static String nomCle(String nomFichierSql) {
        return nomFichierSql.replaceAll("\\.sql(\\.gz)?$", "") + "." + NOM_CLE;
    }

    /**
     * Chemin du programme {@code mysqldump} : celui indiqué s'il est renseigné,
     * sinon le « PATH », sinon les emplacements d'installation habituels.
     *
     * @param cheminConfigure valeur de {@code lesourire.sauvegarde.mysqldump}, ou {@code null}
     * @throws IllegalStateException si le programme reste introuvable (message actionnable)
     */
    public static Path trouverMysqldump(String cheminConfigure) {
        if (cheminConfigure != null && !cheminConfigure.isBlank()) {
            Path chemin = Paths.get(cheminConfigure.trim());
            if (Files.isRegularFile(chemin)) {
                return chemin;
            }
            throw new IllegalStateException("mysqldump est introuvable à l'emplacement indiqué : " + chemin
                    + " (vérifiez lesourire.sauvegarde.mysqldump)");
        }
        Path dansLePath = chercherDansLePath();
        if (dansLePath != null) {
            return dansLePath;
        }
        Path installation = chercherDansLesInstallations();
        if (installation != null) {
            return installation;
        }
        throw new IllegalStateException("mysqldump est introuvable : ajoutez le dossier bin de MariaDB "
                + "au PATH de Windows, ou indiquez le programme (variable LESOURIRE_MYSQLDUMP / "
                + "propriété lesourire.sauvegarde.mysqldump).");
    }

    private static Path chercherDansLePath() {
        String path = System.getenv("PATH");
        if (path == null) {
            return null;
        }
        for (String dossier : path.split(Pattern.quote(java.io.File.pathSeparator))) {
            if (dossier.isBlank()) {
                continue;
            }
            Path candidat = Paths.get(dossier, nomProgramme());
            if (Files.isRegularFile(candidat)) {
                return candidat;
            }
        }
        return null;
    }

    /** Emplacements d'installation classiques, du plus récent au plus ancien. */
    private static Path chercherDansLesInstallations() {
        List<Path> candidats = new ArrayList<>();
        for (Path racine : racinesProgramFiles()) {
            try (Stream<Path> dossiers = Files.list(racine)) {
                dossiers.filter(Files::isDirectory)
                        .filter(dossier -> {
                            String nom = dossier.getFileName().toString().toLowerCase(Locale.ROOT);
                            return nom.startsWith("mariadb") || nom.startsWith("mysql");
                        })
                        .sorted(Comparator.reverseOrder())
                        .forEach(dossier -> candidats.add(dossier.resolve("bin").resolve(nomProgramme())));
            } catch (IOException e) {
                // dossier illisible : on passe au suivant
            }
        }
        candidats.add(Paths.get("/usr/bin", nomProgramme()));
        candidats.add(Paths.get("/usr/local/bin", nomProgramme()));
        candidats.add(Paths.get("/opt/homebrew/bin", nomProgramme()));
        candidats.add(Paths.get("/usr/local/mysql/bin", nomProgramme()));
        for (Path candidat : candidats) {
            if (Files.isRegularFile(candidat)) {
                return candidat;
            }
        }
        return null;
    }

    private static List<Path> racinesProgramFiles() {
        List<Path> racines = new ArrayList<>();
        if (!sousWindows()) {
            return racines;
        }
        String programFiles = System.getenv("ProgramFiles");
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        if (programFiles != null) {
            racines.add(Paths.get(programFiles));
        }
        if (programFilesX86 != null) {
            racines.add(Paths.get(programFilesX86));
        }
        return racines;
    }

    private static boolean sousWindows() {
        return FileSystems.getDefault().getSeparator().equals("\\");
    }

    private static String nomProgramme() {
        return sousWindows() ? PROGRAMME + ".exe" : PROGRAMME;
    }

    /**
     * Lance {@code mysqldump} et écrit le dump dans {@code cible}.
     *
     * @throws IllegalStateException si {@code mysqldump} échoue (message contenant sa sortie)
     * @throws IOException si le programme ne peut pas être exécuté
     */
    public static void executer(Path mysqldump, Coordonnees coordonnees, String utilisateur,
            String motDePasse, Path cible) throws IOException {
        List<String> commande = new ArrayList<>();
        commande.add(mysqldump.toString());
        commande.add("-h");
        commande.add(coordonnees.hote());
        commande.add("-P");
        commande.add(coordonnees.port());
        if (utilisateur != null && !utilisateur.isBlank()) {
            commande.add("-u");
            commande.add(utilisateur);
        }
        commande.add("--single-transaction");
        commande.add("--routines");
        commande.add("--triggers");
        commande.add(coordonnees.base());
        ProcessBuilder constructeur = new ProcessBuilder(commande);
        if (motDePasse != null && !motDePasse.isBlank()) {
            constructeur.environment().put("MYSQL_PWD", motDePasse);
        }
        constructeur.redirectOutput(cible.toFile());
        constructeur.redirectErrorStream(true);
        try {
            Process processus = constructeur.start();
            int code = processus.waitFor();
            if (code != 0) {
                String sortie = finDeFichier(cible);
                Files.deleteIfExists(cible);
                throw new IllegalStateException("mysqldump a échoué (code " + code + ") : " + sortie);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Files.deleteIfExists(cible);
            throw new IOException("Sauvegarde interrompue.", e);
        } catch (IOException e) {
            Files.deleteIfExists(cible);
            throw new IOException("mysqldump n'a pas pu être exécuté (" + e.getMessage() + ")", e);
        }
    }

    private static String finDeFichier(Path fichier) {
        try {
            String contenu = Files.readString(fichier, StandardCharsets.UTF_8).trim();
            return contenu.length() <= 400 ? contenu : contenu.substring(contenu.length() - 400);
        } catch (IOException e) {
            return "(sortie de mysqldump illisible)";
        }
    }

    /**
     * Copie la clé de chiffrement à côté du dump — une sauvegarde de la base
     * seule ne permet pas de relire les données.
     *
     * @return le fichier copié, ou {@code null} si la clé est absente
     */
    public static Path copierCle(Path cle, Path dump) throws IOException {
        if (cle == null || !Files.isRegularFile(cle)) {
            return null;
        }
        Path cible = dump.resolveSibling(nomCle(dump.getFileName().toString()));
        Files.copy(cle, cible, StandardCopyOption.REPLACE_EXISTING);
        return cible;
    }
}
