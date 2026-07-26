package com.lesourire.client.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.lesourire.commun.Role;
import com.lesourire.commun.StatutRdv;
import com.lesourire.commun.TypeRdv;
import com.lesourire.commun.dto.RdvDTO;
import com.lesourire.commun.dto.UtilisateurDTO;

public class ServiceRdvDemo implements ServiceRdv {

    private final AtomicLong seq = new AtomicLong(1);
    private final List<RdvDTO> rdvs = new ArrayList<>();
    private final List<UtilisateurDTO> praticiens = List.of(
            new UtilisateurDTO(1L, "ntowe", "Towe", "Nadine", Role.DENTISTE, null, null, true));

    public ServiceRdvDemo() {
        LocalDate aujourdhui = LocalDate.now();
        rdvs.add(rdv(aujourdhui, LocalTime.of(9, 0), "Mbarga Claire", StatutRdv.CONFIRME));
        rdvs.add(rdv(aujourdhui, LocalTime.of(9, 30), "Nguema Paul", StatutRdv.EN_SALLE_ATTENTE));
        rdvs.add(rdv(aujourdhui, LocalTime.of(10, 15), "Fotso Aline", StatutRdv.PLANIFIE));
        rdvs.add(rdv(aujourdhui.plusDays(1), LocalTime.of(11, 0), "Essomba Jean",
                StatutRdv.PLANIFIE));
    }

    private RdvDTO rdv(LocalDate jour, LocalTime heure, String patient, StatutRdv statut) {
        RdvDTO dto = new RdvDTO();
        dto.id = seq.getAndIncrement();
        dto.patientId = dto.id;
        dto.patientNom = patient;
        dto.patientTelephone = "6XX XX XX XX";
        dto.praticienId = 1L;
        dto.praticienNom = "Nadine Towe";
        dto.debut = LocalDateTime.of(jour, heure);
        dto.fin = dto.debut.plusMinutes(30);
        dto.type = TypeRdv.CONSULTATION;
        dto.statut = statut;
        dto.motif = "Contrôle";
        return dto;
    }

    @Override
    public List<RdvDTO> lister(LocalDateTime debut, LocalDateTime fin, Long praticienId) {
        return rdvs.stream()
                .filter(r -> !r.debut.isBefore(debut) && r.debut.isBefore(fin))
                .filter(r -> praticienId == null || praticienId.equals(r.praticienId))
                .sorted((a, b) -> a.debut.compareTo(b.debut))
                .toList();
    }

    @Override
    public long compter(LocalDateTime debut, LocalDateTime fin) {
        return lister(debut, fin, null).stream()
                .filter(r -> r.statut != StatutRdv.ANNULE && r.statut != StatutRdv.ABSENT)
                .count();
    }

    @Override
    public RdvDTO creer(RdvDTO dto) {
        dto.id = seq.getAndIncrement();
        if (dto.praticienNom == null) {
            dto.praticienNom = "Nadine Towe";
        }
        rdvs.add(dto);
        return dto;
    }

    @Override
    public RdvDTO modifier(Long id, RdvDTO dto) {
        for (int i = 0; i < rdvs.size(); i++) {
            if (rdvs.get(i).id.equals(id)) {
                dto.id = id;
                rdvs.set(i, dto);
                return dto;
            }
        }
        throw new IllegalArgumentException("Rendez-vous introuvable.");
    }

    @Override
    public RdvDTO changerStatut(Long id, StatutRdv statut) {
        for (RdvDTO r : rdvs) {
            if (r.id.equals(id)) {
                r.statut = statut;
                return r;
            }
        }
        throw new IllegalArgumentException("Rendez-vous introuvable.");
    }

    @Override
    public List<UtilisateurDTO> praticiens() {
        return praticiens;
    }
}
