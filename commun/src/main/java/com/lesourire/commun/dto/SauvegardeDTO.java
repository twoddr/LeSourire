package com.lesourire.commun.dto;

import java.time.LocalDateTime;

public record SauvegardeDTO(
        String nom,
        long tailleOctets,
        LocalDateTime dateModification) {

}
