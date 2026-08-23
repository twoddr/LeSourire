package com.lesourire.serveur;

import java.awt.GraphicsEnvironment;
import java.net.BindException;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * Affiche un message lisible quand le serveur ne peut pas démarrer
 * (port occupé, base injoignable, etc.), au lieu de laisser seule
 * une longue stack trace dans la console.
 */
final class DemarrageEchec {

    private DemarrageEchec() {
    }

    static void afficher(Throwable erreur) {
        String message = diagnostiquer(erreur);
        System.err.println();
        System.err.println("=== Le Sourire — serveur non démarré ===");
        System.err.println(message);
        System.err.println();

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // look & feel optionnel
        }
        try {
            JOptionPane.showMessageDialog(
                    null,
                    message,
                    "Le Sourire — Impossible de démarrer le serveur",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ignored) {
            // Pas d'affichage graphique possible (SSH, service Windows sans bureau…)
        }
    }

    static String diagnostiquer(Throwable erreur) {
        String port = System.getenv().getOrDefault("LESOURIRE_PORT", "8420");
        Throwable cause = causeRacine(erreur);
        String texte = chaineMessages(erreur);

        if (estPortOccupe(cause, texte)) {
            return """
                    Le serveur n'a pas pu démarrer : le port %s est déjà utilisé.

                    Un autre processus Le Sourire (ou une ancienne instance) tourne
                    probablement encore.

                    Que faire :
                    • Fermez l'autre fenêtre / arrêttez l'ancien serveur, puis relancez.
                    • Ou changez le port (variable LESOURIRE_PORT) dans config.bat.
                    """.formatted(port).strip();
        }

        if (contient(texte, "Access denied", "Access denied for user", "1045")) {
            return """
                    Le serveur n'a pas pu démarrer : accès refusé à MariaDB.

                    Vérifiez l'utilisateur et le mot de passe dans config.bat
                    (LESOURIRE_BD_UTILISATEUR / LESOURIRE_BD_MOT_DE_PASSE)
                    et que ce compte a les droits sur la base « lesourire ».
                    """.strip();
        }

        if (contient(texte, "Connection refused", "Connexion refusée", "Communications link failure",
                "Could not connect", "08001", "08000")) {
            return """
                    Le serveur n'a pas pu démarrer : MariaDB est injoignable.

                    Vérifiez que le service MariaDB (ou MySQL) est démarré,
                    et que l'URL dans config.bat (LESOURIRE_BD_URL) est correcte
                    — en général jdbc:mariadb://127.0.0.1:3306/lesourire.
                    """.strip();
        }

        if (contient(texte, "Flyway", "Migration", "Validate failed", "checksum")) {
            return """
                    Le serveur n'a pas pu démarrer : problème de migration de la base.

                    La base n'est peut‑être pas au bon niveau de version, ou un
                    script Flyway a échoué. Consultez la console pour le détail,
                    ou contactez le support technique avec ce message :

                    %s
                    """.formatted(resumeCourt(cause, texte)).strip();
        }

        if (contient(texte, "Schema-validation", "ddl-auto")) {
            return """
                    Le serveur n'a pas pu démarrer : le schéma de la base ne
                    correspond pas à cette version du programme.

                    Mettez à jour la base (redémarrage avec un serveur à jour
                    pour laisser Flyway migrer), ou restaurez une sauvegarde
                    compatible.

                    Détail : %s
                    """.formatted(resumeCourt(cause, texte)).strip();
        }

        return """
                Le serveur n'a pas pu démarrer.

                %s

                Consultez la console pour la pile d'erreurs complète si besoin.
                """.formatted(resumeCourt(cause, texte)).strip();
    }

    private static boolean estPortOccupe(Throwable cause, String texte) {
        if (cause instanceof BindException) {
            return true;
        }
        return contient(texte, "Address already in use", "Adresse déjà utilisée",
                "BindException", "port out of range");
    }

    private static boolean contient(String texte, String... fragments) {
        if (texte == null) {
            return false;
        }
        String bas = texte.toLowerCase();
        for (String f : fragments) {
            if (bas.contains(f.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static Throwable causeRacine(Throwable erreur) {
        Throwable courant = erreur;
        while (courant.getCause() != null && courant.getCause() != courant) {
            courant = courant.getCause();
        }
        return courant;
    }

    private static String chaineMessages(Throwable erreur) {
        StringBuilder sb = new StringBuilder();
        Throwable courant = erreur;
        while (courant != null) {
            if (courant.getMessage() != null) {
                sb.append(courant.getClass().getSimpleName())
                        .append(": ")
                        .append(courant.getMessage())
                        .append('\n');
            }
            courant = courant.getCause();
        }
        return sb.toString();
    }

    private static String resumeCourt(Throwable cause, String texte) {
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            String m = cause.getMessage().trim();
            return m.length() > 400 ? m.substring(0, 400) + "…" : m;
        }
        String t = texte == null ? "" : texte.trim();
        return t.length() > 400 ? t.substring(0, 400) + "…" : t;
    }
}
