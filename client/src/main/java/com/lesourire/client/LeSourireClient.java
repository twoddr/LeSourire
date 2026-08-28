package com.lesourire.client;

import com.lesourire.client.vue.ConnexionVue;
import com.lesourire.client.vue.PrincipaleVue;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/** Application de bureau du cabinet Le Sourire. */
public class LeSourireClient extends Application {

    private Stage fenetre;
    private Scene scene;

    @Override
    public void start(Stage stage) {
        this.fenetre = stage;
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        scene = new Scene(construireConnexion());
        scene.getStylesheets().add(
                getClass().getResource("/com/lesourire/client/styles.css").toExternalForm());

        stage.setTitle("Le Sourire — Cabinet Dentaire  " + getVersion());
        stage.setScene(scene);

        // stage.setMinWidth(960);
        // stage.setMinHeight(600);

        // Ouverture à 80 % de l'écran, puis ajustement à la taille
        // MAX(taille naturelle du contenu, 80 % de l'écran).
        Rectangle2D ecran = Screen.getPrimary().getVisualBounds();
        fenetre.setWidth(ecran.getWidth() * 0.8);
        fenetre.setHeight(ecran.getHeight() * 0.8);

        stage.show();
        Platform.runLater(this::dimensionnerFenetre);
    }

    private Parent construireConnexion() {
        return (Parent) new ConnexionVue(this::afficherFenetrePrincipale).getRacine();
    }

    private void afficherFenetrePrincipale() {
        scene.setRoot((Parent) new PrincipaleVue(this::afficherConnexion).getRacine());
        dimensionnerFenetre();
        System.out.println("Fenetre ouverte avec la taille : " + fenetre.getWidth() + "x" + fenetre.getHeight());
    }

    private void afficherConnexion() {
        fenetre.setMaximized(false);
        scene.setRoot(construireConnexion());
        dimensionnerFenetre();
    }

    /**
     * Dimensionne la fenêtre à la taille MAX(taille naturelle du contenu,
     * 80 % de l'écran) puis la recentre.
     */
    private void dimensionnerFenetre() {
        if (fenetre.isMaximized() || fenetre.isFullScreen()) {
            return;
        }
        // sizeToScene() déclenche le layout CSS et adopte la taille
        // naturelle du contenu.
        fenetre.sizeToScene();

        Rectangle2D ecran = Screen.getPrimary().getVisualBounds();
        fenetre.setWidth(Math.max(fenetre.getWidth(), ecran.getWidth() * 0.8));
        fenetre.setHeight(Math.max(fenetre.getHeight(), ecran.getHeight() * 0.8));
        fenetre.centerOnScreen();
    }

    /** Version produit (filtrage Maven ou Manifest JAR). */
    private String getVersion() {
        try (var in = getClass().getResourceAsStream(
                "/com/lesourire/client/version.properties")) {
            if (in != null) {
                var props = new java.util.Properties();
                props.load(in);
                String v = props.getProperty("version");
                if (v != null && !v.isBlank() && !v.contains("${")) {
                    return v.trim();
                }
            }
        } catch (Exception ignore) {
            // ignore
        }
        try {
            String v = getClass().getPackage().getImplementationVersion();
            if (v != null && !v.isBlank()) {
                return v;
            }
        } catch (Exception ignore) {
            // ignore
        }
        return "dev";
    }

    public static void main(String[] args) {
        // Avant tout PDF : pas de scan des polices système (TeX Live, etc.)
        com.lesourire.client.impression.FacturePdf.preparerEnvironnementPdf();
        launch(args);
    }
}
