package ch.arka.projecthub.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlannerTest {

    // Data fissa: i test non devono dipendere da LocalDate.now().
    private val start: LocalDate = LocalDate.of(2026, 1, 15)

    private fun planIn(days: Long, category: Category = Category.SOFTWARE): List<PlannedMilestone> =
        Planner.plan(category, start, start.plusDays(days))

    @Test
    fun deadlineToday_singleMilestoneDueToday() {
        val plan = planIn(0)
        assertEquals(1, plan.size)
        assertEquals(start, plan.single().due)
    }

    @Test
    fun deadlineInOneDay_singleMilestoneOnDeadline() {
        val plan = planIn(1)
        assertEquals(1, plan.size)
        assertEquals(start.plusDays(1), plan.single().due)
    }

    @Test
    fun deadlineIn29Days_singleMilestoneOnDeadline() {
        val plan = planIn(29)
        assertEquals(1, plan.size)
        assertEquals(start.plusDays(29), plan.single().due)
    }

    @Test
    fun deadlineInExactly30Days_singleMilestone_31DaysGivesTwo() {
        assertEquals(1, planIn(30).size)
        assertEquals(2, planIn(31).size)
    }

    @Test
    fun milestoneCount_roundsUpEvery30Days_clampedBetween1And12() {
        val expected = mapOf(
            0L to 1, 1L to 1, 29L to 1, 30L to 1,
            31L to 2, 60L to 2, 61L to 3,
            330L to 11, 331L to 12, 360L to 12,
            361L to 12, 3650L to 12,
        )
        expected.forEach { (days, count) ->
            assertEquals("milestone per $days giorni", count, Planner.milestoneCount(start, start.plusDays(days)))
        }
    }

    @Test
    fun moreThanTwelveMonths_cappedAtMaxMilestones() {
        // 13 mesi, 2 anni, 10 anni: sempre e solo 12 milestone
        listOf(395L, 730L, 3650L).forEach { days ->
            assertEquals("milestone per $days giorni", Planner.MAX_MILESTONES, planIn(days).size)
        }
    }

    @Test
    fun singleMilestone_usesLastPhaseOfCategory() {
        // Comportamento attuale per scadenze entro 30 giorni: resta solo l'ultima fase.
        // È la voce aperta in "Decisioni in sospeso" (TASKS.md): se Arka decide
        // diversamente, questo test va aggiornato insieme al Planner.
        Category.entries.forEach { category ->
            val single = planIn(10, category).single()
            val last = category.phases.last()
            assertEquals(category.name, last.title, single.title)
            assertEquals(category.name, last.tasks, single.tasks)
        }
    }

    @Test
    fun fewerMilestonesThanPhases_keepsFirstAndLastPhase() {
        // Con 2–4 milestone (31–120 giorni) alcune fasi intermedie vengono saltate.
        // Comportamento attuale, collegato alla stessa decisione in sospeso.
        val p = Category.SOFTWARE.phases.map { it.title }
        assertEquals(listOf(p[0], p[4]), planIn(60).map { it.title })
        assertEquals(listOf(p[0], p[2], p[4]), planIn(90).map { it.title })
        assertEquals(listOf(p[0], p[1], p[3], p[4]), planIn(120).map { it.title })
    }

    @Test
    fun asManyMilestonesAsPhases_onePhaseEachWithoutSuffix() {
        // 150 giorni -> 5 milestone: esattamente le 5 fasi del modello, in ordine
        Category.entries.forEach { category ->
            assertEquals(category.name, category.phases.map { it.title }, planIn(150, category).map { it.title })
        }
    }

    @Test
    fun farDeadline_repeatsPhasesWithPartSuffix() {
        // 2 anni -> 12 milestone su 5 fasi: le fasi si ripetono con "parte N"
        val p = Category.SOFTWARE.phases.map { it.title }
        val expected = listOf(
            part(p[0], 1), part(p[0], 2),
            part(p[1], 1), part(p[1], 2), part(p[1], 3),
            part(p[2], 1), part(p[2], 2),
            part(p[3], 1), part(p[3], 2), part(p[3], 3),
            part(p[4], 1), part(p[4], 2),
        )
        assertEquals(expected, planIn(730).map { it.title })
        assertEquals("Analisi – parte 1", planIn(730).first().title)
    }

    @Test
    fun dueDates_evenlySpaced() {
        // 360 giorni -> 12 milestone ogni 30 giorni esatti
        assertEquals((1..12).map { start.plusDays(30L * it) }, planIn(360).map { it.due })
        // 100 giorni -> 4 milestone ogni 25 giorni
        assertEquals((1..4).map { start.plusDays(25L * it) }, planIn(100).map { it.due })
    }

    @Test
    fun invariants_holdForEveryCategoryAndSpan() {
        // copre ogni numero di milestone da 1 a 12, con i bordi dei multipli di 30
        val spans = listOf(
            0L, 1L, 7L, 29L, 30L, 31L, 45L, 59L, 60L, 61L, 90L, 100L, 120L, 121L,
            149L, 150L, 151L, 180L, 200L, 240L, 270L, 300L, 330L, 359L, 360L, 361L,
            400L, 1000L, 3650L,
        )
        Category.entries.forEach { category ->
            spans.forEach { days ->
                val deadline = start.plusDays(days)
                val plan = Planner.plan(category, start, deadline)
                val label = "${category.name}, $days giorni"

                // numero di milestone coerente con milestoneCount
                assertEquals(label, Planner.milestoneCount(start, deadline), plan.size)
                // l'ultima scadenza coincide sempre con la scadenza del progetto
                assertEquals(label, deadline, plan.last().due)
                // scadenze dentro [inizio, scadenza] e strettamente crescenti
                assertTrue(label, plan.all { !it.due.isBefore(start) && !it.due.isAfter(deadline) })
                assertTrue(label, plan.zipWithNext().all { (a, b) -> a.due.isBefore(b.due) })

                // ogni milestone eredita i task della sua fase, e le fasi restano in ordine
                val phaseIndexes = plan.map { m ->
                    val idx = category.phases.indexOfFirst { it.title == phaseTitleOf(m) }
                    assertTrue("$label: fase sconosciuta '${m.title}'", idx >= 0)
                    assertEquals(label, category.phases[idx].tasks, m.tasks)
                    idx
                }
                assertEquals(label, phaseIndexes.sorted(), phaseIndexes)

                // "parte N" solo sulle fasi ripetute, numerata da 1 nell'ordine
                plan.groupBy { phaseTitleOf(it) }.forEach { (title, group) ->
                    val expectedTitles =
                        if (group.size == 1) listOf(title) else (1..group.size).map { part(title, it) }
                    assertEquals(label, expectedTitles, group.map { it.title })
                }

                // con almeno tanti milestone quante sono le fasi, nessuna fase viene saltata
                if (plan.size >= category.phases.size) {
                    assertEquals(label, category.phases.indices.toList(), phaseIndexes.distinct())
                }
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun deadlineBeforeStart_throws() {
        Planner.plan(Category.SOFTWARE, start, start.minusDays(1))
    }

    /** Titolo della fase senza l'eventuale suffisso " – parte N". */
    private fun phaseTitleOf(m: PlannedMilestone): String = m.title.substringBefore(PART_SEPARATOR)

    private fun part(title: String, n: Int): String = "$title$PART_SEPARATOR$n"

    private companion object {
        // stesso separatore usato da Planner.plan() (trattino medio, U+2013)
        const val PART_SEPARATOR = " – parte "
    }
}
