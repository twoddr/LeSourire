package com.lesourire.client.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import com.lesourire.commun.TypeMouvementStock;
import com.lesourire.commun.dto.ArticleDTO;
import com.lesourire.commun.dto.CategorieArticleDTO;
import com.lesourire.commun.dto.FournisseurDTO;
import com.lesourire.commun.dto.MouvementStockDTO;

public class ServiceStockDemo implements ServiceStock {

    private final AtomicLong seq = new AtomicLong(1);
    private final List<CategorieArticleDTO> categories = List.of(
            new CategorieArticleDTO(1L, "Consommables de soins"),
            new CategorieArticleDTO(2L, "Hygiène et protection"),
            new CategorieArticleDTO(3L, "Médicaments et anesthésie"));
    private final List<ArticleDTO> articles = new ArrayList<>();
    private final List<FournisseurDTO> fournisseurs = new ArrayList<>();
    private final List<MouvementStockDTO> mouvements = new ArrayList<>();

    public ServiceStockDemo() {
        FournisseurDTO f = new FournisseurDTO();
        f.id = seq.getAndIncrement();
        f.nom = "Dental Supply Douala";
        f.actif = true;
        fournisseurs.add(f);

        ArticleDTO a = new ArticleDTO();
        a.id = seq.getAndIncrement();
        a.nom = "Gants nitrile M";
        a.categorieId = 2L;
        a.categorieLibelle = "Hygiène et protection";
        a.unite = "boîte";
        a.quantiteStock = new BigDecimal("12");
        a.seuilAlerte = new BigDecimal("5");
        a.actif = true;
        articles.add(a);
    }

    @Override
    public List<CategorieArticleDTO> categories() {
        return categories;
    }

    @Override
    public List<ArticleDTO> rechercherArticles(String recherche, boolean inclureInactifs) {
        String q = recherche == null ? "" : recherche.trim().toLowerCase(Locale.FRENCH);
        return articles.stream()
                .filter(a -> inclureInactifs || a.actif)
                .filter(a -> q.isEmpty() || a.nom.toLowerCase(Locale.FRENCH).contains(q))
                .toList();
    }

    @Override
    public ArticleDTO creerArticle(ArticleDTO dto) {
        dto.id = seq.getAndIncrement();
        dto.quantiteStock = BigDecimal.ZERO;
        dto.categorieLibelle = libelleCat(dto.categorieId);
        articles.add(dto);
        return dto;
    }

    @Override
    public ArticleDTO modifierArticle(Long id, ArticleDTO dto) {
        for (int i = 0; i < articles.size(); i++) {
            if (articles.get(i).id.equals(id)) {
                dto.id = id;
                dto.quantiteStock = articles.get(i).quantiteStock;
                dto.categorieLibelle = libelleCat(dto.categorieId);
                articles.set(i, dto);
                return dto;
            }
        }
        throw new IllegalArgumentException("Article introuvable.");
    }

    @Override
    public List<FournisseurDTO> listerFournisseurs(boolean inclureInactifs) {
        return fournisseurs.stream().filter(f -> inclureInactifs || f.actif).toList();
    }

    @Override
    public FournisseurDTO creerFournisseur(FournisseurDTO dto) {
        dto.id = seq.getAndIncrement();
        fournisseurs.add(dto);
        return dto;
    }

    @Override
    public FournisseurDTO modifierFournisseur(Long id, FournisseurDTO dto) {
        for (int i = 0; i < fournisseurs.size(); i++) {
            if (fournisseurs.get(i).id.equals(id)) {
                dto.id = id;
                fournisseurs.set(i, dto);
                return dto;
            }
        }
        throw new IllegalArgumentException("Fournisseur introuvable.");
    }

    @Override
    public List<MouvementStockDTO> mouvements(Long articleId) {
        return mouvements.stream().filter(m -> articleId.equals(m.articleId)).toList();
    }

    @Override
    public MouvementStockDTO enregistrerMouvement(MouvementStockDTO dto) {
        dto.id = seq.getAndIncrement();
        dto.dateMouvement = dto.dateMouvement == null ? LocalDateTime.now() : dto.dateMouvement;
        for (ArticleDTO a : articles) {
            if (a.id.equals(dto.articleId)) {
                dto.articleNom = a.nom;
                BigDecimal delta = switch (dto.type) {
                    case ENTREE -> dto.quantite;
                    case AJUSTEMENT -> dto.quantite;
                    default -> dto.quantite.negate();
                };
                a.quantiteStock = a.quantiteStock.add(delta);
                if (dto.type == TypeMouvementStock.ENTREE && dto.prixUnitaire != null) {
                    a.prixAchatDernier = dto.prixUnitaire;
                }
                break;
            }
        }
        mouvements.add(dto);
        return dto;
    }

    private String libelleCat(Long id) {
        return categories.stream().filter(c -> c.id().equals(id))
                .map(CategorieArticleDTO::libelle).findFirst().orElse("");
    }
}
