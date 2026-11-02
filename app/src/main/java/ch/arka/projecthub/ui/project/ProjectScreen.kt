package ch.arka.projecthub.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.arka.projecthub.domain.MilestoneUi
import ch.arka.projecthub.domain.ProjectUi
import ch.arka.projecthub.ui.theme.Bad
import ch.arka.projecthub.ui.theme.Faint
import ch.arka.projecthub.ui.theme.Good
import ch.arka.projecthub.ui.theme.Line
import ch.arka.projecthub.ui.theme.Muted
import ch.arka.projecthub.ui.theme.Surface1
import ch.arka.projecthub.ui.theme.Warn
import ch.arka.projecthub.ui.components.DonutChart
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val DateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)

@Composable
fun ProjectScreen(
    project: ProjectUi?,
    onBack: () -> Unit,
    onToggleTask: (Long, Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        if (project == null) {
            // progetto eliminato o non ancora caricato
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("Progetto non trovato", color = Muted)
            }
            return@Scaffold
        }
        val color = Color(project.category.colorHex)
        val days = project.daysLeft()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = inner.calculateTopPadding() + 8.dp,
                bottom = inner.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                    Text(
                        project.category.label,
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = Bad)
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface1),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        DonutChart(
                            progress = project.progress,
                            color = color,
                            modifier = Modifier.size(170.dp),
                            strokeWidth = 18.dp,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${(project.progress * 100).roundToInt()}%",
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${project.doneTasks}/${project.totalTasks} task",
                                    color = Muted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(project.project.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (project.project.goal.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(project.project.goal, color = Muted, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        val (txt, c) = when {
                            project.isComplete -> "Obiettivo raggiunto 🎉" to Good
                            days < 0 -> "Scaduto il ${project.deadline.format(DateFmt)}" to Bad
                            days <= 7 -> "Scadenza ${project.deadline.format(DateFmt)} · $days g" to Warn
                            else -> "Scadenza ${project.deadline.format(DateFmt)} · $days g" to Muted
                        }
                        Text(txt, color = c, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            item { Text("Fasi", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(project.milestones, key = { it.milestone.id }) { m ->
                MilestoneCard(m, color, onToggleTask)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminare il progetto?") },
            text = { Text("Fasi e task verranno cancellati. L'operazione non si può annullare.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Elimina", color = Bad)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun MilestoneCard(m: MilestoneUi, color: Color, onToggleTask: (Long, Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface1),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(m.milestone.title, fontWeight = FontWeight.SemiBold)
                    Text("entro il ${m.due.format(DateFmt)}", color = Faint, fontSize = 12.sp)
                }
                Text(
                    "${m.done}/${m.total}",
                    color = if (m.isComplete) Good else Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { m.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = color,
                trackColor = Line,
            )
            Spacer(Modifier.height(4.dp))
            m.tasks.forEach { t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = t.done,
                        onCheckedChange = { onToggleTask(t.id, it) },
                        colors = CheckboxDefaults.colors(checkedColor = color),
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        t.title,
                        fontSize = 14.sp,
                        color = if (t.done) Faint else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (t.done) TextDecoration.LineThrough else null,
                    )
                }
            }
        }
    }
}
