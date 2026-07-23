package com.lesourire.serveur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lesourire.serveur.entite.MouvementStock;

public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {

    List<MouvementStock> findByArticleIdOrderByDateMouvementDesc(Long articleId);
}
