package com.lesourire.serveur.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.CanalNotification;
import com.lesourire.commun.Rappels;
import com.lesourire.commun.dto.PatientDTO;
import com.lesourire.commun.dto.RappelDTO;
import com.lesourire.commun.dto.RappelEcritureDTO;
import com.lesourire.commun.dto.RappelStatsDTO;
import com.lesourire.serveur.entite.Patient;
import com.lesourire.serveur.entite.Rappel;
import com.lesourire.serveur.entite.Rdv;
import com.lesourire.serveur.notification.NotificationService;
import com.lesourire.serveur.repository.PatientRepository;
import com.lesourire.serveur.repository.RappelRepository;
import com.lesourire.serveur.repository.RdvRepository;

/**
 * Création manuelle d'une notification et compteurs du journal.
 *
 * <p>Les dépendances sont remplacées par des doublures : le test porte sur les
 * règles métier (consentement, coordonnée disponible, message standard, bornes
 * du journal), pas sur la persistance.</p>
 */
class RappelServiceTest {

    private static final String AUTEUR = "secretaire";
    private static final LocalDateTime DEBUT_RDV = LocalDateTime.of(2026, 9, 18, 9, 0);

    private RappelRepository rappelRepository;
    private PatientRepository patientRepository;
    private RdvRepository rdvRepository;
    private NotificationService notificationService;
    private AuditService auditService;
    private RappelService service;

    @BeforeEach
    void preparer() {
        rappelRepository = mock(RappelRepository.class);
        patientRepository = mock(PatientRepository.class);
        rdvRepository = mock(RdvRepository.class);
        notificationService = mock(NotificationService.class);
        auditService = mock(AuditService.class);

        service = new RappelService(rappelRepository, patientRepository, rdvRepository,
                notificationService, auditService);

        when(rappelRepository.save(any(Rappel.class))).thenAnswer(i -> i.getArgument(0));
        when(notificationService.choisirDestinataire(any(Patient.class), any(Rappels.Canal.class)))
                .thenReturn(new NotificationService.DestinataireRappel(Rappels.Canal.SMS,
                        "+237 699 11 22 33"));
        when(notificationService.messageRappel(anyString(), any(LocalDateTime.class)))
                .thenReturn("MESSAGE RAPPEL");
    }

    // -------------------------------------------------------------- création

