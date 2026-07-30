package com.lesourire.serveur.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lesourire.commun.Facturation.StatutFacture;
import com.lesourire.serveur.entite.Facture;

public interface FactureRepository extends JpaRepository<Facture, Long> {

    /** Dernière facture de l'année (les numéros zéro-remplis trient bien). */
    Optional<Facture> findTopByNumeroStartingWithOrderByNumeroDesc(String prefixe);

    @Query("""
            SELECT f FROM Facture f
            JOIN FETCH f.patient p
            LEFT JOIN FETCH f.assureur
            LEFT JOIN FETCH f.societe
            WHERE (:statut IS NULL OR f.statut = :statut)
              AND (:q = ''
                   OR LOWER(f.numero) LIKE CONCAT('%', :q, '%')
                   OR LOWER(p.nom) LIKE CONCAT('%', :q, '%')
                   OR LOWER(p.prenom) LIKE CONCAT('%', :q, '%')
                   OR LOWER(p.numeroDossier) LIKE CONCAT('%', :q, '%'))
            ORDER BY f.dateFacture DESC, f.id DESC
            """)
    List<Facture> rechercher(@Param("q") String q, @Param("statut") StatutFacture statut);

    List<Facture> findByPatientIdOrderByDateFactureDesc(Long patientId);
}
