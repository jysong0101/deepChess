export async function confirmAndDeleteGame({
    confirmDelete,
    deleteRequest,
    setDeleting,
    onDeleted,
    onError,
}) {
    if (!confirmDelete()) {
        return false;
    }

    setDeleting(true);
    try {
        await deleteRequest();
        onDeleted();
        return true;
    } catch (error) {
        setDeleting(false);
        onError(error);
        return false;
    }
}
