async function request(url, options = {}) {
    const response = await fetch(url, {
        credentials: "same-origin",
        ...options,
    });
    if (!response.ok) {
        let message = `게임 API 요청 실패 (${response.status})`;
        try {
            const contentType = response.headers.get("content-type");
            if (contentType?.includes("application/json")) {
                const error = await response.json();
                message = error.detail || error.message || message;
            } else {
                const text = await response.text();
                message = text || message;
            }
        } catch {
            // 오류 본문을 해석할 수 없으면 상태 코드 메시지를 사용한다.
        }
        throw new Error(message);
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

export function saveGame(gameId, title) {
    const options = { method: "PUT" };
    if (title !== undefined) {
        options.headers = { "Content-Type": "application/json" };
        options.body = JSON.stringify({ title });
    }
    return request(`/api/games/${encodeURIComponent(gameId)}/save`, options);
}

export function fetchMyGames({ keyword = "", sort = "CREATED_DESC" } = {}) {
    const params = new URLSearchParams();
    const normalizedKeyword = keyword.trim();
    if (normalizedKeyword) {
        params.set("keyword", normalizedKeyword);
    }
    params.set("sort", sort);
    return request(`/api/games/my?${params.toString()}`);
}

export function updateGameTitle(gameId, title) {
    return request(`/api/games/${encodeURIComponent(gameId)}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title }),
    });
}

export function deleteGame(gameId) {
    return request(`/api/games/${encodeURIComponent(gameId)}`, {
        method: "DELETE",
    });
}
