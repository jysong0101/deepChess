import { deleteGame, fetchMyGames } from "../board/api/game-api.js";
import { confirmAndDeleteGame } from "./game-actions.js";

function createMessage(text, error = false) {
    const message = document.createElement("div");
    message.className = `no-data${error ? " error-message" : ""}`;
    message.textContent = text;
    return message;
}

function renderEmptyList(list) {
    list.replaceChildren(createMessage("아직 저장된 기보가 없습니다."));
}

function createGameItem(game, list) {
    const item = document.createElement("div");
    item.className = "game-item";
    item.tabIndex = 0;
    item.setAttribute("role", "link");

    const summary = document.createElement("div");
    summary.className = "game-summary";

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

    summary.append(preview, date);

    const actions = document.createElement("div");
    actions.className = "game-actions";

    const deleteButton = document.createElement("button");
    deleteButton.type = "button";
    deleteButton.className = "delete-game-button";
    deleteButton.textContent = "삭제";
    deleteButton.setAttribute("aria-label", `${game.preview || "저장된 기보"} 삭제`);

    const deleteError = document.createElement("div");
    deleteError.className = "delete-error";
    deleteError.setAttribute("role", "alert");
    actions.append(deleteButton, deleteError);

    const openGame = () => {
        window.location.href = `/board.html?gameId=${encodeURIComponent(game.gameId)}`;
    };
    item.addEventListener("click", openGame);
    item.addEventListener("keydown", (event) => {
        if (event.target !== item) {
            return;
        }
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            openGame();
        }
    });
    deleteButton.addEventListener("pointerdown", (event) => event.stopPropagation());
    deleteButton.addEventListener("click", async (event) => {
        event.stopPropagation();
        deleteError.textContent = "";

        await confirmAndDeleteGame({
            confirmDelete: () => window.confirm(
                "이 기보를 삭제하시겠습니까?\n삭제 후에는 복구할 수 없습니다.",
            ),
            deleteRequest: () => deleteGame(game.gameId),
            setDeleting: (deleting) => {
                deleteButton.disabled = deleting;
                deleteButton.textContent = deleting ? "삭제 중..." : "삭제";
            },
            onDeleted: () => {
                item.remove();
                if (!list.querySelector(".game-item")) {
                    renderEmptyList(list);
                }
            },
            onError: (error) => {
                deleteError.textContent = error.message;
            },
        });
    });

    item.append(summary, actions);
    return item;
}

async function start() {
    const list = document.querySelector("#gameList");
    try {
        const games = await fetchMyGames();
        if (games.length === 0) {
            renderEmptyList(list);
            return;
        }
        list.replaceChildren(...games.map((game) => createGameItem(game, list)));
    } catch (error) {
        list.replaceChildren(createMessage(error.message, true));
    }
}

start();
