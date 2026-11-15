# Cadence

An Android overlay for Pokémon GO trainer battles. It reads the opponent from the battle screen
and shows how many fast moves, and how many turns, they need to reach each charged move.

Not affiliated with or endorsed by Niantic, Scopely, Nintendo or The Pokémon Company.
The app only reads what is on screen; it does not touch the game's code, memory or servers.

## Status

Early work. Great League first.

- [x] `core`: count and turn maths, species-name matching, moveset lookup (pure Kotlin, unit-tested)
- [x] Great League data in `data/greatleague.json` (786 species, up to 3 movesets each)
- [x] Android app skeleton: screen capture, overlay panel, opponent name reading (first version, not yet tried on a device)
- [ ] Shields and remaining-Pokémon detection
- [ ] Matchup and backline prediction

## How counts work

For each charged move: `count = ceil(energy cost / fast move energy gain)`, and
`turns = count x fast move turns`. Counts assume the Pokémon starts at zero energy.

Example: Bubble gives 11 energy over 3 turns, so Ice Beam (55 energy) takes 5 Bubbles = 15 turns.

## Data

`data/greatleague.json` is generated from [PvPoke](https://github.com/pvpoke/pvpoke) (MIT License, see
`data/PVPOKE-LICENSE.txt`) by `tools/import_pvpoke.py`. To refresh it after a PvPoke update:

    git clone --depth 1 https://github.com/pvpoke/pvpoke
    python tools/import_pvpoke.py --pvpoke pvpoke --out data/greatleague.json

Each species gets up to three movesets: PvPoke's recommended one first, then combinations of its most
used fast and charged moves. "Most used" comes from PvPoke's simulations, not from real battle logs.
Regional forms use the in-game name ("Alolan Marowak"); other forms are also reachable by the plain
species name when that is unambiguous.

## Try the app (Android 11+)

1. Open the project in Android Studio, let Gradle sync, pick the `app` configuration and your phone, press Run.
2. In Cadence: allow "display over other apps", then tap "Start Cadence".
3. When Android asks what to share, choose "Entire screen". Switch to the game and start a battle.

Cadence starts paused. A floating button appears over the game: tap **Start** when a battle begins and
**Pause** when you are done (drag it to move it). While running, the panel appears under the opponent's
card once the name has been read twice in a row. It lists each fast move once, followed by the charged
moves with the fast-move count (red) and turns (white). The panel never takes touches, so the game stays
fully usable. Stop Cadence completely from the app or from the notification.

If the name is not recognised, the card region in `ScreenRegions.kt` and the capture scale in
`OverlayService.kt` (`CAPTURE_SCALE`) are the first things to tune.

## Run the tests

Open the project in Android Studio and run the `core` tests, or from a terminal:

    ./gradlew :core:test

(On Windows: `gradlew.bat :core:test`.)

## Later

- Optimal charged-move throw timing against the opponent's fast-move length.
- Auto-transfer helper for the Pokémon storage (needs a decision on how much to automate).
