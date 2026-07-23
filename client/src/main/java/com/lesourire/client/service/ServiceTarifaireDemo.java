package com.lesourire.client.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import com.lesourire.commun.dto.CategoriePrestationDTO;
import com.lesourire.commun.dto.NouvelleValeurLettreDTO;
import com.lesourire.commun.dto.PrestationDTO;
import com.lesourire.commun.dto.ValeurLettreCleDTO;

public class ServiceTarifaireDemo implements ServiceTarifaire {

    private final AtomicLong seq = new AtomicLong(100);
    private final List<CategoriePrestationDTO> categories = List.of(
            new CategoriePrestationDTO(1L, "Consultation", 1),
            new CategoriePrestationDTO(2L, "Soins conservateurs", 2),
            new CategoriePrestationDTO(3L, "Soins chirurgicaux", 3),
            new CategoriePrestationDTO(4L, "Prothèses dentaires", 4),
            new CategoriePrestationDTO(5L, "Radio diagnostique", 5),
            new CategoriePrestationDTO(6L, "Traitement orthodontique", 6));
    private final List<PrestationDTO> prestations = new ArrayList<>();
    private final List<ValeurLettreCleDTO> valeurs = new ArrayList<>();

    public ServiceTarifaireDemo() {
        PrestationDTO c = new PrestationDTO();
        c.id = 1L;
        c.code = "CONS-JOUR";
        c.libelle = "Consultation de jour";
        c.categorieId = 1L;
        c.categorieLibelle = "Consultation";
        c.tarifForfait = new BigDecimal("15000");
        c.actif = true;
        prestations.add(c);

        PrestationDTO cav = new PrestationDTO();
        cav.id = 2L;
        cav.code = "CAV-2F";
        cav.libelle = "Cavité composée (2 faces)";
        cav.categorieId = 2L;
        cav.categorieLibelle = "Soins conservateurs";
        cav.lettreCle = "D";
        cav.coefficient = new BigDecimal("12");
        cav.actif = true;
        prestations.add(cav);

        valeurs.add(new ValeurLettreCleDTO(1L, "D", new BigDecimal("1200"),
                LocalDate.of(2000, 1, 1), null));
        valeurs.add(new ValeurLettreCleDTO(2L, "Z", new BigDecimal("1200"),
                LocalDate.of(2000, 1, 1), null));
    }

    @Override
    public List<CategoriePrestationDTO> categories() {
        return categories;
    }

    @Override
    public List<PrestationDTO> rechercher(String recherche, boolean inclureInactifs) {
        String q = recherche == null ? "" : recherche.trim().toLowerCase(Locale.FRENCH);
        return prestations.stream()
                .filter(p -> inclureInactifs || p.actif)
                .filter(p -> q.isEmpty()
                        || p.code.toLowerCase(Locale.FRENCH).contains(q)
                        || p.libelle.toLowerCase(Locale.FRENCH).contains(q))
                .toList();
    }

    @Override
    public PrestationDTO creer(PrestationDTO dto) {
        dto.id = seq.getAndIncrement();
        dto.categorieLibelle = categories.stream()
                .filter(c -> c.id().equals(dto.categorieId))
                .map(CategoriePrestationDTO::libelle)
                .findFirst()
                .orElse("");
        prestations.add(dto);
        return dto;
    }

    @Override
    public PrestationDTO modifier(Long id, PrestationDTO dto) {
        for (int i = 0; i < prestations.size(); i++) {
            if (prestations.get(i).id.equals(id)) {
                dto.id = id;
                dto.categorieLibelle = categories.stream()
                        .filter(c -> c.id().equals(dto.categorieId))
                        .map(CategoriePrestationDTO::libelle)
                        .findFirst()
                        .orElse("");
                prestations.set(i, dto);
                return dto;
            }
        }
        throw new IllegalArgumentException("Prestation introuvable.");
    }

    @Override
    public List<ValeurLettreCleDTO> valeursEnVigueur() {
        return valeurs.stream().filter(v -> v.dateFin() == null).toList();
    }

    @Override
    public ValeurLettreCleDTO changerValeur(String lettre, NouvelleValeurLettreDTO dto) {
        LocalDate debut = dto.dateDebut() == null ? LocalDate.now() : dto.dateDebut();
        for (int i = 0; i < valeurs.size(); i++) {
            ValeurLettreCleDTO v = valeurs.get(i);
            if (v.lettreCle().equalsIgnoreCase(lettre) && v.dateFin() == null) {
                valeurs.set(i, new ValeurLettreCleDTO(v.id(), v.lettreCle(), v.valeur(),
                        v.dateDebut(), debut.minusDays(1)));
            }
        }
        ValeurLettreCleDTO n = new ValeurLettreCleDTO(seq.getAndIncrement(),
                lettre.toUpperCase(Locale.ROOT), dto.valeur(), debut, null);
        valeurs.add(n);
        return n;
    }
}
