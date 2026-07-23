import assert from "node:assert/strict";
import test from "node:test";

import {
    buildImportPayload,
    detectImportType,
    extractGameResult,
} from "../../main/resources/static/js/board/domain/game-import.js";
import { STANDARD_INITIAL_FEN } from "../../main/resources/static/js/board/constants.js";

test("FEN과 PGN 입력을 구분한다", () => {
    assert.equal(detectImportType("8/8/8/8/8/8/8/8 w - - 0 1"), "fen");
    assert.equal(detectImportType("1. e4 e5 2. Nf3"), "pgn");
});

test("PGN 결과를 추출한다", () => {
    assert.equal(extractGameResult("1. e4 e5 1-0"), "1-0");
    assert.equal(extractGameResult("1. e4 e5"), null);
});

test("FEN 가져오기 계약을 유지한다", () => {
    const fen = "8/8/8/8/8/8/8/8 w - - 0 1";
    const payload = buildImportPayload({
        input: fen,
        type: "fen",
        history: [],
        createChessGame: () => {
            throw new Error("FEN에서는 replay game이 필요하지 않다");
        },
    });
    assert.deepEqual(payload, {
        isFen: true,
        initialFen: fen,
        pgnContent: null,
        moves: [{ fen, moveSan: "START" }],
    });
});

test("PGN 가져오기 요청의 수순별 FEN을 만든다", () => {
    const replay = {
        moves: [],
        move(san) {
            this.moves.push(san);
        },
        fen() {
            return `fen-after-${this.moves.join("-")}`;
        },
    };
    const payload = buildImportPayload({
        input: "1. e4 e5",
        type: "pgn",
        history: [{ san: "e4" }, { san: "e5" }],
        createChessGame: (fen) => {
            assert.equal(fen, STANDARD_INITIAL_FEN);
            return replay;
        },
    });
    assert.deepEqual(payload.moves, [
        { fen: "fen-after-e4", moveSan: "e4" },
        { fen: "fen-after-e4-e5", moveSan: "e5" },
    ]);
    assert.equal(payload.pgnContent, "1. e4 e5");
});
