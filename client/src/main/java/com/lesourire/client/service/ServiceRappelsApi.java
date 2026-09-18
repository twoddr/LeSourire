package com.lesourire.client.service;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lesourire.client.coeur.ApiClient;
import com.lesourire.commun.Rappels;
import com.lesourire.commun.dto.RappelDTO;
import com.lesourire.commun.dto.RappelEcritureDTO;
import com.lesourire.commun.dto.RappelStatsDTO;
import com.lesourire.commun.dto.RdvDTO;

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
    public RappelDTO creer(RdvDTO rdv, Rappels.Type type, Rappels.Canal canal, String contenu)
            throws Exception {
        RappelEcritureDTO saisie = new RappelEcritureDTO();
        saisie.patientId = rdv.patientId;
        saisie.rdvId = rdv.id;
        saisie.type = type;
        saisie.canal = canal;
        saisie.contenu = contenu;
        return api.post("/api/rappels", saisie, new TypeReference<RappelDTO>() {
        });
    }

    @Override
    public List<RappelDTO> journal(int limite) throws Exception {
        return api.get("/api/rappels/journal?limite=" + limite,
                new TypeReference<List<RappelDTO>>() {
                });
    }

    @Override
    public RappelStatsDTO statistiques() throws Exception {
        return api.get("/api/rappels/stats", new TypeReference<RappelStatsDTO>() {
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
