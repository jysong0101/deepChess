import { fetchMyGames } from "../board/api/game-api.js";

function createMessage(text, error = false) {
    const message = document.createElement("div");
    message.className = `no-data${error ? " error-message" : ""}`;
    message.textContent = text;
    return message;
}

function createGameItem(game) {
    const item = document.createElement("div");
    item.className = "game-item";
    item.tabIndex = 0;
    item.setAttribute("role", "link");

    const preview = document.createElement("div");
    preview.className = "game-preview";
    preview.textContent = `♟️ ${game.preview || "시작 국면"}`;

    const date = document.createElement("div");
    date.className = "game-date";
    date.textContent = new Date(game.createdAt).toLocaleString("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
    });

    const openGame = () => {
        window.location.href = `/board.html?gameId=${encodeURIComponent(game.gameId)}`;
    };
    item.addEventListener("click", openGame);
    item.addEventListener("keydown", (event) => {
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            openGame();
        }
    });
    item.append(preview, date);
    return item;
}

async function start() {
    const list = document.querySelector("#gameList");
    try {
        const games = await fetchMyGames();
        if (games.length === 0) {
            list.replaceChildren(createMessage("아직 저장된 기보가 없습니다."));
            return;
        }
        list.replaceChildren(...games.map(createGameItem));
    } catch (error) {
        list.replaceChildren(createMessage(error.message, true));
    }
}

start();
