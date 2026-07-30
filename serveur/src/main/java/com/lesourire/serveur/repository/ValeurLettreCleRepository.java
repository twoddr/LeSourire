package com.lesourire.serveur.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lesourire.serveur.entite.ValeurLettreCle;

public interface ValeurLettreCleRepository extends JpaRepository<ValeurLettreCle, Long> {

    List<ValeurLettreCle> findByLettreCleOrderByDateDebutDesc(String lettreCle);

    /** Valeur de la lettre-clé applicable à une date donnée. */
    @Query("""
            SELECT v FROM ValeurLettreCle v
            WHERE v.lettreCle = :lettre
              AND v.dateDebut <= :jour
              AND (v.dateFin IS NULL OR v.dateFin >= :jour)
            """)
    Optional<ValeurLettreCle> valeurAuJour(@Param("lettre") String lettre,
            @Param("jour") LocalDate jour);

    Optional<ValeurLettreCle> findByLettreCleAndDateFinIsNull(String lettreCle);

    List<ValeurLettreCle> findByDateFinIsNullOrderByLettreCleAsc();
}
