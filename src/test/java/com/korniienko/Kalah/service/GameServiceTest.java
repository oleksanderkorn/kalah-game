package com.korniienko.kalah.service;

import com.korniienko.kalah.dao.GameRepository;
import com.korniienko.kalah.dto.GameDto;
import com.korniienko.kalah.dto.GameStatusDto;
import com.korniienko.kalah.exceptions.GameNotFoundException;
import com.korniienko.kalah.exceptions.IllegalMoveException;
import com.korniienko.kalah.model.Game;
import com.korniienko.kalah.model.Pit;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.korniienko.kalah.service.GameService.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private GameService gameService;

    private List<Pit> pits = null;

    @BeforeEach
    public void setUp() {
        pits = gameService.initPits(new Game());
    }

    @Test
    public void shouldListAllGamesByDtoRepresentation() {
        final Game gameOne = new Game();
        gameOne.setId(1L);
        final Game gameTwo = new Game();
        gameTwo.setId(2L);
        final Game gameThree = new Game();
        gameThree.setId(3L);
        Mockito.when(gameRepository.findAll()).thenReturn(Lists.list(gameOne, gameTwo, gameThree));
        final String requestUrl = "http://localhost:8080/games";
        final List<GameDto> games = gameService.listGames(requestUrl);
        assertEquals(3, games.size());
        for (GameDto game : games) {
            assertEquals(requestUrl.concat("/").concat(game.getId().toString()), game.getUri());
        }
    }

    @Test
    public void shouldGetGameStatusForAnExistingGame() {
        final Game gameOne = new Game();
        gameOne.setId(1L);
        gameOne.setPits(pits);
        Mockito.when(gameRepository.findById(1L)).thenReturn(Optional.of(gameOne));
        final GameStatusDto status = gameService.status(1L);
        assertNotNull(status);
    }

    @Test
    public void shouldFailToGetGameStatusForMissingGameId() {
        Mockito.when(gameRepository.findById(any())).thenReturn(Optional.empty());
        Exception exception = assertThrows(GameNotFoundException.class, () -> gameService.status(1L));
        String expectedMessage = "Game with id [1] not found on the server.";
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void shouldMakeAMoveAndGetAnExtraMoveIfLandInKalah() {
        final Game gameOne = new Game();
        gameOne.setId(1L);
        gameOne.setPits(pits);
        gameOne.setTurn(Pit.Part.SOUTH);
        Mockito.when(gameRepository.findById(1L)).thenReturn(Optional.of(gameOne));
        final GameStatusDto status = gameService.makeMove(1L, 1);
        assertNotNull(status);
        final Map<Integer, String> statusMap = status.getStatus();
        assertEquals("0", statusMap.get(1));
        for (int i = 2; i < 7; i++) {
            assertEquals(String.valueOf(PIT_INITIAL_WEIGHT + 1), statusMap.get(i));
        }
        assertEquals("1", statusMap.get(7));
        for (int i = 8; i < 13; i++) {
            assertEquals(String.valueOf(PIT_INITIAL_WEIGHT), statusMap.get(i));
        }
        assertEquals("0", statusMap.get(14));
        assertEquals(Pit.Part.SOUTH, gameOne.getTurn());

        final GameStatusDto extraMoveStatus = gameService.makeMove(1L, 2);
        final Map<Integer, String> extraMoveStatusMap = extraMoveStatus.getStatus();
        assertEquals("0", extraMoveStatusMap.get(1));
        assertEquals("0", extraMoveStatusMap.get(2));
        assertEquals("8", extraMoveStatusMap.get(3));

        Exception exception = assertThrows(IllegalMoveException.class, () -> gameService.makeMove(1L, 1));
        String expectedMessage = "Wrong turn [SOUTH], the current turn is [NORTH].";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void shouldMakeAMoveAndCaptureOppositePitIfLandInOwnEmptyPit() {
        final Game gameOne = new Game();
        pits.get(0).setWeight(1);
        pits.get(1).setWeight(0);
        gameOne.setId(1L);
        gameOne.setPits(pits);
        gameOne.setTurn(Pit.Part.SOUTH);
        Mockito.when(gameRepository.findById(1L)).thenReturn(Optional.of(gameOne));
        final GameStatusDto status = gameService.makeMove(1L, 1);
        final Map<Integer, String> statusMap = status.getStatus();
        assertEquals("0", statusMap.get(1));
        assertEquals("0", statusMap.get(2));
        assertEquals("7", statusMap.get(7));
        assertEquals("0", statusMap.get(12));
    }

    @Test
    public void shouldMakeAMoveAndFinishTheGameWhenOutOfStoned() {
        final Game gameOne = new Game();
        gameOne.setId(1L);
        gameOne.setPits(pits);
        gameOne.setTurn(Pit.Part.SOUTH);
        for (int i = 0; i < 5; i++) {
            pits.get(i).setWeight(0);
        }
        pits.get(5).setWeight(1);
        Mockito.when(gameRepository.findById(1L)).thenReturn(Optional.of(gameOne));
        final GameStatusDto status = gameService.makeMove(1L, 6);
        final Map<Integer, String> statusMap = status.getStatus();
        for (int i = 1; i < 7; i++) {
            assertEquals(String.valueOf(0), statusMap.get(i));
        }
        assertEquals("1", statusMap.get(7));
        for (int i = 8; i < 13; i++) {
            assertEquals("0", statusMap.get(i));
        }
        assertEquals("36", statusMap.get(14));
    }

    @Test
    public void shouldFailToMakeMoveForMissingGameId() {
        Mockito.when(gameRepository.findById(any())).thenReturn(Optional.empty());
        Exception exception = assertThrows(GameNotFoundException.class, () -> gameService.makeMove(1L, 1));
        String expectedMessage = "Game with id [1] not found on the server.";
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void shouldInitNewGameAndReturnGameDto() {
        final Game game = new Game();
        game.setId(1L);
        Mockito.when(gameRepository.save(any())).thenReturn(game);
        final String requestUrl = "http://localhost:8080/games";
        final GameDto gameDto = gameService.newGame(requestUrl);
        assertEquals(1L, gameDto.getId());
        assertEquals("http://localhost:8080/games/1", gameDto.getUri());
    }

    @Test
    public void shouldCreateGameUrlByGameAndRequestUrl() {
        final Game game = new Game();
        game.setId(42L);
        final String requestUrl = "https://kalah.io/games";
        final String gameUrl = gameService.gameUrl(game, requestUrl);
        assertEquals("https://kalah.io/games/42", gameUrl);
    }

    @Test
    public void shouldFindPitByIndex() {
        final Pit southKalah = gameService.findPitByIndex(pits, SOUTH_KALAH_INDEX);
        assertTrue(southKalah.isKalah());
        final Pit firstSouthPit = gameService.findPitByIndex(pits, SOUTH_KALAH_INDEX + 1);
        assertFalse(firstSouthPit.isKalah());
        final Pit northKalah = gameService.findPitByIndex(pits, NORTH_KALAH_INDEX);
        assertTrue(northKalah.isKalah());
        final Pit lastNorthPit = gameService.findPitByIndex(pits, NORTH_KALAH_INDEX - 1);
        assertFalse(lastNorthPit.isKalah());
    }

    @Test
    public void shouldInitNewPits() {
        assertEquals(pits.size(), PITS_SIZE);
        for (int i = INITIAL_SOUTH_INDEX; i < SOUTH_KALAH_INDEX; i++) {
            final Pit southPit = gameService.findPitByIndex(pits, i);
            assertEquals(i, southPit.getIndex());
            assertEquals(Pit.Part.SOUTH, southPit.getPart());
            assertEquals(PIT_INITIAL_WEIGHT, southPit.getWeight());
            assertFalse(southPit.isKalah());
        }
        for (int i = INITIAL_NORTH_INDEX; i < NORTH_KALAH_INDEX; i++) {
            final Pit northPit = gameService.findPitByIndex(pits, i);
            assertEquals(i, northPit.getIndex());
            assertEquals(Pit.Part.NORTH, northPit.getPart());
            assertEquals(PIT_INITIAL_WEIGHT, northPit.getWeight());
            assertFalse(northPit.isKalah());
        }
        final Pit southKalah = gameService.findPitByIndex(pits, SOUTH_KALAH_INDEX);
        assertTrue(southKalah.isKalah());
        assertEquals(Pit.Part.SOUTH, southKalah.getPart());
        assertEquals(0, southKalah.getWeight());
        final Pit northKalah = gameService.findPitByIndex(pits, NORTH_KALAH_INDEX);
        assertTrue(northKalah.isKalah());
        assertEquals(Pit.Part.NORTH, northKalah.getPart());
        assertEquals(0, northKalah.getWeight());
    }

    @Test
    public void shouldConvertPitsToStatusMap() {
        final Map<Integer, String> status = gameService.pitsToStatusMap(pits);
        assertEquals(status.keySet().size(), PITS_SIZE);
        for (int i = INITIAL_SOUTH_INDEX; i <= PITS_SIZE; i++) {
            if (i == SOUTH_KALAH_INDEX || i == NORTH_KALAH_INDEX) {
                assertEquals("0", status.get(i));
            } else {
                assertEquals("6", status.get(i));
            }
        }
    }

    @Test
    public void shouldFindOppositePitForSouthPits() {
        for (int i = INITIAL_SOUTH_INDEX; i < SOUTH_KALAH_INDEX; i++) {
            final Pit southPit = gameService.findPitByIndex(pits, i);
            final Pit oppositeToSouthPit = gameService.findOppositePit(pits, southPit);
            assertFalse(southPit.isKalah());
            assertFalse(oppositeToSouthPit.isKalah());
            assertEquals(Pit.Part.SOUTH, southPit.getPart());
            assertEquals(Pit.Part.NORTH, oppositeToSouthPit.getPart());
        }
    }

    @Test
    public void shouldFindOppositePitForNorthPits() {
        for (int i = NORTH_KALAH_INDEX - 1; i > SOUTH_KALAH_INDEX + 1; i--) {
            final Pit northPit = gameService.findPitByIndex(pits, i);
            final Pit oppositeToNorthPit = gameService.findOppositePit(pits, northPit);
            assertFalse(northPit.isKalah());
            assertFalse(oppositeToNorthPit.isKalah());
            assertEquals(Pit.Part.NORTH, northPit.getPart());
            assertEquals(Pit.Part.SOUTH, oppositeToNorthPit.getPart());
        }
    }

    @Test
    public void shouldVerifyThatPartIsNotOutOfStonesForNewGame() {
        assertFalse(gameService.isOutOfStones(Pit.Part.SOUTH, pits));
        assertFalse(gameService.isOutOfStones(Pit.Part.NORTH, pits));
    }

    @Test
    public void shouldVerifyThatPartIsOutOfStonesWhenAllNonKalahStonesAreEmpty() {
        for (Pit pit : pits) {
            if (pit.isKalah()) {
                pit.setWeight(42);
            } else {
                pit.setWeight(0);
            }
        }
        assertTrue(gameService.isOutOfStones(Pit.Part.SOUTH, pits));
        assertTrue(gameService.isOutOfStones(Pit.Part.NORTH, pits));
    }

    @Test
    public void shouldFindOwnKalahByPart() {
        final Pit southKalah = gameService.kalahByPart(Pit.Part.SOUTH, pits);
        assertTrue(southKalah.isKalah());
        assertEquals(southKalah.getIndex(), SOUTH_KALAH_INDEX);
        final Pit northKalah = gameService.kalahByPart(Pit.Part.NORTH, pits);
        assertTrue(northKalah.isKalah());
        assertEquals(northKalah.getIndex(), NORTH_KALAH_INDEX);
    }

    @Test
    public void shouldGetNextPitForThePitByStepAmount() {
        final Pit southKalahPit = gameService.findPitByIndex(pits, SOUTH_KALAH_INDEX);
        final Pit nextToSouthKalah = gameService.nextPit(pits, southKalahPit, 1);
        assertFalse(nextToSouthKalah.isKalah());
        assertEquals(8, nextToSouthKalah.getIndex());

        final Pit northKalahPit = gameService.findPitByIndex(pits, NORTH_KALAH_INDEX);
        final Pit nextToNorthKalah = gameService.nextPit(pits, northKalahPit, 1);
        assertFalse(nextToNorthKalah.isKalah());
        assertEquals(1, nextToNorthKalah.getIndex());

        final Pit pit = pits.get(0);
        final Pit samePit = gameService.nextPit(pits, pit, PITS_SIZE);
        assertEquals(samePit, pit);
    }

    @Test
    public void shouldValidateMoveAndReturnPitWhenNoErrors() {
        final Game game = new Game();
        game.setPits(pits);
        game.setTurn(Pit.Part.NORTH);
        final Pit pit = gameService.validateMove(game, pits.get(12));
        assertEquals(13, pit.getIndex());
    }

    @Test
    public void shouldValidateMoveAndFailIfTurnIsWrong() {
        final Game game = new Game();
        game.setPits(pits);
        game.setTurn(Pit.Part.NORTH);
        Exception exception = assertThrows(IllegalMoveException.class, () -> gameService.validateMove(game, pits.get(0)));

        String expectedMessage = "Wrong turn [SOUTH], the current turn is [NORTH].";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void shouldValidateMoveAndFailIfGameIsOver() {
        final Game game = new Game();
        game.setPits(pits);
        game.setWinner(Pit.Part.NORTH);
        game.setGameOver(true);
        Exception exception = assertThrows(IllegalMoveException.class, () -> gameService.validateMove(game, pits.get(0)));
        String expectedMessage = "Game is over, the winner side is [NORTH].";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void shouldEndTheGameWhenOnePartIsOutOfStonesAndDetectWinnerPart() {
        final Game game = new Game();
        game.setPits(pits);
        pits.get(SOUTH_KALAH_INDEX - 1).setWeight(10);
        pits.get(NORTH_KALAH_INDEX - 1).setWeight(20);
        pits.forEach(p -> {
            if (!p.isKalah() && p.getPart() == Pit.Part.SOUTH) {
                p.setWeight(0);
            }
        });
        gameService.endGameIfNeeded(game, pits);
        assertTrue(game.isGameOver());
        assertEquals(game.getWinner(), Pit.Part.NORTH);

        game.setPits(pits);
        pits.get(SOUTH_KALAH_INDEX - 1).setWeight(20);
        pits.get(NORTH_KALAH_INDEX - 1).setWeight(10);
        pits.forEach(p -> {
            if (!p.isKalah() && p.getPart() == Pit.Part.NORTH) {
                p.setWeight(0);
            }
        });
        gameService.endGameIfNeeded(game, pits);
        assertTrue(game.isGameOver());
        assertEquals(game.getWinner(), Pit.Part.SOUTH);
    }

    @Test
    public void shouldEndTheGameWhenOnePartIsOutOfStonesAndDetectDrawResult() {
        final Game game = new Game();
        game.setPits(pits);
        pits.get(SOUTH_KALAH_INDEX - 1).setWeight(10);
        pits.get(NORTH_KALAH_INDEX - 1).setWeight(10);
        pits.forEach(p -> {
            if (!p.isKalah()) {
                p.setWeight(0);
            }
        });
        gameService.endGameIfNeeded(game, pits);
        assertTrue(game.isGameOver());
        assertTrue(game.isDraw());
        assertNull(game.getWinner());
    }

    @Test
    public void shouldValidateMoveAndFailIfMoveFromKalahPit() {
        final Game game = new Game();
        game.setPits(pits);
        Exception southException = assertThrows(IllegalMoveException.class, () -> {
            final Pit southKalah = gameService.kalahByPart(Pit.Part.SOUTH, pits);
            gameService.validateMove(game, southKalah);
        });
        String southExpectedException = "Cannot make a move from a kalah pit with index [7].";
        assertEquals(southExpectedException, southException.getMessage());

        Exception northException = assertThrows(IllegalMoveException.class, () -> {
            final Pit southKalah = gameService.kalahByPart(Pit.Part.NORTH, pits);
            gameService.validateMove(game, southKalah);
        });
        String northExpectedMessage = "Cannot make a move from a kalah pit with index [14].";
        assertEquals(northExpectedMessage, northException.getMessage());
    }

    @Test
    public void shouldValidateMoveAndFailIfMoveFromEmptyPit() {
        final Game game = new Game();
        game.setPits(pits);
        game.setTurn(Pit.Part.SOUTH);
        pits.get(0).setWeight(0);
        Exception exception = assertThrows(IllegalMoveException.class, () -> gameService.validateMove(game, pits.get(0)));
        String expectedMessage = "Cannot make a move, a pit with index [1] is empty.";
        assertEquals(expectedMessage, exception.getMessage());
    }


}
