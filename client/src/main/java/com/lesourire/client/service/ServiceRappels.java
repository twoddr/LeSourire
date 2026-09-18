package com.lesourire.client.service;

import java.util.List;

import com.lesourire.commun.Rappels;
import com.lesourire.commun.dto.RappelDTO;
import com.lesourire.commun.dto.RappelStatsDTO;
import com.lesourire.commun.dto.RdvDTO;

/**
 * Notifications patients côté client : file d'attente des rappels restant à
 * traiter, création manuelle depuis l'agenda, envoi automatique (SMS),
 * acquittement des envois assistés (WhatsApp, e-mail) et journal des envois.
 */
public interface ServiceRappels {

    List<RappelDTO> aEnvoyer() throws Exception;

    /**
     * Programme une notification rattachée à un rendez-vous (clic droit sur un
     * bloc de l'agenda). Un {@code contenu} vide laisse le serveur appliquer le
     * message standard du cabinet ; le patient doit accepter d'être prévenu.
     */
    RappelDTO creer(RdvDTO rdv, Rappels.Type type, Rappels.Canal canal, String contenu)
            throws Exception;

    /** Derniers envois réussis, les plus récents d'abord. */
    List<RappelDTO> journal(int limite) throws Exception;

    /** Compteurs du journal (en attente, envoyées, échecs, aujourd'hui). */
    RappelStatsDTO statistiques() throws Exception;

    RappelDTO envoyer(Long id) throws Exception;

    RappelDTO marquerEnvoye(Long id) throws Exception;

    RappelDTO annuler(Long id) throws Exception;
}
