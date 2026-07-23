package com.lesourire.serveur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lesourire.serveur.entite.CategorieArticle;

public interface CategorieArticleRepository extends JpaRepository<CategorieArticle, Long> {

    List<CategorieArticle> findAllByOrderByLibelleAsc();
}
