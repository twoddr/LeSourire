package com.lesourire.client.service;

import java.util.List;

import com.lesourire.commun.Facturation.StatutFacture;
import com.lesourire.commun.dto.FactureDTO;
import com.lesourire.commun.dto.PaiementDTO;
import com.lesourire.commun.dto.PrestationDTO;
import com.lesourire.commun.dto.UtilisateurDTO;
import com.lesourire.commun.dto.ValeurLettreCleDTO;

/** Accès aux factures, paiements et au tarifaire nécessaire à l'encodage. */
public interface ServiceFacturation {

    List<FactureDTO> rechercher(String recherche, StatutFacture statut) throws Exception;

    FactureDTO obtenir(Long id) throws Exception;

    FactureDTO creer(FactureDTO facture) throws Exception;

    FactureDTO modifier(Long id, FactureDTO facture) throws Exception;

    FactureDTO emettre(Long id) throws Exception;

    FactureDTO annuler(Long id) throws Exception;

    FactureDTO encaisser(Long id, PaiementDTO paiement) throws Exception;

    FactureDTO supprimerPaiement(Long id, Long paiementId) throws Exception;

    /** Prestations actives du tarifaire, pour l'encodage des lignes. */
    List<PrestationDTO> prestations(String recherche) throws Exception;

    /** Valeurs de D et Z en vigueur, pour l'aperçu des montants. */
    List<ValeurLettreCleDTO> valeursLettres() throws Exception;

    List<UtilisateurDTO> praticiens() throws Exception;
}
