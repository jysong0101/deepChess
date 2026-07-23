import { STANDARD_INITIAL_FEN } from "../constants.js";

export function detectImportType(input) {
    return input.includes("/") && (input.includes(" w ") || input.includes(" b "))
        ? "fen"
        : "pgn";
}

export function extractGameResult(pgn) {
    const results = ["1-0", "0-1", "1/2-1/2"];
    return results.find((result) => pgn.includes(` ${result}`) || pgn.endsWith(result)) ?? null;
}

export function buildImportPayload({
    input,
    type,
    history,
    createChessGame,
}) {
    const isFen = type === "fen";
    const initialFen = isFen ? input : STANDARD_INITIAL_FEN;
    const moves = [];

    if (isFen) {
        moves.push({ fen: input, moveSan: "START" });
    } else {
        const replayGame = createChessGame(initialFen);
        history.forEach((move) => {
            replayGame.move(move.san);
            moves.push({ fen: replayGame.fen(), moveSan: move.san });
        });
    }

    return {
        isFen,
        initialFen,
        pgnContent: isFen ? null : input,
        moves,
    };
}
