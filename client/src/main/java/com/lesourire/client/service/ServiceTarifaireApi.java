package com.lesourire.client.service;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lesourire.client.coeur.ApiClient;
import com.lesourire.commun.dto.CategoriePrestationDTO;
import com.lesourire.commun.dto.NouvelleValeurLettreDTO;
import com.lesourire.commun.dto.PrestationDTO;
import com.lesourire.commun.dto.ValeurLettreCleDTO;

public class ServiceTarifaireApi implements ServiceTarifaire {

    private final ApiClient api;

    public ServiceTarifaireApi(ApiClient api) {
        this.api = api;
    }

    @Override
    public List<CategoriePrestationDTO> categories() throws Exception {
        return api.get("/api/categories-prestation",
                new TypeReference<List<CategoriePrestationDTO>>() {
                });
    }

    @Override
    public List<PrestationDTO> rechercher(String recherche, boolean inclureInactifs)
            throws Exception {
        return api.get("/api/prestations?recherche=" + ApiClient.encoder(recherche)
                        + "&inclureInactifs=" + inclureInactifs,
                new TypeReference<List<PrestationDTO>>() {
                });
    }

    @Override
    public PrestationDTO creer(PrestationDTO dto) throws Exception {
        return api.post("/api/prestations", dto, new TypeReference<PrestationDTO>() {
        });
    }

    @Override
    public PrestationDTO modifier(Long id, PrestationDTO dto) throws Exception {
        return api.put("/api/prestations/" + id, dto, new TypeReference<PrestationDTO>() {
        });
    }

    @Override
    public List<ValeurLettreCleDTO> valeursEnVigueur() throws Exception {
        return api.get("/api/lettres-cles/valeurs",
                new TypeReference<List<ValeurLettreCleDTO>>() {
                });
    }

    @Override
    public ValeurLettreCleDTO changerValeur(String lettre, NouvelleValeurLettreDTO dto)
            throws Exception {
        return api.post("/api/lettres-cles/" + lettre + "/valeurs", dto,
                new TypeReference<ValeurLettreCleDTO>() {
                });
    }
}
