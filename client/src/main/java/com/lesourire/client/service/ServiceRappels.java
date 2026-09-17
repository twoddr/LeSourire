package com.lesourire.client.service;

import java.util.List;

import com.lesourire.commun.dto.RappelDTO;

/**
 * Notifications patients côté client : file d'attente des rappels restant à
 * traiter, envoi automatique (SMS) et acquittement des envois assistés
 * (WhatsApp, e-mail).
 */
public interface ServiceRappels {

    List<RappelDTO> aEnvoyer() throws Exception;

    RappelDTO envoyer(Long id) throws Exception;

    RappelDTO marquerEnvoye(Long id) throws Exception;

    RappelDTO annuler(Long id) throws Exception;
}
