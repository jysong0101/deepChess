import { createEvaluationViewModel } from "../domain/evaluation.js";

export function renderEvaluation(elements, score) {
    if (!score || score === "analyzing") {
        return;
    }
    const viewModel = createEvaluationViewModel(score);
    elements.fill.style.height = `${viewModel.percentage}%`;
    elements.text.textContent = viewModel.displayScore;
    elements.text.classList.toggle("white-adv", viewModel.advantage === "white");
    elements.text.classList.toggle("black-adv", viewModel.advantage === "black");
}

function createMessage(text, className = "empty-message") {
    const message = document.createElement("div");
    message.className = className;
    message.textContent = text;
    return message;
}

export function renderRecommendations(container, detailJson) {
    container.replaceChildren();
    if (!detailJson || detailJson === "analyzing") {
        container.append(createMessage("엔진 분석 중입니다...", "analyzing-text"));
        return;
    }

    try {
        const detail = JSON.parse(detailJson);
        if (!Array.isArray(detail.top_moves) || detail.top_moves.length === 0) {
            container.append(createMessage("분석 데이터가 없습니다."));
            return;
        }

        detail.top_moves.forEach((move) => {
            const item = document.createElement("div");
            item.className = "recommendation-item";

            const notation = document.createElement("span");
            notation.className = "recommendation-move";
            notation.textContent = `${move.rank}. ${move.san}`;

            const score = document.createElement("span");
            score.className = `eval-badge ${String(move.score).includes("-") ? "black-adv" : "white-adv"}`;
            score.textContent = move.score;

            item.append(notation, score);
            container.append(item);
        });
    } catch {
        container.append(createMessage("데이터를 불러올 수 없습니다."));
    }
}
