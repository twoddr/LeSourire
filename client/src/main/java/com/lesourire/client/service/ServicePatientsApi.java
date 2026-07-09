package com.lesourire.client.service;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lesourire.client.coeur.ApiClient;
import com.lesourire.client.coeur.ApiClient.ApiException;
import com.lesourire.commun.dto.AssureurDTO;
import com.lesourire.commun.dto.PatientDTO;
import com.lesourire.commun.dto.SocieteDTO;

/** Implémentation branchée sur l'API REST du serveur. */
public class ServicePatientsApi implements ServicePatients {

    private final ApiClient api;

    public ServicePatientsApi(ApiClient api) {
        this.api = api;
    }

    @Override
    public List<PatientDTO> rechercher(String recherche) throws ApiException {
        return api.get("/api/patients?recherche=" + ApiClient.encoder(recherche),
                new TypeReference<List<PatientDTO>>() {
                });
    }

    @Override
    public PatientDTO creer(PatientDTO patient) throws ApiException {
        return api.post("/api/patients", patient, new TypeReference<PatientDTO>() {
        });
    }

    @Override
    public PatientDTO modifier(PatientDTO patient) throws ApiException {
        return api.put("/api/patients/" + patient.id, patient, new TypeReference<PatientDTO>() {
        });
    }

    @Override
    public List<AssureurDTO> listerAssureurs() throws ApiException {
        return api.get("/api/assureurs", new TypeReference<List<AssureurDTO>>() {
        });
    }

    @Override
    public AssureurDTO creerAssureur(String nom, BigDecimal pourcentageDefaut) throws ApiException {
        AssureurDTO nouveau = new AssureurDTO(null, nom, null, null, pourcentageDefaut, true);
        return api.post("/api/assureurs", nouveau, new TypeReference<AssureurDTO>() {
        });
    }

    @Override
    public List<SocieteDTO> listerSocietes() throws ApiException {
        return api.get("/api/societes", new TypeReference<List<SocieteDTO>>() {
        });
    }

    @Override
    public SocieteDTO creerSociete(String nom, BigDecimal pourcentageDefaut) throws ApiException {
        SocieteDTO nouvelle = new SocieteDTO(null, nom, null, null, pourcentageDefaut, true);
        return api.post("/api/societes", nouvelle, new TypeReference<SocieteDTO>() {
        });
    }
}
