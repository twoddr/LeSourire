package com.lesourire.commun.dto;

import java.time.LocalDateTime;

import com.lesourire.commun.Rappels;

/**
 * Rappel / notification échangé entre serveur et client.
 * Classe mutable à champs publics, comme {@link RdvDTO}.
 */
public class RappelDTO {

    public Long id;
    public Long patientId;
    public String patientNom;
    public String patientTelephone;

    public Long rdvId;
    public LocalDateTime rdvDebut;

    public Rappels.Type type;
    public Rappels.Canal canal;
    public Rappels.Statut statut;

    public LocalDateTime datePrevue;
    public LocalDateTime dateEnvoi;

    /** Nombre de tentatives d'envoi déjà effectuées. */
    public int tentatives;

    public String destinataire;
    public String contenu;
    public String messageErreur;

    /**
     * Lien d'envoi assisté, renseigné par le serveur pour les canaux qui ne
     * peuvent pas être envoyés automatiquement :
     * {@code https://wa.me/…} (WhatsApp) ou {@code mailto:…} (e-mail).
     * Reste {@code null} pour le SMS (envoi automatique).
     */
    public String lienEnvoi;
}
