package com.lesourire.commun.dto;

import java.time.LocalDateTime;

import com.lesourire.commun.Rappels;

/**
 * Saisie d'une notification manuelle (clic droit sur un rendez-vous).
 *
 * <p>Le serveur complète ce qui manque : le destinataire est résolu depuis la
 * fiche patient (canal demandé ou préférence enregistrée) et, si le message
 * n'est pas saisi, le modèle standard du cabinet est appliqué.</p>
 */
public class RappelEcritureDTO {

    public Long patientId;

    /** Rendez-vous concerné : obligatoire pour une confirmation ou un rappel. */
    public Long rdvId;

    public Rappels.Type type;
    public Rappels.Canal canal;

    /** Message personnalisé ; laissé vide, le modèle standard est utilisé. */
    public String contenu;

    /** Échéance d'envoi ; {@code null} = envoi immédiat (prochaine passe). */
    public LocalDateTime datePrevue;
}
