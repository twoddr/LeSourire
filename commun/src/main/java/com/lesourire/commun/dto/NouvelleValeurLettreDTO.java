package com.lesourire.commun.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Demande de mise à jour de la valeur d'une lettre-clé (clôt la période courante). */
public record NouvelleValeurLettreDTO(
        BigDecimal valeur,
        LocalDate dateDebut) {
}
