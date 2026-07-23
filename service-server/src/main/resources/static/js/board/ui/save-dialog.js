export function createSaveDialog({
    saveRequest,
    render,
    onSaved,
}) {
    let gameId = null;
    let saving = false;

    function open(id) {
        gameId = id;
        render.open(`저장된 기보 #${id}`);
    }

    function cancel() {
        if (saving || gameId === null) {
            return false;
        }
        gameId = null;
        render.close();
        return true;
    }

    async function confirm(rawTitle) {
        if (saving || gameId === null) {
            return false;
        }

        const title = rawTitle.trim();
        if (!title) {
            render.setError("기보 제목을 입력해 주세요.");
            render.focusInput();
            return false;
        }

        saving = true;
        render.setSaving(true);
        render.setError("");
        try {
            const response = await saveRequest(gameId, title);
            saving = false;
            render.setSaving(false);
            gameId = null;
            render.close();
            onSaved(response);
            return true;
        } catch (error) {
            render.setError(error.message);
            render.focusInput();
            return false;
        } finally {
            if (saving) {
                saving = false;
                render.setSaving(false);
            }
        }
    }

    return {
        open,
        cancel,
        confirm,
        isSaving: () => saving,
    };
}
