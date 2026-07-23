package com.lesourire.client.service;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lesourire.client.coeur.ApiClient;
import com.lesourire.commun.dto.ArticleDTO;
import com.lesourire.commun.dto.CategorieArticleDTO;
import com.lesourire.commun.dto.FournisseurDTO;
import com.lesourire.commun.dto.MouvementStockDTO;

public class ServiceStockApi implements ServiceStock {

    private final ApiClient api;

    public ServiceStockApi(ApiClient api) {
        this.api = api;
    }

    @Override
    public List<CategorieArticleDTO> categories() throws Exception {
        return api.get("/api/categories-article",
                new TypeReference<List<CategorieArticleDTO>>() {
                });
    }

    @Override
    public List<ArticleDTO> rechercherArticles(String recherche, boolean inclureInactifs)
            throws Exception {
        return api.get("/api/articles?recherche=" + ApiClient.encoder(recherche)
                        + "&inclureInactifs=" + inclureInactifs,
                new TypeReference<List<ArticleDTO>>() {
                });
    }

    @Override
    public ArticleDTO creerArticle(ArticleDTO dto) throws Exception {
        return api.post("/api/articles", dto, new TypeReference<ArticleDTO>() {
        });
    }

    @Override
    public ArticleDTO modifierArticle(Long id, ArticleDTO dto) throws Exception {
        return api.put("/api/articles/" + id, dto, new TypeReference<ArticleDTO>() {
        });
    }

    @Override
    public List<FournisseurDTO> listerFournisseurs(boolean inclureInactifs) throws Exception {
        return api.get("/api/fournisseurs?inclureInactifs=" + inclureInactifs,
                new TypeReference<List<FournisseurDTO>>() {
                });
    }

    @Override
    public FournisseurDTO creerFournisseur(FournisseurDTO dto) throws Exception {
        return api.post("/api/fournisseurs", dto, new TypeReference<FournisseurDTO>() {
        });
    }

    @Override
    public FournisseurDTO modifierFournisseur(Long id, FournisseurDTO dto) throws Exception {
        return api.put("/api/fournisseurs/" + id, dto, new TypeReference<FournisseurDTO>() {
        });
    }

    @Override
    public List<MouvementStockDTO> mouvements(Long articleId) throws Exception {
        return api.get("/api/articles/" + articleId + "/mouvements",
                new TypeReference<List<MouvementStockDTO>>() {
                });
    }

    @Override
    public MouvementStockDTO enregistrerMouvement(MouvementStockDTO dto) throws Exception {
        return api.post("/api/mouvements-stock", dto, new TypeReference<MouvementStockDTO>() {
        });
    }
}
