package com.lesourire.serveur.outils;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Options de ligne de commande de {@link DiagnosticChiffrement}.
 *
 * <p>Les accès à la base reprennent par défaut les variables d'environnement du
 * serveur ({@code LESOURIRE_BD_URL}, {@code LESOURIRE_BD_UTILISATEUR},
 * {@code LESOURIRE_BD_MOT_DE_PASSE}), donc aucun réglage supplémentaire n'est
 * nécessaire sur le poste du cabinet.</p>
 */
final class OptionsDiagnostic {

    private Path rapport = Paths.get("DIAGNOSTIC-CHIFFREMENT.txt");
    private final List<Path> racines = new ArrayList<>();
    private boolean sansParcours;
    private int profondeur = 5;
    private int limite = 200;
    private boolean afficherCles;
    private Path exporterCle;
    private String verifierCle;
    private String rechiffrer;
    private boolean confirme;
    private boolean forcer;
    private boolean aide;
    private String url;
    private String utilisateur;
    private String motDePasse;

    private OptionsDiagnostic() {
        this.url = valeurEnvironnement("LESOURIRE_BD_URL", "jdbc:mariadb://localhost:3306/lesourire");
        this.utilisateur = valeurEnvironnement("LESOURIRE_BD_UTILISATEUR", "admin");
        this.motDePasse = valeurEnvironnement("LESOURIRE_BD_MOT_DE_PASSE", "csa-soft");
    }

    /** Analyse les arguments ; lève {@link IllegalArgumentException} si un argument est invalide. */
    static OptionsDiagnostic analyser(String... args) {
        OptionsDiagnostic options = new OptionsDiagnostic();
        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            switch (argument) {
                case "--rapport" -> options.rapport = Paths.get(valeur(args, ++i, argument));
                case "--racine" -> options.racines.add(Paths.get(valeur(args, ++i, argument)));
                case "--sans-parcours" -> options.sansParcours = true;
                case "--profondeur" -> options.profondeur = entier(valeur(args, ++i, argument), argument);
                case "--limite" -> options.limite = entier(valeur(args, ++i, argument), argument);
                case "--afficher-cles" -> options.afficherCles = true;
                case "--exporter-cle" -> options.exporterCle = Paths.get(valeur(args, ++i, argument));
                case "--verifier-cle" -> options.verifierCle = valeur(args, ++i, argument);
                case "--rechiffrer" -> options.rechiffrer = valeur(args, ++i, argument);
                case "--je-confirme" -> options.confirme = true;
                case "--forcer" -> options.forcer = true;
                case "--bd-url" -> options.url = valeur(args, ++i, argument);
                case "--bd-utilisateur" -> options.utilisateur = valeur(args, ++i, argument);
                case "--bd-mot-de-passe" -> options.motDePasse = valeur(args, ++i, argument);
                case "--aide", "-h", "--help" -> options.aide = true;
                default -> throw new IllegalArgumentException("Option inconnue : " + argument);
            }
        }
        return options;
    }

    private static String valeur(String[] args, int index, String argument) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Valeur attendue après " + argument);
        }
        return args[index];
    }

    private static int entier(String valeur, String argument) {
        try {
            int nombre = Integer.parseInt(valeur);
            if (nombre < 0) {
                throw new NumberFormatException(valeur);
            }
            return nombre;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nombre entier positif attendu après " + argument
                    + " (reçu : " + valeur + ")");
        }
    }

    /** Aide affichée en console (le rapport texte reste la référence). */
    static void aide(PrintStream sortie) {
        sortie.println("""
                Diagnostic du chiffrement Le Sourire — inventorie les clés et relit les données.

                Usage : Diagnostic-Chiffrement [options]

                --rapport <fichier>      Rapport texte à écrire (défaut DIAGNOSTIC-CHIFFREMENT.txt)
                --racine <dossier>       Dossier supplémentaire à explorer (répétable)
                --sans-parcours          N'explore que les emplacements connus de l'installation
                --profondeur <n>         Profondeur d'exploration des dossiers (défaut 5)
                --limite <n>             Valeurs examinées par colonne, 0 = toutes (défaut 200)
                --afficher-cles          Affiche les clés trouvées en base64 (rapport à protéger)
                --exporter-cle <fichier> Écrit la clé qui déchiffre les données dans ce fichier
                --verifier-cle <cle|fichier>
                                         Teste une clé : code 0 si toutes les données sont lisibles
                --rechiffrer <cle|fichier>
                                         Réécrit toutes les données avec cette clé unique
                                         (exige --je-confirme, sauvegarde automatique)
                --je-confirme            Confirme le réchiffrement (aucune écriture sans lui)
                --forcer                 Poursuit malgré une sauvegarde impossible ou des illisibles
                --bd-url / --bd-utilisateur / --bd-mot-de-passe
                                         Connexion MariaDB (défaut : variables LESOURIRE_BD_*)
                --aide                   Affiche cette aide

                Codes de sortie : 0 données lisibles, 1 erreur, 2 données illisibles,
                                  3 réchiffrement partiel.""");
    }

    private static String valeurEnvironnement(String nom, String defaut) {
        String valeur = System.getenv(nom);
        return valeur == null || valeur.isBlank() ? defaut : valeur;
    }

    Path rapport() {
        return rapport;
    }

    /** Dossiers à explorer : ceux demandés, sinon les emplacements probables de la machine. */
    List<Path> racines() {
        if (sansParcours) {
            return List.of();
        }
        return racines.isEmpty() ? ChercheurCles.racinesParDefaut() : List.copyOf(racines);
    }

    int profondeur() {
        return profondeur;
    }

    /** Valeurs examinées par colonne (0 = toutes). */
    int limite() {
        return limite;
    }

    boolean afficherCles() {
        return afficherCles;
    }

    Path exporterCle() {
        return exporterCle;
    }

    String verifierCle() {
        return verifierCle;
    }

    String rechiffrer() {
        return rechiffrer;
    }

    boolean confirme() {
        return confirme;
    }

    boolean forcer() {
        return forcer;
    }

    boolean aide() {
        return aide;
    }

    String url() {
        return url;
    }

    String utilisateur() {
        return utilisateur;
    }

    String motDePasse() {
        return motDePasse;
    }
}
