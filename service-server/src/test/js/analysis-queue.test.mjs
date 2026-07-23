import assert from "node:assert/strict";
import test from "node:test";

import { createAnalysisQueue } from "../../main/resources/static/js/board/services/analysis-queue.js";

const tick = () => new Promise((resolve) => setImmediate(resolve));

function createHarness(analyzePosition, nodes = {}) {
    const state = { gameId: 10, nodes };
    const scheduled = [];
    const queue = createAnalysisQueue({
        analyzePosition,
        getState: () => state,
        updateNode(nodeId, patch) {
            state.nodes[nodeId] = { ...state.nodes[nodeId], ...patch };
        },
        schedule(callback) {
            scheduled.push(callback);
            return scheduled.length;
        },
        cancelSchedule() {},
    });
    return { state, queue, scheduled };
}

test("분석 요청을 동시에 하나씩 순차 처리한다", async () => {
    const calls = [];
    let active = 0;
    let maxActive = 0;
    const harness = createHarness(async (request) => {
        active += 1;
        maxActive = Math.max(maxActive, active);
        calls.push(request.fen);
        await tick();
        active -= 1;
        return { positionId: calls.length, engineScore: "0.1", analysisDetail: "{}" };
    }, {
        a: { dbId: null },
        b: { dbId: null },
    });

    harness.queue.enqueueAll([
        { nodeId: "a", fen: "fen-a", moveSan: "e4", parentId: null },
        { nodeId: "b", fen: "fen-b", moveSan: "e5", parentId: null },
    ]);
    harness.queue.start();
    await tick();
    await tick();
    await tick();

    assert.deepEqual(calls, ["fen-a", "fen-b"]);
    assert.equal(maxActive, 1);
    assert.equal(harness.queue.getSnapshot().status, "idle");
});

test("부모 DB ID가 없으면 자식 요청을 보내지 않는다", async () => {
    let calls = 0;
    const harness = createHarness(async () => {
        calls += 1;
        return { positionId: 2, engineScore: "0.1", analysisDetail: "{}" };
    }, {
        parent: { dbId: null },
        child: { dbId: null },
    });

    harness.queue.enqueue({ nodeId: "child", fen: "fen", moveSan: "e5", parentId: "parent" });
    harness.queue.start();
    await tick();
    assert.equal(calls, 0);
    assert.equal(harness.queue.getSnapshot().status, "waiting-for-parent");

    harness.state.nodes.parent.dbId = 1;
    harness.scheduled.shift()();
    await tick();
    assert.equal(calls, 1);
});

test("실패하면 재시도 상태로 이동하고 다음 실행에서 성공한다", async () => {
    let attempts = 0;
    const harness = createHarness(async () => {
        attempts += 1;
        if (attempts === 1) {
            throw new Error("temporary");
        }
        return { positionId: 1, engineScore: "0.1", analysisDetail: "{}" };
    }, { node: { dbId: null } });

    harness.queue.enqueue({ nodeId: "node", fen: "fen", moveSan: "e4", parentId: null });
    harness.queue.start();
    await tick();
    assert.equal(harness.queue.getSnapshot().status, "retrying");

    harness.scheduled.shift()();
    await tick();
    assert.equal(attempts, 2);
    assert.equal(harness.queue.getSnapshot().status, "idle");
});

test("clear는 대기 작업과 상태를 초기화한다", () => {
    const harness = createHarness(async () => ({}), { node: { dbId: null } });
    harness.queue.enqueue({ nodeId: "node", fen: "fen", moveSan: "e4", parentId: null });
    harness.queue.clear();
    assert.deepEqual(harness.queue.getSnapshot(), { tasks: [], status: "idle" });
});
