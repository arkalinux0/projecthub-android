package ch.arka.projecthub.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.arka.projecthub.domain.Category
import ch.arka.projecthub.domain.ProjectUi
import ch.arka.projecthub.domain.overallProgress
import ch.arka.projecthub.domain.ratio
import ch.arka.projecthub.ui.components.DonutChart
import ch.arka.projecthub.ui.theme.Accent
import ch.arka.projecthub.ui.theme.Bad
import ch.arka.projecthub.ui.theme.Good
import ch.arka.projecthub.ui.theme.Muted
import ch.arka.projecthub.ui.theme.Surface1
import ch.arka.projecthub.ui.theme.Warn
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    projects: List<ProjectUi>,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onStats: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = Accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Icon(Icons.Default.Add, contentDescription = "Nuovo progetto") }
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = inner.calculateTopPadding() + 16.dp,
                bottom = inner.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "I miei progetti",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onStats) { Text("Statistiche") }
                }
            }
            item { OverallCard(projects) }

            if (projects.isEmpty()) {
                item {
                    Text(
                        "Nessun progetto. Tocca + e indica l'obiettivo finale e la scadenza: " +
                            "l'app lo scompone da sola in fasi e task.",
                        color = Muted,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            val byCategory = projects.groupBy { it.category }
            Category.entries.forEach { cat ->
                val list = byCategory[cat].orEmpty()
                if (list.isNotEmpty()) {
                    item(key = "h_${cat.name}") { CategoryHeader(cat, list) }
                    items(list, key = { it.project.id }) { p -> ProjectCard(p) { onOpen(p.project.id) } }
                }
            }
        }
    }
}

@Composable
private fun OverallCard(projects: List<ProjectUi>) {
    val progress = overallProgress(projects)
    val done = projects.sumOf { it.doneTasks }
    val total = projects.sumOf { it.totalTasks }
    val finished = projects.count { it.isComplete }
    Card(
        // senza fillMaxWidth la card si adatta al testo e a 0 progetti resta stretta
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            DonutChart(progress = progress, color = Accent, modifier = Modifier.size(120.dp), strokeWidth = 14.dp) {
                Text("${(progress * 100).roundToInt()}%", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(18.dp))
            Column {
                Text("Avanzamento totale", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("$done / $total task completati", color = Muted, fontSize = 13.sp)
                Text("${projects.size} progetti · $finished conclusi", color = Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CategoryHeader(cat: Category, list: List<ProjectUi>) {
    val color = Color(cat.colorHex)
    val pct = (ratio(list.sumOf { it.doneTasks }, list.sumOf { it.totalTasks }) * 100).roundToInt()
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(cat.label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("${list.size} · $pct%", color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun ProjectCard(p: ProjectUi, onClick: () -> Unit) {
    val color = Color(p.category.colorHex)
    val days = p.daysLeft()
    val (deadlineText, deadlineColor) = when {
        p.isComplete -> "Completato" to Good
        days < 0 -> "Scaduto da ${-days} g" to Bad
        days <= 7 -> "Mancano $days g" to Warn
        else -> "Mancano $days g" to Muted
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            DonutChart(progress = p.progress, color = color, modifier = Modifier.size(64.dp), strokeWidth = 8.dp) {
                Text("${(p.progress * 100).roundToInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.project.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                p.nextMilestone?.let {
                    Text("Ora: ${it.milestone.title}", color = Muted, fontSize = 12.sp, maxLines = 1)
                }
                Text(deadlineText, color = deadlineColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
