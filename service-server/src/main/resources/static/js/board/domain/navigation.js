export function findFirstNodeId(rootNodeId) {
    return rootNodeId ?? null;
}

export function findPreviousNodeId(nodes, currentNodeId) {
    if (!currentNodeId || !nodes[currentNodeId]) {
        return null;
    }
    return nodes[currentNodeId].parentId ?? null;
}

export function findNextNodeId(nodes, rootNodeId, currentNodeId) {
    if (!currentNodeId) {
        return findFirstNodeId(rootNodeId);
    }
    return nodes[currentNodeId]?.children?.[0] ?? null;
}

export function findLastMainLineNodeId(nodes, rootNodeId, currentNodeId = null) {
    let nodeId = currentNodeId ?? rootNodeId;
    if (!nodeId) {
        return null;
    }

    while (nodes[nodeId]?.children?.length > 0) {
        nodeId = nodes[nodeId].children[0];
    }
    return nodeId;
}
