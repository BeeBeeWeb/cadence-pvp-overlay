package com.beebeeweb.cadence.core

import com.beebeeweb.cadence.core.Fixtures.BUBBLE
import com.beebeeweb.cadence.core.Fixtures.CHARGED_40
import com.beebeeweb.cadence.core.Fixtures.CHARGED_60
import com.beebeeweb.cadence.core.Fixtures.ICE_BEAM
import com.beebeeweb.cadence.core.Fixtures.THREE_TURN_FAST
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MovesetRepositoryTest {

    // Fixture data only: these are not real movesets.
    private val species = listOf(
        Species(
            id = "azumarill",
            name = "Azumarill",
            movesets = listOf(
                Moveset(BUBBLE, listOf(ICE_BEAM, CHARGED_60)),
                Moveset(THREE_TURN_FAST, listOf(CHARGED_40)),
                Moveset(BUBBLE, listOf(CHARGED_40)),
            ),
        ),
        Species("sneasel", "Sneasel", listOf(Moveset(THREE_TURN_FAST, listOf(CHARGED_40)))),
    )
    private val repository = MovesetRepository(species)

    @Test
    fun `lookup ignores case and punctuation`() {
        assertNotNull(repository.find("AZUMARILL"))
        assertNotNull(repository.find(" azu-marill "))
    }

    @Test
    fun `unknown species gives no movesets`() {
        assertTrue(repository.topMovesets("Nonexistent").isEmpty())
    }

    @Test
    fun `top movesets are limited and keep their order`() {
        val top = repository.topMovesets("Azumarill", limit = 2)
        assertEquals(2, top.size)
        assertEquals("Bubble", top[0].fast.name)
        assertEquals("Fixture Fast 3T", top[1].fast.name)
    }

    @Test
    fun `default limit is three`() {
        assertEquals(3, repository.topMovesets("Azumarill").size)
    }

    @Test
    fun `counts are calculated for each returned moveset`() {
        val first = repository.topMovesets("Azumarill", limit = 1).single()
        assertEquals(listOf(5, 6), first.counts.map { it.fastMoves })
        assertEquals(listOf(15, 18), first.counts.map { it.turns })
    }

    @Test
    fun `species names are exposed for the matcher`() {
        assertEquals(listOf("Azumarill", "Sneasel"), repository.speciesNames)
    }

    @Test
    fun `first entry wins when two species normalise to the same name`() {
        val duplicate = MovesetRepository(
            listOf(
                Species("a", "Mr. Mime", listOf(Moveset(BUBBLE, listOf(ICE_BEAM)))),
                Species("b", "Mr Mime", listOf(Moveset(BUBBLE, listOf(CHARGED_60)))),
            ),
        )
        assertEquals("a", duplicate.find("mrmime")?.id)
    }

    @Test
    fun `aliases resolve to their species and are offered to the matcher`() {
        val aliased = MovesetRepository(
            listOf(
                Species("m", "Alolan Marowak", listOf(Moveset(BUBBLE, listOf(ICE_BEAM))), aliases = listOf("Marowak (Alolan)")),
            ),
        )
        assertEquals("m", aliased.find("Marowak (Alolan)")?.id)
        assertEquals(listOf("Alolan Marowak", "Marowak (Alolan)"), aliased.speciesNames)
    }

    @Test
    fun `an alias never shadows another species with that name`() {
        val repo = MovesetRepository(
            listOf(
                Species("a", "Giratina (Altered)", listOf(Moveset(BUBBLE, listOf(ICE_BEAM))), aliases = listOf("Giratina")),
                Species("b", "Giratina", listOf(Moveset(BUBBLE, listOf(CHARGED_60)))),
            ),
        )
        assertEquals("b", repo.find("Giratina")?.id)
    }
}
