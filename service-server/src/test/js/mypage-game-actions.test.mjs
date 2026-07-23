import assert from "node:assert/strict";
import test from "node:test";

import { confirmAndDeleteGame } from "../../main/resources/static/js/mypage/game-actions.js";

test("삭제 취소 시 요청과 UI 변경이 없다", async () => {
    let requested = false;
    let deleting = false;

    const deleted = await confirmAndDeleteGame({
        confirmDelete: () => false,
        deleteRequest: async () => {
            requested = true;
        },
        setDeleting: (value) => {
            deleting = value;
        },
        onDeleted() {},
        onError() {},
    });

    assert.equal(deleted, false);
    assert.equal(requested, false);
    assert.equal(deleting, false);
});

test("삭제 성공 시 중복 요청을 막는 상태를 거쳐 항목을 제거한다", async () => {
    const states = [];
    let removed = false;

    const deleted = await confirmAndDeleteGame({
        confirmDelete: () => true,
        deleteRequest: async () => {},
        setDeleting: (value) => states.push(value),
        onDeleted: () => {
            removed = true;
        },
        onError() {},
    });

    assert.equal(deleted, true);
    assert.deepEqual(states, [true]);
    assert.equal(removed, true);
});

test("삭제 실패 시 버튼을 복구하고 오류를 전달한다", async () => {
    const states = [];
    let receivedError;

    const deleted = await confirmAndDeleteGame({
        confirmDelete: () => true,
        deleteRequest: async () => {
            throw new Error("삭제 실패");
        },
        setDeleting: (value) => states.push(value),
        onDeleted() {},
        onError: (error) => {
            receivedError = error;
        },
    });

    assert.equal(deleted, false);
    assert.deepEqual(states, [true, false]);
    assert.match(receivedError.message, /삭제 실패/);
});
