import { STANDARD_INITIAL_FEN } from "./constants.js";

export function createBoardStore() {
    const state = {
        gameId: null,
        initialFen: STANDARD_INITIAL_FEN,
        nodes: {},
        rootNodeId: null,
        currentNodeId: null,
        tempIdCounter: 0,
        analysisQueue: [],
        analysisStatus: "idle",
        currentAnalysisNodeId: null,
        mode: null,
        saveStatus: "hidden",
        lastError: null,
    };

    return {
        getState: () => state,
        setState(patch) {
            Object.assign(state, patch);
        },
        resetGame({ gameId = null, initialFen = STANDARD_INITIAL_FEN } = {}) {
            Object.assign(state, {
                gameId,
                initialFen,
                nodes: {},
                rootNodeId: null,
                currentNodeId: null,
                tempIdCounter: 0,
                analysisQueue: [],
                analysisStatus: "idle",
                currentAnalysisNodeId: null,
                lastError: null,
            });
        },
        replaceTree({ nodes, rootNodeId }) {
            state.nodes = nodes;
            state.rootNodeId = rootNodeId;
            state.currentNodeId = null;
        },
        replaceQueue(tasks) {
            state.analysisQueue = [...tasks];
        },
        setQueueState({ tasks, status, currentNodeId = null, error = null }) {
            state.analysisQueue = [...tasks];
            state.analysisStatus = status;
            state.currentAnalysisNodeId = currentNodeId;
            state.lastError = error;
        },
        addTreeNode(result) {
            state.nodes = result.nodes;
            state.rootNodeId = result.rootNodeId;
            state.currentNodeId = result.nodeId;
        },
        setCurrentNode(nodeId) {
            state.currentNodeId = nodeId;
        },
        updateNode(nodeId, patch) {
            if (!state.nodes[nodeId]) {
                return;
            }
            state.nodes[nodeId] = { ...state.nodes[nodeId], ...patch };
        },
        nextTempNodeId() {
            state.tempIdCounter += 1;
            return `temp_${state.tempIdCounter}`;
        },
    };
}
