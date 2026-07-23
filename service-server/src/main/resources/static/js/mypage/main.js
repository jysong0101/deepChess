import {
    deleteGame,
    fetchMyGames,
    updateGameTitle,
} from "../board/api/game-api.js";
import { createDeleteDialog } from "./delete-dialog.js";

const SORT_OPTIONS = new Set([
    "CREATED_DESC",
    "CREATED_ASC",
    "UPDATED_DESC",
    "TITLE_ASC",
    "TITLE_DESC",
]);

const elements = {
    searchForm: document.querySelector("#librarySearch"),
    keywordInput: document.querySelector("#keywordInput"),
    clearSearchButton: document.querySelector("#clearSearchButton"),
    sortSelect: document.querySelector("#sortSelect"),
    listStatus: document.querySelector("#listStatus"),
    list: document.querySelector("#gameList"),
    deleteModal: document.querySelector("#deleteModal"),
    deleteGameTitle: document.querySelector("#deleteGameTitle"),
    deleteModalError: document.querySelector("#deleteModalError"),
    cancelDeleteButton: document.querySelector("#cancelDeleteButton"),
    confirmDeleteButton: document.querySelector("#confirmDeleteButton"),
};

const query = new URLSearchParams(window.location.search);
const initialSort = query.get("sort");
const state = {
    keyword: query.get("keyword")?.trim() || "",
    sort: SORT_OPTIONS.has(initialSort) ? initialSort : "CREATED_DESC",
    loadingSequence: 0,
};

function createMessage(text, error = false) {
    const message = document.createElement("div");
    message.className = `no-data${error ? " error-message" : ""}`;
    message.textContent = text;
    return message;
}

function formatDate(value) {
    return new Date(value).toLocaleString("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
    });
}

function stopItemNavigation(element) {
    element.addEventListener("pointerdown", (event) => event.stopPropagation());
    element.addEventListener("click", (event) => event.stopPropagation());
}

function createInlineEditor(game, titleElement, editButton) {
    const editor = document.createElement("form");
    editor.className = "title-editor";

    const input = document.createElement("input");
    input.type = "text";
    input.value = game.title;
    input.maxLength = 100;
    input.required = true;
    input.setAttribute("aria-label", "기보 제목");

    const hint = document.createElement("span");
    hint.className = "title-hint";
    hint.textContent = "최대 100자";

    const error = document.createElement("div");
    error.className = "title-error";
    error.setAttribute("role", "alert");

    const saveButton = document.createElement("button");
    saveButton.type = "submit";
    saveButton.className = "primary-button";
    saveButton.textContent = "저장";

    const cancelButton = document.createElement("button");
    cancelButton.type = "button";
    cancelButton.className = "secondary-button";
    cancelButton.textContent = "취소";

    const actions = document.createElement("div");
    actions.className = "editor-actions";
    actions.append(saveButton, cancelButton);
    editor.append(input, hint, actions, error);
    stopItemNavigation(editor);

    const close = () => {
        editor.replaceWith(titleElement);
        editButton.disabled = false;
        editButton.focus();
    };

    cancelButton.addEventListener("click", close);
    input.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            event.preventDefault();
            close();
        }
    });
    editor.addEventListener("submit", async (event) => {
        event.preventDefault();
        const title = input.value.trim();
        if (!title) {
            error.textContent = "기보 제목을 입력해 주세요.";
            input.focus();
            return;
        }

        input.disabled = true;
        saveButton.disabled = true;
        cancelButton.disabled = true;
        saveButton.textContent = "저장 중...";
        error.textContent = "";
        try {
            await updateGameTitle(game.gameId, title);
            await loadGames();
        } catch (requestError) {
            input.disabled = false;
            saveButton.disabled = false;
            cancelButton.disabled = false;
            saveButton.textContent = "저장";
            error.textContent = requestError.message;
            input.focus();
        }
    });

    return { editor, input };
}

