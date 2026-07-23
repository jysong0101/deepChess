package com.deepchess.service_server.service;

import com.deepchess.service_server.grpc.AnalyzeRequest;
import com.deepchess.service_server.grpc.AnalyzeResponse;
import com.deepchess.service_server.grpc.ChessEngineGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
@Service
@Primary // ⬅️ MockChessEngineService 대신 이 클래스가 우선적으로 주입되도록 설정!
public class GrpcChessEngineService implements ChessEngineService {

    private final ManagedChannel channel;
    private final ChessEngineGrpc.ChessEngineBlockingStub blockingStub;

    public GrpcChessEngineService() {
        // 도커로 띄운 파이썬 gRPC 서버(포트 50051)와 연결
        this.channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext() // 테스트용이므로 암호화 제외
                .build();
        this.blockingStub = ChessEngineGrpc.newBlockingStub(channel);
    }

    @Override
    public com.deepchess.service_server.dto.response.AnalysisResponse analyzePosition(String fen, int depth) {
        // 1. 파이썬 서버로 보낼 요청 객체 생성
        AnalyzeRequest request = AnalyzeRequest.newBuilder()
                .setFen(fen)
                .setDepth(depth)
                .build();

        // 2. gRPC 통신으로 결과 받아오기
        AnalyzeResponse response = blockingStub.analyzePosition(request);

        return new com.deepchess.service_server.dto.response.AnalysisResponse(
                response.getEngineScore(),
                response.getBestMove(),
                depth,
                response.getDetailJson(),
                null);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}
