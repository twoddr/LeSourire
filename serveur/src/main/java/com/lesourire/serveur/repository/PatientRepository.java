package com.lesourire.serveur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lesourire.serveur.entite.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    /**
     * Recherche parmi les dossiers actifs sur nom, prénom, n° de dossier
     * et téléphones. Une recherche vide renvoie tous les dossiers actifs.
     */
    @Query("""
            SELECT p FROM Patient p
            WHERE p.actif = true
              AND (:q = ''
                   OR LOWER(p.nom) LIKE CONCAT('%', :q, '%')
                   OR LOWER(p.prenom) LIKE CONCAT('%', :q, '%')
                   OR LOWER(p.numeroDossier) LIKE CONCAT('%', :q, '%')
                   OR p.telephone LIKE CONCAT('%', :q, '%')
                   OR p.telephoneWhatsapp LIKE CONCAT('%', :q, '%'))
            ORDER BY p.nom, p.prenom
            """)
    List<Patient> rechercher(@Param("q") String rechercheMinuscule);
}
