package com.lesourire.commun.dto;

/** Catégorie d'article de stock. */
public record CategorieArticleDTO(Long id, String libelle) {

    @Override
    public String toString() {
        return libelle;
    }
}
