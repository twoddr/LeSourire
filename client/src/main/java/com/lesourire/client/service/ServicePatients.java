package com.lesourire.client.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.lesourire.client.coeur.ApiClient.ApiException;
import com.lesourire.commun.dto.AssureurDTO;
import com.lesourire.commun.dto.CouvertureDTO;
import com.lesourire.commun.dto.PatientDTO;
import com.lesourire.commun.dto.SocieteDTO;

/**
 * Accès aux données patients, couvertures et référentiels des tiers payants.
 * Deux implémentations : via le serveur (normal) ou en mémoire (mode démo).
 */
public interface ServicePatients {

    List<PatientDTO> rechercher(String recherche) throws ApiException;

    /** Dossier complet, historique des couvertures inclus. */
    PatientDTO obtenir(Long id) throws ApiException;

    PatientDTO creer(PatientDTO patient) throws ApiException;

    PatientDTO modifier(PatientDTO patient) throws ApiException;

    CouvertureDTO ajouterCouverture(Long patientId, CouvertureDTO couverture) throws ApiException;

    CouvertureDTO cloturerCouverture(Long patientId, Long couvertureId,
            LocalDate dateFin, String motifFin) throws ApiException;

    List<AssureurDTO> listerAssureurs() throws ApiException;

    AssureurDTO creerAssureur(String nom, BigDecimal pourcentageDefaut) throws ApiException;

    List<SocieteDTO> listerSocietes() throws ApiException;

    SocieteDTO creerSociete(String nom, BigDecimal pourcentageDefaut) throws ApiException;
}
