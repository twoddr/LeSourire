package com.lesourire.serveur.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.lesourire.serveur.entite.Acte;
import com.lesourire.serveur.entite.AuditLog;
import com.lesourire.serveur.entite.Facture;
import com.lesourire.serveur.entite.Patient;
import com.lesourire.serveur.entite.PatientCouverture;
import com.lesourire.serveur.entite.Paiement;
import com.lesourire.serveur.entite.Rappel;
import com.lesourire.serveur.entite.Rdv;
import com.lesourire.serveur.entite.Utilisateur;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Table;

/**
 * L'inventaire {@link ColonnesChiffrees} sert au contrôle de démarrage et à
 * l'outil de secours : il doit rester exactement aligné sur les entités.
 */
class ColonnesChiffreesTest {

    private static final List<Class<?>> ENTITES = List.of(Patient.class, Utilisateur.class,
            PatientCouverture.class, Rappel.class, Rdv.class, Acte.class, Facture.class,
            Paiement.class, AuditLog.class);

    @Test
    @DisplayName("L'inventaire recense 28 colonnes chiffrées réparties sur 9 tables")
    void inventaireComplet() {
        assertEquals(28, ColonnesChiffrees.COLONNES.size());
        assertEquals(9, ColonnesChiffrees.COLONNES.stream()
                .map(ColonnesChiffrees.ColonneChiffree::table).distinct().count());
        assertEquals(ColonnesChiffrees.COLONNES.size(), ColonnesChiffrees.references().stream()
                .distinct().count());
    }

    @Test
    @DisplayName("Chaque champ @Convert(ChiffreurTexte) figure dans l'inventaire, et réciproquement")
    void aligneSurLesEntites() {
        Set<String> attendues = new LinkedHashSet<>();
        for (Class<?> entite : ENTITES) {
            for (Field champ : entite.getDeclaredFields()) {
                Convert conversion = champ.getAnnotation(Convert.class);
                if (conversion != null && ChiffreurTexte.class.equals(conversion.converter())) {
                    attendues.add(nomTable(entite) + "." + nomColonne(champ));
                }
            }
        }
        assertEquals(attendues, new LinkedHashSet<>(ColonnesChiffrees.references()),
                "ColonnesChiffrees doit être mis à jour avec les entités");
        assertTrue(attendues.contains("patient.nom"));
        assertTrue(attendues.contains("audit_log.details"));
    }

    private static String nomTable(Class<?> entite) {
        Table table = entite.getAnnotation(Table.class);
        assertNotNull(table, "Entité sans @Table : " + entite.getName());
        return table.name();
    }

    /** Nom SQL du champ : celui de @Column, sinon la conversion camelCase → snake_case. */
    private static String nomColonne(Field champ) {
        Column colonne = champ.getAnnotation(Column.class);
        if (colonne != null && !colonne.name().isBlank()) {
            return colonne.name();
        }
        return champ.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}