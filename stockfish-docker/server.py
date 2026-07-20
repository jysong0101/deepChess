import grpc
from concurrent import futures
import chess
import chess.engine
import json
import time

import chess_engine_pb2
import chess_engine_pb2_grpc

STOCKFISH_PATH = "/app/stockfish"

class ChessEngineServicer(chess_engine_pb2_grpc.ChessEngineServicer):
    def __init__(self):
        self.engine = chess.engine.SimpleEngine.popen_uci(STOCKFISH_PATH)
        print("♟️ Stockfish Engine is ready for gRPC requests.")

    def AnalyzePosition(self, request, context):
        fen = request.fen
        depth = request.depth
        print(f"📥 요청 수신 - FEN: {fen}, Depth: {depth}")

        try:
            board = chess.Board(fen)
            limit = chess.engine.Limit(depth=depth)
            
            start_time = time.time()
            # 💡 핵심: multipv=3 옵션을 주면 상위 3개의 수가 리스트로 반환됩니다.
            # 엔진은 자동으로 현재 수를 두는 플레이어 입장에서 가장 좋은 수부터 정렬해 줍니다.
            results = self.engine.analyse(board, limit, multipv=3)
            elapsed = time.time() - start_time

            top_moves = []
            
            # 상위 3개 수 데이터 추출
            for i, info in enumerate(results):
                if "pv" in info:
                    move = info["pv"][0]
                    move_uci = move.uci()
                    # 화면에 보여주기 좋게 기보 표기법(SAN, 예: Nf3)으로 변환
                    move_san = board.san(move) 
                    
                    # 평가치는 기존처럼 절대적인 백 기준 뷰(+는 백 유리, -는 흑 유리)를 유지하여
                    # 프론트엔드의 평가치 바와 통일성을 맞춥니다.
                    score_obj = info["score"].white()
                    if score_obj.is_mate():
                        mate_val = score_obj.mate()
                        score_str = f"+M{mate_val}" if mate_val > 0 else f"-M{abs(mate_val)}"
                    else:
                        score_str = f"{score_obj.score() / 100.0:+.2f}"

                    top_moves.append({
                        "rank": i + 1,
                        "uci": move_uci,
                        "san": move_san,
                        "score": score_str
                    })

            # 최선의 수와 점수는 1순위 데이터로 세팅
            best_move_uci = top_moves[0]["uci"] if top_moves else ""
            best_score_str = top_moves[0]["score"] if top_moves else "0.00"

            # 💡 detail_json에 top_moves 리스트를 추가로 말아서 보냅니다.
            detail_dict = {
                "nodes": results[0].get("nodes") if results else 0,
                "time_sec": round(elapsed, 3),
                "top_moves": top_moves
            }
            detail_json = json.dumps(detail_dict)

            print(f"📤 응답 전송 - BestMove: {best_move_uci}, Score: {best_score_str}")

            return chess_engine_pb2.AnalyzeResponse(
                best_move=best_move_uci,
                engine_score=best_score_str,
                detail_json=detail_json
            )
            
        except Exception as e:
            print(f"❌ 분석 중 에러 발생: {e}")
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(str(e))
            return chess_engine_pb2.AnalyzeResponse()

def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    chess_engine_pb2_grpc.add_ChessEngineServicer_to_server(ChessEngineServicer(), server)
    server.add_insecure_port('[::]:50051')
    server.start()
    print("🚀 Python gRPC Server started on port 50051")
    server.wait_for_termination()

if __name__ == '__main__':
    serve()