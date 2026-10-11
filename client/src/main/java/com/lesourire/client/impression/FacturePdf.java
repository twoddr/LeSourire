package com.lesourire.client.impression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.pdfbox.pdmodel.font.FontInfo;
import org.apache.pdfbox.pdmodel.font.FontMappers;
import org.apache.pdfbox.pdmodel.font.FontProvider;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import com.lesourire.client.coeur.Montants;
import com.lesourire.commun.Facturation.DestinataireFacture;
import com.lesourire.commun.Facturation.Payeur;
import com.lesourire.commun.Facturation.StatutFacture;
import com.lesourire.commun.dto.FactureDTO;
import com.lesourire.commun.dto.FactureLigneDTO;
import com.lesourire.commun.dto.PaiementDTO;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

/**
 * Génération PDF d'une facture via HTML/CSS ({@code templates/facture.html})
 * rendu par OpenHTMLtoPDF. Modifier le template (et le CSS dedans) pour peaufiner
 * la mise en page ; {@link ImpressionFacture} ouvre seulement le fichier.
 */
public final class FacturePdf {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_HEURE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final TemplateEngine MOTEUR = creerMoteur();
    private static final String MODELE = chargerRessourceTexte("/templates/facture.html");
    private static volatile boolean pdfBoxPret;

    // ------------------------------------------------------------------------
    // Identité du cabinet imprimée sur chaque facture : en-tête de la 1re page
    // (à la place de l'ancienne bannière) et pied de CHAQUE page.
    // Source unique : modifier ici pour changer les coordonnées affichées.
    // (À terme, ces valeurs pourraient être lues dans les paramètres
    // « cabinet.* » de la base : elles n'y sont pas encore toutes.)
    // ------------------------------------------------------------------------
    private static final String CABINET_NOM = "CABINET DENTAIRE LE SOURIRE";
    private static final String CABINET_PRATICIEN = "Docteur Nadine TOWE";
    private static final String CABINET_SPECIALITE = "Chirurgien Dentiste";
    private static final String CABINET_DIPLOME =
            "Diplômée de l’UFR d’Odonto – Stomatologie d’Abidjan, Côte d’Ivoire";
    private static final String CABINET_TELEPHONES =
            "(237) 233 431 411 / 686 339 308 / 671 512 971";

    private FacturePdf() {
    }

    /**
     * À appeler tôt (démarrage client) : évite que PDFBox passe au peigne fin
     * les polices système (TeX Live, etc.) au premier PDF — lent et bruyant.
     * On n'utilise que DejaVu embarquée.
     */
    public static synchronized void preparerEnvironnementPdf() {
        if (pdfBoxPret) {
            return;
        }
        try {
            Path cache = Path.of(System.getProperty("java.io.tmpdir"), "lesourire-pdfbox");
            Files.createDirectories(cache);
            System.setProperty("pdfbox.fontcache", cache.toString());
        } catch (IOException ignored) {
            // cache optionnel
        }

        // Les polices TeX (.pfb) cassées polluent la console en WARNING + stacktrace
        Logger.getLogger("org.apache.pdfbox.pdmodel.font").setLevel(Level.SEVERE);
        Logger.getLogger("org.apache.pdfbox").setLevel(Level.SEVERE);
        Logger.getLogger("org.apache.fontbox").setLevel(Level.SEVERE);

        // Fournisseur vide : pas de scan de /usr/share/fonts (DejaVu est useFont())
        try {
            Object mapper = FontMappers.instance();
            Method setProvider = mapper.getClass().getMethod("setProvider", FontProvider.class);
            setProvider.invoke(mapper, new FontProvider() {
                @Override
                public String toDebugString() {
                    return "LeSourire: polices embarquées uniquement";
                }

                @Override
                public List<? extends FontInfo> getFontInfo() {
                    return Collections.emptyList();
                }
            });
        } catch (ReflectiveOperationException e) {
            // PDFBox a changé d'API : on garde au moins le silence des logs
        }
        pdfBoxPret = true;
    }

