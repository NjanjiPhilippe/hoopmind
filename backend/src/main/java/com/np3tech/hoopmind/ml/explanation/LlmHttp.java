package com.np3tech.hoopmind.ml.explanation;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.util.function.Consumer;

/** Ce que les deux adaptateurs HTTP partagent : client avec délais, et traduction des erreurs. */
public final class LlmHttp {

    private LlmHttp() {
    }

    public static RestClient client(String baseUrl, ExplanationProperties properties, Consumer<HttpHeaders> headers) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(properties.timeout());
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requests)
                .defaultHeaders(headers)
                .build();
    }

    /**
     * Réseau, délai dépassé, quota (429) ou erreur du service (5xx) : indisponibilité passagère, à retenter.
     * Toute autre erreur (clé refusée, modèle inconnu, requête invalide) : refus, inutile de retenter.
     */
    public static LlmException translate(String provider, RestClientException error) {
        if (error instanceof RestClientResponseException response) {
            int status = response.getStatusCode().value();
            String message = provider + " a répondu HTTP " + status;
            return status == 408 || status == 429 || status >= 500
                    ? new LlmUnavailableException(message, error)
                    : new LlmRejectedException(message, error);
        }
        if (error instanceof ResourceAccessException) {
            return new LlmUnavailableException(provider + " injoignable ou trop lent", error);
        }
        return new LlmRejectedException(provider + " : réponse illisible", error);
    }
}
