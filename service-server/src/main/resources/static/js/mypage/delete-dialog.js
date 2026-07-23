export function createDeleteDialog({
    deleteRequest,
    render,
    reload,
}) {
    let target = null;
    let trigger = null;
    let deleting = false;

    function open(game, sourceElement) {
        target = game;
        trigger = sourceElement;
        render.open(game.title);
    }

    function cancel() {
        if (deleting || !target) {
            return false;
        }
        const returnFocusTo = trigger;
        target = null;
        trigger = null;
        render.close(returnFocusTo);
        return true;
    }

    async function confirm() {
        if (!target || deleting) {
            return false;
        }

        deleting = true;
        render.setDeleting(true);
        render.setError("");
        try {
            await deleteRequest(target.gameId);
            target = null;
            trigger = null;
            render.close();
            await reload();
            return true;
        } catch (error) {
            render.setError(error.message);
            return false;
        } finally {
            deleting = false;
            render.setDeleting(false);
        }
    }

    return {
        open,
        cancel,
        confirm,
        isDeleting: () => deleting,
    };
}
