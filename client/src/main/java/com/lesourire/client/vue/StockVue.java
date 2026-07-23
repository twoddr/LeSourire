package com.lesourire.client.vue;

import java.util.ArrayList;
import java.util.List;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.coeur.Dialogues;
import com.lesourire.client.coeur.Session;
import com.lesourire.client.service.ServiceStock;
import com.lesourire.client.service.ServiceStockApi;
import com.lesourire.client.service.ServiceStockDemo;
import com.lesourire.commun.dto.ArticleDTO;
import com.lesourire.commun.dto.CategorieArticleDTO;
import com.lesourire.commun.dto.FournisseurDTO;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Module Stock : articles, fournisseurs et mouvements. */
public class StockVue {

    private final VBox racine = new VBox();
    private final ServiceStock service;

    private final TextField champRecherche = new TextField();
    private final CheckBox caseInactifsArticles = new CheckBox("Afficher les articles inactifs");
    private final TableView<ArticleDTO> tableauArticles = new TableView<>();
    private final Label labelStatutArticles = new Label();

    private final CheckBox caseInactifsFournisseurs = new CheckBox("Afficher les fournisseurs inactifs");
    private final TableView<FournisseurDTO> tableauFournisseurs = new TableView<>();
    private final Label labelStatutFournisseurs = new Label();

    private List<CategorieArticleDTO> categories = new ArrayList<>();
    private List<FournisseurDTO> fournisseursActifs = new ArrayList<>();

    public StockVue() {
        this.service = Session.estModeDemonstration()
                ? new ServiceStockDemo()
                : new ServiceStockApi(Session.api());
        construire();
        chargerCategoriesPuis();
    }

    public Node getRacine() {
        return racine;
    }

    private void construire() {
        racine.getStyleClass().add("page");
        racine.setPadding(new Insets(28));
        VBox.setVgrow(racine, Priority.ALWAYS);

        Label titre = new Label("Stock");
        titre.getStyleClass().add("titre-page");

        TabPane onglets = new TabPane(
                new Tab("Articles", construireOngletArticles()),
                new Tab("Fournisseurs", construireOngletFournisseurs()));
        onglets.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(onglets, Priority.ALWAYS);

        racine.getChildren().setAll(titre, onglets);
        VBox.setMargin(onglets, new Insets(16, 0, 0, 0));
    }

    private Node construireOngletArticles() {
        Button actualiser = new Button();
        actualiser.setGraphic(new FontIcon(Material2MZ.REFRESH));
        actualiser.setOnAction(e -> chargerArticles());

        Button mouvement = new Button("Mouvement…");
        mouvement.setGraphic(new FontIcon(Material2MZ.SWAP_VERT));
        mouvement.setTooltip(new Tooltip("Entrée, sortie ou ajustement sur l'article sélectionné"));
        mouvement.setOnAction(e -> {
            ArticleDTO sel = tableauArticles.getSelectionModel().getSelectedItem();
            if (sel == null) {
                afficherErreur("Aucun article sélectionné",
                        new IllegalStateException("Sélectionnez un article dans la liste."));
                return;
            }
            ouvrirMouvement(sel);
        });

        Button nouveau = new Button("Nouvel article");
        nouveau.setGraphic(new FontIcon(Material2AL.ADD));
        nouveau.getStyleClass().add("bouton-principal");
        nouveau.setOnAction(e -> ouvrirFicheArticle(null));

        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);
        HBox actions = new HBox(10, espace, actualiser, mouvement, nouveau);
        actions.setAlignment(Pos.CENTER_RIGHT);

        champRecherche.setPromptText("Rechercher par nom ou marque…");
        champRecherche.getStyleClass().add("champ-recherche");
        PauseTransition attente = new PauseTransition(Duration.millis(300));
        attente.setOnFinished(e -> chargerArticles());
        champRecherche.textProperty().addListener((o, a, n) -> attente.playFromStart());
        caseInactifsArticles.setSelected(true);
        caseInactifsArticles.setOnAction(e -> chargerArticles());

        construireTableauArticles();
        VBox.setVgrow(tableauArticles, Priority.ALWAYS);
        labelStatutArticles.getStyleClass().add("note-discrete");

