package com.beebeeweb.cadence

import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.beebeeweb.cadence.core.ChargedMove
import com.beebeeweb.cadence.core.FastMove
import com.beebeeweb.cadence.core.MoveCatalog
import com.beebeeweb.cadence.core.MovesetData
import com.beebeeweb.cadence.core.Species
import com.beebeeweb.cadence.core.TeamData
import com.beebeeweb.cadence.core.TeamMember

/** Lets you enter your own three Pokémon: species, fast move and up to two charged moves. */
class TeamActivity : ComponentActivity() {

    private class Slot(
        val species: AutoCompleteTextView,
        val fast: AutoCompleteTextView,
        val charged: List<AutoCompleteTextView>,
    )

    private lateinit var catalog: MoveCatalog
    private lateinit var speciesByName: Map<String, Species>
    private lateinit var fastByLabel: Map<String, FastMove>
    private lateinit var chargedByLabel: Map<String, ChargedMove>
    private val slots = ArrayList<Slot>()
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val json = assets.open(OverlayService.DATA_ASSET).bufferedReader().use { it.readText() }
        catalog = MovesetData.parseCatalog(json)
        val species = MovesetData.parse(json)
        speciesByName = species.associateBy { it.name.lowercase() }
        val speciesNames = species.map { it.name }.sorted()
        fastByLabel = catalog.fast.associateBy { fastLabel(it).lowercase() }
        chargedByLabel = catalog.charged.associateBy { chargedLabel(it).lowercase() }

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        root.addView(TextView(this).apply {
            text = "Your team"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "Tap a field and type to search."
            setPadding(0, pad / 2, 0, 0)
        })

        val saved = TeamStore(this).load()
        for (index in 0 until TeamData.SLOTS) {
            root.addView(TextView(this).apply {
                text = "Pokémon ${index + 1}"
                setPadding(0, pad, 0, 0)
            })
            val slot = addSlot(root, speciesNames)
            slots += slot
            saved[index]?.let { fill(slot, it) }
        }

        status = TextView(this).apply { setPadding(0, pad, 0, pad) }
        root.addView(status)
        root.addView(Button(this).apply {
            text = "Save"
            setOnClickListener { save() }
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun fastLabel(move: FastMove) = "${move.name} (+${move.energyGain}, ${move.turns}t)"

    private fun chargedLabel(move: ChargedMove) = "${move.name} (${move.energyCost})"

    private fun searchField(hint: String, items: List<String>) = AutoCompleteTextView(this).apply {
        this.hint = hint
        threshold = 1
        setSingleLine()
        setAdapter(ArrayAdapter(this@TeamActivity, android.R.layout.simple_dropdown_item_1line, items))
        setOnClickListener { showDropDown() }
    }

    private fun addSlot(root: LinearLayout, speciesNames: List<String>): Slot {
        val speciesField = searchField("Species", speciesNames)
        val fastField = searchField("Fast move", catalog.fast.map { fastLabel(it) })
        val chargedLabels = catalog.charged.map { chargedLabel(it) }
        val chargedFields = listOf(searchField("Charged move 1", chargedLabels), searchField("Charged move 2 (optional)", chargedLabels))
        val slot = Slot(speciesField, fastField, chargedFields)

        // Picking a species fills in its most common moveset; you can still change every move.
        speciesField.setOnItemClickListener { _, _, _, _ ->
            speciesByName[speciesField.text.toString().trim().lowercase()]?.movesets?.firstOrNull()?.let { moveset ->
                select(slot, moveset.fast, moveset.charged.take(2))
            }
        }

        root.addView(speciesField, matchWidth())
        root.addView(fastField, matchWidth())
        chargedFields.forEach { root.addView(it, matchWidth()) }
        return slot
    }

    private fun fill(slot: Slot, member: TeamMember) {
        slot.species.setText(member.speciesName)
        select(
            slot,
            catalog.fast.firstOrNull { it.id == member.fastId },
            member.chargedIds.mapNotNull { id -> catalog.charged.firstOrNull { it.id == id } },
        )
        slot.species.dismissDropDown()
    }

    private fun select(slot: Slot, fast: FastMove?, charged: List<ChargedMove>) {
        slot.fast.setText(fast?.let { fastLabel(it) }.orEmpty(), false)
        slot.charged.forEachIndexed { i, field ->
            field.setText(charged.getOrNull(i)?.let { chargedLabel(it) }.orEmpty(), false)
        }
    }

    private fun save() {
        val team = ArrayList<TeamMember?>()
        val problems = ArrayList<String>()
        slots.forEachIndexed { index, slot ->
            val typed = slot.species.text.toString().trim()
            if (typed.isEmpty()) {
                team += null
                return@forEachIndexed
            }
            val species = speciesByName[typed.lowercase()]
            val fast = fastByLabel[slot.fast.text.toString().trim().lowercase()]
            val charged = slot.charged
                .map { it.text.toString().trim().lowercase() }
                .filter { it.isNotEmpty() }
                .map { chargedByLabel[it] }
            when {
                species == null -> problems += "Pokémon ${index + 1}: pick a species from the list"
                fast == null -> problems += "Pokémon ${index + 1}: pick a fast move from the list"
                charged.any { it == null } -> problems += "Pokémon ${index + 1}: pick charged moves from the list"
            }
            if (species == null || fast == null || charged.any { it == null }) {
                team += null
            } else {
                team += TeamMember(species.name, fast.id, charged.filterNotNull().map { it.id }.distinct())
            }
        }
        TeamStore(this).save(team)
        status.text = if (problems.isEmpty()) "Saved." else "Saved, except:\n" + problems.joinToString("\n")
    }

    private fun matchWidth() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}
