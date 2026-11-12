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
    fun deadlineToday_threeMilestonesAllDueToday() {
        // scadenza oggi: restano 3 fasi, tutte compresse sullo stesso giorno
        assertEquals(listOf(start, start, start), planIn(0).map { it.due })
    }

    @Test
    fun deadlineInOneDay_threeMilestonesCompressedIntoOneDay() {
        assertEquals(listOf(start, start, start.plusDays(1)), planIn(1).map { it.due })
    }

    @Test
    fun deadlineInTwoOrThreeDays_oneMilestonePerDay() {
        assertEquals((0L..2L).map { start.plusDays(it) }, planIn(2).map { it.due })
        assertEquals((1L..3L).map { start.plusDays(it) }, planIn(3).map { it.due })
    }

    @Test
    fun deadlineIn29Days_threeMilestonesLastOnDeadline() {
        val plan = planIn(29)
        assertEquals(listOf(9L, 19L, 29L).map { start.plusDays(it) }, plan.map { it.due })
    }

    @Test
    fun deadlineInExactly30Days_threeMilestonesEvery10Days() {
        assertEquals(listOf(10L, 20L, 30L).map { start.plusDays(it) }, planIn(30).map { it.due })
    }

    @Test
    fun milestoneCount_roundsUpEvery30Days_clampedBetween3And12() {
        val expected = mapOf(
            0L to 3, 1L to 3, 29L to 3, 30L to 3,
            31L to 3, 60L to 3, 61L to 3, 90L to 3,
            91L to 4, 120L to 4, 121L to 5,
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
    fun shortDeadline_usesFirstMiddleAndLastPhaseOfCategory() {
        // Decisione di Arka (2026-11-02): mai meno di 3 fasi, nemmeno con scadenza
        // vicinissima. Un progetto breve riceve prima fase, fase centrale e ultima,
        // non più la sola fase finale.
        Category.entries.forEach { category ->
            listOf(0L, 1L, 10L, 30L).forEach { days ->
                val plan = planIn(days, category)
                val expected = listOf(category.phases[0], category.phases[2], category.phases[4])
                assertEquals("${category.name}, $days giorni", expected.map { it.title }, plan.map { it.title })
                assertEquals("${category.name}, $days giorni", expected.map { it.tasks }, plan.map { it.tasks })
            }
        }
    }

    @Test
    fun fewerMilestonesThanPhases_keepsFirstMiddleAndLastPhase() {
        // Con meno milestone che fasi (fino a 120 giorni) qualche fase viene saltata,
        // ma mai fino a lasciare solo la prima e l'ultima: 3 milestone = fasi 1, 3, 5;
        // 4 milestone = fasi 1, 2, 4, 5.
        val p = Category.SOFTWARE.phases.map { it.title }
        assertEquals(listOf(p[0], p[2], p[4]), planIn(60).map { it.title })
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
        // copre ogni numero di milestone da 3 a 12, con i bordi dei multipli di 30
        val spans = listOf(
            0L, 1L, 2L, 3L, 7L, 29L, 30L, 31L, 45L, 59L, 60L, 61L, 90L, 91L, 100L, 120L, 121L,
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
                // mai meno di 3 e mai più di 12 milestone
                assertTrue(label, plan.size in Planner.MIN_MILESTONES..Planner.MAX_MILESTONES)
                // scadenze dentro [inizio, scadenza], mai all'indietro; strettamente
                // crescenti appena c'è almeno un giorno per milestone
                assertTrue(label, plan.all { !it.due.isBefore(start) && !it.due.isAfter(deadline) })
                assertTrue(label, plan.zipWithNext().all { (a, b) -> !b.due.isBefore(a.due) })
                if (days >= plan.size) {
                    assertTrue(label, plan.zipWithNext().all { (a, b) -> a.due.isBefore(b.due) })
                }

                // ogni milestone eredita i task della sua fase, e le fasi restano in ordine
                val phaseIndexes = plan.map { m ->
                    val idx = category.phases.indexOfFirst { it.title == phaseTitleOf(m) }
                    assertTrue("$label: fase sconosciuta '${m.title}'", idx >= 0)
                    assertEquals(label, category.phases[idx].tasks, m.tasks)
                    idx
                }
                assertEquals(label, phaseIndexes.sorted(), phaseIndexes)
                // sempre prima fase, almeno una fase intermedia e ultima fase
                assertEquals(label, 0, phaseIndexes.first())
                assertEquals(label, category.phases.lastIndex, phaseIndexes.last())
                assertTrue(label, phaseIndexes.any { it in 1 until category.phases.lastIndex })

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