        VBox page = new VBox(12, actions, champRecherche, caseInactifsArticles,
                tableauArticles, labelStatutArticles);
        page.setPadding(new Insets(16, 0, 0, 0));
        return page;
    }

    private Node construireOngletFournisseurs() {
        Button actualiser = new Button();
        actualiser.setGraphic(new FontIcon(Material2MZ.REFRESH));
        actualiser.setOnAction(e -> chargerFournisseurs());

        Button nouveau = new Button("Nouveau fournisseur");
        nouveau.setGraphic(new FontIcon(Material2AL.ADD));
        nouveau.getStyleClass().add("bouton-principal");
        nouveau.setOnAction(e -> ouvrirFicheFournisseur(null));

        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);
        HBox actions = new HBox(10, espace, actualiser, nouveau);
        actions.setAlignment(Pos.CENTER_RIGHT);

        caseInactifsFournisseurs.setSelected(true);
        caseInactifsFournisseurs.setOnAction(e -> chargerFournisseurs());

        construireTableauFournisseurs();
        VBox.setVgrow(tableauFournisseurs, Priority.ALWAYS);
        labelStatutFournisseurs.getStyleClass().add("note-discrete");

        VBox page = new VBox(12, actions, caseInactifsFournisseurs,
                tableauFournisseurs, labelStatutFournisseurs);
        page.setPadding(new Insets(16, 0, 0, 0));
        return page;
    }

    private void construireTableauArticles() {
        tableauArticles.getColumns().setAll(
                colA("Nom", 220, a -> a.nom),
                colA("Marque", 120, a -> a.marque == null ? "" : a.marque),
                colA("Catégorie", 160, a -> a.categorieLibelle == null ? "" : a.categorieLibelle),
                colA("Stock", 90, a -> a.quantiteStock.stripTrailingZeros().toPlainString()
                        + " " + a.unite),
                colA("Seuil", 70, a -> a.seuilAlerte.stripTrailingZeros().toPlainString()),
                colA("Statut", 80, a -> a.actif ? "Actif" : "Inactif"));
        tableauArticles.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableauArticles.setPlaceholder(new Label("Aucun article."));
        tableauArticles.setRowFactory(t -> {
            TableRow<ArticleDTO> ligne = new TableRow<>() {
                @Override
                protected void updateItem(ArticleDTO item, boolean vide) {
                    super.updateItem(item, vide);
                    getStyleClass().remove("ligne-alerte-stock");
                    if (!vide && item != null && item.enAlerte()) {
                        getStyleClass().add("ligne-alerte-stock");
                    }
                }
            };
            ligne.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !ligne.isEmpty()) {
                    ouvrirFicheArticle(ligne.getItem());
                }
            });
            return ligne;
        });
    }

    private void construireTableauFournisseurs() {
        tableauFournisseurs.getColumns().setAll(
                colF("Nom", 220, f -> f.nom),
                colF("Contact", 140, f -> f.contact == null ? "" : f.contact),
                colF("Téléphone", 120, f -> f.telephone == null ? "" : f.telephone),
                colF("E-mail", 180, f -> f.email == null ? "" : f.email),
                colF("Statut", 80, f -> f.actif ? "Actif" : "Inactif"));
        tableauFournisseurs.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableauFournisseurs.setPlaceholder(new Label("Aucun fournisseur."));
        tableauFournisseurs.setRowFactory(t -> {
            TableRow<FournisseurDTO> ligne = new TableRow<>();
            ligne.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !ligne.isEmpty()) {
                    ouvrirFicheFournisseur(ligne.getItem());
                }
            });
            return ligne;
        });
    }

    private void chargerCategoriesPuis() {
        Async.executer(service::categories,
                cats -> {
                    categories = cats;
                    chargerArticles();
                    chargerFournisseurs();
                },
                e -> afficherErreur("Impossible de charger les catégories", e));
    }

    private void chargerArticles() {
        String q = champRecherche.getText();
        boolean inactifs = caseInactifsArticles.isSelected();
        labelStatutArticles.setText("Chargement…");
        Async.executer(() -> service.rechercherArticles(q, inactifs),
                liste -> {
                    tableauArticles.getItems().setAll(liste);
                    long alertes = liste.stream().filter(ArticleDTO::enAlerte).count();
                    labelStatutArticles.setText(liste.size() + " article(s)"
                            + (alertes > 0 ? " — " + alertes + " sous le seuil" : "")
                            + suffixeDemo());
                },
                e -> {
                    labelStatutArticles.setText("");
                    afficherErreur("Impossible de charger les articles", e);
                });
    }

    private void chargerFournisseurs() {
        boolean inactifs = caseInactifsFournisseurs.isSelected();
        labelStatutFournisseurs.setText("Chargement…");
        Async.executer(() -> service.listerFournisseurs(inactifs),
                liste -> {
                    tableauFournisseurs.getItems().setAll(liste);
                    labelStatutFournisseurs.setText(liste.size() + " fournisseur(s)" + suffixeDemo());
                },
                e -> {
                    labelStatutFournisseurs.setText("");
                    afficherErreur("Impossible de charger les fournisseurs", e);
                });
        Async.executer(() -> service.listerFournisseurs(false),
                liste -> fournisseursActifs = liste,
                e -> { /* silencieux : utilisé pour le dialogue mouvement */ });
    }

    private void ouvrirFicheArticle(ArticleDTO existant) {
        Dialogues.afficher(new FicheArticleDialogue(existant, categories),
                racine.getScene().getWindow()).ifPresent(saisie -> {
            boolean creation = existant == null || existant.id == null;
            Async.executer(
                    () -> creation ? service.creerArticle(saisie)
                            : service.modifierArticle(existant.id, saisie),
                    ok -> chargerArticles(),
                    e -> {
                        afficherErreur("Impossible d'enregistrer l'article", e);
                        ouvrirFicheArticle(saisie);
                    });
        });
    }

    private void ouvrirFicheFournisseur(FournisseurDTO existant) {
        Dialogues.afficher(new FicheFournisseurDialogue(existant),
                racine.getScene().getWindow()).ifPresent(saisie -> {
            boolean creation = existant == null || existant.id == null;
            Async.executer(
                    () -> creation ? service.creerFournisseur(saisie)
                            : service.modifierFournisseur(existant.id, saisie),
                    ok -> chargerFournisseurs(),
                    e -> {
                        afficherErreur("Impossible d'enregistrer le fournisseur", e);
                        ouvrirFicheFournisseur(saisie);
                    });
        });
    }

    private void ouvrirMouvement(ArticleDTO article) {
        Dialogues.afficher(new MouvementStockDialogue(article, fournisseursActifs),
                racine.getScene().getWindow()).ifPresent(mvt ->
                Async.executer(() -> service.enregistrerMouvement(mvt),
                        ok -> chargerArticles(),
                        e -> afficherErreur("Impossible d'enregistrer le mouvement", e)));
    }

    private static String suffixeDemo() {
        return Session.estModeDemonstration()
                ? " — mode démonstration, rien n'est enregistré" : "";
    }

    private static TableColumn<ArticleDTO, String> colA(String titre, double largeur,
            java.util.function.Function<ArticleDTO, String> extracteur) {
        return colonne(titre, largeur, extracteur);
    }

    private static TableColumn<FournisseurDTO, String> colF(String titre, double largeur,
            java.util.function.Function<FournisseurDTO, String> extracteur) {
        return colonne(titre, largeur, extracteur);
    }

    private static <T> TableColumn<T, String> colonne(String titre, double largeur,
            java.util.function.Function<T, String> extracteur) {
        TableColumn<T, String> col = new TableColumn<>(titre);
        col.setPrefWidth(largeur);
        col.setCellValueFactory(d -> {
            String v = extracteur.apply(d.getValue());
            return new SimpleStringProperty(v == null ? "" : v);
        });
        return col;
    }

    private void afficherErreur(String entete, Exception e) {
        Alert alerte = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
        alerte.setHeaderText(entete);
        Dialogues.afficherSansResultat(alerte, racine.getScene().getWindow());
    }
}
