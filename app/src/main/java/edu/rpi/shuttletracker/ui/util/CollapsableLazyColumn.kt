package edu.rpi.shuttletracker.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun <T : Any?> CollapsableList(
    modifier: Modifier = Modifier,
    key: ((item: T) -> Any)? = null,
    header: @Composable (List<T>) -> Unit,
    body: @Composable (T) -> Unit,
    allContent: List<List<T>>,
) {
    val expandedState = remember { allContent.map { false }.toMutableStateList() }

    LazyColumn(
        modifier = modifier,
    ) {
        allContent.forEachIndexed { i, content ->
            val expanded = expandedState[i]

            item { HorizontalDivider() }

            item {
                Row(
                    modifier =
                        Modifier
                            .clickable { expandedState[i] = !expanded }
                            .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector =
                            if (expandedState[i]) {
                                Icons.Outlined.ArrowDropDown
                            } else {
                                Icons.AutoMirrored.Outlined.ArrowRight
                            },
                        contentDescription = "Expanded State",
                    )

                    header(content)
                }
            }

            if (expanded) {
                items(
                    content,
                    key = key,
                ) {
                    body(it)
                }
            }
        }
    }
}
