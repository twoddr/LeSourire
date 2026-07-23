package com.lesourire.commun.dto;

/** Fournisseur de stock. */
public class FournisseurDTO {

    public Long id;
    public String nom;
    public String contact;
    public String telephone;
    public String email;
    public String adresse;
    public String notes;
    public boolean actif = true;

    @Override
    public String toString() {
        return nom == null ? "" : nom;
    }
}
