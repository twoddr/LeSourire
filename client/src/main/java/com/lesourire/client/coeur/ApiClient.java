package com.lesourire.client.coeur;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lesourire.commun.dto.UtilisateurDTO;

/**
 * Client HTTP vers le serveur Le Sourire.
 * Authentification HTTP Basic : les identifiants sont conservés en mémoire
 * pour la durée de la session et joints à chaque requête.
 */
public class ApiClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private String urlBase = "http://localhost:8420";
    private String enteteAuthorization;

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase.endsWith("/")
                ? urlBase.substring(0, urlBase.length() - 1)
                : urlBase;
    }

    public String getUrlBase() {
        return urlBase;
    }

    /** Encode une valeur destinée à un paramètre d'URL. */
    public static String encoder(String valeur) {
        return URLEncoder.encode(valeur == null ? "" : valeur, StandardCharsets.UTF_8);
    }

    /**
     * Tente une connexion avec les identifiants fournis.
     *
     * @return le profil de l'utilisateur si les identifiants sont valides
     * @throws ApiException avec un message affichable à l'utilisateur
     */
    public UtilisateurDTO connexion(String nomUtilisateur, String motDePasse) throws ApiException {
        String jeton = Base64.getEncoder().encodeToString(
                (nomUtilisateur + ":" + motDePasse).getBytes(StandardCharsets.UTF_8));
        String entete = "Basic " + jeton;

        HttpResponse<String> reponse = executer(requete("GET", "/api/auth/moi", null, entete));
        if (reponse.statusCode() == 401 || reponse.statusCode() == 403) {
            throw new ApiException("Nom d'utilisateur ou mot de passe incorrect.");
        }
        verifier(reponse);

        this.enteteAuthorization = entete;
        return lire(reponse.body(), new TypeReference<UtilisateurDTO>() {
        });
    }

    /** GET authentifié. */
    public <T> T get(String chemin, TypeReference<T> type) throws ApiException {
        HttpResponse<String> reponse = executer(requete("GET", chemin, null, enteteAuthorization));
        verifier(reponse);
        return lire(reponse.body(), type);
    }

    /** POST authentifié avec corps JSON. */
    public <T> T post(String chemin, Object corps, TypeReference<T> type) throws ApiException {
        HttpResponse<String> reponse = executer(requete("POST", chemin, corps, enteteAuthorization));
        verifier(reponse);
        return lire(reponse.body(), type);
    }

    /** PUT authentifié avec corps JSON. */
    public <T> T put(String chemin, Object corps, TypeReference<T> type) throws ApiException {
        HttpResponse<String> reponse = executer(requete("PUT", chemin, corps, enteteAuthorization));
        verifier(reponse);
        return lire(reponse.body(), type);
    }

    private HttpRequest requete(String methode, String chemin, Object corps, String entete)
            throws ApiException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + chemin))
                .timeout(Duration.ofSeconds(15));
        if (entete != null) {
            builder.header("Authorization", entete);
        }
        if (corps == null) {
            builder.method(methode, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json");
            builder.method(methode, HttpRequest.BodyPublishers.ofString(ecrire(corps)));
        }
        return builder.build();
    }

    private void verifier(HttpResponse<String> reponse) throws ApiException {
        int code = reponse.statusCode();
        if (code >= 200 && code < 300) {
            return;
        }
        if (code == 400) {
            throw new ApiException(extraireMessage(reponse.body(),
                    "Données refusées par le serveur."));
        }
        if (code == 401 || code == 403) {
            throw new ApiException("Accès refusé : votre rôle ne permet pas cette action.");
        }
        if (code == 404) {
            throw new ApiException("Élément introuvable sur le serveur.");
        }
        throw new ApiException("Erreur serveur (code " + code + ").");
    }

    /** Extrait le champ "message" d'une réponse d'erreur Spring, si présent. */
    private String extraireMessage(String corps, String parDefaut) {
        try {
            var noeud = mapper.readTree(corps);
            if (noeud.hasNonNull("message") && !noeud.get("message").asText().isBlank()) {
                return noeud.get("message").asText();
            }
        } catch (IOException e) {
            // corps non JSON : on garde le message par défaut
        }
        return parDefaut;
    }

    private HttpResponse<String> executer(HttpRequest requete) throws ApiException {
        try {
            return http.send(requete, HttpResponse.BodyHandlers.ofString());
        } catch (ConnectException e) {
            throw new ApiException("Serveur injoignable à l'adresse " + urlBase
                    + ".\nVérifiez que le serveur est démarré et que l'adresse est correcte.");
        } catch (IOException e) {
            throw new ApiException("Erreur de communication avec le serveur : " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Connexion interrompue.");
        }
    }

    private <T> T lire(String json, TypeReference<T> type) throws ApiException {
        try {
            return mapper.readValue(json, type);
        } catch (IOException e) {
            throw new ApiException("Réponse du serveur illisible : " + e.getMessage());
        }
    }

    private String ecrire(Object objet) throws ApiException {
        try {
            return mapper.writeValueAsString(objet);
        } catch (IOException e) {
            throw new ApiException("Impossible de préparer la requête : " + e.getMessage());
        }
    }

    /** Erreur affichable à l'utilisateur. */
    public static class ApiException extends Exception {
        public ApiException(String message) {
            super(message);
        }
    }
}
