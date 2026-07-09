package com.lesourire.serveur.entite;

import java.math.BigDecimal;

import com.lesourire.commun.dto.AssureurDTO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assureur")
public class Assureur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(length = 30)
    private String telephone;

    private String email;

    private String adresse;

    @Column(name = "pourcentage_defaut", nullable = false, precision = 5, scale = 2)
    private BigDecimal pourcentageDefaut = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean actif = true;

    public AssureurDTO versDTO() {
        return new AssureurDTO(id, nom, telephone, email, pourcentageDefaut, actif);
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public BigDecimal getPourcentageDefaut() {
        return pourcentageDefaut;
    }

    public void setPourcentageDefaut(BigDecimal pourcentageDefaut) {
        this.pourcentageDefaut = pourcentageDefaut;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}
