package com.lesourire.serveur.notification;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Envoi de SMS via l'API « SMS Cameroon 2.0 » d'Orange (developer.orange.com).
 *
 * <p>Cette API couvre tous les opérateurs camerounais (MTN et Orange) : le
 * cabinet n'a donc qu'un seul compte à alimenter. Les crédits s'achètent par
 * paquets, payés par Orange Money ou par le crédit d'une carte SIM Orange.</p>
 *
 * <p>Protocole (GSMA OneAPI) :</p>
 * <ol>
 *   <li>jeton OAuth 2.0 <em>client_credentials</em> sur {@code url-token},
 *       valable une heure (mis en cache et renouvelé automatiquement) ;</li>
 *   <li>envoi par {@code POST /smsmessaging/v1/outbound/{senderAddress}/requests},
 *       l'{@code senderAddress} étant celui contractualisé
 *       ({@code tel:+237…} ou un nom d'expéditeur whitelisté).</li>
 * </ol>
 *
 * <p>Aucune bibliothèque supplémentaire n'est nécessaire : l'appel se fait avec
 * le client HTTP du JDK et le JSON avec Jackson, déjà présents.</p>
 *
 * <p><strong>Le Client ID et le Client Secret ne sont jamais stockés en base</strong>
 * (la table {@code parametre} n'est pas chiffrée) : ils proviennent de
 * l'environnement du serveur.</p>
 */
@Component
public class FournisseurOrangeSms implements FournisseurSms {

    private static final Logger log = LoggerFactory.getLogger(FournisseurOrangeSms.class);

    private static final String CORPS_JETON = "grant_type=client_credentials";

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String clientId;
    private final String clientSecret;
    private final String urlToken;
    private final String modeleUrlEnvoi;

    /** Jeton OAuth en cache : il reste valable une heure. */
    private String jeton;
    private Instant expirationJeton = Instant.EPOCH;

    public FournisseurOrangeSms(
            @Value("${lesourire.notification.orange.client-id:}") String clientId,
            @Value("${lesourire.notification.orange.client-secret:}") String clientSecret,
            @Value("${lesourire.notification.orange.url-token:https://api.orange.com/oauth/v3/token}")
            String urlToken,
            @Value("${lesourire.notification.orange.url-envoi:"
                    + "https://api.orange.com/smsmessaging/v1/outbound/%s/requests}")
            String modeleUrlEnvoi) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
        this.urlToken = urlToken;
        this.modeleUrlEnvoi = modeleUrlEnvoi;
    }

    @Override
    public String nom() {
        return "orange";
    }

    @Override
    public ResultatEnvoi envoyer(String expediteur, String numeroE164, String contenu) {
        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            return ResultatEnvoi.echec("Compte Orange non configuré : renseignez "
                    + "LESOURIRE_ORANGE_CLIENT_ID et LESOURIRE_ORANGE_CLIENT_SECRET.");
        }
        if (expediteur == null || expediteur.isBlank()) {
            return ResultatEnvoi.echec("Nom d'expéditeur manquant : renseignez le paramètre "
                    + "« notification.nom_expediteur ».");
        }
        String expediteurNettoye = expediteur.trim();
        try {
            URI url = URI.create(modeleUrlEnvoi.formatted(encoder(expediteurNettoye)));
            HttpRequest requete = HttpRequest.newBuilder(url)
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + jeton())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            corps(expediteurNettoye, numeroE164, contenu), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> reponse = http.send(requete,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (reponse.statusCode() / 100 == 2) {
                return ResultatEnvoi.ok();
            }
            return ResultatEnvoi.echec("Orange a refusé l'envoi (HTTP " + reponse.statusCode()
                    + ") : " + abreger(reponse.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResultatEnvoi.echec("Envoi interrompu.");
        } catch (IOException | RuntimeException e) {
            return ResultatEnvoi.echec("Envoi impossible (" + e.getClass().getSimpleName()
                    + ") : " + e.getMessage());
        }
    }

    /** Corps JSON attendu par l'API (GSMA OneAPI). */
    private String corps(String expediteur, String numeroE164, String contenu) throws IOException {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("message", contenu);

        Map<String, Object> requete = new LinkedHashMap<>();
        requete.put("address", "tel:" + numeroE164);
        requete.put("senderAddress", expediteur);
        // Un nom d'expéditeur alphanumérique se déclare aussi ici ; face à une
        // adresse « tel:+237… » le champ est inutile (voire refusé).
        if (!expediteur.startsWith("tel:")) {
            requete.put("senderName", expediteur);
        }
        requete.put("outboundSMSTextMessage", message);

        return json.writeValueAsString(Map.of("outboundSMSMessageRequest", requete));
    }

    /** Jeton OAuth 2.0, mis en cache jusqu'à une minute avant son expiration. */
    private synchronized String jeton() throws IOException, InterruptedException {
        if (jeton != null && Instant.now().isBefore(expirationJeton)) {
            return jeton;
        }
        String basic = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        HttpRequest requete = HttpRequest.newBuilder(URI.create(urlToken))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(CORPS_JETON, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> reponse = http.send(requete,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (reponse.statusCode() / 100 != 2) {
            throw new IOException("authentification Orange refusée (HTTP " + reponse.statusCode()
                    + ") : " + abreger(reponse.body()));
        }
        JsonNode corps = json.readTree(reponse.body());
        String valeur = corps.path("access_token").asText(null);
        if (valeur == null || valeur.isBlank()) {
            throw new IOException("réponse d'authentification Orange sans « access_token ».");
        }
        long duree = corps.path("expires_in").asLong(3600);
        jeton = valeur;
        // Marge de sécurité : renouvellement une minute avant l'expiration réelle.
        expirationJeton = Instant.now().plusSeconds(Math.max(60, duree - 60));
        log.info("Jeton OAuth Orange obtenu (valable {} s).", duree);
        return jeton;
    }

    private static String encoder(String valeur) {
        return URLEncoder.encode(valeur, StandardCharsets.UTF_8);
    }

    private static String abreger(String corps) {
        if (corps == null) {
            return "";
        }
        String texte = corps.strip();
        return texte.length() > 300 ? texte.substring(0, 300) + "…" : texte;
    }
}
