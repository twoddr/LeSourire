package com.lesourire.client.service;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lesourire.client.coeur.ApiClient;
import com.lesourire.commun.dto.AssureurDTO;
import com.lesourire.commun.dto.SocieteDTO;

public class ServiceTiersPayantsApi implements ServiceTiersPayants {

    private final ApiClient api;

    public ServiceTiersPayantsApi(ApiClient api) {
        this.api = api;
    }

    @Override
    public List<AssureurDTO> listerAssureurs(boolean inclureInactifs) throws Exception {
        return api.get("/api/assureurs?inclureInactifs=" + inclureInactifs,
                new TypeReference<List<AssureurDTO>>() {
                });
    }

    @Override
    public AssureurDTO creerAssureur(AssureurDTO dto) throws Exception {
        return api.post("/api/assureurs", dto, new TypeReference<AssureurDTO>() {
        });
    }

    @Override
    public AssureurDTO modifierAssureur(Long id, AssureurDTO dto) throws Exception {
        return api.put("/api/assureurs/" + id, dto, new TypeReference<AssureurDTO>() {
        });
    }

    @Override
    public List<SocieteDTO> listerSocietes(boolean inclureInactifs) throws Exception {
        return api.get("/api/societes?inclureInactifs=" + inclureInactifs,
                new TypeReference<List<SocieteDTO>>() {
                });
    }

    @Override
    public SocieteDTO creerSociete(SocieteDTO dto) throws Exception {
        return api.post("/api/societes", dto, new TypeReference<SocieteDTO>() {
        });
    }

    @Override
    public SocieteDTO modifierSociete(Long id, SocieteDTO dto) throws Exception {
        return api.put("/api/societes/" + id, dto, new TypeReference<SocieteDTO>() {
        });
    }
}
