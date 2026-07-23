import {
    ANALYSIS_DEPTH,
    ANALYSIS_RETRY_DELAY_MS,
    PARENT_WAIT_DELAY_MS,
} from "../constants.js";

export function createAnalysisQueue({
    analyzePosition,
    getState,
    updateNode,
    onQueueChanged = () => {},
    onAnalysisStarted = () => {},
    onAnalysisSucceeded = () => {},
    onAnalysisFailed = () => {},
    parentWaitDelayMs = PARENT_WAIT_DELAY_MS,
    retryDelayMs = ANALYSIS_RETRY_DELAY_MS,
    schedule = (callback, delay) => setTimeout(callback, delay),
    cancelSchedule = (timerId) => clearTimeout(timerId),
}) {
    let tasks = [];
    let status = "idle";
    let timerId = null;
    let generation = 0;

    const publish = (currentNodeId = null, error = null) => {
        onQueueChanged({
            tasks: [...tasks],
            status,
            currentNodeId,
            error,
        });
    };

    const scheduleProcess = (delay, expectedGeneration) => {
        timerId = schedule(() => {
            timerId = null;
            if (generation === expectedGeneration) {
                processNext();
            }
        }, delay);
    };

    const processNext = async () => {
        if (status === "analyzing" || tasks.length === 0) {
            if (tasks.length === 0) {
                status = "idle";
                publish();
            }
            return;
        }

        const task = tasks[0];
        const state = getState();
        const node = state.nodes[task.nodeId];
        const request = {
            gameId: state.gameId,
            fen: task.fen,
            moveSan: task.moveSan,
            depth: ANALYSIS_DEPTH,
        };

        if (node?.dbId) {
            request.positionId = node.dbId;
        }

        if (task.parentId) {
            const parentNode = state.nodes[task.parentId];
            if (!parentNode?.dbId) {
                status = "waiting-for-parent";
                publish(task.nodeId);
                scheduleProcess(parentWaitDelayMs, generation);
                return;
            }
            request.parentPositionId = parentNode.dbId;
        }

        const runGeneration = generation;
        status = "analyzing";
        publish(task.nodeId);
        onAnalysisStarted(task);

        try {
            const result = await analyzePosition(request);
            if (generation !== runGeneration) {
                return;
            }
            updateNode(task.nodeId, {
                dbId: result.positionId,
                engineScore: result.engineScore,
                bestMoveUci: result.bestMoveUci,
                analysisDetail: result.analysisDetail,
            });
            tasks.shift();
            status = "idle";
            publish();
            onAnalysisSucceeded(task, result);
            processNext();
        } catch (error) {
            if (generation !== runGeneration) {
                return;
            }
            status = "retrying";
            publish(task.nodeId, error);
            onAnalysisFailed(task, error);
            scheduleProcess(retryDelayMs, generation);
        }
    };

    return {
        enqueue(task) {
            tasks.push(task);
            publish();
        },
        enqueueAll(newTasks) {
            tasks.push(...newTasks);
            publish();
        },
        start() {
            processNext();
        },
        clear() {
            generation += 1;
            tasks = [];
            status = "idle";
            if (timerId !== null) {
                cancelSchedule(timerId);
                timerId = null;
            }
            publish();
        },
        getSnapshot() {
            return { tasks: [...tasks], status };
        },
    };
}
