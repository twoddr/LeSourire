package com.lesourire.serveur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lesourire.serveur.entite.Fournisseur;

public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    List<Fournisseur> findByActifTrueOrderByNomAsc();

    List<Fournisseur> findAllByOrderByNomAsc();

    boolean existsByNomIgnoreCase(String nom);

    boolean existsByNomIgnoreCaseAndIdNot(String nom, Long id);
}
