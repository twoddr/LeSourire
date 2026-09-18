package com.lesourire.commun.dto;

import java.time.LocalDateTime;

/**
 * Compteurs du journal des notifications patients.
 *
 * <p>Alimente le bandeau « En attente / Envoyées » et le dialogue d'historique
 * (nombre d'envois réussis, échecs, dernière envoi réussi).</p>
 */
public class RappelStatsDTO {

    public long enAttente;
    public long envoyees;
    public long echecs;
    public long annulees;

    /** Envois réussis depuis le début de la journée. */
    public long envoyeesAujourdhui;

    /** Horodatage du dernier envoi réussi, {@code null} si aucun. */
    public LocalDateTime derniereEnvoyeeLe;
}
