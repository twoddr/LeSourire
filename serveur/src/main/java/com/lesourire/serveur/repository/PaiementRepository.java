package com.lesourire.serveur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lesourire.serveur.entite.Paiement;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    List<Paiement> findByFactureIdOrderByDatePaiementAscIdAsc(Long factureId);

    long countByFactureId(Long factureId);
}
