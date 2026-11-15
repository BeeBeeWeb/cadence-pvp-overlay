package com.beebeeweb.cadence.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** One of your own Pokémon: a species name plus the ids of the moves it runs. */
data class TeamMember(
    val speciesName: String,
    val fastId: String,
    val chargedIds: List<String>,
)

/** Saves and loads your team: [SLOTS] slots, each a [TeamMember] or empty (null). */
object TeamData {

    const val SLOTS = 3

    fun toJson(team: List<TeamMember?>): String {
        require(team.size == SLOTS) { "A team has $SLOTS slots (got ${team.size})" }
        val slots = team.map { member ->
            if (member == null) {
                JsonNull
            } else {
                JsonObject(
                    mapOf(
                        "species" to JsonPrimitive(member.speciesName),
                        "fast" to JsonPrimitive(member.fastId),
                        "charged" to JsonArray(member.chargedIds.map { JsonPrimitive(it) }),
                    ),
                )
            }
        }
        return JsonArray(slots).toString()
    }

    /** Always returns [SLOTS] entries; bad or missing data gives empty slots. */
    fun fromJson(json: String?): List<TeamMember?> {
        val empty = List<TeamMember?>(SLOTS) { null }
        if (json.isNullOrBlank()) return empty
        return try {
            val slots = Json.parseToJsonElement(json).jsonArray
            List(SLOTS) { index ->
                val element = slots.getOrNull(index)
                if (element == null || element is JsonNull) {
                    null
                } else {
                    val o = element.jsonObject
                    TeamMember(
                        speciesName = o.getValue("species").jsonPrimitive.content,
                        fastId = o.getValue("fast").jsonPrimitive.content,
                        chargedIds = o.getValue("charged").jsonArray.map { it.jsonPrimitive.content },
                    )
                }
            }
        } catch (e: Exception) {
            empty
        }
    }
}
