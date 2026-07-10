import chess
import chess.engine

STOCKFISH_PATH = "/app/stockfish"
# 테스트할 시작 국면 FEN
TEST_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

def test_stockfish():
    print("🤖 Stockfish 엔진 로딩 중...")
    try:
        # 1. 서브프로세스로 스톡피시 엔진 실행
        engine = chess.engine.SimpleEngine.popen_uci(STOCKFISH_PATH)
        
        # 2. 체스 보드 세팅
        board = chess.Board(TEST_FEN)
        print(f"📍 분석할 FEN: {TEST_FEN}")
        print("⏳ 깊이(Depth) 20까지 분석을 시작합니다...\n")
        
        # 3. 엔진 분석 수행 (깊이 20 제한)
        limit = chess.engine.Limit(depth=20)
        result = engine.analyse(board, limit)
        
        # 4. 결과 데이터 추출
        best_move = result.get("pv")[0] if result.get("pv") else None
        score_obj = result["score"].white() # 백(White) 기준 점수 객체
        
        # 점수 포맷팅 (강제 체크메이트인 경우 M3, 일반 점수인 경우 +1.25 포맷)
        if score_obj.is_mate():
            score_str = f"M{score_obj.mate()}"
        else:
            # 폰 1개의 가치가 100(centipawns)이므로 100으로 나눔
            score_str = f"{score_obj.score() / 100.0:+.2f}"
        
        # 5. 결과 출력
        print("========================================")
        print(f"✅ 최선의 수 (Best Move): {best_move}")
        print(f"📊 평가치 (Score): {score_str}")
        print(f"🔍 탐색 노드 수 (Nodes): {result.get('nodes')} 개")
        print(f"⏱️ 탐색 시간 (Time): {result.get('time'):.3f} 초")
        print("========================================")
        
        # 6. 엔진 안전 종료
        engine.quit()
        
    except FileNotFoundError:
        print(f"❌ 엔진을 찾을 수 없습니다. 경로를 확인하세요: {STOCKFISH_PATH}")
    except Exception as e:
        print(f"❌ 엔진 실행 중 오류 발생: {e}")

if __name__ == "__main__":
    test_stockfish()