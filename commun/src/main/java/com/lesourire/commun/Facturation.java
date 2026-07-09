package com.lesourire.commun;

/** Énumérations liées à la facturation et aux paiements. */
public final class Facturation {

    private Facturation() {
    }

    /** Cycle de vie d'une facture. */
    public enum StatutFacture {
        BROUILLON,
        EMISE,
        PARTIELLEMENT_PAYEE,
        PAYEE,
        ANNULEE
    }

    /** Mode de règlement d'un paiement. */
    public enum ModePaiement {
        ESPECES("Espèces"),
        CHEQUE("Chèque"),
        VIREMENT("Virement"),
        MOBILE_MONEY("Mobile Money"),
        CARTE("Carte bancaire");

        private final String libelle;

        ModePaiement(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }

    /** Qui règle une partie de la facture (quote-part). */
    public enum Payeur {
        PATIENT,
        ASSUREUR,
        SOCIETE
    }
}
