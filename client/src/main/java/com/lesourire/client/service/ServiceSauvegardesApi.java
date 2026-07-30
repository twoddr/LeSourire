package com.lesourire.client.service;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lesourire.client.coeur.ApiClient;
import com.lesourire.commun.dto.SauvegardeDTO;

public class ServiceSauvegardesApi implements ServiceSauvegardes {

    private final ApiClient api;

    public ServiceSauvegardesApi(ApiClient api) {
        this.api = api;
    }

    @Override
    public List<SauvegardeDTO> lister() throws Exception {
        return api.get("/api/sauvegardes", new TypeReference<List<SauvegardeDTO>>() {
        });
    }

    @Override
    public SauvegardeDTO lancer() throws Exception {
        return api.post("/api/sauvegardes", null, new TypeReference<SauvegardeDTO>() {
        });
    }
}
