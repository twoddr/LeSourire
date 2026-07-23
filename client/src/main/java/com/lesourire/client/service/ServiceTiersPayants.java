package com.lesourire.client.service;

import java.util.List;

import com.lesourire.commun.dto.AssureurDTO;
import com.lesourire.commun.dto.SocieteDTO;

/** Gestion des tiers payants (assureurs et sociétés). */
public interface ServiceTiersPayants {

    List<AssureurDTO> listerAssureurs(boolean inclureInactifs) throws Exception;

    AssureurDTO creerAssureur(AssureurDTO dto) throws Exception;

    AssureurDTO modifierAssureur(Long id, AssureurDTO dto) throws Exception;

    List<SocieteDTO> listerSocietes(boolean inclureInactifs) throws Exception;

    SocieteDTO creerSociete(SocieteDTO dto) throws Exception;

    SocieteDTO modifierSociete(Long id, SocieteDTO dto) throws Exception;
}
