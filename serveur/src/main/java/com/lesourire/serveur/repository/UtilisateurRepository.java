package com.lesourire.serveur.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lesourire.serveur.entite.Utilisateur;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByNomUtilisateurAndActifTrue(String nomUtilisateur);
}
