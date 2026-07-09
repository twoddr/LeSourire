package com.lesourire.serveur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lesourire.serveur.entite.Societe;

public interface SocieteRepository extends JpaRepository<Societe, Long> {

    List<Societe> findByActifTrueOrderByNom();
}
