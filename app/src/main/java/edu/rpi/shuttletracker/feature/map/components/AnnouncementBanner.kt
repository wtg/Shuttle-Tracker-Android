package edu.rpi.shuttletracker.feature.map.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.ui.theme.AnnouncementErrorColors
import edu.rpi.shuttletracker.core.ui.theme.AnnouncementWarningColors
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.core.util.isSafeHttpUrl
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.AnnouncementType
import edu.rpi.shuttletracker.feature.map.utils.MessageSegment
import edu.rpi.shuttletracker.feature.map.utils.parseMessageSegments
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A summary of the most severe active announcement, colored to match its severity. Tapping it
 * opens [AnnouncementSheet] with the full list; the chevron is a pure "there's more" cue, not a
 * dismiss control - dismissal lives per-card in the sheet, where it's a deliberate action rather
 * than an easy-to-mis-tap icon on a compact row.
 * */
@Composable
fun AnnouncementStrip(
    announcements: List<Announcement>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (announcements.isEmpty()) return

    val mostSevere = announcements.first()
    val extraCount = announcements.size - 1
    val (containerColor, contentColor) = mostSevere.type.colors()

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(mostSevere.type.iconRes()),
                contentDescription = stringResource(mostSevere.type.labelRes()),
                modifier = Modifier.size(20.dp),
                tint = contentColor,
            )

            Text(
                text = mostSevere.message.toPlainSummary(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            if (extraCount > 0) {
                Text(
                    text = stringResource(R.string.announcements_more_count, extraCount),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor,
            )
        }
    }
}

/**
 * Full-detail list of every active announcement, opened from [AnnouncementStrip].
 *
 * @param updatedAt when the list was last refreshed from the API; omitted while simulated.
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementSheet(
    show: Boolean,
    sheetState: SheetState,
    announcements: List<Announcement>,
    updatedAt: Instant?,
    onDismiss: () -> Unit,
) {
    if (!show) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.announcements_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (updatedAt != null) {
                    Text(
                        text = stringResource(R.string.announcements_updated_at, updatedAt.toLocalTimeText()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            announcements.forEach { announcement ->
                AnnouncementCard(announcement)
            }
        }
    }
}

@Composable
private fun AnnouncementCard(announcement: Announcement) {
    val (containerColor, contentColor) = announcement.type.colors()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(announcement.type.iconRes()),
                contentDescription = stringResource(announcement.type.labelRes()),
                modifier = Modifier.size(20.dp),
                tint = contentColor,
            )

            Column {
                Text(
                    text = stringResource(announcement.type.labelRes()),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
                AnnouncementMessage(message = announcement.message, color = contentColor)
            }
        }
    }
}

@Composable
private fun AnnouncementMessage(
    message: String,
    color: Color,
) {
    val annotated = remember(message, color) { message.toAnnotatedMessage(color) }

    Text(text = annotated, color = color, style = MaterialTheme.typography.bodyMedium)
}

private val LOCAL_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

private fun Instant.toLocalTimeText(): String = LOCAL_TIME_FORMATTER.withZone(ZoneId.systemDefault()).format(this)

private fun String.toPlainSummary(): String =
    parseMessageSegments(this).joinToString(separator = "") { segment ->
        when (segment) {
            is MessageSegment.PlainText -> segment.text
            is MessageSegment.Link -> segment.label
        }
    }

private fun String.toAnnotatedMessage(color: Color): AnnotatedString =
    buildAnnotatedString {
        parseMessageSegments(this@toAnnotatedMessage).forEach { segment ->
            when (segment) {
                is MessageSegment.PlainText -> append(segment.text)
                is MessageSegment.Link -> {
                    if (isSafeHttpUrl(segment.url)) {
                        withLink(
                            LinkAnnotation.Url(
                                url = segment.url,
                                styles =
                                    TextLinkStyles(
                                        style =
                                            SpanStyle(
                                                color = color,
                                                textDecoration = TextDecoration.Underline,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                    ),
                            ),
                        ) {
                            append(segment.label)
                        }
                    } else {
                        withStyle(SpanStyle(color = color)) {
                            append(segment.label)
                        }
                    }
                }
            }
        }
    }

private fun AnnouncementType.iconRes(): Int =
    when (this) {
        AnnouncementType.Error -> R.drawable.ic_error
        AnnouncementType.Warning -> R.drawable.ic_warning
        AnnouncementType.Info -> R.drawable.ic_info
    }

private fun AnnouncementType.labelRes(): Int =
    when (this) {
        AnnouncementType.Error -> R.string.announcement_type_error
        AnnouncementType.Warning -> R.string.announcement_type_warning
        AnnouncementType.Info -> R.string.announcement_type_info
    }

@Composable
private fun AnnouncementType.colors(): Pair<Color, Color> =
    when (this) {
        AnnouncementType.Error -> errorColors()
        AnnouncementType.Warning -> warningColors()
        AnnouncementType.Info ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

@Composable
private fun errorColors(): Pair<Color, Color> {
    val isDark = !MaterialTheme.colorScheme.background.isLight()
    return if (isDark) {
        AnnouncementErrorColors.DarkContainer to AnnouncementErrorColors.DarkOnContainer
    } else {
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
}

@Composable
private fun warningColors(): Pair<Color, Color> {
    val isDark = !MaterialTheme.colorScheme.background.isLight()
    return if (isDark) {
        AnnouncementWarningColors.DarkContainer to AnnouncementWarningColors.DarkOnContainer
    } else {
        AnnouncementWarningColors.LightContainer to AnnouncementWarningColors.LightOnContainer
    }
}

private fun Color.isLight(): Boolean = luminance() > 0.5f

private fun Color.luminance(): Float = (0.2126f * red + 0.7152f * green + 0.0722f * blue)

private fun previewAnnouncements() =
    listOf(
        Announcement(
            id = "snow-days",
            message = "Shuttles will not run or run with limited capacity until the weather improves.",
            type = AnnouncementType.Error,
            active = true,
        ),
        Announcement(
            id = "snow-delay",
            message = "Due to snowy roads, expect delays on West route shuttles.",
            type = AnnouncementType.Warning,
            active = true,
        ),
        Announcement(
            id = "chasan-weekday-hours",
            message =
                "Chasan stop is only available M-F 7am-5:30pm. " +
                    "[View RPI Shuttle Info](https://administration.rpi.edu/parking-transportation/rensselaer-shuttle)",
            type = AnnouncementType.Info,
            active = true,
        ),
    )

@Preview(showBackground = true)
@Composable
private fun AnnouncementStripPreview() {
    ShuttleTrackerTheme(dynamicColor = false) {
        AnnouncementStrip(announcements = previewAnnouncements(), onClick = {})
    }
}
