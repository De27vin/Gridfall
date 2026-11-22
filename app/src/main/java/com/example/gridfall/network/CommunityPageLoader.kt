package com.example.gridfall.network

/**
 * /mine intentionally includes immutable publication history. Filter for the active Android
 * view without inventing cursors or dropping live rows. History-only pages are advanced until
 * a visible page/end; checkpoint them so an interrupted scan can retry without starting over.
 */
internal suspend fun loadVisibleCommunityPage(
    cursor: String?,
    publishedOnly: Boolean,
    excludedCodes: Set<String> = emptySet(),
    onSkippedPage: (String) -> Unit = {},
    fetch: suspend (String?) -> CommunityPage
): CommunityPage {
    val visited = mutableSetOf<String>()
    cursor?.let(visited::add)
    var next = cursor
    while (true) {
        val page = fetch(next)
        val after = page.nextCursor
        if (after != null && !visited.add(after)) {
            throw CommunityProtocolException("Community Maps returned a repeated page. Please refresh.")
        }
        val visible = page.items.filter { (!publishedOnly || it.published) && it.code !in excludedCodes }
        if (visible.isNotEmpty() || after == null) return CommunityPage(visible, after)
        onSkippedPage(after)
        next = after
    }
}
