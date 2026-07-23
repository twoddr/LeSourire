package com.lesourire.client.service;

import java.util.List;

import com.lesourire.commun.dto.UtilisateurDTO;
import com.lesourire.commun.dto.UtilisateurEcritureDTO;

/** Accès au personnel (utilisateurs de l'application). */
public interface ServiceUtilisateurs {

    List<UtilisateurDTO> rechercher(String recherche, boolean inclureInactifs) throws Exception;

    UtilisateurDTO creer(UtilisateurEcritureDTO saisie) throws Exception;

    UtilisateurDTO modifier(Long id, UtilisateurEcritureDTO saisie) throws Exception;
}
