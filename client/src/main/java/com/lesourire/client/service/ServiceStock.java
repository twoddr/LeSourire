package com.lesourire.client.service;

import java.util.List;

import com.lesourire.commun.dto.ArticleDTO;
import com.lesourire.commun.dto.CategorieArticleDTO;
import com.lesourire.commun.dto.FournisseurDTO;
import com.lesourire.commun.dto.MouvementStockDTO;

public interface ServiceStock {

    List<CategorieArticleDTO> categories() throws Exception;

    List<ArticleDTO> rechercherArticles(String recherche, boolean inclureInactifs) throws Exception;

    ArticleDTO creerArticle(ArticleDTO dto) throws Exception;

    ArticleDTO modifierArticle(Long id, ArticleDTO dto) throws Exception;

    List<FournisseurDTO> listerFournisseurs(boolean inclureInactifs) throws Exception;

    FournisseurDTO creerFournisseur(FournisseurDTO dto) throws Exception;

    FournisseurDTO modifierFournisseur(Long id, FournisseurDTO dto) throws Exception;

    List<MouvementStockDTO> mouvements(Long articleId) throws Exception;

    MouvementStockDTO enregistrerMouvement(MouvementStockDTO dto) throws Exception;
}
