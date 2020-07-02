package com.korniienko.kalah.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.korniienko.kalah.dao.UserRepository;
import com.korniienko.kalah.dto.GameDto;
import com.korniienko.kalah.dto.GameStatusDto;
import com.korniienko.kalah.exceptions.GameNotFoundException;
import com.korniienko.kalah.exceptions.IllegalMoveException;
import com.korniienko.kalah.security.jwt.JwtAuthenticationEntryPoint;
import com.korniienko.kalah.security.jwt.JwtTokenProvider;
import com.korniienko.kalah.service.GameService;
import com.korniienko.kalah.service.MyUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GameController.class)
@ActiveProfiles("test")
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GameService gameService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MyUserDetailsService userDetailsService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser
    public void shouldCreateANewGame() throws Exception {
        final GameDto gameDto = new GameDto(1L, "http://localhost:8080/games/1");
        when(gameService.newGame(anyString())).thenReturn(gameDto);

        this.mockMvc.perform(post("/games"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(gameDto.getId().intValue())))
                .andExpect(jsonPath("$.uri", is(gameDto.getUri())));
    }

    @Test
    @WithMockUser
    public void shouldMakeAMoveForExistingGameWithCorrectTurn() throws Exception {
        final Map<Integer, String> status = IntStream.range(1, 15).boxed().collect(Collectors.toMap(Function.identity(), String::valueOf));
        final GameStatusDto gameStatusDto = new GameStatusDto(status);
        final JsonNode expected = objectMapper.valueToTree(gameStatusDto);
        when(gameService.makeMove(anyLong(), anyInt())).thenReturn(gameStatusDto);
        this.mockMvc.perform(put("/games/1/pits/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expected.toString()));
    }

    @Test
    @WithMockUser
    public void shouldFailToMakeAMoveForExistingGameWithWrongTurn() throws Exception {
        final IllegalMoveException exception = new IllegalMoveException("Wrong turn [SOUTH], the current turn is [NORTH].");
        when(gameService.makeMove(anyLong(), anyInt())).thenThrow(exception);
        this.mockMvc.perform(put("/games/1/pits/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.name())))
                .andExpect(jsonPath("$.message", is(exception.getMessage())));
    }

    @Test
    @WithMockUser
    public void shouldFailToMakeAMoveForNonExistingGame() throws Exception {
        final GameNotFoundException exception = new GameNotFoundException("Game with id [%d] not found on the server.");
        when(gameService.makeMove(anyLong(), anyInt())).thenThrow(exception);
        this.mockMvc.perform(put("/games/1/pits/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(HttpStatus.NOT_FOUND.name())))
                .andExpect(jsonPath("$.message", is(exception.getMessage())));
    }

    @Test
    @WithMockUser
    public void shouldListAllGames() throws Exception {
        final List<GameDto> gameDtos = IntStream.range(1, 6)
                .mapToObj(n -> new GameDto((long) n, String.format("http://localhost:8080/games/%d", n)))
                .collect(Collectors.toList());
        when(gameService.listGames(anyString())).thenReturn(gameDtos);
        this.mockMvc.perform(get("/games"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].uri", is("http://localhost:8080/games/1")))
                .andExpect(jsonPath("$[4].id", is(5)))
                .andExpect(jsonPath("$[4].uri", is("http://localhost:8080/games/5")));
    }

    @Test
    @WithMockUser
    public void shouldGetGameStatusForAnExistingGame() throws Exception {
        final Map<Integer, String> status = IntStream.range(1, 15)
                .boxed()
                .collect(Collectors.toMap(Function.identity(), String::valueOf));
        final GameStatusDto gameStatusDto = new GameStatusDto(status);
        final JsonNode expected = objectMapper.valueToTree(gameStatusDto);
        Mockito.when(gameService.status(anyLong())).thenReturn(gameStatusDto);
        this.mockMvc.perform(get("/games/1/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expected.toString()));
    }

    @Test
    @WithMockUser
    public void shouldFailToGetGameStatusForNonExistingGame() throws Exception {
        final GameNotFoundException exception = new GameNotFoundException("Game with id [1] not found on the server.");
        Mockito.when(gameService.status(anyLong())).thenThrow(exception);
        this.mockMvc.perform(get("/games/1/status"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(HttpStatus.NOT_FOUND.name())))
                .andExpect(jsonPath("$.message", is(exception.getMessage())));
    }

}
