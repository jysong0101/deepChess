import assert from "node:assert/strict";
import test from "node:test";

import { deleteGame, fetchMyGames } from "../../main/resources/static/js/board/api/game-api.js";

test("deleteGame은 인코딩된 ID로 DELETE하고 204를 null로 처리한다", async (context) => {
    const originalFetch = globalThis.fetch;
    context.after(() => {
        globalThis.fetch = originalFetch;
    });

    let request;
    globalThis.fetch = async (url, options) => {
        request = { url, options };
        return new Response(null, { status: 204 });
    };

    assert.equal(await deleteGame("game/1"), null);
    assert.equal(request.url, "/api/games/game%2F1");
    assert.equal(request.options.method, "DELETE");
    assert.equal(request.options.credentials, "same-origin");
});

test("기존 JSON API 응답 처리를 유지한다", async (context) => {
    const originalFetch = globalThis.fetch;
    context.after(() => {
        globalThis.fetch = originalFetch;
    });

    globalThis.fetch = async () => new Response(
        JSON.stringify([{ gameId: 1 }]),
        {
            status: 200,
            headers: { "Content-Type": "application/json" },
        },
    );

    assert.deepEqual(await fetchMyGames(), [{ gameId: 1 }]);
});

test("실패 응답은 상태 코드를 포함한 오류를 발생시킨다", async (context) => {
    const originalFetch = globalThis.fetch;
    context.after(() => {
        globalThis.fetch = originalFetch;
    });

    globalThis.fetch = async () => new Response(null, { status: 404 });
    await assert.rejects(() => deleteGame(1), /404/);
});
