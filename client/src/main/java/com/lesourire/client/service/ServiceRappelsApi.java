package com.lesourire.client.service;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lesourire.client.coeur.ApiClient;
import com.lesourire.commun.dto.RappelDTO;

public class ServiceRappelsApi implements ServiceRappels {

    private final ApiClient api;

    public ServiceRappelsApi(ApiClient api) {
        this.api = api;
    }

    @Override
    public List<RappelDTO> aEnvoyer() throws Exception {
        return api.get("/api/rappels/a-envoyer", new TypeReference<List<RappelDTO>>() {
        });
    }

    @Override
    public RappelDTO envoyer(Long id) throws Exception {
        return api.post("/api/rappels/" + id + "/envoyer", null,
                new TypeReference<RappelDTO>() {
                });
    }

    @Override
    public RappelDTO marquerEnvoye(Long id) throws Exception {
        return api.post("/api/rappels/" + id + "/marquer-envoye", null,
                new TypeReference<RappelDTO>() {
                });
    }

    @Override
    public RappelDTO annuler(Long id) throws Exception {
        return api.post("/api/rappels/" + id + "/annuler", null,
                new TypeReference<RappelDTO>() {
                });
    }
}
