package com.beebeeweb.cadence

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.beebeeweb.cadence.core.MoveCatalog
import com.beebeeweb.cadence.core.MovesetData
import com.beebeeweb.cadence.core.Species
import com.beebeeweb.cadence.core.TeamData
import com.beebeeweb.cadence.core.TeamMember

/** Lets you enter your own three Pokémon: species, fast move and up to two charged moves. */
class TeamActivity : ComponentActivity() {

    private class Slot(
        val species: AutoCompleteTextView,
        val fast: Spinner,
        val charged: List<Spinner>,
    )

    private lateinit var catalog: MoveCatalog
    private lateinit var speciesByName: Map<String, Species>
    private val slots = ArrayList<Slot>()
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val json = assets.open(OverlayService.DATA_ASSET).bufferedReader().use { it.readText() }
        catalog = MovesetData.parseCatalog(json)
        val species = MovesetData.parse(json)
        speciesByName = species.associateBy { it.name.lowercase() }
        val speciesNames = species.map { it.name }.sorted()

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        root.addView(TextView(this).apply {
            text = "Your team"
            textSize = 24f
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

    private fun addSlot(root: LinearLayout, speciesNames: List<String>): Slot {
        val speciesField = AutoCompleteTextView(this).apply {
            hint = "Species"
            threshold = 1
            setSingleLine()
            setAdapter(ArrayAdapter(this@TeamActivity, android.R.layout.simple_dropdown_item_1line, speciesNames))
        }
        val fastLabels = catalog.fast.map { "${it.name}  (+${it.energyGain}, ${it.turns}t)" }
        val fast = Spinner(this).apply {
            adapter = spinnerAdapter(fastLabels)
        }
        val chargedLabels = listOf("none") + catalog.charged.map { "${it.name}  (${it.energyCost})" }
        val charged = List(2) {
            Spinner(this).apply {
                adapter = spinnerAdapter(chargedLabels)
            }
        }
        val slot = Slot(speciesField, fast, charged)

        // Picking a species preselects its most common moveset; you can still change every move.
        speciesField.setOnItemClickListener { _, _, _, _ ->
            speciesByName[speciesField.text.toString().trim().lowercase()]?.movesets?.firstOrNull()?.let { moveset ->
                select(slot, moveset.fast.id, moveset.charged.take(2).map { it.id })
            }
        }

        root.addView(speciesField, matchWidth())
        root.addView(fast, matchWidth())
        charged.forEach { root.addView(it, matchWidth()) }
        return slot
    }

    private fun fill(slot: Slot, member: TeamMember) {
        slot.species.setText(member.speciesName)
        slot.species.dismissDropDown()
        select(slot, member.fastId, member.chargedIds)
    }

    private fun select(slot: Slot, fastId: String, chargedIds: List<String>) {
        val fastIndex = catalog.fast.indexOfFirst { it.id == fastId }
        if (fastIndex >= 0) slot.fast.setSelection(fastIndex)
        slot.charged.forEachIndexed { i, spinner ->
            val chargedIndex = catalog.charged.indexOfFirst { it.id == chargedIds.getOrNull(i) }
            spinner.setSelection(chargedIndex + 1) // 0 is "none"
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
            if (species == null) {
                problems += "Pokémon ${index + 1}: pick a species from the list"
                team += null
                return@forEachIndexed
            }
            val chargedIds = slot.charged
                .map { it.selectedItemPosition }
                .filter { it > 0 }
                .map { catalog.charged[it - 1].id }
                .distinct()
            team += TeamMember(species.name, catalog.fast[slot.fast.selectedItemPosition].id, chargedIds)
        }
        TeamStore(this).save(team)
        status.text = if (problems.isEmpty()) "Saved." else "Saved, except:\n" + problems.joinToString("\n")
    }

    /** Spinner rows with an explicit dark text colour, so the selected move is always visible. */
    private fun spinnerAdapter(labels: List<String>): ArrayAdapter<String> {
        val dark = Color.rgb(33, 33, 33)
        return object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, labels) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                (super.getView(position, convertView, parent) as TextView).apply { setTextColor(dark) }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
                (super.getDropDownView(position, convertView, parent) as TextView).apply { setTextColor(dark) }
        }
    }

    private fun matchWidth() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}
