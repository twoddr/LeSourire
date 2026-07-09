package com.lesourire.client.service;

import java.util.List;

import com.lesourire.client.coeur.ApiClient.ApiException;
import com.lesourire.commun.dto.AssureurDTO;
import com.lesourire.commun.dto.PatientDTO;
import com.lesourire.commun.dto.SocieteDTO;

/**
 * Accès aux données patients et aux référentiels des tiers payants.
 * Deux implémentations : via le serveur (normal) ou en mémoire (mode démo).
 */
public interface ServicePatients {

    List<PatientDTO> rechercher(String recherche) throws ApiException;

    PatientDTO creer(PatientDTO patient) throws ApiException;

    PatientDTO modifier(PatientDTO patient) throws ApiException;

    List<AssureurDTO> listerAssureurs() throws ApiException;

    AssureurDTO creerAssureur(String nom, java.math.BigDecimal pourcentageDefaut) throws ApiException;

    List<SocieteDTO> listerSocietes() throws ApiException;

    SocieteDTO creerSociete(String nom, java.math.BigDecimal pourcentageDefaut) throws ApiException;
}
