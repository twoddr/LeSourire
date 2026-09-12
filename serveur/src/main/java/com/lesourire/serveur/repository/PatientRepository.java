package com.lesourire.serveur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lesourire.serveur.entite.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    /**
     * Dossiers actifs, sans tri ni filtre SQL : nom, prénom et téléphones sont
     * chiffrés en base, la recherche et le tri se font donc en mémoire dans le
     * service, sur les valeurs déchiffrées.
     */
    List<Patient> findByActifTrue();
}
