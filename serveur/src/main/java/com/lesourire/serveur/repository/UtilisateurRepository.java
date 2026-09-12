package com.lesourire.serveur.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lesourire.commun.Role;
import com.lesourire.serveur.entite.Utilisateur;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByNomUtilisateurAndActifTrue(String nomUtilisateur);

    Optional<Utilisateur> findByNomUtilisateur(String nomUtilisateur);

    boolean existsByNomUtilisateurAndIdNot(String nomUtilisateur, Long id);

    long countByRoleAndActifTrue(Role role);

    /**
     * Utilisateurs actifs d'un rôle. Le nom/prénom étant chiffrés en base, aucun
     * tri SQL n'est possible : le tri se fait en mémoire dans le service.
     */
    List<Utilisateur> findByRoleAndActifTrue(Role role);
}
