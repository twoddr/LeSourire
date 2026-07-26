package com.lesourire.serveur.entite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parametre")
public class Parametre {

    @Id
    @Column(length = 100)
    private String cle;

    @Column(columnDefinition = "TEXT")
    private String valeur;

    public String getCle() {
        return cle;
    }

    public String getValeur() {
        return valeur;
    }
}
