package com.lesourire.serveur.outils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.spec.SecretKeySpec;

import com.lesourire.serveur.crypto.CheminsInstallation;
import com.lesourire.serveur.crypto.Chiffrement;
import com.lesourire.serveur.crypto.ColonnesChiffrees;

/**
 * Lecture des données chiffrées et identification de la clé (ou des clés)
 * utilisée(s) pour chaque valeur.
 *
 * <p>Les colonnes sont relues une seule fois par lots successifs, puis
 * confrontées aux clés candidates : cela évite un accès disque par valeur et
 * permet de repérer des lignes écrites avec des clés différentes
 * (accumulation de clés au fil des réinstallations).</p>
 */
final class MoteurDiagnostic {

    /** Décomposition d'une URL JDBC MariaDB servant à appeler {@code mysqldump}. */
    private static final Pattern JDBC =
            Pattern.compile("jdbc:mariadb://([^:/]+)(?::(\\d+))?/([^?]+)");

    /** Valeur chiffrée et référence de la ligne qui la porte. */
    record Valeur(long id, String table, String colonne, String stocke) {

        String reference() {
            return table + "." + colonne + " (id " + id + ")";
        }
    }

    /** Résultat de l'analyse des valeurs d'une colonne. */
    record AnalyseColonne(String table, String colonne, int valeursTotales, int valeursChiffrees,
            int valeursLisibles, int valeursIlisibles) {

        boolean saine() {
            return valeursIlisibles == 0;
        }
    }

    /** Analyse complète : valeurs examinées, résultats par colonne et usage des clés. */
    record Analyse(List<Valeur> valeurs, Map<String, AnalyseColonne> colonnes,
            Map<String, Integer> usageParCle, Map<String, List<Valeur>> orphelines) {

        boolean toutesLisibles() {
            return orphelines.isEmpty();
        }
    }

    private MoteurDiagnostic() {
    }

    /**
     * Ouvre une connexion JDBC sur la base indiquée. Le pilote MariaDB est
     * embarqué dans le serveur packagé.
     */
    static Connection ouvrir(OptionsDiagnostic options) throws SQLException {
        return DriverManager.getConnection(options.url(), options.utilisateur(), options.motDePasse());
    }

    /** Lecture des colonnes chiffrées, limitée à {@code limite} valeurs par colonne (0 = toutes). */
    static List<Valeur> lireValeurs(Connection connexion, int limite) throws SQLException {
        List<Valeur> valeurs = new ArrayList<>();
        for (ColonnesChiffrees.ColonneChiffree colonne : ColonnesChiffrees.COLONNES) {
            valeurs.addAll(lireColonne(connexion, colonne, limite));
        }
        return valeurs;
    }

    private static List<Valeur> lireColonne(Connection connexion, ColonnesChiffrees.ColonneChiffree colonne,
            int limite) throws SQLException {
        String sql = "SELECT id, " + colonne.colonne() + " FROM " + colonne.table()
                + " WHERE " + colonne.colonne() + " LIKE '" + Chiffrement.PREFIXE + "%'"
                + " ORDER BY id" + (limite > 0 ? " LIMIT " + limite : "");
        List<Valeur> valeurs = new ArrayList<>();
        try (PreparedStatement requete = connexion.prepareStatement(sql);
                ResultSet resultats = requete.executeQuery()) {
            while (resultats.next()) {
                valeurs.add(new Valeur(resultats.getLong("id"), colonne.table(), colonne.colonne(),
                        resultats.getString(colonne.colonne())));
            }
        }
        return valeurs;
    }

    /**
     * Confronte chaque valeur aux clés candidates : la première clé qui la
     * déchiffre est retenue (« clé gagnante » de la ligne), les valeurs qu'aucune
     * clé ne relit sont isolées.
     */
    static Analyse analyser(List<Valeur> valeurs, List<ChercheurCles.CleCandidate> cles) {
        Map<String, SecretKeySpec> preparees = new LinkedHashMap<>();
        for (ChercheurCles.CleCandidate candidate : cles) {
            preparees.put(candidate.base64(), Chiffrement.cleDepuisBase64(candidate.base64()));
        }
        Map<String, Integer> usage = new LinkedHashMap<>();
        Map<String, List<Valeur>> orphelines = new LinkedHashMap<>();
        Map<String, AnalyseColonne> colonnes = new LinkedHashMap<>();
        for (Valeur valeur : valeurs) {
            if (!Chiffrement.estChiffre(valeur.stocke())) {
                // Les lectures filtrent déjà sur « enc:v1: » ; par sécurité, une valeur
                // en clair n'est ni comptée ni déclarée illisible.
                continue;
            }
            String gagnante = null;
            for (Map.Entry<String, SecretKeySpec> cle : preparees.entrySet()) {
                if (Chiffrement.dechiffrableAvec(valeur.stocke(), cle.getValue())) {
                    gagnante = cle.getKey();
                    break;
                }
            }
            if (gagnante != null) {
                usage.merge(gagnante, 1, Integer::sum);
            } else {
                orphelines.computeIfAbsent(valeur.reference(), k -> new ArrayList<>()).add(valeur);
            }
            colonnes.merge(valeur.table() + "." + valeur.colonne(),
                    new AnalyseColonne(valeur.table(), valeur.colonne(), 1, 1,
                            gagnante != null ? 1 : 0, gagnante == null ? 1 : 0),
                    MoteurDiagnostic::cumuler);
        }
        return new Analyse(valeurs, colonnes, usage, orphelines);
    }

