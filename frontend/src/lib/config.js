const wsProtocol = window.location.protocol === "https:" ? "wss:" : "ws:"
const wsHost = window.location.hostname || "127.0.0.1"
const danmuWSBaseUrl = process.env.VUE_APP_DANMU_WS_BASE_URL
    || `${wsProtocol}//${wsHost}:9101/ws/bulletScreen/`

const VIEWER_ID_KEY = "TIANMU_VIEWER_ID"

function getGuestViewerId() {
    let viewerId = localStorage.getItem(VIEWER_ID_KEY)
    if (!viewerId) {
        viewerId = typeof crypto.randomUUID === "function"
            ? crypto.randomUUID()
            : `${Date.now()}-${Math.random().toString(36).slice(2)}`
        localStorage.setItem(VIEWER_ID_KEY, viewerId)
    }
    return viewerId
}

export function getDanmuWSUrl(videoId, userId) {
    const viewerId = userId ? `user:${userId}` : `guest:${getGuestViewerId()}`
    return `${danmuWSBaseUrl}${videoId}?viewerId=${encodeURIComponent(viewerId)}`
}

export function getSearchHistoryKey() {
    return "TIANMU_searchHistories"
}

export const TIANMU_USER = "TIANMU_USER"
export const TIANMU_LOGIN = "TIANMU_LOGIN"
