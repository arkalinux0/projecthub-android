package ch.arka.projecthub.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.arka.projecthub.domain.Category
import ch.arka.projecthub.domain.Planner
import ch.arka.projecthub.ui.theme.Faint
import ch.arka.projecthub.ui.theme.Muted
import ch.arka.projecthub.ui.theme.Surface1
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AddProjectScreen(
    onBack: () -> Unit,
    onCreate: (name: String, goal: String, category: Category, deadline: LocalDate) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.SOFTWARE) }
    var deadline by remember { mutableStateOf<LocalDate?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val valid = name.isNotBlank() && deadline != null && !deadline!!.isBefore(today)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(
            Modifier
                .padding(inner)
                // Con edge-to-edge la tastiera non ridimensiona più la finestra: senza
                // imePadding l'area scorrevole finisce sotto la tastiera e lo scroll si
                // ferma con "Crea progetto" ancora coperto. consumeWindowInsets evita di
                // contare due volte la barra di navigazione, già inclusa in `inner`.
                .consumeWindowInsets(inner)
                .imePadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                }
                Text("Nuovo progetto", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome del progetto") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                label = { Text("Obiettivo finale (facoltativo)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Text("Categoria", color = Muted, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { c ->
                    FilterChip(
                        selected = c == category,
                        onClick = { category = c },
                        label = { Text(c.label) },
                    )
                }
            }

            Text("Scadenza", color = Muted, fontSize = 13.sp)
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(deadline?.format(DateFmt) ?: "Scegli la data")
            }

            val d = deadline
            if (d != null && !d.isBefore(today)) {
                Text("Piano generato", fontWeight = FontWeight.Bold)
                val plan = remember(category, d) { Planner.plan(category, today, d) }
                plan.forEachIndexed { i, m ->
                    Row {
                        Text("${i + 1}.", color = Faint, modifier = Modifier.width(24.dp))
                        Column {
                            Text(m.title, fontWeight = FontWeight.Medium)
                            Text("entro il ${m.due.format(DateFmt)} · ${m.tasks.size} task", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
                Text(
                    "Fasi e task derivano da un modello per categoria: puoi spuntarli o eliminare il progetto, " +
                        "ma non modificarli singolarmente in questa versione.",
                    color = Faint,
                    fontSize = 12.sp,
                )
            } else if (d != null) {
                Text("La scadenza deve essere oggi o nel futuro.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Button(
                onClick = { deadline?.let { onCreate(name, goal, category, it) } },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(),
            ) { Text("Crea progetto", fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (deadline ?: today.plusMonths(1))
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        deadline = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Annulla") } },
        ) { DatePicker(state = state) }
    }
}