    public static void ecrire(FactureDTO facture, Path destination) throws IOException {
        ecrire(facture, destination, false);
    }

    /**
     * Écrit le PDF de la facture. {@code copiePatient} marque le document
     * « Copie patient » : utile lorsque l'original est adressé au tiers payant.
     */
    public static void ecrire(FactureDTO facture, Path destination, boolean copiePatient)
            throws IOException {
        preparerEnvironnementPdf();
        String html = rendreHtml(facture, copiePatient);
        URL images = FacturePdf.class.getResource("/images/");
        if (images == null) {
            throw new IOException("Ressources images introuvables (classpath /images/).");
        }

        try (OutputStream out = Files.newOutputStream(destination)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, images.toExternalForm());
            builder.useFont(
                    () -> FacturePdf.class.getResourceAsStream("/fonts/DejaVuSans.ttf"),
                    "DejaVu Sans");
            builder.useFont(
                    () -> FacturePdf.class.getResourceAsStream("/fonts/DejaVuSans-Bold.ttf"),
                    "DejaVu Sans",
                    700,
                    FontStyle.NORMAL,
                    true);
            builder.toStream(out);
            builder.run();
        }
    }

    private static String rendreHtml(FactureDTO f, boolean copiePatient) {
        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("numero", texte(f.numero));
        ctx.setVariable("statut", libelleStatut(f.statut));
        ctx.setVariable("dateFacture",
                f.dateFacture == null ? "—" : f.dateFacture.format(DATE));
        ctx.setVariable("dateEcheance",
                f.dateEcheance == null ? null : f.dateEcheance.format(DATE));
        ctx.setVariable("patientNom", texte(f.patientNom));
        ctx.setVariable("patientDossier",
                f.patientNumeroDossier == null || f.patientNumeroDossier.isBlank()
                        ? null
                        : f.patientNumeroDossier);

        ctx.setVariable("logoUri", "logo.png");
        ctx.setVariable("cabinetNom", CABINET_NOM);
        ctx.setVariable("cabinetPraticien", CABINET_PRATICIEN);
        ctx.setVariable("cabinetSpecialite", CABINET_SPECIALITE);
        ctx.setVariable("cabinetDiplome", CABINET_DIPLOME);
        ctx.setVariable("cabinetTelephones", CABINET_TELEPHONES);

        List<LigneVue> lignes = new ArrayList<>();
        if (f.lignes != null) {
            for (FactureLigneDTO l : f.lignes) {
                lignes.add(new LigneVue(
                        texte(l.designation),
                        l.prestationCode == null || l.prestationCode.isBlank()
                                ? "—" : l.prestationCode,
                        l.dents == null || l.dents.isBlank() ? "—" : l.dents,
                        String.valueOf(l.quantite),
                        Montants.formater(l.prixUnitaire),
                        Montants.formater(l.montant)));
            }
        }
        ctx.setVariable("lignes", lignes);

        ctx.setVariable("montantBrut", Montants.formaterAvecDevise(f.montantBrut));
        ctx.setVariable("remise", Montants.formaterAvecDevise(f.remise));
        ctx.setVariable("montantNet", Montants.formaterAvecDevise(f.montantNet));
        ctx.setVariable("quotePatient", Montants.formaterAvecDevise(f.quotePatient));
        ctx.setVariable("restePatient", reste(f.soldePatient));
        ctx.setVariable("quoteAssureur", Montants.formaterAvecDevise(f.quoteAssureur));
        String nomAssureur = f.assureurNom == null || f.assureurNom.isBlank() ? null : f.assureurNom;
        ctx.setVariable("assureurNom", nomAssureur);
        ctx.setVariable("assureurNiu", vide(f.assureurNiu));
        ctx.setVariable("assureurAdresse", vide(f.assureurAdresse));
        ctx.setVariable("assureurTelephone", vide(f.assureurTelephone));
        ctx.setVariable("pourcentageAssureur", pourcentage(f.pourcentageAssureur));
        ctx.setVariable("quoteSociete", Montants.formaterAvecDevise(f.quoteSociete));
        String nomSociete = f.societeNom == null || f.societeNom.isBlank() ? null : f.societeNom;
        ctx.setVariable("societeNom", nomSociete);
        ctx.setVariable("societeNiu", vide(f.societeNiu));
        ctx.setVariable("societeAdresse", vide(f.societeAdresse));
        ctx.setVariable("societeTelephone", vide(f.societeTelephone));
        ctx.setVariable("pourcentageSociete", pourcentage(f.pourcentageSociete));

        // Destinataire du document : le tiers payant à qui la facture est adressée
        // (Assureur ou Société), ou le patient à défaut de couverture.
        boolean destinataireTiers = f.destinataire == DestinataireFacture.ASSUREUR
                || f.destinataire == DestinataireFacture.SOCIETE;
        ctx.setVariable("destinataireTiers", destinataireTiers);
        ctx.setVariable("copiePatient", copiePatient);
        String destinataireNom = null;
        if (f.destinataire == DestinataireFacture.ASSUREUR) {
            destinataireNom = nomAssureur;
            ctx.setVariable("destinataireNiu", vide(f.assureurNiu));
            ctx.setVariable("destinataireAdresse", vide(f.assureurAdresse));
            ctx.setVariable("destinataireTelephone", vide(f.assureurTelephone));
        } else if (f.destinataire == DestinataireFacture.SOCIETE) {
            destinataireNom = nomSociete;
            ctx.setVariable("destinataireNiu", vide(f.societeNiu));
            ctx.setVariable("destinataireAdresse", vide(f.societeAdresse));
            ctx.setVariable("destinataireTelephone", vide(f.societeTelephone));
        }
        ctx.setVariable("destinataireNom", destinataireNom);
        // Montant total pris en charge par les tiers payants (assureur + société).
        ctx.setVariable("montantPrisEnCharge", Montants.formaterAvecDevise(
                f.quoteAssureur.add(f.quoteSociete)));

        // Prise en charge : nom de chaque tiers payant, sa part et son montant.
        List<PriseEnChargeVue> prisesEnCharge = new ArrayList<>();
        if (nomAssureur != null) {
            prisesEnCharge.add(new PriseEnChargeVue("Assureur", nomAssureur,
                    pourcentage(f.pourcentageAssureur),
                    Montants.formaterAvecDevise(f.quoteAssureur)));
        }
        if (nomSociete != null) {
            prisesEnCharge.add(new PriseEnChargeVue("Société", nomSociete,
                    pourcentage(f.pourcentageSociete),
                    Montants.formaterAvecDevise(f.quoteSociete)));
        }
        ctx.setVariable("prisesEnCharge", prisesEnCharge);

        List<PaiementVue> paiements = new ArrayList<>();
        if (f.paiements != null) {
            for (PaiementDTO p : f.paiements) {
                String payeur = p.payeur == null ? "—" : p.payeur.getLibelle();
                if (p.payeur == Payeur.ASSUREUR && nomAssureur != null) {
                    payeur += " (" + nomAssureur + ")";
                } else if (p.payeur == Payeur.SOCIETE && nomSociete != null) {
                    payeur += " (" + nomSociete + ")";
                }
                paiements.add(new PaiementVue(
                        p.datePaiement == null ? "—" : p.datePaiement.format(DATE_HEURE),
                        payeur,
                        p.mode == null ? "—" : p.mode.getLibelle(),
                        Montants.formaterAvecDevise(p.montant),
                        p.reference == null || p.reference.isBlank() ? "—" : p.reference));
            }
        }
        ctx.setVariable("paiements", paiements);
        ctx.setVariable("notes",
                f.notes == null || f.notes.isBlank() ? null : f.notes.trim());

        // StringTemplateResolver : le « nom » du modèle est le contenu HTML lui-même
        // (accès fiable aux ressources du module JPMS).
        return MOTEUR.process(MODELE, ctx);
    }

    private static TemplateEngine creerMoteur() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(true);

        TemplateEngine moteur = new TemplateEngine();
        moteur.setTemplateResolver(resolver);
        return moteur;
    }

    private static String chargerRessourceTexte(String chemin) {
        try (InputStream in = FacturePdf.class.getResourceAsStream(chemin)) {
            if (in == null) {
                throw new IllegalStateException("Ressource introuvable : " + chemin);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Lecture impossible : " + chemin, e);
        }
    }

    /** Formate une part de prise en charge, ex. « 70 % ». */
    private static String pourcentage(BigDecimal valeur) {
        if (valeur == null) {
            return "—";
        }
        return valeur.stripTrailingZeros().toPlainString().replace('.', ',') + " %";
    }

    private static String reste(BigDecimal solde) {
        if (solde == null || solde.signum() == 0) {
            return null;
        }
        return "reste " + Montants.formater(solde);
    }

    private static String libelleStatut(StatutFacture statut) {
        if (statut == null) {
            return "";
        }
        return switch (statut) {
            case BROUILLON -> "Brouillon";
            case EMISE -> "Émise";
            case PARTIELLEMENT_PAYEE -> "Partiellement payée";
            case PAYEE -> "Payée";
            case ANNULEE -> "Annulée";
        };
    }

    private static String texte(String valeur) {
        return valeur == null ? "" : valeur;
    }

    /** Valeur optionnelle : {@code null} si absente ou vide (pour masquer la ligne). */
    private static String vide(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    /** Ligne déjà formatée pour le template HTML. */
    public static final class LigneVue {
        private final String designation;
        private final String codification;
        private final String dents;
        private final String quantite;
        private final String prixUnitaire;
        private final String montant;

        public LigneVue(String designation, String codification, String dents, String quantite,
                String prixUnitaire, String montant) {
            this.designation = designation;
            this.codification = codification;
            this.dents = dents;
            this.quantite = quantite;
            this.prixUnitaire = prixUnitaire;
            this.montant = montant;
        }

        public String getDesignation() {
            return designation;
        }

        public String getCodification() {
            return codification;
        }

        public String getDents() {
            return dents;
        }

        public String getQuantite() {
            return quantite;
        }

        public String getPrixUnitaire() {
            return prixUnitaire;
        }

        public String getMontant() {
            return montant;
        }
    }

    /** Tiers payant (assureur ou société) déjà formaté pour le template HTML. */
    public static final class PriseEnChargeVue {
        private final String type;
        private final String nom;
        private final String pourcentage;
        private final String quote;

        public PriseEnChargeVue(String type, String nom, String pourcentage, String quote) {
            this.type = type;
            this.nom = nom;
            this.pourcentage = pourcentage;
            this.quote = quote;
        }

        public String getType() {
            return type;
        }

        public String getNom() {
            return nom;
        }

        public String getPourcentage() {
            return pourcentage;
        }

        public String getQuote() {
            return quote;
        }
    }

    /** Paiement déjà formaté pour le template HTML. */
    public static final class PaiementVue {
        private final String date;
        private final String payeur;
        private final String mode;
        private final String montant;
        private final String reference;

        public PaiementVue(String date, String payeur, String mode, String montant,
                String reference) {
            this.date = date;
            this.payeur = payeur;
            this.mode = mode;
            this.montant = montant;
            this.reference = reference;
        }

        public String getDate() {
            return date;
        }

        public String getPayeur() {
            return payeur;
        }

        public String getMode() {
            return mode;
        }

        public String getMontant() {
            return montant;
        }

        public String getReference() {
            return reference;
        }
    }
}
