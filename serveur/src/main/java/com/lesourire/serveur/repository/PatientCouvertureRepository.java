package com.lesourire.serveur.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lesourire.serveur.entite.PatientCouverture;

public interface PatientCouvertureRepository extends JpaRepository<PatientCouverture, Long> {

    @Query("""
            SELECT c FROM PatientCouverture c
            LEFT JOIN FETCH c.assureur
            LEFT JOIN FETCH c.societe
            WHERE c.patient.id = :patientId
            ORDER BY c.dateDebut DESC, c.id DESC
            """)
    List<PatientCouverture> historique(@Param("patientId") Long patientId);

    /** Couvertures actives à la date donnée pour un ensemble de patients. */
    @Query("""
            SELECT c FROM PatientCouverture c
            LEFT JOIN FETCH c.assureur
            LEFT JOIN FETCH c.societe
            WHERE c.patient.id IN :patientIds
              AND c.dateDebut <= :jour
              AND (c.dateFin IS NULL OR c.dateFin >= :jour)
            """)
    List<PatientCouverture> actives(@Param("patientIds") Collection<Long> patientIds,
            @Param("jour") LocalDate jour);
}
