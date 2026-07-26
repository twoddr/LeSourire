package com.lesourire.client.service;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lesourire.client.coeur.ApiClient;
import com.lesourire.commun.StatutRdv;
import com.lesourire.commun.dto.RdvDTO;
import com.lesourire.commun.dto.StatutRdvDTO;
import com.lesourire.commun.dto.UtilisateurDTO;

public class ServiceRdvApi implements ServiceRdv {

    private final ApiClient api;

    public ServiceRdvApi(ApiClient api) {
        this.api = api;
    }

    @Override
    public List<RdvDTO> lister(LocalDateTime debut, LocalDateTime fin, Long praticienId)
            throws Exception {
        StringBuilder url = new StringBuilder("/api/rdv?debut=")
                .append(ApiClient.encoder(debut.toString()))
                .append("&fin=")
                .append(ApiClient.encoder(fin.toString()));
        if (praticienId != null) {
            url.append("&praticienId=").append(praticienId);
        }
        return api.get(url.toString(), new TypeReference<List<RdvDTO>>() {
        });
    }

    @Override
    public long compter(LocalDateTime debut, LocalDateTime fin) throws Exception {
        Number n = api.get("/api/rdv/count?debut=" + ApiClient.encoder(debut.toString())
                        + "&fin=" + ApiClient.encoder(fin.toString()),
                new TypeReference<Long>() {
                });
        return n.longValue();
    }

    @Override
    public RdvDTO creer(RdvDTO dto) throws Exception {
        return api.post("/api/rdv", dto, new TypeReference<RdvDTO>() {
        });
    }

    @Override
    public RdvDTO modifier(Long id, RdvDTO dto) throws Exception {
        return api.put("/api/rdv/" + id, dto, new TypeReference<RdvDTO>() {
        });
    }

    @Override
    public RdvDTO changerStatut(Long id, StatutRdv statut) throws Exception {
        return api.put("/api/rdv/" + id + "/statut", new StatutRdvDTO(statut),
                new TypeReference<RdvDTO>() {
                });
    }

    @Override
    public List<UtilisateurDTO> praticiens() throws Exception {
        return api.get("/api/praticiens", new TypeReference<List<UtilisateurDTO>>() {
        });
    }
}
