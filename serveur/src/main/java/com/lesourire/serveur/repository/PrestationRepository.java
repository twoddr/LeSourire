package com.lesourire.serveur.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lesourire.serveur.entite.Prestation;

public interface PrestationRepository extends JpaRepository<Prestation, Long> {

    Optional<Prestation> findByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    @Query("""
            SELECT p FROM Prestation p
            WHERE (:inclureInactifs = TRUE OR p.actif = TRUE)
              AND (:q = '' OR LOWER(p.code) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.libelle) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY p.categorie.ordreAffichage, p.code
            """)
    List<Prestation> rechercher(@Param("q") String q,
            @Param("inclureInactifs") boolean inclureInactifs);
}