    private static AnalyseColonne cumuler(AnalyseColonne gauche, AnalyseColonne droite) {
        return new AnalyseColonne(gauche.table(), gauche.colonne(),
                gauche.valeursTotales() + droite.valeursTotales(),
                gauche.valeursChiffrees() + droite.valeursChiffrees(),
                gauche.valeursLisibles() + droite.valeursLisibles(),
                gauche.valeursIlisibles() + droite.valeursIlisibles());
    }

    /** Colonne d'une analyse, ou colonne vide si elle ne contient aucune valeur chiffrée. */
    static AnalyseColonne colonne(Map<String, AnalyseColonne> analyse, String table, String colonne) {
        AnalyseColonne resultat = analyse.get(table + "." + colonne);
        return resultat == null ? new AnalyseColonne(table, colonne, 0, 0, 0, 0) : resultat;
    }

    /** Clé candidate correspondant à un base64, ou {@code null}. */
    static ChercheurCles.CleCandidate candidate(List<ChercheurCles.CleCandidate> cles, String base64) {
        for (ChercheurCles.CleCandidate cle : cles) {
            if (cle.base64().equals(base64)) {
                return cle;
            }
        }
        return null;
    }

    /**
     * Clé fournie soit directement en base64, soit par le chemin d'un fichier
     * {@code cle-chiffrement.key} ; {@code null} si la valeur n'est ni l'un ni
     * l'autre.
     */
    static String resoudreCle(String valeurOuChemin) {
        if (valeurOuChemin == null || valeurOuChemin.isBlank()) {
            return null;
        }
        String valeur = valeurOuChemin.trim();
        try {
            Chiffrement.cleDepuisBase64(valeur);
            return valeur;
        } catch (IllegalStateException e) {
            // Pas une clé : on tente un fichier.
        }
        Path fichier = Paths.get(valeur);
        if (!fichier.isAbsolute()) {
            fichier = CheminsInstallation.repertoireCourant().resolve(fichier);
        }
        if (!Files.isRegularFile(fichier)) {
            return null;
        }
        try {
            String contenu = Files.readString(fichier, StandardCharsets.UTF_8).trim();
            Chiffrement.cleDepuisBase64(contenu);
            return contenu;
        } catch (IOException | IllegalStateException e) {
            return null;
        }
    }

    /** Vrai si un serveur Le Sourire répond déjà sur le port (aucune écriture tolérée). */
    static boolean serveurActif(int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", port), 800);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Sauvegarde de la base par {@code mysqldump} avant un réchiffrement.
     *
     * @throws IOException si {@code mysqldump} est absent ou échoue
     */
    static Path sauvegarderBase(OptionsDiagnostic options, Path dossier) throws IOException {
        java.util.regex.Matcher analyse = JDBC.matcher(options.url());
        if (!analyse.find()) {
            throw new IOException("URL JDBC non reconnue : " + options.url());
        }
        String hote = analyse.group(1);
        String port = analyse.group(2) != null ? analyse.group(2) : "3306";
        String base = analyse.group(3);
        Files.createDirectories(dossier);
        Path cible = dossier.resolve("sauvegarde-avant-rechiffrement-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".sql");
        ProcessBuilder constructeur = new ProcessBuilder("mysqldump", "-h", hote, "-P", port,
                "-u", options.utilisateur(), "--single-transaction", "--routines", "--triggers", base);
        constructeur.environment().put("MYSQL_PWD", options.motDePasse());
        constructeur.redirectOutput(cible.toFile());
        constructeur.redirectError(ProcessBuilder.Redirect.INHERIT);
        try {
            Process processus = constructeur.start();
            int code = processus.waitFor();
            if (code != 0) {
                throw new IOException("mysqldump a échoué (code " + code + ")");
            }
        } catch (IOException e) {
            throw new IOException("mysqldump est introuvable ou inutilisable (" + e.getMessage()
                    + ") : ajoutez le dossier bin de MariaDB au PATH, ou sauvegardez la base "
                    + "manuellement puis relancez avec --forcer", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Sauvegarde interrompue.", e);
        }
        return cible;
    }
}
