import assert from "node:assert/strict";
import test from "node:test";

import { createDeleteDialog } from "../../main/resources/static/js/mypage/delete-dialog.js";

function createHarness({ deleteRequest = async () => {} } = {}) {
    const calls = [];
    let reloads = 0;
    const dialog = createDeleteDialog({
        deleteRequest,
        reload: async () => {
            reloads += 1;
        },
        render: {
            open: (title) => calls.push(["open", title]),
            close: (trigger) => calls.push(["close", trigger]),
            setDeleting: (value) => calls.push(["deleting", value]),
            setError: (message) => calls.push(["error", message]),
        },
    });
    return { dialog, calls, reloads: () => reloads };
}

test("모달은 삭제 대상 제목을 표시하고 취소 시 요청하지 않는다", () => {
    let requested = false;
    const { dialog, calls } = createHarness({
        deleteRequest: async () => {
            requested = true;
        },
    });
    const trigger = {};

    dialog.open({ gameId: 1, title: "카로칸 연습" }, trigger);
    assert.deepEqual(calls[0], ["open", "카로칸 연습"]);
    assert.equal(dialog.cancel(), true);
    assert.equal(requested, false);
    assert.deepEqual(calls[1], ["close", trigger]);
});

test("삭제 중 중복 요청을 차단하고 성공 후 현재 목록을 다시 불러온다", async () => {
    let resolveDelete;
    let requests = 0;
    const { dialog, calls, reloads } = createHarness({
        deleteRequest: () => {
            requests += 1;
            return new Promise((resolve) => {
                resolveDelete = resolve;
            });
        },
    });
    dialog.open({ gameId: 7, title: "엔드게임" });

    const first = dialog.confirm();
    const duplicate = await dialog.confirm();
    assert.equal(duplicate, false);
    assert.equal(requests, 1);
    assert.equal(dialog.isDeleting(), true);

    resolveDelete();
    assert.equal(await first, true);
    assert.equal(reloads(), 1);
    assert.deepEqual(calls.slice(1), [
        ["deleting", true],
        ["error", ""],
        ["close", undefined],
        ["deleting", false],
    ]);
});

test("삭제 실패 시 모달과 목록을 유지하고 재시도할 수 있다", async () => {
    let requests = 0;
    const { dialog, calls, reloads } = createHarness({
        deleteRequest: async () => {
            requests += 1;
            if (requests === 1) {
                throw new Error("삭제 실패");
            }
        },
    });
    dialog.open({ gameId: 9, title: "실패 테스트" });

    assert.equal(await dialog.confirm(), false);
    assert.equal(reloads(), 0);
    assert.ok(calls.some(([type, value]) => type === "error" && value === "삭제 실패"));

    assert.equal(await dialog.confirm(), true);
    assert.equal(requests, 2);
    assert.equal(reloads(), 1);
});
