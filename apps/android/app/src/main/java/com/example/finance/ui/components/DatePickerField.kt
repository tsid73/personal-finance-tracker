package com.example.finance.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.finance.util.DateUtils
import java.time.LocalDate

@Composable
fun DatePickerField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    monthKey: String? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null
) {
    val context = LocalContext.current
    val selectedDate = remember(value) { DateUtils.parseDateOrNull(value) ?: LocalDate.now() }

    val dialog = remember(context, value, minDate, maxDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onValueChange(DateUtils.formatDate(LocalDate.of(year, month + 1, dayOfMonth)))
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).apply {
            minDate?.let {
                datePicker.minDate = it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            maxDate?.let {
                datePicker.maxDate = it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Default.CalendarToday, contentDescription = label)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { dialog.show() }
        )
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(onClick = { onValueChange(DateUtils.today()) }, label = { Text("Today") })
            AssistChip(
                onClick = { onValueChange(DateUtils.formatDate(LocalDate.now().minusDays(1))) },
                label = { Text("Yesterday") }
            )
        }
        monthKey?.let { currentMonthKey ->
            Row(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { onValueChange(DateUtils.startOfMonth(currentMonthKey)) },
                    label = { Text("Start of month") }
                )
                AssistChip(
                    onClick = { onValueChange(DateUtils.endOfMonth(currentMonthKey)) },
                    label = { Text("End of month") }
                )
            }
        }
    }
}
