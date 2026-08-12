package com.lesourire.client.service;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lesourire.client.coeur.ApiClient;
import com.lesourire.commun.dto.EncaissementDTO;
import com.lesourire.commun.dto.ImpayeDTO;

public class ServiceComptabiliteApi implements ServiceComptabilite {

    private final ApiClient api;

    public ServiceComptabiliteApi(ApiClient api) {
        this.api = api;
    }

    @Override
    public List<EncaissementDTO> encaissements(LocalDate date) throws Exception {
        String q = date == null ? "" : "?date=" + date;
        return api.get("/api/comptabilite/encaissements" + q,
                new TypeReference<List<EncaissementDTO>>() {
                });
    }

    @Override
    public List<EncaissementDTO> journal(LocalDate debut, LocalDate fin) throws Exception {
        return api.get("/api/comptabilite/journal?debut=" + debut + "&fin=" + fin,
                new TypeReference<List<EncaissementDTO>>() {
                });
    }

    @Override
    public List<ImpayeDTO> impayes() throws Exception {
        return api.get("/api/comptabilite/impayes",
                new TypeReference<List<ImpayeDTO>>() {
                });
    }
}
