import assert from "node:assert/strict";
import test from "node:test";

import { createSaveDialog } from "../../main/resources/static/js/board/ui/save-dialog.js";

function createHarness({ saveRequest = async () => ({ message: "저장 완료" }) } = {}) {
    const calls = [];
    const savedResponses = [];
    const dialog = createSaveDialog({
        saveRequest,
        render: {
            open: (title) => calls.push(["open", title]),
            close: () => calls.push(["close"]),
            setSaving: (value) => calls.push(["saving", value]),
            setError: (message) => calls.push(["error", message]),
            focusInput: () => calls.push(["focus"]),
        },
        onSaved: (response) => savedResponses.push(response),
    });
    return { dialog, calls, savedResponses };
}

test("저장창은 gameId 기반 기본 제목을 미리 채운다", () => {
    const { dialog, calls } = createHarness();
    dialog.open(42);
    assert.deepEqual(calls, [["open", "저장된 기보 #42"]]);
});

test("빈 제목은 요청하지 않고 입력 오류를 표시한다", async () => {
    let requested = false;
    const { dialog, calls } = createHarness({
        saveRequest: async () => {
            requested = true;
        },
    });
    dialog.open(1);

    assert.equal(await dialog.confirm("   "), false);
    assert.equal(requested, false);
    assert.ok(calls.some(([type, value]) => type === "error" && value.includes("입력")));
    assert.ok(calls.some(([type]) => type === "focus"));
});

test("수정하거나 기본값 그대로 저장할 수 있고 중복 요청을 차단한다", async () => {
    let resolveSave;
    const requests = [];
    const { dialog, savedResponses } = createHarness({
        saveRequest: (gameId, title) => {
            requests.push({ gameId, title });
            return new Promise((resolve) => {
                resolveSave = resolve;
            });
        },
    });
    dialog.open(7);

    const first = dialog.confirm("  엔드게임 연습  ");
    assert.equal(await dialog.confirm("중복"), false);
    assert.deepEqual(requests, [{ gameId: 7, title: "엔드게임 연습" }]);

    resolveSave({ message: "저장 완료" });
    assert.equal(await first, true);
    assert.deepEqual(savedResponses, [{ message: "저장 완료" }]);
});

test("저장 실패 시 창을 유지하고 재시도할 수 있다", async () => {
    let attempts = 0;
    const { dialog, calls } = createHarness({
        saveRequest: async () => {
            attempts += 1;
            if (attempts === 1) {
                throw new Error("저장 실패");
            }
            return { message: "저장 완료" };
        },
    });
    dialog.open(3);

    assert.equal(await dialog.confirm("복기"), false);
    assert.ok(calls.some(([type, value]) => type === "error" && value === "저장 실패"));
    assert.equal(await dialog.confirm("복기"), true);
    assert.equal(attempts, 2);
});
