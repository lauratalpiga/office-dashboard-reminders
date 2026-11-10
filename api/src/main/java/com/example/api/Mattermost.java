package com.example.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class Mattermost {

    // Base url of the Mattermost server (from application.properties)
    @Value("${mattermost.url}")
    private String baseUrl;

    // Token used to authenticate API requests (from application.properties)
    @Value("${mattermost.token}")
    private String token;

    // The Id of the channel where the reminders will be sent
    @Value("${mattermost.channel}")
    private String channelId;

    // The Dashbot id
    @Value("${mattermost.botUserId}")
    private String botUserId;

    // Http client used to send REST calls
    private final RestTemplate restTemplate = new RestTemplate();

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }


    /**
     * This method sends a text message in a Mattermost channel using the REST API
     *
     * @param message
     */
    public void sendToChannel(String message) {

        // API endpoint to create a new post in Mattermost
        String url = baseUrl + "/api/v4/posts";

        // Set HTTP headers (authentication + JSON content)
        HttpHeaders headers = createAuthHeaders();

        // JSON body required by Mattermost API
        Map<String, Object> body = Map.of(
            "channel_id", channelId,
            "message", message
        );

        // Combine headers and body into a single HTTP request
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        // Execute HTTP POST request to Mattermost
        restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    public void sendToChannel(String channelId, String message) {

        String url = baseUrl + "/api/v4/posts";

        HttpHeaders headers = createAuthHeaders();

        Map<String, Object> body = Map.of(
            "channel_id", channelId,
            "message", message
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    /**
     * This method sends a message to the user with the given email
     * If the user is not found, nothing happens
     *
     * @param workMail
     * @param message
     */
    public void sendDirectMessage(String workMail, String message) {
        if(workMail == null || workMail.isBlank()) {
            System.out.println("No work mail provided");
            return;
        }

        HttpHeaders headers = createAuthHeaders();

        //finding the user based on the email
        String userUrl = baseUrl + "/api/v4/users/email/" + workMail;
        HttpEntity<Void> userRequest = new HttpEntity<>(headers);

        ResponseEntity<Map> userResponse =
            restTemplate.exchange(userUrl, HttpMethod.GET, userRequest, Map.class);

        Map userBody = userResponse.getBody();
        if (userBody == null || !userBody.containsKey("id")) {
            System.out.println("Mattermost user not found for email: " + workMail);
            return;
        }

        String userId = userBody.get("id").toString();

        // creating the channel with that user
        String dmUrl = baseUrl + "/api/v4/channels/direct";

        HttpEntity<String[]> dmRequest =
            new HttpEntity<>(new String[]{botUserId, userId}, headers);

        ResponseEntity<Map> dmResponse =
            restTemplate.exchange(dmUrl, HttpMethod.POST, dmRequest, Map.class);

        Map dmBody = dmResponse.getBody();
        if (dmBody == null || !dmBody.containsKey("id")) {
            System.out.println("Could not create DM channel with user: " + workMail);
            return;
        }

        String dmChannelId = dmBody.get("id").toString();

        //sending message
        sendToChannel(dmChannelId, message);
    }

}
