package com.lesourire.serveur.repository;

import java.time.LocalDate;
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

    /**
     * Factures filtrées par statut et période. Nom/prénom du patient étant
     * chiffrés en base, le filtre par texte se fait en mémoire dans le service.
     */
    @Query("""
            SELECT f FROM Facture f
            JOIN FETCH f.patient p
            LEFT JOIN FETCH f.assureur
            LEFT JOIN FETCH f.societe
            WHERE (:statut IS NULL OR f.statut = :statut)
              AND (:debut IS NULL OR f.dateFacture >= :debut)
              AND (:fin IS NULL OR f.dateFacture <= :fin)
            ORDER BY f.dateFacture DESC, f.id DESC
            """)
    List<Facture> rechercher(@Param("statut") StatutFacture statut,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin);

    List<Facture> findByPatientIdOrderByDateFactureDesc(Long patientId);

    /** Relances par payeur (vue SQL V3). */
    @Query(value = """
            SELECT id, numero, date_facture, date_echeance,
                   patient_nom, patient_prenom, payeur_type, payeur_nom, solde
            FROM v_facture_relance
            ORDER BY (date_echeance IS NULL), date_echeance, numero, payeur_type
            """, nativeQuery = true)
    List<Object[]> listerRelances();
}
