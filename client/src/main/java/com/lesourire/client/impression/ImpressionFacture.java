package com.lesourire.client.impression;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.lesourire.commun.Facturation.StatutFacture;
import com.lesourire.commun.dto.FactureDTO;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/**
 * Ouverture d'une facture pour aperçu / impression.
 * <p>
 * Flux : génération d'un PDF temporaire → ouverture avec l'application
 * PDF par défaut de l'OS → l'utilisateur imprime depuis ce lecteur.
 * Le fichier est marqué {@code deleteOnExit} ; vous pouvez enrichir
 * {@link FacturePdf} sans toucher aux boutons de l'interface.
 */
public final class ImpressionFacture {

    private ImpressionFacture() {
    }

    /**
     * Génère le PDF de la facture et l'ouvre dans le lecteur système.
     * La facture doit être complète (lignes, et paiements si déjà chargés).
     */
    public static void imprimer(FactureDTO facture, Window proprietaire) {
        if (facture == null || facture.id == null) {
            alerter(proprietaire, "Impression impossible",
                    "Enregistrez d'abord la facture.");
            return;
        }
        if (facture.statut == StatutFacture.BROUILLON) {
            alerter(proprietaire, "Facture encore en brouillon",
                    "Émettez la facture avant de l'ouvrir pour impression.");
            return;
        }
        if (facture.statut == StatutFacture.ANNULEE) {
            alerter(proprietaire, "Facture annulée",
                    "Une facture annulée ne s'imprime pas.");
            return;
        }

        Path pdf;
        try {
            String suffixe = (facture.numero == null ? "facture" : facture.numero)
                    .replaceAll("[^A-Za-z0-9._-]", "_");
            pdf = Files.createTempFile("lesourire-" + suffixe + "-", ".pdf");
            FacturePdf.ecrire(facture, pdf);
            pdf.toFile().deleteOnExit();
        } catch (Exception e) {
            alerter(proprietaire, "Impossible de créer le PDF",
                    e.getMessage() == null ? e.toString() : e.getMessage());
            return;
        }

        try {
            ouvrirAvecApplicationParDefaut(pdf);
        } catch (Exception e) {
            alerter(proprietaire, "Impossible d'ouvrir le PDF",
                    "Le fichier a été créé ici :\n" + pdf + "\n\n"
                            + "Ouvrez-le manuellement via l'explorateur de fichiers.\n\n"
                            + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    /**
     * Sous Linux, {@link Desktop#open} bloque souvent l'UI JavaFX (AWT) :
     * on utilise {@code xdg-open} à la place.
     */
    private static void ouvrirAvecApplicationParDefaut(Path pdf) throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("windows")) {
            if (!Desktop.isDesktopSupported()
                    || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new IOException("Aucune application n'est associée aux fichiers PDF.");
            }
            Desktop.getDesktop().open(pdf.toFile());
            return;
        }
        // Linux / macOS : xdg-open (ou open sur Mac)
        String commande = os.contains("mac") ? "open" : "xdg-open";
        new ProcessBuilder(commande, pdf.toAbsolutePath().toString())
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
    }

    private static void alerter(Window proprietaire, String titre, String message) {
        Alert alerte = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alerte.setHeaderText(titre);
        if (proprietaire != null) {
            alerte.initOwner(proprietaire);
        }
        alerte.showAndWait();
    }
}