    @Test
    @DisplayName("Un patient introuvable ou inactif est refusé")
    void creerRefuseUnPatientIntrouvable() {
        when(patientRepository.findById(7L)).thenReturn(Optional.empty());

        ResponseStatusException erreur = assertThrows(ResponseStatusException.class,
                () -> service.creer(saisie(7L, 3L, Rappels.Type.RAPPEL_RDV, Rappels.Canal.SMS),
                        AUTEUR));

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatusCode());
    }

    @Test
    @DisplayName("Un patient qui a refusé d'être notifié est refusé")
    void creerRefuseUnPatientSansConsentement() {
        when(patientRepository.findById(7L))
                .thenReturn(Optional.of(patient(false, "699 11 22 33")));

        ResponseStatusException erreur = assertThrows(ResponseStatusException.class,
                () -> service.creer(saisie(7L, 3L, Rappels.Type.RAPPEL_RDV, Rappels.Canal.SMS),
                        AUTEUR));

        assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
    }

    @Test
    @DisplayName("Le canal demandé sans coordonnée est refusé avec un motif clair")
    void creerRefuseUnCanalSansCoordonnee() {
        when(patientRepository.findById(7L)).thenReturn(Optional.of(patient(true, null)));
        when(rdvRepository.findById(3L)).thenReturn(Optional.of(rdv()));
        when(notificationService.choisirDestinataire(any(Patient.class),
                eq(Rappels.Canal.WHATSAPP))).thenReturn(null);

        ResponseStatusException erreur = assertThrows(ResponseStatusException.class,
                () -> service.creer(saisie(7L, 3L, Rappels.Type.RAPPEL_RDV, Rappels.Canal.WHATSAPP),
                        AUTEUR));

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatusCode());
        assertTrue(erreur.getReason().contains("coordonnée"), erreur.getReason());
    }

    @Test
    @DisplayName("Une confirmation sans rendez-vous est refusée")
    void creerExigeUnRendezVousPourUneConfirmation() {
        when(patientRepository.findById(7L)).thenReturn(Optional.of(patient(true, "699 11 22 33")));

        ResponseStatusException erreur = assertThrows(ResponseStatusException.class,
                () -> service.creer(saisie(7L, null, Rappels.Type.CONFIRMATION_RDV,
                        Rappels.Canal.SMS), AUTEUR));

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatusCode());
        assertTrue(erreur.getReason().contains("rendez-vous"), erreur.getReason());
    }

    @Test
    @DisplayName("Une notification est programmée immédiatement, message standard inclus")
    void creerProgrammeUnRappelImmediat() {
        when(patientRepository.findById(7L)).thenReturn(Optional.of(patient(true, "699 11 22 33")));
        when(rdvRepository.findById(3L)).thenReturn(Optional.of(rdv()));

        RappelDTO cree = service.creer(
                saisie(7L, 3L, Rappels.Type.RAPPEL_RDV, Rappels.Canal.SMS), AUTEUR);

        assertEquals(Rappels.Statut.EN_ATTENTE, cree.statut);
        assertEquals(Rappels.Canal.SMS, cree.canal);
        assertEquals("+237 699 11 22 33", cree.destinataire);
        assertEquals("MESSAGE RAPPEL", cree.contenu);
        assertEquals(0, cree.tentatives);
        assertNotNull(cree.datePrevue);
        assertEquals(DEBUT_RDV, cree.rdvDebut);

        verify(rappelRepository).save(any(Rappel.class));
        verify(auditService).enregistrer(eq(AUTEUR), eq("CREATION"), eq("rappel"), any(),
                anyString());
    }

    @Test
    @DisplayName("Un message saisi par le secrétariat est conservé tel quel")
    void creerConserveLeMessagePersonnalise() {
        when(patientRepository.findById(7L)).thenReturn(Optional.of(patient(true, "699 11 22 33")));
        when(rdvRepository.findById(3L)).thenReturn(Optional.of(rdv()));
        RappelEcritureDTO saisie = saisie(7L, 3L, Rappels.Type.RAPPEL_RDV, Rappels.Canal.SMS);
        saisie.contenu = "  Bonjour Ada, pensez à votre rendez-vous.  ";

        RappelDTO cree = service.creer(saisie, AUTEUR);

        assertEquals("Bonjour Ada, pensez à votre rendez-vous.", cree.contenu);
        verify(notificationService, never()).messageRappel(anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Sans type précisé, la notification est un rappel")
    void creerParDefautEnRappel() {
        when(patientRepository.findById(7L)).thenReturn(Optional.of(patient(true, "699 11 22 33")));
        when(rdvRepository.findById(3L)).thenReturn(Optional.of(rdv()));

        RappelDTO cree = service.creer(saisie(7L, 3L, null, Rappels.Canal.SMS), AUTEUR);

        assertEquals(Rappels.Type.RAPPEL_RDV, cree.type);
    }
// ------------------------------------------------------- journal et compteurs

    @Test
    @DisplayName("Le journal borne la demande et ne remonte que les envois réussis")
    void journalBorneLaLimite() {
        when(rappelRepository.findByStatutOrderByDateEnvoiDesc(eq(Rappels.Statut.ENVOYE),
                any(Pageable.class))).thenReturn(List.of());

        service.journal(5000);
        service.journal(0);

        ArgumentCaptor<Pageable> pages = ArgumentCaptor.forClass(Pageable.class);
        verify(rappelRepository, org.mockito.Mockito.times(2))
                .findByStatutOrderByDateEnvoiDesc(eq(Rappels.Statut.ENVOYE), pages.capture());
        assertEquals(1000, pages.getAllValues().get(0).getPageSize());
        assertEquals(1, pages.getAllValues().get(1).getPageSize());
    }

    @Test
    @DisplayName("Les compteurs reprennent chaque état et le dernier envoi")
    void statistiquesCompteLesEtats() {
        LocalDateTime dernierEnvoi = LocalDateTime.of(2026, 9, 18, 8, 30);
        Rappel dernier = new Rappel();
        dernier.setDateEnvoi(dernierEnvoi);

        when(rappelRepository.countByStatut(Rappels.Statut.EN_ATTENTE)).thenReturn(4L);
        when(rappelRepository.countByStatut(Rappels.Statut.ENVOYE)).thenReturn(12L);
        when(rappelRepository.countByStatut(Rappels.Statut.ECHEC)).thenReturn(1L);
        when(rappelRepository.countByStatut(Rappels.Statut.ANNULE)).thenReturn(2L);
        when(rappelRepository.countByStatutAndDateEnvoiGreaterThanEqual(
                eq(Rappels.Statut.ENVOYE), any(LocalDateTime.class))).thenReturn(3L);
        when(rappelRepository.findByStatutOrderByDateEnvoiDesc(eq(Rappels.Statut.ENVOYE),
                any(Pageable.class))).thenReturn(List.of(dernier));

        RappelStatsDTO stats = service.statistiques();

        assertEquals(4, stats.enAttente);
        assertEquals(12, stats.envoyees);
        assertEquals(1, stats.echecs);
        assertEquals(2, stats.annulees);
        assertEquals(3, stats.envoyeesAujourdhui);
        assertEquals(dernierEnvoi, stats.derniereEnvoyeeLe);
    }

    @Test
    @DisplayName("Sans envoi réussi, le journal n'affiche pas de dernier envoi")
    void statistiquesSansEnvoi() {
        when(rappelRepository.findByStatutOrderByDateEnvoiDesc(eq(Rappels.Statut.ENVOYE),
                any(Pageable.class))).thenReturn(List.of());

        RappelStatsDTO stats = service.statistiques();

        assertNull(stats.derniereEnvoyeeLe);
        assertEquals(0, stats.envoyees);
    }

    // ------------------------------------------------------------------ outils

    private static RappelEcritureDTO saisie(Long patientId, Long rdvId, Rappels.Type type,
            Rappels.Canal canal) {
        RappelEcritureDTO saisie = new RappelEcritureDTO();
        saisie.patientId = patientId;
        saisie.rdvId = rdvId;
        saisie.type = type;
        saisie.canal = canal;
        return saisie;
    }

    private static Patient patient(boolean consentement, String telephone) {
        PatientDTO dto = new PatientDTO();
        dto.nom = "Mbarga";
        dto.prenom = "Ada";
        dto.telephone = telephone;
        dto.canalNotification = CanalNotification.AUTO;
        dto.consentementRappel = consentement;

        Patient patient = new Patient();
        patient.appliquer(dto);
        return patient;
    }

    private static Rdv rdv() {
        Rdv rdv = new Rdv();
        rdv.setDebut(DEBUT_RDV);
        rdv.setFin(DEBUT_RDV.plusMinutes(30));
        return rdv;
    }
}
