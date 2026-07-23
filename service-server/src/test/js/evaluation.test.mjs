import assert from "node:assert/strict";
import test from "node:test";

import {
    calculateEvaluationPercentage,
    formatEvaluationScore,
} from "../../main/resources/static/js/board/domain/evaluation.js";

test("0.0은 중앙값이다", () => {
    assert.equal(calculateEvaluationPercentage("0.0"), 50);
});

test("양수는 중앙보다 높고 표시 문자열에 +가 붙는다", () => {
    assert.ok(calculateEvaluationPercentage("1.5") > 50);
    assert.equal(formatEvaluationScore("1.5"), "+1.5");
});

test("음수는 중앙보다 낮다", () => {
    assert.ok(calculateEvaluationPercentage("-1.5") < 50);
    assert.equal(formatEvaluationScore("-1.5"), "-1.5");
});

test("백 메이트 점수는 100%다", () => {
    assert.equal(calculateEvaluationPercentage("+M3"), 100);
    assert.equal(calculateEvaluationPercentage("M3"), 100);
});

test("흑 메이트 점수는 0%다", () => {
    assert.equal(calculateEvaluationPercentage("-M2"), 0);
});

test("잘못된 문자열은 중앙값이다", () => {
    assert.equal(calculateEvaluationPercentage("invalid"), 50);
});

test("일반 점수는 5%와 95% 사이로 제한된다", () => {
    assert.equal(calculateEvaluationPercentage("999"), 95);
    assert.equal(calculateEvaluationPercentage("-999"), 5);
});
