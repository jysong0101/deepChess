package com.deepchess.service_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.deepchess.service_server.dto.response.AnalysisResponse;
import com.deepchess.service_server.entity.Analysis;
import com.deepchess.service_server.entity.Game;
import com.deepchess.service_server.entity.Position;
import com.deepchess.service_server.entity.User;
import com.deepchess.service_server.repository.AnalysisRepository;
import com.deepchess.service_server.repository.GameRepository;
import com.deepchess.service_server.repository.PositionRepository;
import com.deepchess.service_server.repository.UserRepository;
import com.deepchess.service_server.service.ChessEngineService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GameOwnershipIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private AnalysisRepository analysisRepository;

    @MockitoBean
    private ChessEngineService chessEngineService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = saveUser("a");
        userB = saveUser("b");
        when(chessEngineService.analyzePosition(anyString(), anyInt()))
                .thenReturn(new AnalysisResponse("0.25", "e2e4", 18, "{}", null));
        clearInvocations(chessEngineService);
    }

    @Test
    void ownerCanReadDraftAndSavedGameTrees() throws Exception {
        Game draft = saveGame(userA, false);
        Game saved = saveGame(userA, true);
        savePosition(draft, null, "e4");
        savePosition(saved, null, "d4");

        mockMvc.perform(get("/api/games/{gameId}/tree", draft.getGameId()).with(as(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].moveSan").value("e4"));
        mockMvc.perform(get("/api/games/{gameId}/tree", saved.getGameId()).with(as(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].moveSan").value("d4"));
    }

    @Test
    void nonOwnerAndUnknownGameCannotReadTree() throws Exception {
        Game game = saveGame(userA, true);

        mockMvc.perform(get("/api/games/{gameId}/tree", game.getGameId()).with(as(userB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/games/{gameId}/tree", Long.MAX_VALUE).with(as(userA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousCannotReadTree() throws Exception {
        mockMvc.perform(get("/api/games/{gameId}/tree", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanSaveGameIdempotently() throws Exception {
        Game game = saveGame(userA, false);

        mockMvc.perform(put("/api/games/{gameId}/save", game.getGameId()).with(as(userA)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/games/{gameId}/save", game.getGameId()).with(as(userA)))
                .andExpect(status().isOk());

        assertThat(gameRepository.findById(game.getGameId()).orElseThrow().isSaved()).isTrue();
    }

    @Test
    void nonOwnerAndAnonymousCannotSaveGame() throws Exception {
        Game game = saveGame(userA, false);

        mockMvc.perform(put("/api/games/{gameId}/save", game.getGameId()).with(as(userB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/games/{gameId}/save", game.getGameId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanAnalyzeNewPosition() throws Exception {
        Game game = saveGame(userA, false);

        mockMvc.perform(post("/api/analysis")
                        .with(as(userA))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("gameId", game.getGameId().toString())
                        .param("fen", "fen-new")
                        .param("moveSan", "e4")
                        .param("depth", "18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionId").isNumber());

        assertThat(positionRepository.findByGame_GameId(game.getGameId())).hasSize(1);
        assertThat(analysisRepository.countByPosition_Game_GameId(game.getGameId())).isEqualTo(1);
        verify(chessEngineService).analyzePosition("fen-new", 18);
    }

    @Test
    void unauthorizedGameAnalysisDoesNotCallEngine() throws Exception {
        Game game = saveGame(userA, false);

        mockMvc.perform(post("/api/analysis")
                        .with(as(userB))
                        .param("gameId", game.getGameId().toString())
                        .param("fen", "fen"))
                .andExpect(status().isNotFound());

        verify(chessEngineService, never()).analyzePosition(anyString(), anyInt());
    }

    @Test
    void positionFromAnotherOwnerCannotBeMixedIntoGame() throws Exception {
        Game gameA = saveGame(userA, false);
        Game gameB = saveGame(userB, false);
        Position positionB = savePosition(gameB, null, "e4");

        mockMvc.perform(post("/api/analysis")
                        .with(as(userA))
                        .param("gameId", gameA.getGameId().toString())
                        .param("positionId", positionB.getPositionId().toString())
                        .param("fen", positionB.getFen()))
                .andExpect(status().isNotFound());

        verify(chessEngineService, never()).analyzePosition(anyString(), anyInt());
    }

    @Test
    void parentFromAnotherGameCannotBeMixedIntoGame() throws Exception {
        Game target = saveGame(userA, false);
        Game another = saveGame(userA, false);
        Position anotherParent = savePosition(another, null, "e4");

        mockMvc.perform(post("/api/analysis")
                        .with(as(userA))
                        .param("gameId", target.getGameId().toString())
                        .param("parentPositionId", anotherParent.getPositionId().toString())
                        .param("fen", "fen-child")
                        .param("moveSan", "e5"))
                .andExpect(status().isNotFound());

        verify(chessEngineService, never()).analyzePosition(anyString(), anyInt());
    }

    @Test
    void deletingSavedGameRemovesOnlyItsGraph() throws Exception {
        Game deletedGame = saveGame(userA, true);
        Position root = savePosition(deletedGame, null, "e4");
        Position child = savePosition(deletedGame, root, "e5");
        saveAnalysis(root);
        saveAnalysis(child);

        Game untouchedGame = saveGame(userA, true);
        Position untouchedPosition = savePosition(untouchedGame, null, "d4");
        saveAnalysis(untouchedPosition);

        mockMvc.perform(delete("/api/games/{gameId}", deletedGame.getGameId()).with(as(userA)))
                .andExpect(status().isNoContent());

        assertThat(gameRepository.findById(deletedGame.getGameId())).isEmpty();
        assertThat(positionRepository.findByGame_GameId(deletedGame.getGameId())).isEmpty();
        assertThat(analysisRepository.countByPosition_Game_GameId(deletedGame.getGameId())).isZero();
        assertThat(gameRepository.findById(untouchedGame.getGameId())).isPresent();
        assertThat(positionRepository.findByGame_GameId(untouchedGame.getGameId())).hasSize(1);
        assertThat(analysisRepository.countByPosition_Game_GameId(untouchedGame.getGameId())).isEqualTo(1);
    }

    @Test
    void nonOwnerDraftAnonymousAndRepeatedDeleteAreRejected() throws Exception {
        Game ownedSaved = saveGame(userA, true);
        Game ownedDraft = saveGame(userA, false);

        mockMvc.perform(delete("/api/games/{gameId}", ownedSaved.getGameId()).with(as(userB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/games/{gameId}", ownedDraft.getGameId()).with(as(userA)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/games/{gameId}", ownedSaved.getGameId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/games/{gameId}", ownedSaved.getGameId()).with(as(userA)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/games/{gameId}", ownedSaved.getGameId()).with(as(userA)))
                .andExpect(status().isNotFound());
    }

    private User saveUser(String suffix) {
        return userRepository.save(User.builder()
                .email(suffix + "@example.com")
                .googleUid("google-" + suffix)
                .nickname("user-" + suffix)
                .profileImage(null)
                .isProfileSet(true)
                .build());
    }

    private Game saveGame(User user, boolean saved) {
        return gameRepository.save(Game.builder()
                .user(user)
                .fenContent("initial-fen")
                .isSaved(saved)
                .build());
    }

    private Position savePosition(Game game, Position parent, String moveSan) {
        return positionRepository.save(Position.builder()
                .game(game)
                .parentPosition(parent)
                .fen("fen-" + moveSan)
                .moveSan(moveSan)
                .build());
    }

    private Analysis saveAnalysis(Position position) {
        return analysisRepository.save(Analysis.builder()
                .position(position)
                .depth(18)
                .engineScore("0.1")
                .bestMoveUci("e2e4")
                .analysisDetail("{}")
                .build());
    }

    private OAuth2LoginRequestPostProcessor as(User user) {
        return oauth2Login().attributes(attributes ->
                attributes.put("sub", user.getGoogleUid()));
    }
}
