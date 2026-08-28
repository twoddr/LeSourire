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
        preparerEnvironnementPdf();
        String html = rendreHtml(facture);
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

    private static String rendreHtml(FactureDTO f) {
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

        ctx.setVariable("banniereUri", "banniere.png");
        ctx.setVariable("logoUri", "logo.png");

        List<LigneVue> lignes = new ArrayList<>();
        if (f.lignes != null) {
            for (FactureLigneDTO l : f.lignes) {
                lignes.add(new LigneVue(
                        texte(l.designation),
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
        ctx.setVariable("assureurNom",
                f.assureurNom == null || f.assureurNom.isBlank() ? null : f.assureurNom);
        ctx.setVariable("quoteSociete", Montants.formaterAvecDevise(f.quoteSociete));
        ctx.setVariable("societeNom",
                f.societeNom == null || f.societeNom.isBlank() ? null : f.societeNom);

        List<PaiementVue> paiements = new ArrayList<>();
        if (f.paiements != null) {
            for (PaiementDTO p : f.paiements) {
                paiements.add(new PaiementVue(
                        p.datePaiement == null ? "—" : p.datePaiement.format(DATE_HEURE),
                        p.payeur == null ? "—" : p.payeur.getLibelle(),
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

    /** Ligne déjà formatée pour le template HTML. */
    public static final class LigneVue {
        private final String designation;
        private final String dents;
        private final String quantite;
        private final String prixUnitaire;
        private final String montant;

        public LigneVue(String designation, String dents, String quantite,
                String prixUnitaire, String montant) {
            this.designation = designation;
            this.dents = dents;
            this.quantite = quantite;
            this.prixUnitaire = prixUnitaire;
            this.montant = montant;
        }

        public String getDesignation() {
            return designation;
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
