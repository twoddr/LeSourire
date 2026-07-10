package com.lesourire.commun.dto;

import java.time.LocalDate;

/** Demande de clôture d'une couverture (seule modification autorisée). */
public record ClotureCouvertureDTO(LocalDate dateFin, String motifFin) {
}
