package com.lesourire.serveur.entite;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.lesourire.commun.dto.ValeurLettreCleDTO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "valeur_lettre_cle")
public class ValeurLettreCle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_lettre_cle", nullable = false, length = 5)
    private String lettreCle;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valeur;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    public ValeurLettreCleDTO versDTO() {
        return new ValeurLettreCleDTO(id, lettreCle, valeur, dateDebut, dateFin);
    }

    public Long getId() {
        return id;
    }

    public String getLettreCle() {
        return lettreCle;
    }

    public void setLettreCle(String lettreCle) {
        this.lettreCle = lettreCle;
    }

    public BigDecimal getValeur() {
        return valeur;
    }

    public void setValeur(BigDecimal valeur) {
        this.valeur = valeur;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }
}
