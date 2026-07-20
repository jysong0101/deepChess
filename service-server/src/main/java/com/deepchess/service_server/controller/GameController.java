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
    
}