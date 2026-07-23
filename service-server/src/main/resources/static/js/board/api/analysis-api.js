export async function analyzePosition(data) {
    const body = new URLSearchParams();
    Object.entries(data).forEach(([key, value]) => {
        if (value !== null && value !== undefined) {
            body.set(key, String(value));
        }
    });

    const response = await fetch("/api/analysis", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
        body,
    });

    if (!response.ok) {
        throw new Error(`분석 요청 실패 (${response.status})`);
    }
    return response.json();
}
