async function request(url, options = {}) {
    const response = await fetch(url, {
        credentials: "same-origin",
        ...options,
    });
    if (!response.ok) {
        throw new Error(`게임 API 요청 실패 (${response.status}): ${url}`);
    }

    if (response.status === 204) {
        return null;
    }

    const contentType = response.headers.get("content-type");
    return contentType?.includes("application/json")
        ? response.json()
        : response.text();
}

export function createGame() {
    return request("/api/games", { method: "POST" });
}

export function importGame(payload) {
    return request("/api/games/import", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    });
}

export function fetchGameTree(gameId) {
    return request(`/api/games/${encodeURIComponent(gameId)}/tree`);
}

export function saveGame(gameId) {
    return request(`/api/games/${encodeURIComponent(gameId)}/save`, {
        method: "PUT",
    });
}

export function fetchMyGames() {
    return request("/api/games/my");
}

export function deleteGame(gameId) {
    return request(`/api/games/${encodeURIComponent(gameId)}`, {
        method: "DELETE",
    });
}
