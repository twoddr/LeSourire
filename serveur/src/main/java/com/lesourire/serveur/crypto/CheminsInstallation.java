package com.lesourire.serveur.crypto;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Résolution des fichiers d'installation (clé de chiffrement, sauvegardes…)
 * <strong>indépendamment du répertoire de travail</strong>.
 *
 * <p>Historiquement la clé {@code fichiers/cle-chiffrement.key} était résolue
 * relativement à {@code user.dir} : le serveur packagé, lancé avec un répertoire
 * de travail différent (ou après ré-extraction du paquet dans un nouveau
 * dossier), ne retrouvait plus la clé, en générait une autre et rendait les
 * données déjà chiffrées illisibles. Les lanceurs transmettent désormais la
 * racine du paquet via {@code -Dlesourire.home} (ou {@code LESOURIRE_HOME}) ;
 * les emplacements ci-dessous servent de replis déterministes.</p>
 */
public final class CheminsInstallation {

    /** Nom du fichier de clé, à la racine du dossier {@code fichiers/}. */
    public static final String NOM_FICHIER_CLE = "cle-chiffrement.key";

    /** Nom du dossier de la clé, relatif à la racine de l'installation. */
    public static final String DOSSIER_FICHIERS = "fichiers";

    /** Propriété système positionnée par les lanceurs (racine du paquet). */
    public static final String PROPRIETE_HOME = "lesourire.home";

    /** Variable d'environnement équivalente à {@link #PROPRIETE_HOME}. */
    public static final String VARIABLE_HOME = "LESOURIRE_HOME";

    /** Nombre de dossiers parents examinés autour du JAR et du répertoire courant. */
    private static final int NIVEAUX_ANCETRES = 4;

    private CheminsInstallation() {
    }

    /** Racine transmise explicitement par le lanceur, ou {@code null}. */
    public static Path racineExplicite() {
        String parPropriete = System.getProperty(PROPRIETE_HOME);
        if (parPropriete != null && !parPropriete.isBlank()) {
            return normaliser(Paths.get(parPropriete.trim()));
        }
        String parVariable = System.getenv(VARIABLE_HOME);
        if (parVariable != null && !parVariable.isBlank()) {
            return normaliser(Paths.get(parVariable.trim()));
        }
        return null;
    }

    /**
     * Dossier de l'archive (ou des classes) en cours d'exécution, ou
     * {@code null} si l'emplacement n'est pas déterminable.
     */
    public static Path dossierJar() {
        try {
            URI uri = CheminsInstallation.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            Path chemin = Paths.get(uri);
            return Files.isDirectory(chemin) ? normaliser(chemin) : normaliser(chemin.getParent());
        } catch (Exception e) {
            return null;
        }
    }

    /** Répertoire courant de la JVM ({@code user.dir}). */
    public static Path repertoireCourant() {
        return normaliser(Paths.get(System.getProperty("user.dir", ".")));
    }

    /**
     * Racine du projet compilé ({@code pom.xml}) autour du JAR ou du répertoire
     * courant : évite de générer une clé dans {@code target/} en développement.
     * {@code null} si aucune racine de projet n'est trouvée (installation packagée).
     */
    public static Path racineProjet() {
        for (Path depart : new Path[]{dossierJar(), repertoireCourant()}) {
            for (Path candidat : ancetres(depart)) {
                if (Files.isRegularFile(candidat.resolve("pom.xml"))) {
                    return candidat;
                }
            }
        }
        return null;
    }

    /**
     * Emplacements de la clé pour une racine donnée, par ordre de priorité :
     * {@code fichiers/}, puis {@code serveur/fichiers/} (emplacement hérité des
     * paquets Windows 0.1.x), puis la racine elle-même.
     */
    public static List<Path> emplacementsCle(Path racine) {
        if (racine == null) {
            return List.of();
        }
        List<Path> emplacements = new ArrayList<>(3);
        emplacements.add(racine.resolve(DOSSIER_FICHIERS).resolve(NOM_FICHIER_CLE));
        emplacements.add(racine.resolve("serveur").resolve(DOSSIER_FICHIERS).resolve(NOM_FICHIER_CLE));
        emplacements.add(racine.resolve(NOM_FICHIER_CLE));
        return emplacements;
    }

    /**
     * Candidats de lecture de la clé, du plus prioritaire au moins prioritaire :
     * racine explicite du lanceur, dossier du JAR et ses parents, répertoire
     * courant et ses parents (couvre l'exécution depuis la racine du projet).
     * La liste est dédoublonnée et dans un ordre stable.
     */
    public static List<Path> candidatsFichierCle() {
        Set<Path> candidats = new LinkedHashSet<>();
        candidats.addAll(emplacementsCle(racineExplicite()));
        for (Path racine : ancetres(dossierJar())) {
            candidats.addAll(emplacementsCle(racine));
        }
        for (Path racine : ancetres(repertoireCourant())) {
            candidats.addAll(emplacementsCle(racine));
        }
        return List.copyOf(candidats);
    }

    /**
     * Emplacement par défaut pour {@code fichiers/cle-chiffrement.key} : racine
     * du paquet donnée par le lanceur, sinon racine du projet compilé, sinon
     * dossier du JAR, sinon répertoire courant.
     */
    public static Path emplacementGeneration() {
        Path[] racines = {racineExplicite(), racineProjet(), dossierJar(), repertoireCourant()};
        for (Path racine : racines) {
            if (racine != null) {
                return racine.resolve(DOSSIER_FICHIERS).resolve(NOM_FICHIER_CLE);
            }
        }
        throw new IllegalStateException("Aucun emplacement disponible pour la clé de chiffrement.");
    }

    /** Le chemin et ses parents (du plus proche au plus lointain), bornés. */
    public static List<Path> ancetres(Path depart) {
        if (depart == null) {
            return List.of();
        }
        List<Path> liste = new ArrayList<>(NIVEAUX_ANCETRES + 1);
        Path courant = normaliser(depart);
        for (int niveau = 0; niveau <= NIVEAUX_ANCETRES && courant != null; niveau++) {
            liste.add(courant);
            Path parent = courant.getParent();
            if (parent == null || parent.equals(courant)) {
                courant = null;
            } else {
                courant = normaliser(parent);
            }
        }
        return liste;
    }

    private static Path normaliser(Path chemin) {
        return chemin == null ? null : chemin.toAbsolutePath().normalize();
    }
}
