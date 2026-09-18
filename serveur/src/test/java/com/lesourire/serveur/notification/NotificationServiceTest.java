package com.lesourire.serveur.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.lesourire.commun.CanalNotification;
import com.lesourire.commun.Rappels;
import com.lesourire.commun.dto.PatientDTO;
import com.lesourire.serveur.entite.Patient;
import com.lesourire.serveur.repository.ParametreRepository;

/**
 * Règles de choix du destinataire et modèles de messages.
 *
 * <p>Les paramètres sont absents (base vide) : le service retombe sur ses
 * valeurs par défaut, ce qui est exactement le comportement d'un cabinet qui
 * vient d'être installé.</p>
 */
class NotificationServiceTest {

    private NotificationService service;

    @BeforeEach
    void preparer() {
        ParametreRepository parametres = mock(ParametreRepository.class);
        when(parametres.findById(anyString())).thenReturn(Optional.empty());
        service = new NotificationService(parametres, List.of(), "");
    }

    // ------------------------------------------------------------ destinataire

    @Test
    @DisplayName("Le SMS part en premier quand un numéro est renseigné")
    void smsParDefautQuandUnNumeroExiste() {
        Patient patient = patient(CanalNotification.AUTO, "699 11 22 33", null, "ada@example.cm",
                true);

        NotificationService.DestinataireRappel destinataire = service.choisirDestinataire(patient);

        assertEquals(Rappels.Canal.SMS, destinataire.canal());
        assertEquals("699 11 22 33", destinataire.adresse());
    }

    @Test
    @DisplayName("Sans numéro, WhatsApp prend le relais")
    void whatsappQuandAucunTelephone() {
        Patient patient = patient(CanalNotification.AUTO, null, "+237 677 44 55 66",
                "ada@example.cm", true);

        NotificationService.DestinataireRappel destinataire = service.choisirDestinataire(patient);

        assertEquals(Rappels.Canal.WHATSAPP, destinataire.canal());
        assertEquals("+237 677 44 55 66", destinataire.adresse());
    }

    @Test
    @DisplayName("L'e-mail reste le dernier recours")
    void emailQuandAucunNumeroNiWhatsapp() {
        Patient patient = patient(CanalNotification.AUTO, null, null, "ada@example.cm", true);

        NotificationService.DestinataireRappel destinataire = service.choisirDestinataire(patient);

        assertEquals(Rappels.Canal.EMAIL, destinataire.canal());
        assertEquals("ada@example.cm", destinataire.adresse());
    }

    @Test
    @DisplayName("La préférence de la fiche patient est respectée")
    void preferenceEmailRespectee() {
        Patient patient = patient(CanalNotification.EMAIL, "699 11 22 33", null,
                "ada@example.cm", true);

        NotificationService.DestinataireRappel destinataire = service.choisirDestinataire(patient);

        assertEquals(Rappels.Canal.EMAIL, destinataire.canal());
    }

    @Test
    @DisplayName("Un patient sans coordonnée n'est pas notifiable")
    void aucuneCoordonnee() {
        Patient patient = patient(CanalNotification.AUTO, null, null, null, true);

        assertNull(service.choisirDestinataire(patient));
    }

    @Test
    @DisplayName("Un patient qui refuse les rappels n'est pas notifiable")
    void refusDuPatient() {
        Patient patient = patient(CanalNotification.SMS, "699 11 22 33", null, null, false);

        assertNull(service.choisirDestinataire(patient));
        assertNull(service.choisirDestinataire(patient, Rappels.Canal.SMS));
    }

    @Test
    @DisplayName("Un canal explicitement demandé sans coordonnée est refusé")
    void canalDemandeSansCoordonnee() {
        Patient patient = patient(CanalNotification.AUTO, "699 11 22 33", null, null, true);

        // Pas d'e-mail enregistré : la demande d'e-mail ne peut pas aboutir.
        assertNull(service.choisirDestinataire(patient, Rappels.Canal.EMAIL));
        // Le numéro principal fait aussi office de ligne WhatsApp.
        NotificationService.DestinataireRappel whatsapp =
                service.choisirDestinataire(patient, Rappels.Canal.WHATSAPP);
        assertEquals(Rappels.Canal.WHATSAPP, whatsapp.canal());
        assertEquals("699 11 22 33", whatsapp.adresse());
    }

    @Test
    @DisplayName("Un canal demandé nul retombe sur la préférence du patient")
    void canalDemandeNull() {
        Patient patient = patient(CanalNotification.WHATSAPP, "699 11 22 33",
                "+237 677 44 55 66", null, true);

        NotificationService.DestinataireRappel destinataire =
                service.choisirDestinataire(patient, null);

        assertEquals(Rappels.Canal.WHATSAPP, destinataire.canal());
        assertEquals("+237 677 44 55 66", destinataire.adresse());
    }

    // ---------------------------------------------------------------- messages

    @Test
    @DisplayName("Le rappel reprend le prénom et l'heure du rendez-vous")
    void messageRappel() {
        String message = service.messageRappel("Ada", LocalDateTime.of(2026, 9, 18, 9, 0));

        assertTrue(message.contains("Cabinet Dentaire Le Sourire"), message);
        assertTrue(message.contains("Ada") && message.contains("rappel"), message);
        assertTrue(message.contains("09:00"), message);
    }

    @Test
    @DisplayName("La confirmation nomme le praticien quand il est connu")
    void messageConfirmation() {
        String message = service.messageConfirmation("Ada",
                LocalDateTime.of(2026, 9, 18, 9, 0), "Dr Towe");

        assertTrue(message.contains("Ada") && message.contains("Dr Towe"), message);
        assertTrue(message.contains("09:00"), message);
    }

    @Test
    @DisplayName("Seul le SMS part sans intervention humaine")
    void canalAutomatique() {
        assertTrue(service.canalAutomatique(Rappels.Canal.SMS));
        assertFalse(service.canalAutomatique(Rappels.Canal.WHATSAPP));
        assertFalse(service.canalAutomatique(Rappels.Canal.EMAIL));
    }

    private static Patient patient(CanalNotification canal, String telephone, String whatsapp,
            String email, boolean consentement) {
        PatientDTO dto = new PatientDTO();
        dto.nom = "Mbarga";
        dto.prenom = "Ada";
        dto.telephone = telephone;
        dto.telephoneWhatsapp = whatsapp;
        dto.email = email;
        dto.canalNotification = canal;
        dto.consentementRappel = consentement;

        Patient patient = new Patient();
        patient.appliquer(dto);
        return patient;
    }
}