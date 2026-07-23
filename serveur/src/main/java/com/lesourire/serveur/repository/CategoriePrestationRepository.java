package com.lesourire.serveur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lesourire.serveur.entite.CategoriePrestation;

public interface CategoriePrestationRepository extends JpaRepository<CategoriePrestation, Long> {

    List<CategoriePrestation> findAllByOrderByOrdreAffichageAscLibelleAsc();
}
