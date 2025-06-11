// ui/components/common/DateRangeSlider.kt
package com.example.snapsort.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSlider(
    dateRange: Pair<Date, Date>?,
    onDateRangeChanged: (Date, Date) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dateRange == null) return

    val (maxDate, minDate) = dateRange
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    var sliderRange by remember {
        mutableStateOf(0f..1f)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Plage de dates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Display selected dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val endDate = Date(
                    maxDate.time - ((maxDate.time - minDate.time) * sliderRange.endInclusive).toLong()
                )
                val startDate = Date(
                    maxDate.time - ((maxDate.time - minDate.time) * sliderRange.start).toLong()
                )

                Text(
                    text = "Fin: ${dateFormat.format(endDate)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Début: ${dateFormat.format(startDate)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            RangeSlider(
                value = sliderRange,
                onValueChange = { range ->
                    sliderRange = range
                    val endDate = Date(
                        maxDate.time - ((maxDate.time - minDate.time) * range.endInclusive).toLong()
                    )
                    val startDate = Date(
                        maxDate.time - ((maxDate.time - minDate.time) * range.start).toLong()
                    )
                    onDateRangeChanged(startDate, endDate)
                },
                valueRange = 0f..1f,
                steps = 50,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
