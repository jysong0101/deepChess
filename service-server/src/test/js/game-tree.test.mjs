import assert from "node:assert/strict";
import test from "node:test";

import {
    addNodeToTree,
    buildTreeFromApi,
    createGameNode,
    findExistingChildId,
} from "../../main/resources/static/js/board/domain/game-tree.js";
import {
    findFirstNodeId,
    findLastMainLineNodeId,
    findNextNodeId,
    findPreviousNodeId,
} from "../../main/resources/static/js/board/domain/navigation.js";

function node(id, parentId, moveSan) {
    return createGameNode({
        id,
        parentId,
        moveSan,
        fen: "8/8/8/8/8/8/8/8 w - - 0 1",
    });
}

test("루트 노드를 생성한다", () => {
    const result = addNodeToTree({}, null, node("root", null, "e4"));
    assert.equal(result.rootNodeId, "root");
    assert.equal(result.nodes.root.moveSan, "e4");
});

test("자식과 부모 ID를 연결한다", () => {
    const rootResult = addNodeToTree({}, null, node("root", null, "e4"));
    const childResult = addNodeToTree(
        rootResult.nodes,
        rootResult.rootNodeId,
        node("child", "root", "e5"),
    );
    assert.deepEqual(childResult.nodes.root.children, ["child"]);
    assert.equal(childResult.nodes.child.parentId, "root");
});

test("동일한 수의 기존 자식을 찾고 중복 추가하지 않는다", () => {
    const nodes = {
        root: { ...node("root", null, "e4"), children: ["child"] },
        child: node("child", "root", "e5"),
    };
    assert.equal(findExistingChildId(nodes, "root", "e5"), "child");
    const result = addNodeToTree(nodes, "root", node("duplicate", "root", "e5"));
    assert.equal(result.added, false);
    assert.equal(result.nodeId, "child");
    assert.equal(result.nodes.duplicate, undefined);
});

test("다른 수는 variation으로 추가한다", () => {
    const nodes = {
        root: { ...node("root", null, "e4"), children: ["main"] },
        main: node("main", "root", "e5"),
    };
    const result = addNodeToTree(nodes, "root", node("variation", "root", "c5"));
    assert.deepEqual(result.nodes.root.children, ["main", "variation"]);
});

test("API 응답으로 트리를 만들고 미분석 노드를 큐 대상으로 반환한다", () => {
    const result = buildTreeFromApi([
        { positionId: 1, parentPositionId: null, fen: "fen1", moveSan: "e4", engineScore: "0.2" },
        { positionId: 2, parentPositionId: 1, fen: "fen2", moveSan: "e5", engineScore: null },
    ]);
    assert.equal(result.rootNodeId, 1);
    assert.deepEqual(result.nodes[1].children, [2]);
    assert.deepEqual(result.analysisTasks.map((task) => task.nodeId), [2]);
});

test("처음, 이전, 다음, 마지막 메인 라인을 탐색한다", () => {
    const nodes = {
        root: { ...node("root", null, "e4"), children: ["main", "variation"] },
        main: { ...node("main", "root", "e5"), children: ["last"] },
        variation: node("variation", "root", "c5"),
        last: node("last", "main", "Nf3"),
    };
    assert.equal(findFirstNodeId("root"), "root");
    assert.equal(findPreviousNodeId(nodes, "main"), "root");
    assert.equal(findNextNodeId(nodes, "root", null), "root");
    assert.equal(findNextNodeId(nodes, "root", "root"), "main");
    assert.equal(findLastMainLineNodeId(nodes, "root"), "last");
});
