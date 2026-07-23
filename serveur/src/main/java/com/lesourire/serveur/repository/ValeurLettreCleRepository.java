package com.lesourire.serveur.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lesourire.serveur.entite.ValeurLettreCle;

public interface ValeurLettreCleRepository extends JpaRepository<ValeurLettreCle, Long> {

    List<ValeurLettreCle> findByLettreCleOrderByDateDebutDesc(String lettreCle);

    Optional<ValeurLettreCle> findByLettreCleAndDateFinIsNull(String lettreCle);

    List<ValeurLettreCle> findByDateFinIsNullOrderByLettreCleAsc();
}
