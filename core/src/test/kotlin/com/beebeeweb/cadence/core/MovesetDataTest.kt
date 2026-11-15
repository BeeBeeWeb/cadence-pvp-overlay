package com.beebeeweb.cadence.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class MovesetDataTest {

    // Fixture data in Cadence's format; not real movesets.
    private val fixture = """
        {
          "version": 1,
          "fastMoves": {"BUBBLE": {"name": "Bubble", "gain": 11, "turns": 3}},
          "chargedMoves": {
            "ICE_BEAM": {"name": "Ice Beam", "cost": 55},
            "FIXTURE_60": {"name": "Fixture 60", "cost": 60}
          },
          "species": [
            {"id": "azumarill", "name": "Azumarill",
             "movesets": [{"fast": "BUBBLE", "charged": ["ICE_BEAM", "FIXTURE_60"]}]},
            {"id": "marowak_alolan", "name": "Alolan Marowak", "aliases": ["Marowak (Alolan)"],
             "movesets": [{"fast": "BUBBLE", "charged": ["FIXTURE_60"]}]}
          ]
        }
    """.trimIndent()

    @Test
    fun catalogListsEveryMoveSortedByName() {
        val catalog = MovesetData.parseCatalog(fixture)
        assertEquals(listOf("Bubble"), catalog.fast.map { it.name })
        assertEquals(listOf("Fixture 60", "Ice Beam"), catalog.charged.map { it.name })
    }

    @Test
    fun `parses species, aliases and movesets`() {
        val species = MovesetData.parse(fixture)
        assertEquals(listOf("Azumarill", "Alolan Marowak"), species.map { it.name })
        assertEquals(listOf("Marowak (Alolan)"), species[1].aliases)
        assertEquals(emptyList<String>(), species[0].aliases)
        assertEquals(listOf("Ice Beam", "Fixture 60"), species[0].movesets.single().charged.map { it.name })
    }

    @Test
    fun `parsed data feeds the repository and gives the expected counts`() {
        val repository = MovesetRepository(MovesetData.parse(fixture))
        val first = repository.topMovesets("Azumarill").single().counts.first()
        assertEquals(5, first.fastMoves)
        assertEquals(15, first.turns)
        assertNotNull(repository.find("marowak (alolan)"))
    }

    @Test
    fun `unsupported data version is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            MovesetData.parse(fixture.replace("\"version\": 1", "\"version\": 2"))
        }
    }

    @Test
    fun `unknown move ids are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            MovesetData.parse(fixture.replace("\"charged\": [\"FIXTURE_60\"]", "\"charged\": [\"NOT_A_MOVE\"]"))
        }
    }

    // ---- the real data file (skipped if it is not next to the module) ----

    private fun realData(): List<Species> {
        val file = listOf("../data/greatleague.json", "data/greatleague.json").map(::File).firstOrNull { it.exists() }
        assumeTrue("data/greatleague.json not found", file != null)
        return MovesetData.parse(file!!.readText())
    }

    @Test
    fun `real data has a sensible number of species and movesets`() {
        val species = realData()
        assertTrue("only ${species.size} species", species.size > 500)
        assertTrue(species.all { it.movesets.isNotEmpty() && it.movesets.size <= 2 }) // top two fast moves
        assertTrue(species.all { s -> s.movesets.all { it.charged.isNotEmpty() && it.charged.size <= 3 } }) // top three charged
    }

    @Test
    fun `real data lists the same charged moves under each fast move`() {
        // PvPoke tracks fast and charged move usage separately, so it cannot pair them.
        for (s in realData()) {
            val first = s.movesets.first().charged.map { it.id }
            assertTrue("${s.name} has differing charged lists", s.movesets.all { m -> m.charged.map { it.id } == first })
        }
    }

    @Test
    fun `real data agrees with the figures confirmed for bubble and ice beam`() {
        val pairs = realData().flatMap { it.movesets }.filter { m ->
            m.fast.id == "BUBBLE" && m.charged.any { it.id == "ICE_BEAM" }
        }
        assertTrue("no Bubble + Ice Beam moveset found", pairs.isNotEmpty())
        val count = CountCalculator.countsFor(pairs.first()).counts.first { it.move.id == "ICE_BEAM" }
        assertEquals(5, count.fastMoves)
        assertEquals(15, count.turns)
    }

    @Test
    fun `real data resolves the opponents seen in the test battle video`() {
        val repository = MovesetRepository(realData())
        val matcher = SpeciesMatcher(repository.speciesNames)
        for (name in listOf("Sneasel", "Combusken", "Magcargo", "Flareon")) {
            val matched = matcher.match(name)
            assertNotNull("$name not matched", matched)
            assertTrue("$name has no movesets", repository.topMovesets(matched!!).isNotEmpty())
        }
    }

    @Test
    fun `real data reads regional forms the way the game writes them`() {
        val repository = MovesetRepository(realData())
        val matched = SpeciesMatcher(repository.speciesNames).match("Alolan Marowak")
        assertNotNull(matched)
        assertNotNull(repository.find(matched!!))
    }
}
