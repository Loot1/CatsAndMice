package fr.Eniox59.clickgame.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Classe utilitaire pour envoyer des messages à un webhook Discord
 */
public class DiscordWebhook {
    private final String url;
    private String content;

    /**
     * Crée une nouvelle instance de DiscordWebhook
     * @param url L'URL du webhook Discord
     */
    public DiscordWebhook(String url) {
        this.url = url;
    }

    /**
     * Définit le contenu du message
     * @param content Le contenu du message
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Envoie le message au webhook Discord
     * @throws IOException Si une erreur survient lors de l'envoi
     */
    public void execute() throws IOException {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("Le contenu du message ne peut pas être vide");
        }

        // Création du JSON de la requête
        String jsonPayload = String.format("{\"content\":\"%s\"}", 
            content.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t"));
        
        // Création de la connexion HTTP avec URI pour éviter la dépréciation
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "ClickGame-Webhook");
        connection.setDoOutput(true);

        // Envoi des données
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Vérification de la réponse
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode > 299) {
            throw new IOException("Erreur lors de l'envoi du webhook: " + 
                connection.getResponseMessage() + " (HTTP " + responseCode + ")");
        }
    }
}
