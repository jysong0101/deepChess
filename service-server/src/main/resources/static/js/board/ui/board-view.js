export function clearLegalMoves(boardElement) {
    boardElement.querySelectorAll(".square-55d63.legal-move")
        .forEach((square) => square.classList.remove("legal-move"));
}

export function showLegalMoves(boardElement, moves) {
    moves.forEach((move) => {
        boardElement.querySelector(`.square-${move.to}`)
            ?.classList.add("legal-move");
    });
}
