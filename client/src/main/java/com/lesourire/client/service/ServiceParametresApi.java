package com.lesourire.client.service;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lesourire.client.coeur.ApiClient;
import com.lesourire.commun.dto.ParametreDTO;

public class ServiceParametresApi implements ServiceParametres {

    private final ApiClient api;

    public ServiceParametresApi(ApiClient api) {
        this.api = api;
    }

    @Override
    public List<ParametreDTO> lister() throws Exception {
        return api.get("/api/parametres", new TypeReference<List<ParametreDTO>>() {
        });
    }

    @Override
    public ParametreDTO modifier(String cle, String valeur) throws Exception {
        // La clé est dans l'URL ; le corps ne sert qu'à envoyer la valeur
        ParametreDTO corps = new ParametreDTO(cle, valeur, null);
        return api.put("/api/parametres/" + ApiClient.encoder(cle), corps,
                new TypeReference<ParametreDTO>() {
                });
    }
}
