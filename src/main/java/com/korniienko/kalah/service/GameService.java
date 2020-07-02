package com.korniienko.kalah.service;

import com.korniienko.kalah.dao.GameRepository;
import com.korniienko.kalah.dto.GameDto;
import com.korniienko.kalah.dto.GameStatusDto;
import com.korniienko.kalah.exceptions.GameNotFoundException;
import com.korniienko.kalah.exceptions.IllegalMoveException;
import com.korniienko.kalah.model.Game;
import com.korniienko.kalah.model.Pit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class GameService {

    final static int PITS_SIZE = 14;
    final static int PIT_INITIAL_WEIGHT = 6;
    static final int SOUTH_KALAH_INDEX = 7;
    static final int NORTH_KALAH_INDEX = 14;
    static final int INITIAL_NORTH_INDEX = 8;
    static final int INITIAL_SOUTH_INDEX = 1;

    private final GameRepository gameRepository;

    @Autowired
    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public GameDto newGame(String requestUrl) {
        Game game = new Game();
        game.setPits(initPits(game));
        game = gameRepository.save(game);
        return new GameDto(game.getId(), gameUrl(game, requestUrl));
    }

    public List<GameDto> listGames(String requestUrl) {
        return gameRepository
                .findAll()
                .stream()
                .map(game -> new GameDto(game.getId(), gameUrl(game, requestUrl)))
                .collect(Collectors.toList());
    }

    public GameStatusDto status(Long gameId) {
        return gameRepository.findById(gameId)
                .map(game -> new GameStatusDto(pitsToStatusMap(game.getPits())))
                .orElseThrow(gameNotFound(gameId));
    }

    public GameStatusDto makeMove(Long gameId, Integer pitIndex) {
        return gameRepository.findById(gameId)
                .map(game -> {
                    validateAndMove(game, pitIndex);
                    return new GameStatusDto(pitsToStatusMap(game.getPits()));
                })
                .orElseThrow(gameNotFound(gameId));
    }

    private Supplier<RuntimeException> gameNotFound(Long gameId) {
        return () -> new GameNotFoundException(String.format("Game with id [%d] not found on the server.", gameId));
    }

    void validateAndMove(Game game, Integer pitIndex) {
        game.getPits()
                .stream()
                .filter(p -> Objects.equals(p.getIndex(), pitIndex))
                .findAny()
                .map(pit -> validateMove(game, pit))
                .ifPresent(pit -> movePit(game, pit));
    }

    Pit validateMove(Game game, Pit pit) {
        if (game.isGameOver()) {
            final String message = String.format("Game is over, the winner side is [%s].", game.getWinner().name());
            throw new IllegalMoveException(message);
        } else if (pit.isKalah()) {
            final String message = String.format("Cannot make a move from a kalah pit with index [%s].", pit.getIndex());
            throw new IllegalMoveException(message);
        } else if (pit.getPart() != game.getTurn()) {
            final String message = String.format("Wrong turn [%s], the current turn is [%s].", pit.getPart().name(), game.getTurn().name());
            throw new IllegalMoveException(message);
        } else if (pit.getWeight() == 0) {
            final String message = String.format("Cannot make a move, a pit with index [%d] is empty.", pit.getIndex());
            throw new IllegalMoveException(message);
        }
        return pit;
    }

    void movePit(Game game, Pit pit) {
        final List<Pit> pits = game.getPits();
        final Pit ownKalah = kalahByPart(pit.getPart(), pits);
        final Integer weight = pit.getWeight();
        for (int i = 1; i <= weight; i++) {
            final Pit nextPit = nextPit(pits, pit, i);
            final boolean ownPart = nextPit.getPart() == pit.getPart();
            final boolean canMovePit = !nextPit.isKalah() || nextPit.isKalah() && ownPart;
            if (canMovePit) {
                final boolean lastStone = i == weight;
                final boolean isEmptyHouse = nextPit.getWeight() == 0 && !nextPit.isKalah();
                final boolean landedOnEmptyKalah = ownPart && lastStone && isEmptyHouse;
                if (landedOnEmptyKalah) {
                    captureOppositePit(pit, pits, ownKalah, nextPit);
                } else {
                    moveStoneToTheNextPit(pit, nextPit);
                }
                switchTurnIfNeeded(game, nextPit, lastStone);
            }
        }
        endGameIfNeeded(game, pits);
        gameRepository.save(game);
    }

    void endGameIfNeeded(Game game, List<Pit> pits) {
        for (Pit.Part part : Pit.Part.values()) {
            if (isOutOfStones(part, game.getPits())) {
                final Pit oppositeKalah = moveStonesToOppositeKalah(pits, part.opposite());
                final Pit ownKalah = kalahByPart(part, pits);
                detectWinner(game, part, oppositeKalah, ownKalah);
                game.setGameOver(true);
            }
        }
    }

    List<Pit> initPits(Game game) {
        final ArrayList<Pit> pits = new ArrayList<>();
        for (int i = INITIAL_SOUTH_INDEX; i <= PITS_SIZE; i++) {
            final boolean isKalah = i == SOUTH_KALAH_INDEX || i == NORTH_KALAH_INDEX;
            final Pit.Part part = i <= 7 ? Pit.Part.SOUTH : Pit.Part.NORTH;
            pits.add(new Pit(game, i, isKalah, part, isKalah ? 0 : PIT_INITIAL_WEIGHT));
        }
        return pits;
    }

    Pit findPitByIndex(List<Pit> pits, Integer index) {
        return pits.stream().filter(p -> Objects.equals(p.getIndex(), index)).findFirst().get();
    }

    Pit findOppositePit(List<Pit> pits, Pit pit) {
        return pits.stream().filter(p -> p.getIndex() == PITS_SIZE - pit.getIndex()).findFirst().get();
    }

    boolean isOutOfStones(Pit.Part part, List<Pit> pits) {
        return calculateNonKalahStonesWeight(part, pits) == 0;
    }

    Pit kalahByPart(Pit.Part part, List<Pit> pits) {
        return pits.stream()
                .filter(p -> p.isKalah() && p.getPart() == part)
                .findAny().get();
    }

    Pit nextPit(List<Pit> pits, Pit pit, int stepAmount) {
        final int nextPitIndex = pit.getIndex() + stepAmount - 1;
        return pits.get(nextPitIndex < PITS_SIZE ? nextPitIndex : nextPitIndex - PITS_SIZE);
    }

    Map<Integer, String> pitsToStatusMap(List<Pit> pits) {
        return pits.stream().collect(Collectors.toMap(Pit::getIndex, pit -> pit.getWeight().toString()));
    }

    String gameUrl(Game game, String requestUrl) {
        return String.format("%s/%d", requestUrl, game.getId());
    }

    private void detectWinner(Game game, Pit.Part part, Pit oppositeKalah, Pit ownKalah) {
        if (ownKalah.getWeight() > oppositeKalah.getWeight()) {
            game.setWinner(part);
        } else if (oppositeKalah.getWeight() > ownKalah.getWeight()) {
            game.setWinner(part.opposite());
        } else {
            game.setDraw(true);
        }
    }

    private void switchTurnIfNeeded(Game game, Pit nextPit, boolean lastStone) {
        if (lastStone && !nextPit.isKalah()) {
            game.switchTurn();
        }
    }

    private void moveStoneToTheNextPit(Pit pit, Pit nextPit) {
        nextPit.setWeight(nextPit.getWeight() + 1);
        pit.setWeight(pit.getWeight() - 1);
    }

    private void captureOppositePit(Pit pit, List<Pit> pits, Pit ownKalah, Pit nextPit) {
        Pit opposite = findOppositePit(pits, nextPit);
        final int capturedAmount = nextPit.getWeight() + opposite.getWeight() + 1;
        opposite.setWeight(0);
        ownKalah.setWeight(ownKalah.getWeight() + capturedAmount);
        pit.setWeight(0);
    }

    private Pit moveStonesToOppositeKalah(List<Pit> pits, Pit.Part part) {
        final Pit oppositeKalah = kalahByPart(part, pits);
        oppositeKalah.setWeight(oppositeKalah.getWeight() + calculateNonKalahStonesWeight(part, pits));
        emptyNonKalahPitsByPart(pits, part);
        return oppositeKalah;
    }

    private void emptyNonKalahPitsByPart(List<Pit> pits, Pit.Part part) {
        pits.stream().filter(p -> p.getPart() == part && !p.isKalah()).forEach(p -> p.setWeight(0));
    }

    private int calculateNonKalahStonesWeight(Pit.Part part, List<Pit> pits) {
        return pits.stream()
                .filter(p -> p.getPart() == part && !p.isKalah())
                .map(Pit::getWeight).mapToInt(Integer::intValue).sum();
    }
}
