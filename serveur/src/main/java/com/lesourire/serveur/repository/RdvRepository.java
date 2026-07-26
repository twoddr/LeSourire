package com.lesourire.serveur.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lesourire.commun.StatutRdv;
import com.lesourire.serveur.entite.Rdv;

public interface RdvRepository extends JpaRepository<Rdv, Long> {

    @Query("""
            SELECT r FROM Rdv r
            WHERE r.debut >= :debut AND r.debut < :fin
              AND (:praticienId IS NULL OR r.praticien.id = :praticienId)
            ORDER BY r.debut
            """)
    List<Rdv> trouverEntre(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin,
            @Param("praticienId") Long praticienId);

    @Query("""
            SELECT COUNT(r) FROM Rdv r
            WHERE r.praticien.id = :praticienId
              AND r.statut NOT IN :exclus
              AND r.debut < :fin AND r.fin > :debut
              AND (:exclureId IS NULL OR r.id <> :exclureId)
            """)
    long compterChevauchements(@Param("praticienId") Long praticienId,
            @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin,
            @Param("exclureId") Long exclureId, @Param("exclus") List<StatutRdv> exclus);

    long countByDebutGreaterThanEqualAndDebutLessThanAndStatutNotIn(
            LocalDateTime debut, LocalDateTime fin, List<StatutRdv> exclus);
}
