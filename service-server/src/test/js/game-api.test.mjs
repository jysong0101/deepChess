import assert from "node:assert/strict";
import test from "node:test";

import {
    deleteGame,
    fetchMyGames,
    saveGame,
    updateGameTitle,
} from "../../main/resources/static/js/board/api/game-api.js";

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

test("보관함 검색어를 trim하고 허용된 정렬 값을 쿼리에 전달한다", async (context) => {
    const originalFetch = globalThis.fetch;
    context.after(() => {
        globalThis.fetch = originalFetch;
    });

    let requestedUrl;
    globalThis.fetch = async (url) => {
        requestedUrl = url;
        return new Response("[]", {
            status: 200,
            headers: { "Content-Type": "application/json" },
        });
    };

    await fetchMyGames({ keyword: "  카로칸  ", sort: "TITLE_ASC" });
    assert.equal(
        requestedUrl,
        "/api/games/my?keyword=%EC%B9%B4%EB%A1%9C%EC%B9%B8&sort=TITLE_ASC",
    );
});

test("제목 수정은 인코딩된 ID와 JSON PATCH 요청을 사용한다", async (context) => {
    const originalFetch = globalThis.fetch;
    context.after(() => {
        globalThis.fetch = originalFetch;
    });

    let request;
    globalThis.fetch = async (url, options) => {
        request = { url, options };
        return new Response(
            JSON.stringify({ gameId: 1, title: "새 제목" }),
            {
                status: 200,
                headers: { "Content-Type": "application/json" },
            },
        );
    };

    await updateGameTitle("game/1", "새 제목");
    assert.equal(request.url, "/api/games/game%2F1");
    assert.equal(request.options.method, "PATCH");
    assert.equal(request.options.body, JSON.stringify({ title: "새 제목" }));
});

test("게임 저장 시 사용자가 결정한 제목을 JSON으로 전달한다", async (context) => {
    const originalFetch = globalThis.fetch;
    context.after(() => {
        globalThis.fetch = originalFetch;
    });

    let request;
    globalThis.fetch = async (url, options) => {
        request = { url, options };
        return new Response(JSON.stringify({ message: "저장 완료" }), {
            status: 200,
            headers: { "Content-Type": "application/json" },
        });
    };

    await saveGame(12, "저장된 기보 #12");
    assert.equal(request.url, "/api/games/12/save");
    assert.equal(request.options.method, "PUT");
    assert.equal(request.options.body, JSON.stringify({ title: "저장된 기보 #12" }));
});

test("실패 응답은 상태 코드를 포함한 오류를 발생시킨다", async (context) => {
    const originalFetch = globalThis.fetch;
    context.after(() => {
        globalThis.fetch = originalFetch;
    });

    globalThis.fetch = async () => new Response(null, { status: 404 });
    await assert.rejects(() => deleteGame(1), /404/);
});

test("JSON 오류 응답의 detail을 사용자 메시지로 전달한다", async (context) => {
    const originalFetch = globalThis.fetch;
    context.after(() => {
        globalThis.fetch = originalFetch;
    });

    globalThis.fetch = async () => new Response(
        JSON.stringify({ detail: "기보 제목을 입력해 주세요." }),
        {
            status: 400,
            headers: { "Content-Type": "application/json" },
        },
    );

    await assert.rejects(
        () => updateGameTitle(1, ""),
        /기보 제목을 입력해 주세요/,
    );
});
