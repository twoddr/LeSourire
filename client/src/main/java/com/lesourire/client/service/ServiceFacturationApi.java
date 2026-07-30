package com.lesourire.client.service;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lesourire.client.coeur.ApiClient;
import com.lesourire.commun.Facturation.StatutFacture;
import com.lesourire.commun.dto.FactureDTO;
import com.lesourire.commun.dto.PaiementDTO;
import com.lesourire.commun.dto.PrestationDTO;
import com.lesourire.commun.dto.UtilisateurDTO;
import com.lesourire.commun.dto.ValeurLettreCleDTO;

/** Implémentation branchée sur l'API REST du serveur. */
public class ServiceFacturationApi implements ServiceFacturation {

    private final ApiClient api;

    public ServiceFacturationApi(ApiClient api) {
        this.api = api;
    }

    @Override
    public List<FactureDTO> rechercher(String recherche, StatutFacture statut) throws Exception {
        String chemin = "/api/factures?recherche=" + ApiClient.encoder(recherche)
                + (statut == null ? "" : "&statut=" + statut.name());
        return api.get(chemin, new TypeReference<List<FactureDTO>>() {
        });
    }

    @Override
    public FactureDTO obtenir(Long id) throws Exception {
        return api.get("/api/factures/" + id, new TypeReference<FactureDTO>() {
        });
    }

    @Override
    public FactureDTO creer(FactureDTO facture) throws Exception {
        return api.post("/api/factures", facture, new TypeReference<FactureDTO>() {
        });
    }

    @Override
    public FactureDTO modifier(Long id, FactureDTO facture) throws Exception {
        return api.put("/api/factures/" + id, facture, new TypeReference<FactureDTO>() {
        });
    }

    @Override
    public FactureDTO emettre(Long id) throws Exception {
        return api.post("/api/factures/" + id + "/emettre", null, new TypeReference<FactureDTO>() {
        });
    }

    @Override
    public FactureDTO annuler(Long id) throws Exception {
        return api.post("/api/factures/" + id + "/annuler", null, new TypeReference<FactureDTO>() {
        });
    }

    @Override
    public FactureDTO encaisser(Long id, PaiementDTO paiement) throws Exception {
        return api.post("/api/factures/" + id + "/paiements", paiement,
                new TypeReference<FactureDTO>() {
                });
    }

    @Override
    public FactureDTO supprimerPaiement(Long id, Long paiementId) throws Exception {
        return api.delete("/api/factures/" + id + "/paiements/" + paiementId,
                new TypeReference<FactureDTO>() {
                });
    }

    @Override
    public List<PrestationDTO> prestations(String recherche) throws Exception {
        return api.get("/api/prestations?recherche=" + ApiClient.encoder(recherche),
                new TypeReference<List<PrestationDTO>>() {
                });
    }

    @Override
    public List<ValeurLettreCleDTO> valeursLettres() throws Exception {
        return api.get("/api/lettres-cles/valeurs", new TypeReference<List<ValeurLettreCleDTO>>() {
        });
    }

    @Override
    public List<UtilisateurDTO> praticiens() throws Exception {
        return api.get("/api/praticiens", new TypeReference<List<UtilisateurDTO>>() {
        });
    }
}
