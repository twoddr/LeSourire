package com.lesourire.serveur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lesourire.commun.Rappels;
import com.lesourire.serveur.entite.Rappel;

public interface RappelRepository extends JpaRepository<Rappel, Long> {

    @Modifying(clearAutomatically = true)
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

    List<Rappel> findByRdvIdAndTypeAndStatut(Long rdvId, Rappels.Type type, Rappels.Statut statut);
}
