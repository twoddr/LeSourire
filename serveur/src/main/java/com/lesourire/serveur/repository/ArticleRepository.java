package com.lesourire.serveur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lesourire.serveur.entite.Article;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    @Query("""
            SELECT a FROM Article a
            WHERE (:inclureInactifs = TRUE OR a.actif = TRUE)
              AND (:q = '' OR LOWER(a.nom) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(a.marque, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY a.nom
            """)
    List<Article> rechercher(@Param("q") String q, @Param("inclureInactifs") boolean inclureInactifs);
}
