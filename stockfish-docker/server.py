import grpc
from concurrent import futures
import chess
import chess.engine
import json
import time

# gRPC 컴파일러가 자동 생성해 줄 파이썬 모듈 (아직 없지만 도커 빌드 시 생성됨!)
import chess_engine_pb2
import chess_engine_pb2_grpc

STOCKFISH_PATH = "/app/stockfish"

# .proto 파일에서 정의한 서비스(ChessEngine)를 구현하는 클래스
class ChessEngineServicer(chess_engine_pb2_grpc.ChessEngineServicer):
    def __init__(self):
        # 서버가 켜질 때 엔진을 미리 백그라운드로 띄워둡니다. (매 요청마다 켜면 느림)
        self.engine = chess.engine.SimpleEngine.popen_uci(STOCKFISH_PATH)
        print("♟️ Stockfish Engine is ready for gRPC requests.")

    def AnalyzePosition(self, request, context):
        fen = request.fen
        depth = request.depth
        print(f"📥 요청 수신 - FEN: {fen}, Depth: {depth}")

        try:
            board = chess.Board(fen)
            limit = chess.engine.Limit(depth=depth)
            
            # 엔진 분석 실행
            start_time = time.time()
            result = self.engine.analyse(board, limit)
            elapsed = time.time() - start_time

            # 최선의 수 추출 (UCI 포맷: 예 'e2e4')
            best_move = result.get("pv")[0].uci() if result.get("pv") else ""
            
            score_obj = result["score"].white()
            if score_obj.is_mate():
                mate_val = score_obj.mate()
                # 백이 이기는 메이트면 양수, 흑이 이기는 메이트면 음수입니다.
                if mate_val > 0:
                    score_str = f"+M{mate_val}"
                else:
                    score_str = f"-M{abs(mate_val)}" # -M3 형태로 명확히 출력
            else:
                score_str = f"{score_obj.score() / 100.0:+.2f}"

            # 부가 정보(노드수, 연산시간)는 유연하게 JSON으로 말아서 전송
            detail_dict = {
                "nodes": result.get("nodes"),
                "time_sec": round(elapsed, 3)
            }
            detail_json = json.dumps(detail_dict)

            print(f"📤 응답 전송 - BestMove: {best_move}, Score: {score_str}")

            # .proto에서 정의한 AnalyzeResponse 객체에 담아 리턴!
            return chess_engine_pb2.AnalyzeResponse(
                best_move=best_move,
                engine_score=score_str,
                detail_json=detail_json
            )
            
        except Exception as e:
            print(f"❌ 분석 중 에러 발생: {e}")
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(str(e))
            return chess_engine_pb2.AnalyzeResponse()

def serve():
    # 최대 10개의 동시 요청을 처리할 수 있는 스레드 풀 생성
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    # 우리가 만든 Servicer 클래스를 서버에 등록
    chess_engine_pb2_grpc.add_ChessEngineServicer_to_server(ChessEngineServicer(), server)
    
    # 50051 포트 오픈
    server.add_insecure_port('[::]:50051')
    server.start()
    print("🚀 Python gRPC Server started on port 50051")
    server.wait_for_termination()

if __name__ == '__main__':
    serve()