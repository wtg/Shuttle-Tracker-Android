package edu.rpi.shuttletracker.feature.map.utils

/** One piece of an announcement message, produced by [parseMessageSegments]: plain text or a link. */
sealed interface MessageSegment {
    data class PlainText(
        val text: String,
    ) : MessageSegment

    data class Link(
        val label: String,
        val url: String,
    ) : MessageSegment
}

private val MARKDOWN_LINK_REGEX = Regex("""\[([^\[\]]*)]\(([^()\s]+)\)""")

/**
 * Splits a message on `[label](url)` Markdown links, leaving everything else as plain text.
 * Malformed brackets/parens (unmatched, nested, empty) simply fail to match and pass through as text.
 * */
fun parseMessageSegments(message: String): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    var lastIndex = 0

    for (match in MARKDOWN_LINK_REGEX.findAll(message)) {
        if (match.range.first > lastIndex) {
            segments += MessageSegment.PlainText(message.substring(lastIndex, match.range.first))
        }

        val (label, url) = match.destructured
        segments += MessageSegment.Link(label.ifEmpty { url }, url)
        lastIndex = match.range.last + 1
    }

    if (lastIndex < message.length) {
        segments += MessageSegment.PlainText(message.substring(lastIndex))
    }

    return segments
}
