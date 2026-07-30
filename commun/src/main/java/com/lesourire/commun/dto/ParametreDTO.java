package com.lesourire.commun.dto;

/** Paramètre applicatif (clé / valeur / description). */
public record ParametreDTO(
        String cle,
        String valeur,
        String description) {
}
