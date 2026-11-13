package ch.arka.projecthub.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.arka.projecthub.domain.CategoryStats
import ch.arka.projecthub.domain.Stats
import ch.arka.projecthub.ui.theme.Accent
import ch.arka.projecthub.ui.theme.Line
import ch.arka.projecthub.ui.theme.Muted
import ch.arka.projecthub.ui.theme.Surface1
import ch.arka.projecthub.ui.theme.Warn
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    stats: Stats,
    onBack: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(
            Modifier
                .padding(inner)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                }
                Text("Statistiche", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            StreakCard(stats)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CountCard("Questa settimana", stats.doneThisWeek, Modifier.weight(1f))
                CountCard("Questo mese", stats.doneThisMonth, Modifier.weight(1f))
            }

            CategoryCard(stats.byCategory)

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StreakCard(stats: Stats) {
    val (hint, hintColor) = when {
        stats.streak == 0 -> "Completa un task oggi per iniziare una serie." to Muted
        stats.streakAtRisk -> "Oggi ancora nessun task: completane uno per non interromperla." to Warn
        else -> "Oggi: ${taskCount(stats.doneToday)}." to Muted
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Serie attuale", color = Muted, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${stats.streak}", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Accent)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (stats.streak == 1) "giorno di fila" else "giorni di fila",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            Text(hint, color = hintColor, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CountCard(label: String, count: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Surface1),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(label, color = Muted, fontSize = 13.sp)
            Text("$count", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(if (count == 1) "task completato" else "task completati", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CategoryCard(categories: List<CategoryStats>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Per categoria", fontWeight = FontWeight.SemiBold)
            if (categories.isEmpty()) {
                Text("Ancora nessun progetto.", color = Muted, fontSize = 13.sp)
            }
            categories.forEach { c ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.category.label, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(
                            "${c.done}/${c.total} · ${(c.progress * 100).roundToInt()}%",
                            color = Muted,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { c.progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Color(c.category.colorHex),
                        trackColor = Line,
                    )
                }
            }
        }
    }
}

private fun taskCount(n: Int): String = if (n == 1) "1 task completato" else "$n task completati"
