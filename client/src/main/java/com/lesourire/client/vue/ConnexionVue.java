package com.lesourire.client.vue;

import java.util.prefs.Preferences;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import com.lesourire.client.coeur.ApiClient;
import com.lesourire.client.coeur.Session;
import com.lesourire.commun.Role;
import com.lesourire.commun.dto.UtilisateurDTO;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Écran de connexion : identifiants + adresse du serveur (repliée). */
public class ConnexionVue {

    private static final Preferences PREFS = Preferences.userNodeForPackage(ConnexionVue.class);
    private static final String PREF_URL_SERVEUR = "urlServeur";

    private final StackPane racine = new StackPane();
    private final Runnable onConnexionReussie;

    private final TextField champUtilisateur = new TextField();
    private final PasswordField champMotDePasse = new PasswordField();
    private final TextField champServeur = new TextField();
    private final Button boutonConnexion = new Button("Se connecter");
    private final Label labelErreur = new Label();
    private final ProgressIndicator indicateur = new ProgressIndicator();

    public ConnexionVue(Runnable onConnexionReussie) {
        this.onConnexionReussie = onConnexionReussie;
        construire();
    }

    public Node getRacine() {
        return racine;
    }

    private void construire() {
        racine.getStyleClass().add("fond-connexion");

        // En-tête
        Label badge = new Label("LS");
        badge.getStyleClass().add("badge-logo");
        Label titre = new Label("Le Sourire");
        titre.getStyleClass().add("titre-application");
        Label sousTitre = new Label("Cabinet Dentaire — Douala");
        sousTitre.getStyleClass().add("sous-titre-application");
        VBox entete = new VBox(8, badge, titre, sousTitre);
        entete.setAlignment(Pos.CENTER);

        // Formulaire
        champUtilisateur.setPromptText("Nom d'utilisateur");
        champMotDePasse.setPromptText("Mot de passe");
        champServeur.setText(PREFS.get(PREF_URL_SERVEUR, "http://localhost:8420"));

        TitledPane parametresServeur = new TitledPane("Adresse du serveur", champServeur);
        parametresServeur.setExpanded(false);
        parametresServeur.getStyleClass().add("panneau-serveur");

        boutonConnexion.setDefaultButton(true);
        boutonConnexion.setMaxWidth(Double.MAX_VALUE);
        boutonConnexion.getStyleClass().add("bouton-principal");
        boutonConnexion.setGraphic(new FontIcon(Material2AL.LOGIN));
        boutonConnexion.setOnAction(e -> seConnecter());

        indicateur.setVisible(false);
        indicateur.setMaxSize(22, 22);
        HBox ligneBouton = new HBox(10, boutonConnexion, indicateur);
        ligneBouton.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(boutonConnexion, javafx.scene.layout.Priority.ALWAYS);

        labelErreur.getStyleClass().add("label-erreur");
        labelErreur.setWrapText(true);

        Hyperlink lienDemo = new Hyperlink("Mode démonstration (sans serveur)");
        lienDemo.getStyleClass().add("lien-demo");
        lienDemo.setOnAction(e -> ouvrirModeDemonstration());

        VBox carte = new VBox(14,
                entete,
                champUtilisateur,
                champMotDePasse,
                parametresServeur,
                ligneBouton,
                labelErreur,
                lienDemo);
        carte.getStyleClass().add("carte-connexion");
        carte.setMaxWidth(400);
        carte.setPadding(new Insets(36));
        carte.setAlignment(Pos.CENTER);

        racine.getChildren().add(carte);
    }

    private void seConnecter() {
        String utilisateur = champUtilisateur.getText().trim();
        String motDePasse = champMotDePasse.getText();
        String urlServeur = champServeur.getText().trim();

        if (utilisateur.isEmpty() || motDePasse.isEmpty()) {
            afficherErreur("Veuillez saisir votre nom d'utilisateur et votre mot de passe.");
            return;
        }

        labelErreur.setText("");
        boutonConnexion.setDisable(true);
        indicateur.setVisible(true);

        // Appel réseau hors du fil JavaFX
        Thread tache = new Thread(() -> {
            ApiClient api = new ApiClient();
            api.setUrlBase(urlServeur);
            try {
                UtilisateurDTO profil = api.connexion(utilisateur, motDePasse);
                PREFS.put(PREF_URL_SERVEUR, urlServeur);
                Platform.runLater(() -> {
                    Session.ouvrir(profil, api, false);
                    onConnexionReussie.run();
                });
            } catch (ApiClient.ApiException ex) {
                Platform.runLater(() -> afficherErreur(ex.getMessage()));
            }
        }, "connexion-serveur");
        tache.setDaemon(true);
        tache.start();
    }

    /** Permet de présenter l'interface sans serveur (démonstrations client). */
    private void ouvrirModeDemonstration() {
        UtilisateurDTO demo = new UtilisateurDTO(
                0L, "demo", "Démonstration", null, Role.ADMINISTRATEUR, null, null, true);
        Session.ouvrir(demo, new ApiClient(), true);
        onConnexionReussie.run();
    }

    private void afficherErreur(String message) {
        boutonConnexion.setDisable(false);
        indicateur.setVisible(false);
        labelErreur.setText(message);
    }
}
