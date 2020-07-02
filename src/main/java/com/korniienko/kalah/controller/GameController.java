package com.korniienko.kalah.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.korniienko.kalah.dto.GameDto;
import com.korniienko.kalah.dto.GameStatusDto;
import com.korniienko.kalah.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping(path = "/games")
public class GameController {

    private final GameService gameService;
    private final ObjectMapper objectMapper;

    @Autowired
    public GameController(GameService gameService, ObjectMapper objectMapper) {
        this.gameService = gameService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<JsonNode> newGame(HttpServletRequest request) {
        final GameDto game = gameService.newGame(request.getRequestURL().toString());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.valueToTree(game));
    }

    @PutMapping("/{gameId}/pits/{pitId}")
    public ResponseEntity<GameStatusDto> move(@PathVariable("gameId") Long gameId, @PathVariable("pitId") Integer pitId) {
        final GameStatusDto gameStatusDto = gameService.makeMove(gameId, pitId);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(gameStatusDto);
    }

    @GetMapping("/{gameId}/status")
    public ResponseEntity<GameStatusDto> status(@PathVariable("gameId") Long gameId) {
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(gameService.status(gameId));
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<GameDto> listGames(HttpServletRequest request) {
        return gameService.listGames(request.getRequestURL().toString());
    }

}
