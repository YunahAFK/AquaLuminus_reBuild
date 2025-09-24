package com.example.aqualuminus_rebuild.ui.screens.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TimePicker(
    selectedHour: Int,
    selectedMinute: Int,
    selectedAmPm: Int,
    onHourChanged: (Int) -> Unit,
    onMinuteChanged: (Int) -> Unit,
    onAmPmChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // display current selected time
            val displayHour = if (selectedHour == 0) 12 else selectedHour
            val amPmText = if (selectedAmPm == 0) "AM" else "PM"
            Text(
                text = String.format("%02d:%02d %s", displayHour, selectedMinute, amPmText),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Hour Picker
                NumberPicker(
                    value = selectedHour,
                    onValueChange = onHourChanged,
                    range = 1..12,
                    modifier = Modifier.weight(1f),
                    label = "Hour"
                )

                // Separator
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Minute Picker
                NumberPicker(
                    value = selectedMinute,
                    onValueChange = onMinuteChanged,
                    range = 0..59,
                    modifier = Modifier.weight(1f),
                    formatValue = { "%02d".format(it) },
                    label = "Minute"
                )

                // AM/PM Picker
                AmPmPicker(
                    value = selectedAmPm,
                    onValueChange = onAmPmChanged,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    formatValue: (Int) -> String = { it.toString() },
    label: String = ""
) {
    val listState = rememberLazyListState()
    val values = range.toList()
    val itemHeight = 48.dp
    val visibleItemsCount = 3

    // find the index of current value
    val currentIndex = values.indexOf(value).coerceAtLeast(0)

    // track if we're programmatically scrolling
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (listState.firstVisibleItemIndex != currentIndex) {
            isProgrammaticScroll = true
            listState.animateScrollToItem(
                index = currentIndex,
                scrollOffset = 0
            )
            isProgrammaticScroll = false
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .height(itemHeight * visibleItemsCount)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                // add top spacer for proper centering
                item {
                    Spacer(modifier = Modifier.height(itemHeight))
                }

                itemsIndexed(values) { index, item ->
                    val isSelected = item == value
                    val alpha = if (isSelected) 1f else 0.6f

                    Text(
                        text = formatValue(item),
                        style = if (isSelected) {
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clickable { onValueChange(item) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(itemHeight))
                }
            }

            // Selection Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
            )
        }
    }
}

@Composable
fun AmPmPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("AM", "PM")
    val listState = rememberLazyListState()
    val itemHeight = 48.dp

    // track if we're programmatically scrolling
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (listState.firstVisibleItemIndex != value) {
            isProgrammaticScroll = true
            listState.animateScrollToItem(value)
            isProgrammaticScroll = false
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Period",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .height(itemHeight * 3)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Spacer(modifier = Modifier.height(itemHeight))
                }

                itemsIndexed(options) { index, item ->
                    val isSelected = index == value
                    val alpha = if (isSelected) 1f else 0.6f

                    Text(
                        text = item,
                        style = if (isSelected) {
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clickable { onValueChange(index) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(itemHeight))
                }
            }

            // Selection Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTimePicker() {
    MaterialTheme {
        TimePicker(
            selectedHour = 9,
            selectedMinute = 30,
            selectedAmPm = 0,
            onHourChanged = {},
            onMinuteChanged = {},
            onAmPmChanged = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewNumberPicker() {
    MaterialTheme {
        NumberPicker(
            value = 5,
            onValueChange = {},
            range = 1..12,
            label = "Hour"
        )
    }
}
