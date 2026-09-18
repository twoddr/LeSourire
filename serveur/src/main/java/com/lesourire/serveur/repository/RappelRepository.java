package com.lesourire.serveur.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lesourire.commun.Rappels;
import com.lesourire.serveur.entite.Rappel;

public interface RappelRepository extends JpaRepository<Rappel, Long> {

    // flushAutomatically : sans ça, un save() non encore flushé est perdu quand
    // clearAutomatically vide le contexte (ex. modification d'heure de RDV).
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Rappel r SET r.statut = :annule
            WHERE r.rdv.id = :rdvId
              AND r.type = :type
              AND r.statut = :enAttente
            """)
    int annulerEnAttentePourRdv(@Param("rdvId") Long rdvId,
            @Param("type") Rappels.Type type,
            @Param("annule") Rappels.Statut annule,
            @Param("enAttente") Rappels.Statut enAttente);

    /** Annule toutes les notifications en attente d'un rendez-vous (tous types). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Rappel r SET r.statut = :annule
            WHERE r.rdv.id = :rdvId
              AND r.statut = :enAttente
            """)
    int annulerToutEnAttentePourRdv(@Param("rdvId") Long rdvId,
            @Param("annule") Rappels.Statut annule,
            @Param("enAttente") Rappels.Statut enAttente);

    List<Rappel> findByRdvIdAndTypeAndStatut(Long rdvId, Rappels.Type type, Rappels.Statut statut);

    /** Boîte de réception : rappels d'un état donné, les plus urgents d'abord. */
    List<Rappel> findByStatutOrderByDatePrevueAsc(Rappels.Statut statut);

    /** Identifiants des envois automatiques arrivés à échéance. */
    @Query("""
            SELECT r.id FROM Rappel r
            WHERE r.statut = :enAttente
              AND r.canal = :canal
              AND r.datePrevue <= :limite
            ORDER BY r.datePrevue
            """)
    List<Long> idsDus(@Param("enAttente") Rappels.Statut enAttente,
            @Param("canal") Rappels.Canal canal,
            @Param("limite") LocalDateTime limite);

    // ------------------------------------------------- journal des notifications

    /** Derniers rappels d'un état donné, les plus récents d'abord (paginé). */
    List<Rappel> findByStatutOrderByDateEnvoiDesc(Rappels.Statut statut, Pageable pageable);

    long countByStatut(Rappels.Statut statut);

    /** Compteur « envoyées aujourd'hui » : s'appuie sur l'horodatage d'envoi. */
    long countByStatutAndDateEnvoiGreaterThanEqual(Rappels.Statut statut, LocalDateTime depuis);
}

