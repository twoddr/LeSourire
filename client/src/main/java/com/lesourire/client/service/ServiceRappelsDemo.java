package com.lesourire.client.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import com.lesourire.commun.Rappels;
import com.lesourire.commun.dto.RappelDTO;
import com.lesourire.commun.dto.RappelStatsDTO;
import com.lesourire.commun.dto.RdvDTO;

/** Version de démonstration : les envois ne quittent pas le poste. */
public class ServiceRappelsDemo implements ServiceRappels {

    private static final DateTimeFormatter FORMAT_RDV =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

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

        // Déjà parti : alimente l'historique du mode démonstration.
        RappelDTO envoye = rappel("Tabi Serge", Rappels.Type.CONFIRMATION_RDV, Rappels.Canal.SMS,
                LocalDateTime.now().minusDays(1), "+237 655 77 88 99",
                "Cabinet Dentaire Le Sourire : Serge, votre RDV est enregistré.");
        envoye.statut = Rappels.Statut.ENVOYE;
        envoye.dateEnvoi = LocalDateTime.now().minusDays(1).withHour(10).withMinute(15);
        envoye.tentatives = 1;
        rappels.add(envoye);
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

    @Override
    public RappelDTO creer(RdvDTO rdv, Rappels.Type type, Rappels.Canal canal, String contenu) {
        String destinataire = destinataireDemo(rdv, canal);
        RappelDTO dto = new RappelDTO();
        dto.id = seq.getAndIncrement();
        dto.patientId = rdv.patientId;
        dto.patientNom = rdv.patientNom == null ? "Patient" : rdv.patientNom;
        dto.patientTelephone = rdv.patientTelephone;
        dto.rdvId = rdv.id;
        dto.rdvDebut = rdv.debut;
        dto.type = type == null ? Rappels.Type.RAPPEL_RDV : type;
        dto.canal = canal == null ? Rappels.Canal.SMS : canal;
        dto.statut = Rappels.Statut.EN_ATTENTE;
        dto.datePrevue = LocalDateTime.now();
        dto.destinataire = destinataire;
        dto.contenu = contenu == null || contenu.isBlank()
                ? messageDemo(dto.type, rdv) : contenu.trim();
        dto.lienEnvoi = lienDemo(dto.canal, destinataire);
        rappels.add(dto);
        return dto;
    }

    @Override
    public List<RappelDTO> journal(int limite) {
        return rappels.stream()
                .filter(r -> r.statut == Rappels.Statut.ENVOYE && r.dateEnvoi != null)
                .sorted(Comparator.comparing((RappelDTO r) -> r.dateEnvoi).reversed())
                .limit(Math.max(1, limite))
                .toList();
    }

    @Override
    public RappelStatsDTO statistiques() {
        RappelStatsDTO stats = new RappelStatsDTO();
        stats.enAttente = compter(Rappels.Statut.EN_ATTENTE);
        stats.envoyees = compter(Rappels.Statut.ENVOYE);
        stats.echecs = compter(Rappels.Statut.ECHEC);
        stats.annulees = compter(Rappels.Statut.ANNULE);
        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        stats.envoyeesAujourdhui = rappels.stream()
                .filter(r -> r.statut == Rappels.Statut.ENVOYE && r.dateEnvoi != null
                        && !r.dateEnvoi.isBefore(debutJour))
                .count();
        stats.derniereEnvoyeeLe = rappels.stream()
                .filter(r -> r.statut == Rappels.Statut.ENVOYE && r.dateEnvoi != null)
                .map(r -> r.dateEnvoi)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return stats;
    }

    private long compter(Rappels.Statut statut) {
        return rappels.stream().filter(r -> r.statut == statut).count();
    }

    private static String messageDemo(Rappels.Type type, RdvDTO rdv) {
        String prenom = rdv.patientNom == null ? "" : rdv.patientNom.split(" ")[0];
        String entete = "Cabinet Dentaire Le Sourire : " + prenom + ", ";
        if (type == Rappels.Type.REVISITE) {
            return entete + "il est temps de venir pour votre visite de contrôle. "
                    + "Appelez-nous pour convenir d'un rendez-vous.";
        }
        String quand = rdv.debut == null ? "" : " le " + rdv.debut.format(FORMAT_RDV);
        return type == Rappels.Type.CONFIRMATION_RDV
                ? entete + "votre RDV est enregistré" + quand + ". Merci d'arriver 10 min avant."
                : entete + "rappel de votre RDV" + quand + ". Merci de prévenir en cas d'empêchement.";
    }

    private static String destinataireDemo(RdvDTO rdv, Rappels.Canal canal) {
        if (canal != Rappels.Canal.EMAIL) {
            return rdv.patientTelephone == null ? "+237 600 00 00 00" : rdv.patientTelephone;
        }
        String base = (rdv.patientNom == null ? "patient" : rdv.patientNom)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", ".");
        return base + "@example.cm";
    }

    private static String lienDemo(Rappels.Canal canal, String destinataire) {
        if (canal == Rappels.Canal.WHATSAPP) {
            return "https://wa.me/237" + destinataire.replaceAll("[^0-9]", "");
        }
        return canal == Rappels.Canal.EMAIL ? "mailto:" + destinataire : null;
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
