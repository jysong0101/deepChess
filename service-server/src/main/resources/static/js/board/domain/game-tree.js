export function createGameNode({
    id,
    dbId = null,
    fen,
    moveSan,
    parentId = null,
    children = [],
    engineScore = "analyzing",
    bestMoveUci = null,
    analysisDetail = null,
}) {
    return {
        id,
        dbId,
        fen,
        moveSan,
        parentId,
        children: [...children],
        engineScore,
        bestMoveUci,
        analysisDetail,
    };
}

export function findExistingChildId(nodes, parentId, moveSan) {
    if (!parentId || !nodes[parentId]) {
        return null;
    }
    return nodes[parentId].children.find(
        (childId) => nodes[childId]?.moveSan === moveSan,
    ) ?? null;
}

export function addNodeToTree(nodes, rootNodeId, node) {
    const existingChildId = findExistingChildId(nodes, node.parentId, node.moveSan);
    if (existingChildId) {
        return { nodes, rootNodeId, nodeId: existingChildId, added: false };
    }

    const nextNodes = { ...nodes, [node.id]: node };
    let nextRootNodeId = rootNodeId;

    if (node.parentId && nextNodes[node.parentId]) {
        nextNodes[node.parentId] = {
            ...nextNodes[node.parentId],
            children: [...nextNodes[node.parentId].children, node.id],
        };
    } else if (!node.parentId) {
        nextRootNodeId = node.id;
    }

    return {
        nodes: nextNodes,
        rootNodeId: nextRootNodeId,
        nodeId: node.id,
        added: true,
    };
}

export function buildTreeFromApi(treeData) {
    const nodes = {};
    const analysisTasks = [];
    let rootNodeId = null;

    treeData.forEach((item) => {
        const needsAnalysis = !item.engineScore;
        nodes[item.positionId] = createGameNode({
            id: item.positionId,
            dbId: item.positionId,
            fen: item.fen,
            moveSan: item.moveSan,
            parentId: item.parentPositionId,
            engineScore: needsAnalysis ? "analyzing" : item.engineScore,
            bestMoveUci: item.bestMoveUci,
            analysisDetail: item.analysisDetail,
        });

        if (needsAnalysis) {
            analysisTasks.push({
                nodeId: item.positionId,
                fen: item.fen,
                moveSan: item.moveSan,
                parentId: item.parentPositionId,
            });
        }
    });

    treeData.forEach((item) => {
        if (item.parentPositionId && nodes[item.parentPositionId]) {
            nodes[item.parentPositionId].children.push(item.positionId);
        } else if (!rootNodeId) {
            rootNodeId = item.positionId;
        }
    });

    return { nodes, rootNodeId, analysisTasks };
}

export function getMoveDisplayMetadata(fen) {
    const parts = fen.split(" ");
    const isBlackMove = parts[1] === "w";
    let moveNumber = Number.parseInt(parts[5], 10);
    if (isBlackMove) {
        moveNumber -= 1;
    }
    return { isBlackMove, moveNumber };
}
