package com.korniienko.kalah;

import com.fasterxml.jackson.databind.JsonNode;
import com.korniienko.kalah.dto.UserCredentialsDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KalahApplicationTests {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    private HttpHeaders headers = new HttpHeaders();

    @BeforeEach
    public void setup() {
        final UserCredentialsDto userCredentialsDto = new UserCredentialsDto(randomUUID().toString(), randomUUID().toString());
        HttpEntity<UserCredentialsDto> entity = new HttpEntity<>(userCredentialsDto, headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                apiRequestUrl("/auth/signup"), HttpMethod.POST, entity, JsonNode.class);
        String token = response.getBody().get("token").asText();
        headers = new HttpHeaders();
        headers.add("Authorization", "Bearer ".concat(token));
        log.debug("Got token:" + token);
    }

    @Test
    void testNewGame() {
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                apiRequestUrl("/games"), HttpMethod.POST, entity, JsonNode.class);
        final String contentType = response.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0);
        assertEquals(MediaType.APPLICATION_JSON_VALUE, contentType);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.hasBody());
        final JsonNode responseBodyJson = response.getBody();
        assertNotNull(responseBodyJson);
        assertTrue(responseBodyJson.has("id"));
        assertTrue(responseBodyJson.has("uri"));
    }

    @Test
    void testListGames() {
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<JsonNode> responseNoGames = restTemplate.exchange(
                apiRequestUrl("/games"), HttpMethod.GET, entity, JsonNode.class);
        assertEquals(HttpStatus.OK, responseNoGames.getStatusCode());
        assertTrue(responseNoGames.hasBody());
        final JsonNode noGamesJsonBody = responseNoGames.getBody();
        assertNotNull(noGamesJsonBody);
        assertTrue(noGamesJsonBody.isArray());

        restTemplate.exchange(apiRequestUrl("/games"), HttpMethod.POST, entity, JsonNode.class);
        restTemplate.exchange(apiRequestUrl("/games"), HttpMethod.POST, entity, JsonNode.class);
        restTemplate.exchange(apiRequestUrl("/games"), HttpMethod.POST, entity, JsonNode.class);

        ResponseEntity<JsonNode> responseWithGames = restTemplate.exchange(
                apiRequestUrl("/games"), HttpMethod.GET, entity, JsonNode.class);
        assertEquals(HttpStatus.OK, responseWithGames.getStatusCode());
        assertTrue(responseWithGames.hasBody());
        final JsonNode gamesJsonBody = responseWithGames.getBody();
        assertNotNull(gamesJsonBody);
        assertTrue(gamesJsonBody.isArray());
        assertEquals(3, gamesJsonBody.size());
    }

    @Test
    void getGameStatus() {
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<JsonNode> gameResponse = restTemplate.exchange(
                apiRequestUrl("/games"), HttpMethod.POST, entity, JsonNode.class);
        final JsonNode responseBodyJson = gameResponse.getBody();
        final String gameStatusUri = responseBodyJson.get("uri").asText().concat("/status");

        ResponseEntity<JsonNode> statusResponse =
                restTemplate.exchange(gameStatusUri, HttpMethod.GET, entity, JsonNode.class);
        final String contentType = statusResponse.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0);
        assertEquals(MediaType.APPLICATION_JSON_VALUE, contentType);
        final JsonNode statusResponseBody = statusResponse.getBody();
        assertTrue(statusResponseBody.has("status"));
        String expected = "{\"1\":\"6\",\"2\":\"6\",\"3\":\"6\",\"4\":\"6\",\"5\":\"6\",\"6\":\"6\",\"7\":\"0\"," +
                "\"8\":\"6\",\"9\":\"6\",\"10\":\"6\",\"11\":\"6\",\"12\":\"6\",\"13\":\"6\",\"14\":\"0\"}";
        assertEquals(expected, statusResponseBody.get("status").toString());
    }

    @Test
    void testMakeMove() {
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<JsonNode> gameResponse = restTemplate.exchange(
                apiRequestUrl("/games"), HttpMethod.POST, entity, JsonNode.class);
        final JsonNode responseBodyJson = gameResponse.getBody();
        final String gameUri = responseBodyJson.get("uri").asText();

        ResponseEntity<JsonNode> moveResponse =
                restTemplate.exchange(gameUri.concat("/pits/").concat("1"), HttpMethod.PUT, entity, JsonNode.class);
        final String contentType = moveResponse.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0);
        assertEquals(MediaType.APPLICATION_JSON_VALUE, contentType);
        final HttpStatus statusCode = moveResponse.getStatusCode();
        final JsonNode moveResponseBody = moveResponse.getBody();
        if (statusCode.is4xxClientError()) {
            assertEquals(HttpStatus.BAD_REQUEST.name(), moveResponseBody.get("status").asText());
            assertEquals("Wrong turn [SOUTH], the current turn is [NORTH].", moveResponseBody.get("message").asText());
            ResponseEntity<JsonNode> moveOppositePartResponse =
                    restTemplate.exchange(gameUri.concat("/pits/").concat("8"), HttpMethod.PUT, entity, JsonNode.class);
            final JsonNode oppositeJsonResponse = moveOppositePartResponse.getBody();
            assertTrue(oppositeJsonResponse.has("status"));
            String expected = "{\"1\":\"6\",\"2\":\"6\",\"3\":\"6\",\"4\":\"6\",\"5\":\"6\",\"6\":\"6\",\"7\":\"0\"," +
                    "\"8\":\"0\",\"9\":\"7\",\"10\":\"7\",\"11\":\"7\",\"12\":\"7\",\"13\":\"7\",\"14\":\"1\"}";
            assertEquals(expected, oppositeJsonResponse.get("status").toString());
        } else if (statusCode.is2xxSuccessful()) {
            assertTrue(moveResponseBody.has("status"));
            String expected = "{\"1\":\"0\",\"2\":\"7\",\"3\":\"7\",\"4\":\"7\",\"5\":\"7\",\"6\":\"7\",\"7\":\"1\"," +
                    "\"8\":\"6\",\"9\":\"6\",\"10\":\"6\",\"11\":\"6\",\"12\":\"6\",\"13\":\"6\",\"14\":\"0\"}";
            assertEquals(expected, moveResponseBody.get("status").toString());
        }
    }

    private String apiRequestUrl(String uri) {
        return "http://localhost:".concat(String.valueOf(port)).concat(uri);
    }

}
