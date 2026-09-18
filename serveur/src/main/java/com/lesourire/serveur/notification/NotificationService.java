package com.lesourire.serveur.notification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lesourire.commun.Rappels;
import com.lesourire.serveur.entite.Parametre;
import com.lesourire.serveur.entite.Patient;
import com.lesourire.serveur.repository.ParametreRepository;

/**
 * Paramétrage et envoi des notifications patients.
 *
 * <p>Le canal automatique est le SMS (API Orange). WhatsApp et l'e-mail sont
 * des canaux <em>assistés</em> : le serveur ne fait que préparer le message et
 * le lien d'envoi ({@link LiensManuels}), le secrétariat clique.</p>
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public static final String PARAM_ACTIVE = "notification.active";
    public static final String PARAM_FOURNISSEUR = "notification.fournisseur";
    public static final String PARAM_INDICATIF = "notification.indicatif_pays";
    public static final String PARAM_EXPEDITEUR = "notification.nom_expediteur";
    public static final String PARAM_MAX_TENTATIVES = "notification.max_tentatives";

    /** Au-delà, l'opérateur découpe le message (et le facture) en plusieurs SMS. */
    private static final int LONGUEUR_SMS_SIMPLE = 160;

    private static final DateTimeFormatter FORMAT_RDV =
            DateTimeFormatter.ofPattern("EEEE d MMMM 'à' HH:mm", Locale.FRENCH);

    private final ParametreRepository parametreRepository;
    private final List<FournisseurSms> fournisseurs;
    private final String expediteurForce;

    public NotificationService(ParametreRepository parametreRepository,
            List<FournisseurSms> fournisseurs,
            @Value("${lesourire.notification.orange.expediteur:}") String expediteurForce) {
        this.parametreRepository = parametreRepository;
        this.fournisseurs = fournisseurs;
        this.expediteurForce = expediteurForce == null ? "" : expediteurForce.trim();
    }

    // ---------------------------------------------------------------- réglages

    /** L'envoi automatique par le planificateur est-il activé ? */
    public boolean active() {
        return booleen(parametre(PARAM_ACTIVE, "false"));
    }

    /** Indicatif téléphonique utilisé pour compléter les numéros locaux. */
    public String indicatif() {
        return parametre(PARAM_INDICATIF, NormaliseurTelephone.INDICATIF_DEFAUT);
    }

    /**
     * Adresse d'expédition contractualisée : {@code tel:+237…} ou nom
     * d'expéditeur whitelisté. L'environnement prime sur le paramètre.
     */
    public String expediteur() {
        if (!expediteurForce.isEmpty()) {
            return expediteurForce;
        }
        return parametre(PARAM_EXPEDITEUR, "CABINET LE SOURIRE");
    }

    public int maxTentatives() {
        try {
            return Math.max(1, Integer.parseInt(parametre(PARAM_MAX_TENTATIVES, "3")));
        } catch (NumberFormatException e) {
            return 3;
        }
    }

    /** Seul le SMS peut partir sans intervention humaine. */
    public boolean canalAutomatique(Rappels.Canal canal) {
        return canal == Rappels.Canal.SMS;
    }

    /** Numéro au format international, ou {@code null} si inexploitable. */
    public String numeroNormalise(String brut) {
        return NormaliseurTelephone.normaliser(brut, indicatif());
    }

    // ------------------------------------------------------------------ envois

    /** Envoie un SMS en normalisant le destinataire. */
    public ResultatEnvoi envoyerSms(String destinataireBrut, String contenu) {
        String numero = numeroNormalise(destinataireBrut);
        if (numero == null) {
            return ResultatEnvoi.echec("Numéro de téléphone inexploitable : " + destinataireBrut);
        }
        if (contenu != null && contenu.length() > LONGUEUR_SMS_SIMPLE) {
            log.info("SMS de {} caractères : l'opérateur le facturera en plusieurs parties.",
                    contenu.length());
        }
        return fournisseur().envoyer(expediteur(), numero, contenu);
    }

    /** Lien d'envoi assisté pour les canaux non automatiques (WhatsApp, e-mail). */
    public String lienManuel(Rappels.Canal canal, String destinataire, String contenu) {
        return LiensManuels.lienEnvoi(canal, destinataire, contenu, indicatif());
    }

    private FournisseurSms fournisseur() {
        String souhaite = parametre(PARAM_FOURNISSEUR, "journal");
        return fournisseurs.stream()
                .filter(f -> f.nom().equalsIgnoreCase(souhaite))
                .findFirst()
                .orElseGet(() -> fournisseurs.stream()
                        .filter(f -> f.nom().equals("journal"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "Aucun fournisseur de SMS « journal » disponible.")));
    }

    // ----------------------------------------------------------- destinataires

    /** Canal retenu et adresse concrète pour joindre un patient. */
    public record DestinataireRappel(Rappels.Canal canal, String adresse) {
    }

    /**
     * Canal et adresse de notification selon la préférence de la fiche patient.
     *
     * <p>Le SMS vient en premier : c'est le canal réellement lu au Cameroun et
     * le seul que le serveur sait envoyer tout seul. WhatsApp prend le relais
     * (envoi assisté par le secrétariat) et l'e-mail reste le dernier recours.
     * La préférence saisie sur la fiche est respectée ; si le moyen
     * correspondant est absent, on retombe sur l'ordre par défaut.</p>
     *
     * <p>Aucun envoi n'est possible si le patient a refusé d'être notifié.</p>
     *
     * @return {@code null} si le patient refuse les rappels ou n'a aucune
     *         coordonnée exploitable
     */
    public DestinataireRappel choisirDestinataire(Patient patient) {
        if (patient == null || !patient.isConsentementRappel()) {
            return null;
        }
        DestinataireRappel parSms =
                new DestinataireRappel(Rappels.Canal.SMS, nonVide(patient.getTelephone()));
        DestinataireRappel parWhatsapp =
                new DestinataireRappel(Rappels.Canal.WHATSAPP, nonVide(patient.numeroWhatsapp()));
        DestinataireRappel parEmail =
                new DestinataireRappel(Rappels.Canal.EMAIL, nonVide(patient.getEmail()));

        return switch (patient.getCanalNotification()) {
            case SMS, AUTO -> premier(parSms, parWhatsapp, parEmail);
            case WHATSAPP -> premier(parWhatsapp, parSms, parEmail);
            case EMAIL -> premier(parEmail, parSms, parWhatsapp);
        };
    }

    /**
     * Canal et adresse pour un canal explicitement demandé (notification
     * manuelle depuis un rendez-vous). Le consentement reste exigé, mais aucune
     * bascule silencieuse n'a lieu : si la coordonnée du canal voulu manque,
     * {@code null} est renvoyé pour que l'appelant explique le refus.
     */
    public DestinataireRappel choisirDestinataire(Patient patient, Rappels.Canal souhaite) {
        if (souhaite == null) {
            return choisirDestinataire(patient);
        }
        if (patient == null || !patient.isConsentementRappel()) {
            return null;
        }
        String adresse = switch (souhaite) {
            case SMS -> nonVide(patient.getTelephone());
            case WHATSAPP -> nonVide(patient.numeroWhatsapp());
            case EMAIL -> nonVide(patient.getEmail());
        };
        return adresse == null ? null : new DestinataireRappel(souhaite, adresse);
    }

    private static DestinataireRappel premier(DestinataireRappel... candidats) {
        for (DestinataireRappel candidat : candidats) {
            if (candidat.adresse() != null) {
                return candidat;
            }
        }
        return null;
    }

    private static String nonVide(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    // ---------------------------------------------------------------- messages

    /** Message de confirmation envoyé dès la prise de rendez-vous. */
    public String messageConfirmation(String patientPrenom, LocalDateTime debut, String praticien) {
        return entete(patientPrenom) + "votre RDV est enregistré le " + debut.format(FORMAT_RDV)
                + (praticien == null || praticien.isBlank() ? "" : " avec " + praticien)
                + ". Merci d'arriver 10 min avant.";
    }

    /** Message de rappel de la veille. */
    public String messageRappel(String patientPrenom, LocalDateTime debut) {
        return entete(patientPrenom) + "rappel de votre RDV le " + debut.format(FORMAT_RDV)
                + ". Merci de prévenir en cas d'empêchement.";
    }

    /** Invitation à une consultation de contrôle après intervention. */
    public String messageRevisite(String patientPrenom) {
        return entete(patientPrenom) + "il est temps de venir pour votre visite de contrôle. "
                + "Appelez-nous pour convenir d'un rendez-vous.";
    }

    private String entete(String patientPrenom) {
        String nom = parametre("cabinet.nom", "Cabinet Dentaire Le Sourire");
        if (patientPrenom != null && !patientPrenom.isBlank()) {
            return nom + " : " + patientPrenom + ", ";
        }
        return nom + " : ";
    }

    // ----------------------------------------------------------------- lecture

    private String parametre(String cle, String defaut) {
        return parametreRepository.findById(cle)
                .map(Parametre::getValeur)
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .orElse(defaut);
    }

    private static boolean booleen(String valeur) {
        if (valeur == null) {
            return false;
        }
        return switch (valeur.trim().toLowerCase(Locale.ROOT)) {
            case "true", "oui", "1", "vrai", "yes", "on" -> true;
            default -> false;
        };
    }
}
