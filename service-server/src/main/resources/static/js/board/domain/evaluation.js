export function calculateEvaluationPercentage(scoreValue) {
    if (!scoreValue) {
        return 50;
    }

    const score = String(scoreValue).trim();
    if (score.startsWith("+M") || (score.startsWith("M") && !score.startsWith("-M"))) {
        return 100;
    }
    if (score.startsWith("-M")) {
        return 0;
    }

    const numericScore = Number.parseFloat(score);
    if (Number.isNaN(numericScore)) {
        return 50;
    }

    const percentage = (1 / (1 + Math.exp(-0.3 * numericScore))) * 100;
    return Math.max(5, Math.min(95, percentage));
}

export function formatEvaluationScore(scoreValue) {
    if (!scoreValue) {
        return "0.0";
    }

    const score = String(scoreValue).trim();
    if (!score.includes("M")) {
        const numericScore = Number.parseFloat(score);
        if (!Number.isNaN(numericScore) && numericScore > 0 && !score.startsWith("+")) {
            return `+${score}`;
        }
    }
    return score;
}

export function createEvaluationViewModel(scoreValue) {
    const percentage = calculateEvaluationPercentage(scoreValue);
    return {
        percentage,
        displayScore: formatEvaluationScore(scoreValue),
        advantage: percentage >= 50 ? "white" : "black",
    };
}
