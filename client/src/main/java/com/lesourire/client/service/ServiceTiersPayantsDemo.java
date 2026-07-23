package com.lesourire.client.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.lesourire.commun.dto.AssureurDTO;
import com.lesourire.commun.dto.SocieteDTO;

public class ServiceTiersPayantsDemo implements ServiceTiersPayants {

    private final AtomicLong seq = new AtomicLong(10);
    private final List<AssureurDTO> assureurs = new ArrayList<>();
    private final List<SocieteDTO> societes = new ArrayList<>();

    public ServiceTiersPayantsDemo() {
        assureurs.add(new AssureurDTO(1L, "Assurance Alpha", null, null, new BigDecimal("70"), true));
        assureurs.add(new AssureurDTO(2L, "Mutuelle Bêta", null, null, new BigDecimal("80"), true));
        societes.add(new SocieteDTO(1L, "Société Gamma", null, null, new BigDecimal("50"), true));
    }

    @Override
    public List<AssureurDTO> listerAssureurs(boolean inclureInactifs) {
        return assureurs.stream().filter(a -> inclureInactifs || a.actif()).toList();
    }

    @Override
    public AssureurDTO creerAssureur(AssureurDTO dto) {
        AssureurDTO a = new AssureurDTO(seq.getAndIncrement(), dto.nom().trim(), dto.telephone(),
                dto.email(), dto.pourcentageDefaut(), dto.actif());
        assureurs.add(a);
        return a;
    }

    @Override
    public AssureurDTO modifierAssureur(Long id, AssureurDTO dto) {
        for (int i = 0; i < assureurs.size(); i++) {
            if (assureurs.get(i).id().equals(id)) {
                AssureurDTO a = new AssureurDTO(id, dto.nom().trim(), dto.telephone(),
                        dto.email(), dto.pourcentageDefaut(), dto.actif());
                assureurs.set(i, a);
                return a;
            }
        }
        throw new IllegalArgumentException("Assureur introuvable.");
    }

    @Override
    public List<SocieteDTO> listerSocietes(boolean inclureInactifs) {
        return societes.stream().filter(s -> inclureInactifs || s.actif()).toList();
    }

    @Override
    public SocieteDTO creerSociete(SocieteDTO dto) {
        SocieteDTO s = new SocieteDTO(seq.getAndIncrement(), dto.nom().trim(), dto.telephone(),
                dto.email(), dto.pourcentageDefaut(), dto.actif());
        societes.add(s);
        return s;
    }

    @Override
    public SocieteDTO modifierSociete(Long id, SocieteDTO dto) {
        for (int i = 0; i < societes.size(); i++) {
            if (societes.get(i).id().equals(id)) {
                SocieteDTO s = new SocieteDTO(id, dto.nom().trim(), dto.telephone(),
                        dto.email(), dto.pourcentageDefaut(), dto.actif());
                societes.set(i, s);
                return s;
            }
        }
        throw new IllegalArgumentException("Société introuvable.");
    }
}
