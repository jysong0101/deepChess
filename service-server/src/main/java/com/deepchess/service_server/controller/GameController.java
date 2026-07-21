package com.deepchess.service_server.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.deepchess.service_server.entity.Game;
import com.deepchess.service_server.entity.Position;
import com.deepchess.service_server.entity.User;
import com.deepchess.service_server.repository.AnalysisRepository;
import com.deepchess.service_server.repository.GameRepository;
import com.deepchess.service_server.repository.PositionRepository;
import com.deepchess.service_server.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class GameController {

    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final PositionRepository positionRepository;
    private final AnalysisRepository analysisRepository;

    @PostMapping("/api/games")
    public Map<String, Object> createNewGame(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User == null) return Map.of("error", "로그인 필요");
        String googleUid = oAuth2User.getAttribute("sub");
        User user = userRepository.findByGoogleUid(googleUid)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        Game game = Game.builder()
                .user(user)
                .fenContent("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
                .isSaved(false) // 💡 새 게임은 무조건 임시(Draft) 상태로 시작
                .build();
        game = gameRepository.save(game);
        
        return Map.of("gameId", game.getGameId(), "message", "새 게임이 생성되었습니다!");
    }

    // 🚀 새로 추가된 기보 일괄 불러오기(Batch Insert) API
    @SuppressWarnings("unchecked")
    @PostMapping("/api/games/import")
    public Map<String, Object> importGame(@AuthenticationPrincipal OAuth2User oAuth2User, 
                                          @RequestBody Map<String, Object> payload) {
        
        if (oAuth2User == null) return Map.of("error", "로그인이 필요합니다.");
        String googleUid = oAuth2User.getAttribute("sub");
        User user = userRepository.findByGoogleUid(googleUid)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        String initialFen = (String) payload.get("initialFen");
        String pgnContent = (String) payload.get("pgnContent");

        // 1. 새 Game 생성
        Game game = Game.builder()
                .user(user)
                .fenContent(initialFen)
                .pgnContent(pgnContent)
                .isSaved(false) // 💡 불러온 기보도 우선은 임시 상태로 시작
                .build();
        game = gameRepository.save(game);

        // 2. 프론트에서 파싱해 넘겨준 기보 배열 순회 및 트리 생성
        List<Map<String, String>> moves = (List<Map<String, String>>) payload.get("moves");
        Position parentPosition = null;
        Long lastPositionId = null;

        if (moves != null && !moves.isEmpty()) {
            for (Map<String, String> moveData : moves) {
                Position pos = Position.builder()
                        .game(game)
                        .parentPosition(parentPosition)
                        .fen(moveData.get("fen"))
                        .moveSan(moveData.get("moveSan"))
                        .build();
                
                // Position 간의 부모-자식 관계 꼬리물기를 위해 즉시 저장 후 갱신
                pos = positionRepository.save(pos);
                parentPosition = pos;
                lastPositionId = pos.getPositionId();
            }
        }

        return Map.of(
            "gameId", game.getGameId(),
            "lastPositionId", lastPositionId != null ? lastPositionId : -1L,
            "message", "기보 일괄 저장 완료"
        );
    }

    @GetMapping("/api/games/{gameId}/tree")
    public List<Map<String, Object>> getGameTree(@PathVariable Long gameId) {
        List<Position> positions = positionRepository.findByGame_GameId(gameId);
        List<Map<String, Object>> treeData = new ArrayList<>();

        for (Position pos : positions) {
            Map<String, Object> node = new HashMap<>();
            node.put("positionId", pos.getPositionId());
            node.put("parentPositionId", pos.getParentPosition() != null ? pos.getParentPosition().getPositionId() : null);
            node.put("fen", pos.getFen());
            node.put("moveSan", pos.getMoveSan());

            analysisRepository.findById(pos.getPositionId()).ifPresent(analysis -> {
                node.put("engineScore", analysis.getEngineScore());
                node.put("bestMoveUci", analysis.getBestMoveUci());

                // DB에 저장된 추천 수 JSON 문자열을 프론트로 넘겨줍니다.
                node.put("analysisDetail", analysis.getAnalysisDetail());
            });

            treeData.add(node);
        }
        return treeData;
    }
    
    // 💡 추가됨: 분석을 즐기다가 유저가 명시적으로 '저장'을 눌렀을 때 호출되는 API
    @PutMapping("/api/games/{gameId}/save")
    public Map<String, Object> saveGameToLibrary(@AuthenticationPrincipal OAuth2User oAuth2User, @PathVariable Long gameId) {
        if (oAuth2User == null) return Map.of("error", "로그인이 필요합니다.");
        
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));
        game.markAsSaved();
        gameRepository.save(game);
        
        return Map.of("message", "보관함에 정식으로 저장되었습니다!");
    }

    // 💡 추가됨: 내 보관함 기보 목록 조회 API
    @GetMapping("/api/games/my")
    public List<Map<String, Object>> getMySavedGames(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User == null) throw new IllegalArgumentException("로그인이 필요합니다.");
        
        String googleUid = oAuth2User.getAttribute("sub");
        User user = userRepository.findByGoogleUid(googleUid)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        List<Game> savedGames = gameRepository.findByUserAndIsSavedTrueOrderByCreatedAtDesc(user);
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (Game game : savedGames) {
            Map<String, Object> gameData = new HashMap<>();
            gameData.put("gameId", game.getGameId());
            gameData.put("createdAt", game.getCreatedAt().toString());
            // 목록에서 간단히 보여줄 PGN 또는 초기 FEN 정보
            gameData.put("preview", game.getPgnContent() != null ? game.getPgnContent() : game.getFenContent());
            response.add(gameData);
        }
        return response;
    }

    
}