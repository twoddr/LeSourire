package com.lesourire.client.service;

import java.util.ArrayList;
import java.util.List;

import com.lesourire.commun.dto.ParametreDTO;

public class ServiceParametresDemo implements ServiceParametres {

    private final List<ParametreDTO> parametres = new ArrayList<>();

    public ServiceParametresDemo() {
        parametres.add(new ParametreDTO("cabinet.nom",
                "Cabinet Dentaire Le Sourire", "Nom affiché sur les documents"));
        parametres.add(new ParametreDTO("cabinet.telephone",
                "(237) 233 431 411", "Téléphone du cabinet"));
        parametres.add(new ParametreDTO("rappel.jours_avant_rdv",
                "2", "Nombre de jours avant RDV pour le rappel"));
    }

    @Override
    public List<ParametreDTO> lister() {
        return List.copyOf(parametres);
    }

    @Override
    public ParametreDTO modifier(String cle, String valeur) {
        for (int i = 0; i < parametres.size(); i++) {
            ParametreDTO p = parametres.get(i);
            if (p.cle().equals(cle)) {
                ParametreDTO maj = new ParametreDTO(cle, valeur, p.description());
                parametres.set(i, maj);
                return maj;
            }
        }
        throw new IllegalArgumentException("Paramètre inconnu : " + cle);
    }
}
