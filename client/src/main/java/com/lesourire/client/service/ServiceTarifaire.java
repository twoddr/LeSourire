package com.lesourire.client.service;

import java.util.List;

import com.lesourire.commun.dto.CategoriePrestationDTO;
import com.lesourire.commun.dto.NouvelleValeurLettreDTO;
import com.lesourire.commun.dto.PrestationDTO;
import com.lesourire.commun.dto.ValeurLettreCleDTO;

public interface ServiceTarifaire {

    List<CategoriePrestationDTO> categories() throws Exception;

    List<PrestationDTO> rechercher(String recherche, boolean inclureInactifs) throws Exception;

    PrestationDTO creer(PrestationDTO dto) throws Exception;

    PrestationDTO modifier(Long id, PrestationDTO dto) throws Exception;

    List<ValeurLettreCleDTO> valeursEnVigueur() throws Exception;

    ValeurLettreCleDTO changerValeur(String lettre, NouvelleValeurLettreDTO dto) throws Exception;
}
