package com.lesourire.commun;

/** Nature d'un mouvement de stock. */
public enum TypeMouvementStock {
    ENTREE("Entrée (achat/livraison)"),
    SORTIE("Sortie (consommation)"),
    AJUSTEMENT("Ajustement d'inventaire"),
    PEREMPTION("Retrait pour péremption");

    private final String libelle;

    TypeMouvementStock(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
