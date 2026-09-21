package com.lesourire.serveur.outils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.lesourire.serveur.crypto.CheminsInstallation;
import com.lesourire.serveur.crypto.Chiffrement;

/**
 * Inventorie toutes les clés de chiffrement accessibles sur la machine : la
 * configuration, les emplacements connus de l'installation, puis, en dernier
 * recours, les fichiers {@code cle-chiffrement.key} et les archives ZIP/JAR qui
 * en contiennent (sauvegardes et paquets successifs).
 *
 * <p>Indispensable pour l'incident « données illisibles » : la clé réellement
 * utilisée à l'écriture se trouve souvent dans l'ancien dossier (ou dans son
 * archive) plutôt qu'à l'emplacement attendu.</p>
 */
final class ChercheurCles {

    /** Clé candidate : valeur base64 et provenance lisible. */
    record CleCandidate(String base64, String origine) {

        /** Empreinte courte utilisée pour l'affichage (jamais la clé). */
        String empreinte() {
            return Chiffrement.empreinte(base64);
        }
    }

    private static final String NOM_FICHIER = CheminsInstallation.NOM_FICHIER_CLE;
    private static final long TAILLE_MAX_ARCHIVE = 500L * 1024 * 1024;
    private static final int ARCHIVES_MAX = 300;
    private static final int DOSSIERS_MAX = 100_000;
    private static final int LONGUEUR_MAX_VALEUR = 200;

    /** Dossiers techniques ignorés lors du parcours (bruit et lenteur). */
    private static final Set<String> DOSSIERS_IGNORES = Set.of("$recycle.bin",
            "system volume information", "node_modules", ".git", ".cache", "__pycache__",
            "proc", "sys", "dev", "run");

    private ChercheurCles() {
    }

    /**
     * Clés candidates, sans doublon (première provenance conservée).
     *
     * @param racines       dossiers parcourus en dernier recours
     * @param profondeurMax profondeur maximale du parcours
     * @param remarque      journal des valeurs ignorées (fichiers illisibles, contenu invalide…)
     */
    static List<CleCandidate> chercher(List<Path> racines, int profondeurMax, Consumer<String> remarque) {
        Map<String, CleCandidate> cles = new LinkedHashMap<>();
        ajouterValeur(System.getenv("LESOURIRE_CHIFFREMENT_CLE"),
                "variable d'environnement LESOURIRE_CHIFFREMENT_CLE", cles, remarque);
        ajouterValeur(System.getProperty("lesourire.chiffrement.cle"),
                "propriété lesourire.chiffrement.cle", cles, remarque);
        ajouterCheminConfigure(System.getenv("LESOURIRE_CHIFFREMENT_FICHIER"),
                "variable LESOURIRE_CHIFFREMENT_FICHIER", cles, remarque);
        ajouterCheminConfigure(System.getProperty("lesourire.chiffrement.fichier"),
                "propriété lesourire.chiffrement.fichier", cles, remarque);
        for (Path candidat : CheminsInstallation.candidatsFichierCle()) {
            ajouterFichier(candidat, cles, remarque);
        }
        for (Path racine : racines) {
            parcourir(racine, profondeurMax, cles, remarque);
        }
        return new ArrayList<>(cles.values());
    }

    /** Racines de parcours proposées par défaut (paquet, dossier courant, téléchargements…). */
    static List<Path> racinesParDefaut() {
        List<Path> racines = new ArrayList<>();
        ajouterSiDossier(racines, CheminsInstallation.racineExplicite());
        ajouterSiDossier(racines, CheminsInstallation.dossierJar());
        for (Path ancetre : CheminsInstallation.ancetres(CheminsInstallation.repertoireCourant())) {
            ajouterSiDossier(racines, ancetre);
        }
        String maison = System.getProperty("user.home");
        if (maison != null) {
            Path dossier = Paths.get(maison);
            ajouterSiDossier(racines, dossier);
            ajouterSiDossier(racines, dossier.resolve("Téléchargements"));
            ajouterSiDossier(racines, dossier.resolve("Downloads"));
            ajouterSiDossier(racines, dossier.resolve("Bureau"));
            ajouterSiDossier(racines, dossier.resolve("Desktop"));
            ajouterSiDossier(racines, dossier.resolve("Documents"));
        }
        ajouterSiDossier(racines, Paths.get(System.getProperty("java.io.tmpdir", "/tmp")));
        return racines;
    }

    private static void ajouterSiDossier(List<Path> racines, Path dossier) {
        if (dossier != null && Files.isDirectory(dossier) && !racines.contains(dossier)) {
            racines.add(dossier);
        }
    }

