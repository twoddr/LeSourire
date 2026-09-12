package com.lesourire.serveur.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertisseur JPA qui chiffre (et déchiffre) un champ texte sensible via
 * {@link Chiffrement}. À poser explicitement avec {@code @Convert} sur les
 * attributs concernés (patient, utilisateur, patient_couverture).
 */
@Converter
public class ChiffreurTexte implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribut) {
        return Chiffrement.chiffrer(attribut);
    }

    @Override
    public String convertToEntityAttribute(String colonne) {
        return Chiffrement.dechiffrer(colonne);
    }
}
