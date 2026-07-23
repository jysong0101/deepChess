async function requestJson(url, options = {}) {
    const response = await fetch(url, {
        credentials: "same-origin",
        ...options,
    });
    if (!response.ok) {
        throw new Error(`게임 API 요청 실패 (${response.status}): ${url}`);
    }
    return response.json();
}

export function createGame() {
    return requestJson("/api/games", { method: "POST" });
}

export function importGame(payload) {
    return requestJson("/api/games/import", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    });
}

export function fetchGameTree(gameId) {
    return requestJson(`/api/games/${encodeURIComponent(gameId)}/tree`);
}

export function saveGame(gameId) {
    return requestJson(`/api/games/${encodeURIComponent(gameId)}/save`, {
        method: "PUT",
    });
}

export function fetchMyGames() {
    return requestJson("/api/games/my");
}