    private static void ajouterCheminConfigure(String chemin, String origine,
            Map<String, CleCandidate> cles, Consumer<String> remarque) {
        if (chemin == null || chemin.isBlank()) {
            return;
        }
        Path brut = Paths.get(chemin.trim());
        List<Path> pistes = new ArrayList<>(2);
        if (brut.isAbsolute()) {
            pistes.add(brut);
        } else {
            Path racine = CheminsInstallation.racineExplicite();
            if (racine != null) {
                pistes.add(racine.resolve(brut));
            }
            pistes.add(CheminsInstallation.repertoireCourant().resolve(brut));
        }
        for (Path piste : pistes) {
            if (Files.isRegularFile(piste)) {
                ajouterFichier(piste, cles, remarque);
            } else if (piste.equals(pistes.get(pistes.size() - 1))) {
                remarque.accept("Chemin de clé introuvable (" + origine + ") : " + piste);
            }
        }
    }

    /** Parcourt un dossier à la recherche de fichiers de clé et d'archives qui en contiennent. */
    private static void parcourir(Path racine, int profondeurMax, Map<String, CleCandidate> cles,
            Consumer<String> remarque) {
        if (racine == null || !Files.isDirectory(racine)) {
            return;
        }
        try {
            Files.walkFileTree(racine, new Visiteur(racine, profondeurMax, cles, remarque));
        } catch (IOException e) {
            remarque.accept("Parcours interrompu de " + racine + " : " + e.getMessage());
        }
    }

    private static void ajouterFichier(Path fichier, Map<String, CleCandidate> cles,
            Consumer<String> remarque) {
        if (fichier == null || !Files.isRegularFile(fichier)) {
            return;
        }
        try {
            ajouterValeur(Files.readString(fichier, StandardCharsets.UTF_8).trim(),
                    "fichier " + fichier, cles, remarque);
        } catch (IOException e) {
            remarque.accept("Fichier de clé illisible : " + fichier + " (" + e.getMessage() + ")");
        }
    }

    /** Lit les entrées {@code …/cle-chiffrement.key} d'une archive ZIP ou JAR. */
    private static void lireArchive(Path archive, Map<String, CleCandidate> cles,
            Consumer<String> remarque) {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            Enumeration<? extends ZipEntry> entrees = zip.entries();
            while (entrees.hasMoreElements()) {
                ZipEntry entree = entrees.nextElement();
                if (entree.isDirectory() || !entree.getName().endsWith(NOM_FICHIER)) {
                    continue;
                }
                try (InputStream flux = zip.getInputStream(entree)) {
                    ajouterValeur(new String(flux.readAllBytes(), StandardCharsets.UTF_8).trim(),
                            "archive " + archive + " → " + entree.getName(), cles, remarque);
                }
            }
        } catch (IOException | UncheckedIOException e) {
            remarque.accept("Archive non lue : " + archive + " (" + e.getMessage() + ")");
        }
    }

    private static void ajouterValeur(String valeur, String origine, Map<String, CleCandidate> cles,
            Consumer<String> remarque) {
        if (valeur == null || valeur.isBlank()) {
            return;
        }
        String propre = valeur.trim();
        if (propre.length() > LONGUEUR_MAX_VALEUR) {
            remarque.accept("Contenu ignoré (trop long pour une clé) : " + origine);
            return;
        }
        if (cles.containsKey(propre)) {
            return;
        }
        try {
            Chiffrement.cleDepuisBase64(propre);
        } catch (IllegalStateException e) {
            remarque.accept("Contenu ignoré (" + origine + ") : " + e.getMessage());
            return;
        }
        cles.put(propre, new CleCandidate(propre, origine));
    }

    /** Parcours borné : profondeur, nombre de dossiers et d'archives limités. */
    private static final class Visiteur extends SimpleFileVisitor<Path> {

        private final Path racine;
        private final int profondeurMax;
        private final Map<String, CleCandidate> cles;
        private final Consumer<String> remarque;
        private int dossiersVisites;
        private int archivesOuvertes;

        Visiteur(Path racine, int profondeurMax, Map<String, CleCandidate> cles,
                Consumer<String> remarque) {
            this.racine = racine;
            this.profondeurMax = profondeurMax;
            this.cles = cles;
            this.remarque = remarque;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dossier, BasicFileAttributes attributs) {
            if (dossiersVisites++ > DOSSIERS_MAX) {
                return FileVisitResult.TERMINATE;
            }
            Path nom = dossier.getFileName();
            if (nom != null && DOSSIERS_IGNORES.contains(nom.toString().toLowerCase())) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return racine.relativize(dossier).getNameCount() > profondeurMax
                    ? FileVisitResult.SKIP_SUBTREE
                    : FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path fichier, BasicFileAttributes attributs) {
            String nom = fichier.getFileName().toString();
            if (nom.equalsIgnoreCase(NOM_FICHIER)) {
                ajouterFichier(fichier, cles, remarque);
            } else if (estArchive(nom) && attributs.size() <= TAILLE_MAX_ARCHIVE
                    && archivesOuvertes++ < ARCHIVES_MAX) {
                lireArchive(fichier, cles, remarque);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path fichier, IOException e) {
            return FileVisitResult.CONTINUE;
        }
    }

    private static boolean estArchive(String nom) {
        String minuscule = nom.toLowerCase();
        return minuscule.endsWith(".zip") || minuscule.endsWith(".jar");
    }
}
