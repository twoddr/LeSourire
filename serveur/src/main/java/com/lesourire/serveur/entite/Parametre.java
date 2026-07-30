package com.lesourire.serveur.entite;

import java.time.LocalDateTime;

import com.lesourire.commun.dto.ParametreDTO;

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

    @Column(length = 255)
    private String description;

    @Column(name = "modifie_le", insertable = false, updatable = false)
    private LocalDateTime modifieLe;

    public ParametreDTO versDTO() {
        return new ParametreDTO(cle, valeur, description);
    }

    public String getCle() {
        return cle;
    }

    public String getValeur() {
        return valeur;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }

}
