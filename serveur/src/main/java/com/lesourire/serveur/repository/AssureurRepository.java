package com.lesourire.serveur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lesourire.serveur.entite.Assureur;

public interface AssureurRepository extends JpaRepository<Assureur, Long> {

    List<Assureur> findByActifTrueOrderByNom();

    List<Assureur> findAllByOrderByNomAsc();

    boolean existsByNomIgnoreCase(String nom);

    boolean existsByNomIgnoreCaseAndIdNot(String nom, Long id);
}
