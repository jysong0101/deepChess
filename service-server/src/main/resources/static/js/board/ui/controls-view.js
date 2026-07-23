export function setVisible(element, visible) {
    element.style.display = visible ? "block" : "none";
}

export function renderSaveButton(button, status) {
    setVisible(button, status !== "hidden");
    button.disabled = status === "saved" || status === "saving";
    button.classList.toggle("is-saved", status === "saved");
    button.textContent = status === "saved"
        ? "✅ 저장 완료"
        : status === "saving"
            ? "저장 중..."
            : "💾 기보 저장하기";
}

export function renderResultMessage(element, text, type = "normal") {
    element.className = type === "success"
        ? "success-message"
        : type === "library"
            ? "library-message"
            : "";
    element.textContent = text;
}

export function renderGameResult(element, result) {
    element.textContent = result ? `결과: ${result}` : "";
}
