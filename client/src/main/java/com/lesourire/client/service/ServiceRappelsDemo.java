package com.lesourire.client.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.lesourire.commun.Rappels;
import com.lesourire.commun.dto.RappelDTO;

/** Version de démonstration : les envois ne quittent pas le poste. */
public class ServiceRappelsDemo implements ServiceRappels {

    private final AtomicLong seq = new AtomicLong(1);
    private final List<RappelDTO> rappels = new ArrayList<>();

    public ServiceRappelsDemo() {
        LocalDateTime demain = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0);
        rappels.add(rappel("Mbarga Claire", Rappels.Type.CONFIRMATION_RDV, Rappels.Canal.SMS,
                LocalDateTime.now().plusMinutes(1), "+237 699 11 22 33",
                "Cabinet Dentaire Le Sourire : Claire, votre RDV est enregistré "
                        + "le jeudi 18 septembre à 09:00."));
        rappels.add(rappel("Nguema Paul", Rappels.Type.RAPPEL_RDV, Rappels.Canal.WHATSAPP,
                demain, "+237 677 44 55 66",
                "Cabinet Dentaire Le Sourire : Paul, rappel de votre RDV de contrôle."));
        rappels.add(rappel("Fotso Aline", Rappels.Type.RAPPEL_RDV, Rappels.Canal.EMAIL,
                demain.plusHours(2), "aline.fotso@example.cm",
                "Cabinet Dentaire Le Sourire : Aline, rappel de votre RDV de contrôle."));
    }

    private RappelDTO rappel(String patient, Rappels.Type type, Rappels.Canal canal,
            LocalDateTime datePrevue, String destinataire, String contenu) {
        RappelDTO dto = new RappelDTO();
        dto.id = seq.getAndIncrement();
        dto.patientId = dto.id;
        dto.patientNom = patient;
        dto.patientTelephone = destinataire;
        dto.type = type;
        dto.canal = canal;
        dto.statut = Rappels.Statut.EN_ATTENTE;
        dto.datePrevue = datePrevue;
        dto.destinataire = destinataire;
        dto.contenu = contenu;
        dto.lienEnvoi = canal == Rappels.Canal.WHATSAPP
                ? "https://wa.me/237" + destinataire.replaceAll("[^0-9]", "")
                : canal == Rappels.Canal.EMAIL ? "mailto:" + destinataire : null;
        return dto;
    }

    @Override
    public List<RappelDTO> aEnvoyer() {
        return rappels.stream()
                .filter(r -> r.statut == Rappels.Statut.EN_ATTENTE)
                .toList();
    }

    @Override
    public RappelDTO envoyer(Long id) {
        return traiter(id, Rappels.Statut.ENVOYE);
    }

    @Override
    public RappelDTO marquerEnvoye(Long id) {
        return traiter(id, Rappels.Statut.ENVOYE);
    }

    @Override
    public RappelDTO annuler(Long id) {
        return traiter(id, Rappels.Statut.ANNULE);
    }

    private RappelDTO traiter(Long id, Rappels.Statut statut) {
        for (RappelDTO r : rappels) {
            if (r.id.equals(id)) {
                r.statut = statut;
                r.dateEnvoi = LocalDateTime.now();
                r.tentatives++;
                return r;
            }
        }
        throw new IllegalArgumentException("Rappel introuvable.");
    }
}
