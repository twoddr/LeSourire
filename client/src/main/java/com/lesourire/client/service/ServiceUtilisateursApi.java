package com.lesourire.client.service;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lesourire.client.coeur.ApiClient;
import com.lesourire.commun.dto.UtilisateurDTO;
import com.lesourire.commun.dto.UtilisateurEcritureDTO;

public class ServiceUtilisateursApi implements ServiceUtilisateurs {

    private final ApiClient api;

    public ServiceUtilisateursApi(ApiClient api) {
        this.api = api;
    }

    @Override
    public List<UtilisateurDTO> rechercher(String recherche, boolean inclureInactifs)
            throws Exception {
        return api.get("/api/utilisateurs?recherche=" + ApiClient.encoder(recherche)
                        + "&inclureInactifs=" + inclureInactifs,
                new TypeReference<List<UtilisateurDTO>>() {
                });
    }

    @Override
    public UtilisateurDTO creer(UtilisateurEcritureDTO saisie) throws Exception {
        return api.post("/api/utilisateurs", saisie, new TypeReference<UtilisateurDTO>() {
        });
    }

    @Override
    public UtilisateurDTO modifier(Long id, UtilisateurEcritureDTO saisie) throws Exception {
        return api.put("/api/utilisateurs/" + id, saisie, new TypeReference<UtilisateurDTO>() {
        });
    }
}