function createGameItem(game) {
    const item = document.createElement("article");
    item.className = "game-item";
    item.tabIndex = 0;
    item.setAttribute("role", "link");
    item.setAttribute("aria-label", `${game.title} 기보 열기`);

    const summary = document.createElement("div");
    summary.className = "game-summary";

    const title = document.createElement("h3");
    title.className = "game-title";
    title.textContent = game.title;

    const date = document.createElement("div");
    date.className = "game-date";
    date.textContent = `최근 수정: ${formatDate(game.updatedAt)}`;

    const preview = document.createElement("div");
    preview.className = "game-preview";
    preview.textContent = game.preview || "시작 국면";
    summary.append(title, date, preview);

    const actions = document.createElement("div");
    actions.className = "game-actions";

    const editButton = document.createElement("button");
    editButton.type = "button";
    editButton.className = "secondary-button";
    editButton.textContent = "제목 수정";
    editButton.setAttribute("aria-label", `${game.title} 제목 수정`);

    const deleteButton = document.createElement("button");
    deleteButton.type = "button";
    deleteButton.className = "delete-game-button";
    deleteButton.textContent = "삭제";
    deleteButton.setAttribute("aria-label", `${game.title} 삭제`);
    actions.append(editButton, deleteButton);
    stopItemNavigation(actions);

    const openGame = () => {
        window.location.href = `/board.html?gameId=${encodeURIComponent(game.gameId)}`;
    };
    item.addEventListener("click", openGame);
    item.addEventListener("keydown", (event) => {
        if (event.target === item && (event.key === "Enter" || event.key === " ")) {
            event.preventDefault();
            openGame();
        }
    });

    editButton.addEventListener("click", () => {
        editButton.disabled = true;
        const { editor, input } = createInlineEditor(game, title, editButton);
        title.replaceWith(editor);
        input.select();
    });
    deleteButton.addEventListener("click", () => deleteDialog.open(game, deleteButton));

    item.append(summary, actions);
    return item;
}

function updateUrl() {
    const params = new URLSearchParams();
    if (state.keyword) {
        params.set("keyword", state.keyword);
    }
    if (state.sort !== "CREATED_DESC") {
        params.set("sort", state.sort);
    }
    const search = params.toString();
    window.history.replaceState(null, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
}

async function loadGames() {
    const sequence = ++state.loadingSequence;
    elements.listStatus.textContent = "기보를 불러오는 중입니다...";
    try {
        const games = await fetchMyGames({ keyword: state.keyword, sort: state.sort });
        if (sequence !== state.loadingSequence) {
            return;
        }
        if (games.length === 0) {
            const text = state.keyword
                ? `“${state.keyword}”과 일치하는 저장 기보가 없습니다.`
                : "아직 저장된 기보가 없습니다.";
            elements.list.replaceChildren(createMessage(text));
        } else {
            elements.list.replaceChildren(...games.map(createGameItem));
        }
        elements.listStatus.textContent = `${games.length}개의 기보`;
        updateUrl();
    } catch (error) {
        if (sequence === state.loadingSequence) {
            elements.list.replaceChildren(createMessage(error.message, true));
            elements.listStatus.textContent = "기보를 불러오지 못했습니다.";
        }
    }
}

const deleteDialog = createDeleteDialog({
    deleteRequest: deleteGame,
    reload: loadGames,
    render: {
        open(title) {
            elements.deleteGameTitle.textContent = title;
            elements.deleteModalError.textContent = "";
            elements.deleteModal.hidden = false;
            document.body.classList.add("modal-open");
            elements.cancelDeleteButton.focus();
        },
        close(returnFocusTo) {
            elements.deleteModal.hidden = true;
            document.body.classList.remove("modal-open");
            returnFocusTo?.focus();
        },
        setDeleting(deleting) {
            elements.confirmDeleteButton.disabled = deleting;
            elements.cancelDeleteButton.disabled = deleting;
            elements.confirmDeleteButton.textContent = deleting ? "삭제 중..." : "영구 삭제";
        },
        setError(message) {
            elements.deleteModalError.textContent = message;
        },
    },
});

elements.confirmDeleteButton.addEventListener("click", deleteDialog.confirm);
elements.cancelDeleteButton.addEventListener("click", deleteDialog.cancel);
elements.deleteModal.addEventListener("click", (event) => {
    if (event.target === elements.deleteModal) {
        deleteDialog.cancel();
    }
});
elements.deleteModal.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
        event.preventDefault();
        deleteDialog.cancel();
        return;
    }
    if (event.key !== "Tab") {
        return;
    }
    const buttons = [elements.cancelDeleteButton, elements.confirmDeleteButton]
        .filter((button) => !button.disabled);
    const first = buttons[0];
    const last = buttons.at(-1);
    if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
    }
});

let debounceTimer;
elements.searchForm.addEventListener("submit", (event) => {
    event.preventDefault();
    clearTimeout(debounceTimer);
    state.keyword = elements.keywordInput.value.trim();
    loadGames();
});
elements.keywordInput.addEventListener("input", () => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
        state.keyword = elements.keywordInput.value.trim();
        loadGames();
    }, 350);
});
elements.clearSearchButton.addEventListener("click", () => {
    clearTimeout(debounceTimer);
    elements.keywordInput.value = "";
    state.keyword = "";
    loadGames();
    elements.keywordInput.focus();
});
elements.sortSelect.addEventListener("change", () => {
    state.sort = elements.sortSelect.value;
    loadGames();
});

elements.keywordInput.value = state.keyword;
elements.sortSelect.value = state.sort;
loadGames();
