package com.lesourire.serveur.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lesourire.serveur.entite.Paiement;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    List<Paiement> findByFactureIdOrderByDatePaiementAscIdAsc(Long factureId);

    long countByFactureId(Long factureId);

    @Query("""
            SELECT p FROM Paiement p
            JOIN FETCH p.facture f
            JOIN FETCH f.patient
            LEFT JOIN FETCH p.recuPar
            WHERE p.datePaiement >= :debut AND p.datePaiement < :fin
            ORDER BY p.datePaiement DESC, p.id DESC
            """)
    List<Paiement> findEntre(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
}
