package com.deepchess.service_server.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.deepchess.service_server.dto.request.ImportGameRequest;
import com.deepchess.service_server.dto.request.ImportMoveRequest;
import com.deepchess.service_server.dto.request.SaveGameRequest;
import com.deepchess.service_server.dto.request.UpdateGameTitleRequest;
import com.deepchess.service_server.dto.response.CreateGameResponse;
import com.deepchess.service_server.dto.response.GameTreeNodeResponse;
import com.deepchess.service_server.dto.response.ImportGameResponse;
import com.deepchess.service_server.dto.response.MessageResponse;
import com.deepchess.service_server.dto.response.SavedGameResponse;
import com.deepchess.service_server.dto.response.UpdateGameTitleResponse;
import com.deepchess.service_server.entity.Analysis;
import com.deepchess.service_server.entity.Game;
import com.deepchess.service_server.entity.Position;
import com.deepchess.service_server.entity.User;
import com.deepchess.service_server.repository.AnalysisRepository;
import com.deepchess.service_server.repository.GameRepository;
import com.deepchess.service_server.repository.PositionRepository;
import com.deepchess.service_server.service.CurrentUserService;
import com.deepchess.service_server.service.GameAccessService;
import com.deepchess.service_server.service.GameDeletionService;
import com.deepchess.service_server.service.GameLibraryService;
import com.deepchess.service_server.service.SavedGameSort;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class GameController {

    private final GameRepository gameRepository;
    private final PositionRepository positionRepository;
    private final AnalysisRepository analysisRepository;
    private final CurrentUserService currentUserService;
    private final GameAccessService gameAccessService;
    private final GameDeletionService gameDeletionService;
    private final GameLibraryService gameLibraryService;

    @PostMapping("/api/games")
    public CreateGameResponse createNewGame(@AuthenticationPrincipal OAuth2User oAuth2User) {
        User user = currentUserService.requireCurrentUser(oAuth2User);

        Game game = Game.builder()
                .user(user)
                .fenContent("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
                .isSaved(false) // 💡 새 게임은 무조건 임시(Draft) 상태로 시작
                .build();
        game = gameRepository.save(game);
        
        return new CreateGameResponse(game.getGameId(), "새 게임이 생성되었습니다!");
    }

    @PostMapping("/api/games/import")
    public ImportGameResponse importGame(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @RequestBody ImportGameRequest request) {
        User user = currentUserService.requireCurrentUser(oAuth2User);

        Game game = Game.builder()
                .user(user)
                .fenContent(request.initialFen())
                .pgnContent(request.pgnContent())
                .isSaved(false)
                .build();
        game = gameRepository.save(game);

        Position parentPosition = null;
        Long lastPositionId = null;

        if (request.moves() != null && !request.moves().isEmpty()) {
            for (ImportMoveRequest move : request.moves()) {
                Position pos = Position.builder()
                        .game(game)
                        .parentPosition(parentPosition)
                        .fen(move.fen())
                        .moveSan(move.moveSan())
                        .build();
                pos = positionRepository.save(pos);
                parentPosition = pos;
                lastPositionId = pos.getPositionId();
            }
        }

        return new ImportGameResponse(
                game.getGameId(),
                lastPositionId != null ? lastPositionId : -1L,
                "기보 일괄 저장 완료");
    }

    @GetMapping("/api/games/{gameId}/tree")
    public List<GameTreeNodeResponse> getGameTree(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @PathVariable Long gameId) {
        User user = currentUserService.requireCurrentUser(oAuth2User);
        gameAccessService.requireOwnedGame(gameId, user);

        List<Position> positions = positionRepository.findByGame_GameId(gameId);
        List<GameTreeNodeResponse> treeData = new ArrayList<>();

        for (Position pos : positions) {
            Analysis analysis = analysisRepository.findByPosition_PositionId(pos.getPositionId()).orElse(null);
            treeData.add(new GameTreeNodeResponse(
                    pos.getPositionId(),
                    pos.getParentPosition() != null ? pos.getParentPosition().getPositionId() : null,
                    pos.getFen(),
                    pos.getMoveSan(),
                    analysis != null ? analysis.getEngineScore() : null,
                    analysis != null ? analysis.getBestMoveUci() : null,
                    analysis != null ? analysis.getAnalysisDetail() : null));
        }
        return treeData;
    }
    
    // 💡 추가됨: 분석을 즐기다가 유저가 명시적으로 '저장'을 눌렀을 때 호출되는 API
    @PutMapping("/api/games/{gameId}/save")
    public MessageResponse saveGameToLibrary(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @PathVariable Long gameId,
            @RequestBody(required = false) SaveGameRequest request) {
        User user = currentUserService.requireCurrentUser(oAuth2User);
        gameLibraryService.saveOwnedGame(gameId, user, request == null ? null : request.title());
        
        return new MessageResponse("보관함에 정식으로 저장되었습니다!");
    }

    // 💡 추가됨: 내 보관함 기보 목록 조회 API
    @GetMapping("/api/games/my")
    public List<SavedGameResponse> getMySavedGames(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "CREATED_DESC") String sort) {
        User user = currentUserService.requireCurrentUser(oAuth2User);

        List<Game> savedGames = gameLibraryService.findSavedGames(user, keyword, SavedGameSort.parse(sort));
        List<SavedGameResponse> response = new ArrayList<>();
        
        for (Game game : savedGames) {
            response.add(new SavedGameResponse(
                    game.getGameId(),
                    game.getTitle(),
                    game.getCreatedAt().toString(),
                    game.getUpdatedAt().toString(),
                    game.getPgnContent() != null ? game.getPgnContent() : game.getFenContent()));
        }
        return response;
    }

    @PatchMapping("/api/games/{gameId}")
    public UpdateGameTitleResponse updateGameTitle(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @PathVariable Long gameId,
            @RequestBody UpdateGameTitleRequest request) {
        User user = currentUserService.requireCurrentUser(oAuth2User);
        Game game = gameLibraryService.renameSavedGame(gameId, user, request.title());
        return new UpdateGameTitleResponse(
                game.getGameId(),
                game.getTitle(),
                "기보 제목이 변경되었습니다.");
    }

    @DeleteMapping("/api/games/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSavedGame(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @PathVariable Long gameId) {
        User user = currentUserService.requireCurrentUser(oAuth2User);
        gameDeletionService.deleteOwnedSavedGame(gameId, user);
    }
}
