package com.deepchess.service_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.deepchess.service_server.entity.Game;
import com.deepchess.service_server.entity.User;
import com.deepchess.service_server.repository.GameRepository;
import com.deepchess.service_server.repository.UserRepository;
import com.deepchess.service_server.service.GameTitleMigrationService;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GameLibraryIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameTitleMigrationService migrationService;

    @Autowired
    private EntityManager entityManager;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = saveUser("library-a");
        userB = saveUser("library-b");
    }

    @Test
    void savingDraftCreatesDefaultTitleAndRepeatSaveKeepsIt() throws Exception {
        Game game = saveGame(userA, false, null);

        mockMvc.perform(put("/api/games/{gameId}/save", game.getGameId()).with(as(userA)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/games/{gameId}/save", game.getGameId()).with(as(userA)))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        Game saved = gameRepository.findById(game.getGameId()).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("저장된 기보 #" + game.getGameId());
    }

    @Test
    void savingDraftUsesTrimmedUserTitleAndRejectsInvalidTitle() throws Exception {
        Game titled = saveGame(userA, false, null);
        Game invalid = saveGame(userA, false, null);

        mockMvc.perform(put("/api/games/{gameId}/save", titled.getGameId())
                        .with(as(userA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  킹스 인디언 복기  \"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/games/{gameId}/save", invalid.getGameId())
                        .with(as(userA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest());

        entityManager.flush();
        entityManager.clear();
        assertThat(gameRepository.findById(titled.getGameId()).orElseThrow().getTitle())
                .isEqualTo("킹스 인디언 복기");
        assertThat(gameRepository.findById(invalid.getGameId()).orElseThrow().isSaved())
                .isFalse();
    }

    @Test
    void legacySavedGameTitlesAreBackfilled() {
        Game legacy = saveGame(userA, true, null);

        migrationService.backfillSavedGameTitles();
        entityManager.flush();
        entityManager.clear();

        assertThat(gameRepository.findById(legacy.getGameId()).orElseThrow().getTitle())
                .isEqualTo("저장된 기보 #" + legacy.getGameId());
    }

    @Test
    void ownerCanRenameSavedGameAndTitleIsTrimmed() throws Exception {
        Game game = saveGame(userA, true, "기존 제목");
        entityManager.flush();
        LocalDateTime before = game.getUpdatedAt();
        Thread.sleep(5);

        mockMvc.perform(patch("/api/games/{gameId}", game.getGameId())
                        .with(as(userA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"  카로칸 디펜스 연습  "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(game.getGameId()))
                .andExpect(jsonPath("$.title").value("카로칸 디펜스 연습"));

        entityManager.flush();
        entityManager.clear();
        Game renamed = gameRepository.findById(game.getGameId()).orElseThrow();
        assertThat(renamed.getTitle()).isEqualTo("카로칸 디펜스 연습");
        assertThat(renamed.getUpdatedAt()).isAfter(before);
    }

    @Test
    void nonOwnerAndDraftCannotBeRenamed() throws Exception {
        Game saved = saveGame(userA, true, "소유자 기보");
        Game draft = saveGame(userA, false, null);

        mockMvc.perform(renameRequest(saved, userB, "침입"))
                .andExpect(status().isNotFound());
        mockMvc.perform(renameRequest(draft, userA, "임시 기보"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/games/{gameId}", saved.getGameId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"미로그인\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTitlesReturnBadRequest() throws Exception {
        Game game = saveGame(userA, true, "기존 제목");
        String tooLong = "가".repeat(101);

        mockMvc.perform(renameRequest(game, userA, ""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(renameRequest(game, userA, "   "))
                .andExpect(status().isBadRequest());
        mockMvc.perform(renameRequest(game, userA, tooLong))
                .andExpect(status().isBadRequest());
        mockMvc.perform(renameRequest(game, userA, "줄바꿈\n제목"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchTrimsKeywordIgnoresCaseAndExcludesOtherUsersAndDrafts() throws Exception {
        saveGame(userA, true, "Caro-Kann 연습");
        saveGame(userA, true, "엔드게임 복기");
        saveGame(userA, false, "caro draft");
        saveGame(userB, true, "CARO 다른 사용자");

        mockMvc.perform(get("/api/games/my")
                        .with(as(userA))
                        .param("keyword", "  caro  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Caro-Kann 연습"));

        mockMvc.perform(get("/api/games/my").with(as(userA)).param("keyword", "없는 제목"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/games/my").with(as(userA)).param("keyword", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void allSupportedSortOrdersAndInvalidValueAreHandled() throws Exception {
        Game alpha = saveGame(userA, true, "Alpha");
        Game beta = saveGame(userA, true, "Beta");
        setDates(alpha, "2026-01-01 10:00:00", "2026-01-03 10:00:00");
        setDates(beta, "2026-01-02 10:00:00", "2026-01-02 10:00:00");
        entityManager.flush();
        entityManager.clear();

        assertFirstTitle("CREATED_DESC", "Beta");
        assertFirstTitle("CREATED_ASC", "Alpha");
        assertFirstTitle("UPDATED_DESC", "Alpha");
        assertFirstTitle("TITLE_ASC", "Alpha");
        assertFirstTitle("TITLE_DESC", "Beta");

        mockMvc.perform(get("/api/games/my").with(as(userA)).param("sort", "DROP_TABLE"))
                .andExpect(status().isBadRequest());
    }

    private void assertFirstTitle(String sort, String title) throws Exception {
        mockMvc.perform(get("/api/games/my").with(as(userA)).param("sort", sort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value(title))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder renameRequest(
            Game game,
            User user,
            String title) {
        return patch("/api/games/{gameId}", game.getGameId())
                .with(as(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + escapeJson(title) + "\"}");
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private void setDates(Game game, String createdAt, String updatedAt) {
        entityManager.createNativeQuery(
                        "update games set created_at = cast(:createdAt as timestamp), "
                                + "updated_at = cast(:updatedAt as timestamp) where game_id = :gameId")
                .setParameter("createdAt", createdAt)
                .setParameter("updatedAt", updatedAt)
                .setParameter("gameId", game.getGameId())
                .executeUpdate();
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

    private Game saveGame(User user, boolean saved, String title) {
        return gameRepository.save(Game.builder()
                .user(user)
                .title(title)
                .fenContent("initial-fen")
                .isSaved(saved)
                .build());
    }

    private OAuth2LoginRequestPostProcessor as(User user) {
        return oauth2Login().attributes(attributes ->
                attributes.put("sub", user.getGoogleUid()));
    }
}
