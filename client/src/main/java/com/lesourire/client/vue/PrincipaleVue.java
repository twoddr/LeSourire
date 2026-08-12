package com.lesourire.client.vue;

import java.util.EnumMap;
import java.util.Map;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import com.lesourire.client.coeur.Session;
import com.lesourire.commun.Role;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Fenêtre principale : barre latérale de navigation (filtrée selon le rôle
 * de l'utilisateur connecté) et zone de contenu affichant le module actif.
 */
public class PrincipaleVue {

    private final BorderPane racine = new BorderPane();
    private final StackPane contenu = new StackPane();
    private final Map<Module, Node> vuesChargees = new EnumMap<>(Module.class);
    private final Runnable onDeconnexion;

    public PrincipaleVue(Runnable onDeconnexion) {
        this.onDeconnexion = onDeconnexion;
        construire();
    }

    public Node getRacine() {
        return racine;
    }

    private void construire() {
        racine.setLeft(construireBarreLaterale());

        contenu.getStyleClass().add("zone-contenu");
        racine.setCenter(contenu);

        afficherModule(Module.TABLEAU_BORD);
    }

    private Node construireBarreLaterale() {
        Role role = Session.utilisateur().role();

        // En-tête
        Label badge = new Label("LS");
        badge.getStyleClass().add("badge-logo-petit");
        Label nomApp = new Label("Le Sourire");
        nomApp.getStyleClass().add("sidebar-titre");
        Label sousTitre = new Label("Cabinet dentaire");
        sousTitre.getStyleClass().add("sidebar-sous-titre");
        VBox textes = new VBox(0, nomApp, sousTitre);
        javafx.scene.layout.HBox entete = new javafx.scene.layout.HBox(10, badge, textes);
        entete.setAlignment(Pos.CENTER_LEFT);
        entete.setPadding(new Insets(4, 8, 12, 8));

        // Navigation filtrée par rôle
        ToggleGroup groupe = new ToggleGroup();
        VBox navigation = new VBox(4);
        for (Module module : Module.values()) {
            if (!module.estAccessiblePar(role)) {
                continue;
            }
            ToggleButton bouton = new ToggleButton(module.getLibelle());
            bouton.setToggleGroup(groupe);
            bouton.setMaxWidth(Double.MAX_VALUE);
            bouton.setAlignment(Pos.CENTER_LEFT);
            bouton.getStyleClass().add("nav-bouton");
            FontIcon icone = new FontIcon(module.getIcone());
            icone.getStyleClass().add("nav-icone");
            bouton.setGraphic(icone);
            bouton.setUserData(module);
            bouton.setOnAction(e -> afficherModule(module));
            if (module == Module.TABLEAU_BORD) {
                bouton.setSelected(true);
            }
            navigation.getChildren().add(bouton);
        }
        // Empêche la désélection du module actif par re-clic
        groupe.selectedToggleProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau == null && ancien != null) {
                ancien.setSelected(true);
            }
        });

        // Pied : utilisateur connecté + déconnexion
        Label nomUtilisateur = new Label(Session.utilisateur().nomComplet());
        nomUtilisateur.getStyleClass().add("sidebar-utilisateur-nom");
        Label libelleRole = new Label(Session.utilisateur().role().getLibelle()
                + (Session.estModeDemonstration() ? " (démo)" : ""));
        libelleRole.getStyleClass().add("sidebar-utilisateur-role");
        VBox blocUtilisateur = new VBox(2, nomUtilisateur, libelleRole);
        blocUtilisateur.setPadding(new Insets(8));

        Button boutonDeconnexion = new Button("Se déconnecter");
        boutonDeconnexion.setGraphic(new FontIcon(Material2AL.LOG_OUT));
        boutonDeconnexion.setMaxWidth(Double.MAX_VALUE);
        boutonDeconnexion.getStyleClass().add("bouton-deconnexion");
        boutonDeconnexion.setTooltip(new Tooltip("Fermer la session et revenir à l'écran de connexion"));
        boutonDeconnexion.setOnAction(e -> {
            Session.fermer();
            onDeconnexion.run();
        });

        Region espace = new Region();
        VBox.setVgrow(espace, Priority.ALWAYS);

        VBox barre = new VBox(6,
                entete,
                new Separator(),
                navigation,
                espace,
                new Separator(),
                blocUtilisateur,
                boutonDeconnexion);
        barre.getStyleClass().add("sidebar");
        barre.setPadding(new Insets(16, 12, 16, 12));
        barre.setPrefWidth(240);
        return barre;
    }

    private void afficherModule(Module module) {
        Node vue = vuesChargees.computeIfAbsent(module, this::creerVue);
        contenu.getChildren().setAll(vue);
    }

    private Node creerVue(Module module) {
        return switch (module) {
            case TABLEAU_BORD -> TableauBordVue.creer();
            case PATIENTS -> new PatientsVue().getRacine();
            case AGENDA -> new AgendaVue().getRacine();
            case FACTURATION -> new FacturationVue().getRacine();
            case STOCK -> new StockVue().getRacine();
            case COMPTABILITE -> new ComptabiliteVue().getRacine();
            case ADMINISTRATION -> new AdministrationVue().getRacine();
            default -> VueModuleEnConstruction.creer(module);
        };
    }
}
