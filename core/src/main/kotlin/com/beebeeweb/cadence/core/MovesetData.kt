package com.beebeeweb.cadence.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads Cadence's own data file (for example `data/greatleague.json`, built from PvPoke by
 * `tools/import_pvpoke.py`). The file lists every move once and refers to them by id from each
 * species' movesets.
 */
object MovesetData {

    const val SUPPORTED_VERSION = 1

    fun parse(json: String): List<Species> {
        val root = Json.parseToJsonElement(json).jsonObject
        val version = root.getValue("version").jsonPrimitive.int
        require(version == SUPPORTED_VERSION) { "Unsupported data version $version (expected $SUPPORTED_VERSION)" }

        val fastMoves = root.getValue("fastMoves").jsonObject.mapValues { (id, element) ->
            val o = element.jsonObject
            FastMove(id, o.text("name"), o.number("gain"), o.number("turns"))
        }
        val chargedMoves = root.getValue("chargedMoves").jsonObject.mapValues { (id, element) ->
            val o = element.jsonObject
            ChargedMove(id, o.text("name"), o.number("cost"))
        }

        return root.getValue("species").jsonArray.map { element ->
            val s = element.jsonObject
            val id = s.text("id")
            Species(
                id = id,
                name = s.text("name"),
                aliases = s["aliases"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                movesets = s.getValue("movesets").jsonArray.map { m ->
                    val o = m.jsonObject
                    val fastId = o.text("fast")
                    Moveset(
                        fast = requireNotNull(fastMoves[fastId]) { "Unknown fast move $fastId for $id" },
                        charged = o.getValue("charged").jsonArray.map { c ->
                            val chargedId = c.jsonPrimitive.content
                            requireNotNull(chargedMoves[chargedId]) { "Unknown charged move $chargedId for $id" }
                        },
                    )
                },
            )
        }
    }

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonObject.number(key: String): Int = getValue(key).jsonPrimitive.int
}
