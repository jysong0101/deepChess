import { getMoveDisplayMetadata } from "../domain/game-tree.js";

function createMoveElement(node) {
    const move = document.createElement("div");
    move.className = "move-clickable";
    move.dataset.id = String(node.id);
    move.textContent = node.moveSan ?? "";
    return move;
}

function createEmptyMove() {
    const empty = document.createElement("div");
    empty.className = "empty-move";
    return empty;
}

function appendLine(fragment, nodes, startNodeId) {
    let currentId = startNodeId;
    let activeRow = null;
    let movesContainer = null;

    while (currentId) {
        const node = nodes[currentId];
        if (!node) {
            break;
        }
        const { isBlackMove, moveNumber } = getMoveDisplayMetadata(node.fen);

        if (!isBlackMove) {
            activeRow = document.createElement("div");
            activeRow.className = "turn-row";
            const number = document.createElement("div");
            number.className = "turn-num";
            number.textContent = `${moveNumber}.`;
            movesContainer = document.createElement("div");
            movesContainer.className = "turn-moves";
            movesContainer.append(createMoveElement(node));
            activeRow.append(number, movesContainer);
            fragment.append(activeRow);
        } else {
            if (!activeRow) {
                activeRow = document.createElement("div");
                activeRow.className = "turn-row";
                const number = document.createElement("div");
                number.className = "turn-num";
                number.textContent = `${moveNumber}...`;
                movesContainer = document.createElement("div");
                movesContainer.className = "turn-moves";
                movesContainer.append(createEmptyMove());
                activeRow.append(number, movesContainer);
                fragment.append(activeRow);
            }
            movesContainer.append(createMoveElement(node));
        }

        const hasVariations = node.children.length > 1;
        if (isBlackMove || node.children.length === 0 || hasVariations) {
            if (!isBlackMove && movesContainer) {
                movesContainer.append(createEmptyMove());
            }
            activeRow = null;
            movesContainer = null;
        }

        if (hasVariations) {
            node.children.slice(1).forEach((variationId) => {
                const variation = document.createElement("div");
                variation.className = "variation-block";
                appendLine(variation, nodes, variationId);
                fragment.append(variation);
            });
        }
        currentId = node.children[0] ?? null;
    }
}

export function renderMoveTree(container, nodes, rootNodeId) {
    const fragment = document.createDocumentFragment();
    if (rootNodeId) {
        appendLine(fragment, nodes, rootNodeId);
    }
    container.replaceChildren(fragment);
}

export function renderActiveMove(container, nodeId, shouldScroll = false) {
    container.querySelectorAll(".move-clickable.active-node")
        .forEach((element) => element.classList.remove("active-node"));
    if (nodeId === null || nodeId === undefined) {
        return;
    }

    const active = [...container.querySelectorAll(".move-clickable")]
        .find((element) => element.dataset.id === String(nodeId));
    if (!active) {
        return;
    }
    active.classList.add("active-node");
    if (shouldScroll) {
        active.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }
}
