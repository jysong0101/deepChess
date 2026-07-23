import { analyzePosition } from "./api/analysis-api.js";
import {
    createGame,
    fetchGameTree,
    importGame,
    saveGame,
} from "./api/game-api.js";
import { SNAPBACK, STANDARD_INITIAL_FEN } from "./constants.js";
import { buildImportPayload, detectImportType, extractGameResult } from "./domain/game-import.js";
import { addNodeToTree, buildTreeFromApi, createGameNode } from "./domain/game-tree.js";
import {
    findLastMainLineNodeId,
    findNextNodeId,
    findPreviousNodeId,
} from "./domain/navigation.js";
import { createAnalysisQueue } from "./services/analysis-queue.js";
import { createBoardStore } from "./state.js";
import { renderEvaluation, renderRecommendations } from "./ui/analysis-view.js";
import { clearLegalMoves, showLegalMoves } from "./ui/board-view.js";
import {
    renderGameResult,
    renderResultMessage,
    renderSaveButton,
    setVisible,
} from "./ui/controls-view.js";
import { renderActiveMove, renderMoveTree } from "./ui/move-tree-view.js";

function parseNodeId(rawId) {
    return Number.isNaN(Number(rawId)) ? rawId : Number(rawId);
}

export function createBoardApp({
    ChessConstructor = window.Chess,
    ChessboardFactory = window.Chessboard,
} = {}) {
    const elements = {
        board: document.querySelector("#myBoard"),
        history: document.querySelector("#historyList"),
        evalFill: document.querySelector("#evalFill"),
        evalText: document.querySelector("#evalText"),
        recommendations: document.querySelector("#topMovesList"),
        newGameButton: document.querySelector("#newGameBtn"),
        importSection: document.querySelector("#importSection"),
        importInput: document.querySelector("#importData"),
        importButton: document.querySelector("#importBtn"),
        result: document.querySelector("#result"),
        gameResult: document.querySelector("#gameResult"),
        saveButton: document.querySelector("#permanentSaveBtn"),
        firstButton: document.querySelector("#moveFirstBtn"),
        previousButton: document.querySelector("#movePrevBtn"),
        nextButton: document.querySelector("#moveNextBtn"),
        lastButton: document.querySelector("#moveLastBtn"),
        flipButton: document.querySelector("#flipBoardBtn"),
    };

    const store = createBoardStore();
    const game = new ChessConstructor();
    let board;

    const renderCurrentNode = (shouldScroll = false) => {
        const state = store.getState();
        renderActiveMove(elements.history, state.currentNodeId, shouldScroll);
        const node = state.currentNodeId ? state.nodes[state.currentNodeId] : null;
        renderEvaluation(
            { fill: elements.evalFill, text: elements.evalText },
            node?.engineScore ?? "0.0",
        );
        renderRecommendations(elements.recommendations, node?.analysisDetail ?? null);
    };

    const queue = createAnalysisQueue({
        analyzePosition,
        getState: store.getState,
        updateNode: store.updateNode,
        onQueueChanged: store.setQueueState,
        onAnalysisSucceeded(task) {
            if (store.getState().currentNodeId === task.nodeId) {
                renderCurrentNode();
            }
        },
        onAnalysisFailed(task, error) {
            console.error(`노드 ${task.nodeId} 분석 실패, 재시도합니다.`, error);
        },
    });

    const navigateToNode = (nodeId, shouldScroll = false) => {
        if (nodeId === null || nodeId === undefined) {
            store.setCurrentNode(null);
            game.load(store.getState().initialFen);
            board.position(store.getState().initialFen);
            renderCurrentNode(shouldScroll);
            return;
        }

        const node = store.getState().nodes[nodeId];
        if (!node) {
            return;
        }
        store.setCurrentNode(nodeId);
        game.load(node.fen);
        board.position(node.fen);
        renderCurrentNode(shouldScroll);
    };

    const moveFirst = () => navigateToNode(null, true);
    const movePrevious = () => {
        const state = store.getState();
        if (!state.currentNodeId) {
            return;
        }
        navigateToNode(findPreviousNodeId(state.nodes, state.currentNodeId), true);
    };
    const moveNext = () => {
        const state = store.getState();
        const nextId = findNextNodeId(state.nodes, state.rootNodeId, state.currentNodeId);
        if (nextId !== null) {
            navigateToNode(nextId, true);
        }
    };
    const moveLast = () => {
        const state = store.getState();
        const lastId = findLastMainLineNodeId(
            state.nodes,
            state.rootNodeId,
            state.currentNodeId,
        );
        if (lastId !== null) {
            navigateToNode(lastId, true);
        }
    };

    const renderTree = () => {
        const state = store.getState();
        renderMoveTree(elements.history, state.nodes, state.rootNodeId);
        renderCurrentNode(true);
    };

    const handleDragStart = (source) => {
        if (game.game_over()) {
            return false;
        }
        const moves = game.moves({ square: source, verbose: true });
        if (moves.length === 0) {
            return false;
        }
        showLegalMoves(elements.board, moves);
        return undefined;
    };

    const handleDrop = (source, target) => {
        clearLegalMoves(elements.board);
        const state = store.getState();
        if (!state.gameId) {
            return SNAPBACK;
        }

        const move = game.move({ from: source, to: target, promotion: "q" });
        if (move === null) {
            return SNAPBACK;
        }

        const node = createGameNode({
            id: store.nextTempNodeId(),
            fen: game.fen(),
            moveSan: move.san,
            parentId: state.currentNodeId,
        });
        const result = addNodeToTree(state.nodes, state.rootNodeId, node);
        store.addTreeNode(result);

        if (!result.added) {
            renderCurrentNode(true);
            return undefined;
        }

        renderTree();
        queue.enqueue({
            nodeId: node.id,
            fen: node.fen,
            moveSan: node.moveSan,
            parentId: node.parentId,
        });
        queue.start();
        return undefined;
    };

    const initializeBoard = () => {
        board = ChessboardFactory("myBoard", {
            draggable: true,
            position: "start",
            pieceTheme: "https://chessboardjs.com/img/chesspieces/wikipedia/{piece}.png",
            onDragStart: handleDragStart,
            onDrop: handleDrop,
            onSnapEnd: () => board.position(game.fen()),
            onSnapbackEnd: () => clearLegalMoves(elements.board),
        });
        window.addEventListener("resize", () => board.resize());
    };

    const loadTree = async () => {
        const { gameId } = store.getState();
        if (!gameId) {
            return;
        }
        const treeData = await fetchGameTree(gameId);
        const tree = buildTreeFromApi(treeData);
        queue.clear();
        store.replaceTree(tree);
        renderTree();
        moveFirst();
        queue.enqueueAll(tree.analysisTasks);
        queue.start();
    };

    const handleNewGame = async () => {
        try {
            const response = await createGame();
            queue.clear();
            store.resetGame({ gameId: response.gameId });
            game.reset();
            board.start();
            elements.history.replaceChildren();
            renderGameResult(elements.gameResult, null);
            renderEvaluation(
                { fill: elements.evalFill, text: elements.evalText },
                "0.0",
            );
            store.setState({ saveStatus: "ready" });
            renderSaveButton(elements.saveButton, "ready");
        } catch (error) {
            store.setState({ lastError: error });
            renderResultMessage(elements.result, error.message);
        }
    };

    const handleImport = async () => {
        const input = elements.importInput.value.trim();
        if (!input) {
            return;
        }

        const type = detectImportType(input);
        const loaded = type === "fen" ? game.load(input) : game.load_pgn(input);
        if (!loaded) {
            window.alert("유효하지 않은 형식입니다.");
            return;
        }

        renderGameResult(elements.gameResult, extractGameResult(input));
        const payload = buildImportPayload({
            input,
            type,
            history: game.history({ verbose: true }),
            createChessGame: (fen) => new ChessConstructor(fen),
        });

        try {
            queue.clear();
            const response = await importGame(payload);
            store.resetGame({
                gameId: response.gameId,
                initialFen: payload.initialFen,
            });
            board.position(game.fen());
            renderResultMessage(elements.result, "✅ DB 연동 및 분석 시작!", "success");
            store.setState({ saveStatus: "ready" });
            renderSaveButton(elements.saveButton, "ready");
            await loadTree();
        } catch (error) {
            store.setState({ lastError: error });
            renderResultMessage(elements.result, error.message);
        }
    };

    const handleSave = async () => {
        const { gameId } = store.getState();
        if (!gameId) {
            return;
        }
        store.setState({ saveStatus: "saving" });
        renderSaveButton(elements.saveButton, "saving");
        try {
            const response = await saveGame(gameId);
            window.alert(response.message);
            store.setState({ saveStatus: "saved" });
            renderSaveButton(elements.saveButton, "saved");
        } catch (error) {
            store.setState({ saveStatus: "ready", lastError: error });
            renderSaveButton(elements.saveButton, "ready");
            renderResultMessage(elements.result, error.message);
        }
    };

    const bindEvents = () => {
        elements.firstButton.addEventListener("click", moveFirst);
        elements.previousButton.addEventListener("click", movePrevious);
        elements.nextButton.addEventListener("click", moveNext);
        elements.lastButton.addEventListener("click", moveLast);
        elements.flipButton.addEventListener("click", () => board.flip());
        elements.newGameButton.addEventListener("click", handleNewGame);
        elements.importButton.addEventListener("click", handleImport);
        elements.saveButton.addEventListener("click", handleSave);
        elements.history.addEventListener("click", (event) => {
            const move = event.target.closest(".move-clickable");
            if (move) {
                navigateToNode(parseNodeId(move.dataset.id));
            }
        });
        document.addEventListener("keydown", (event) => {
            if (document.activeElement?.matches("input, textarea")) {
                return;
            }
            const handlers = {
                ArrowLeft: movePrevious,
                ArrowRight: moveNext,
                ArrowUp: moveFirst,
                ArrowDown: moveLast,
            };
            handlers[event.key]?.();
        });
    };

    const start = async () => {
        initializeBoard();
        bindEvents();

        const gameId = new URLSearchParams(window.location.search).get("gameId");
        if (gameId) {
            store.resetGame({ gameId });
            store.setState({ mode: "saved", saveStatus: "hidden" });
            setVisible(elements.newGameButton, false);
            setVisible(elements.importSection, false);
            renderSaveButton(elements.saveButton, "hidden");
            renderResultMessage(elements.result, "🗄️ 보관함 기보를 불러왔습니다.", "library");
            try {
                await loadTree();
            } catch (error) {
                store.setState({ lastError: error });
                renderResultMessage(elements.result, error.message);
            }
            return;
        }

        const mode = window.localStorage.getItem("mode");
        store.setState({ mode: mode ?? "new" });
        if (mode === "import") {
            setVisible(elements.newGameButton, false);
            setVisible(elements.importSection, true);
        } else {
            setVisible(elements.importSection, false);
            setVisible(elements.newGameButton, true);
            await handleNewGame();
        }
        window.localStorage.removeItem("mode");
    };

    return { start };
}
