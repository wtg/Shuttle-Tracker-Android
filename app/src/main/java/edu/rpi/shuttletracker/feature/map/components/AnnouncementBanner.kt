package edu.rpi.shuttletracker.feature.map.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.ui.theme.AnnouncementWarningColors
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.AnnouncementType
import edu.rpi.shuttletracker.feature.map.utils.MessageSegment
import edu.rpi.shuttletracker.feature.map.utils.isSafeHttpUrl
import edu.rpi.shuttletracker.feature.map.utils.parseMessageSegments

private const val MAX_COLLAPSED_BANNERS = 1
private val EXPANDED_MAX_HEIGHT = 260.dp

/**
 * Shows all active, unexpired announcements in a compact card stack over the map. Only the most
 * severe banner is shown by default; the rest are revealed with an explicit expand action so the
 * container never grows large enough to block the map underneath it.
 * */
@Composable
fun AnnouncementBanners(
    announcements: List<Announcement>,
    modifier: Modifier = Modifier,
) {
    if (announcements.isEmpty()) return

    var expanded by rememberSaveable(announcements.map { it.id }) { mutableStateOf(false) }
    val visibleAnnouncements = if (expanded) announcements else announcements.take(MAX_COLLAPSED_BANNERS)
    val hiddenCount = announcements.size - visibleAnnouncements.size

    Column(modifier = modifier.widthIn(max = 480.dp)) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = EXPANDED_MAX_HEIGHT)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            visibleAnnouncements.forEach { announcement ->
                AnnouncementCard(announcement)
            }
        }

        if (hiddenCount > 0 || expanded) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    if (expanded) {
                        stringResource(R.string.announcements_show_less)
                    } else {
                        stringResource(R.string.announcements_show_more, hiddenCount)
                    },
                )
            }
        }
    }
}

@Composable
private fun AnnouncementCard(announcement: Announcement) {
    val (containerColor, contentColor) = announcement.type.colors()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
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
        AnnouncementType.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        AnnouncementType.Warning -> warningColors()
        AnnouncementType.Info ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
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

@Preview(showBackground = true)
@Composable
private fun AnnouncementBannersPreview() {
    ShuttleTrackerTheme(dynamicColor = false) {
        AnnouncementBanners(
            announcements =
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
                ),
        )
    }
}
