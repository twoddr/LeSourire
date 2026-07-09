package com.lesourire.client;

import atlantafx.base.theme.PrimerLight;

import com.lesourire.client.vue.ConnexionVue;
import com.lesourire.client.vue.PrincipaleVue;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Application de bureau du cabinet Le Sourire. */
public class LeSourireClient extends Application {

    private Stage fenetre;
    private Scene scene;

    @Override
    public void start(Stage stage) {
        this.fenetre = stage;
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        scene = new Scene(construireConnexion(), 1200, 750);
        scene.getStylesheets().add(
                getClass().getResource("/com/lesourire/client/styles.css").toExternalForm());

        stage.setTitle("Le Sourire — Cabinet Dentaire");
        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(600);
        stage.show();
    }

    private Parent construireConnexion() {
        return (Parent) new ConnexionVue(this::afficherFenetrePrincipale).getRacine();
    }

    private void afficherFenetrePrincipale() {
        scene.setRoot((Parent) new PrincipaleVue(this::afficherConnexion).getRacine());
        fenetre.setMaximized(true);
    }

    private void afficherConnexion() {
        fenetre.setMaximized(false);
        scene.setRoot(construireConnexion());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
