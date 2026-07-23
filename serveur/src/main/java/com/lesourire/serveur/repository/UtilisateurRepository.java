package com.lesourire.serveur.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lesourire.commun.Role;
import com.lesourire.serveur.entite.Utilisateur;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByNomUtilisateurAndActifTrue(String nomUtilisateur);

    Optional<Utilisateur> findByNomUtilisateur(String nomUtilisateur);

    boolean existsByNomUtilisateurAndIdNot(String nomUtilisateur, Long id);

    long countByRoleAndActifTrue(Role role);

    @Query("""
            SELECT u FROM Utilisateur u
            WHERE (:inclureInactifs = TRUE OR u.actif = TRUE)
              AND (:q = '' OR LOWER(u.nomUtilisateur) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(u.prenom, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY u.nom, u.prenom, u.nomUtilisateur
            """)
    List<Utilisateur> rechercher(@Param("q") String q, @Param("inclureInactifs") boolean inclureInactifs);
}
