package com.beebeeweb.cadence

import android.content.Context
import com.beebeeweb.cadence.core.TeamData
import com.beebeeweb.cadence.core.TeamMember

/** Keeps your three Pokémon on the phone. */
class TeamStore(context: Context) {

    private val prefs = context.getSharedPreferences("cadence_team", Context.MODE_PRIVATE)

    fun load(): List<TeamMember?> = TeamData.fromJson(prefs.getString(KEY, null))

    fun save(team: List<TeamMember?>) {
        prefs.edit().putString(KEY, TeamData.toJson(team)).apply()
    }

    private companion object {
        const val KEY = "team"
    }
}
