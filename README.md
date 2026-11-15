# Cadence

An Android overlay for Pokémon GO trainer battles. It reads the opponent from the battle screen
and shows how many fast moves, and how many turns, they need to reach each charged move.

Not affiliated with or endorsed by Niantic, Scopely, Nintendo or The Pokémon Company.
The app only reads what is on screen; it does not touch the game's code, memory or servers.

## Status

Early work. Great League first.

- [x] `core`: count and turn maths, species-name matching, moveset lookup (pure Kotlin, unit-tested)
- [x] Great League data in `data/greatleague.json` (786 species: top two fast moves, top three charged moves)
- [x] Android app skeleton: screen capture, overlay panel, opponent name reading (first version, not yet tried on a device)
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

Each species gets its two most used fast moves, each listed with the same three most used charged moves
(PvPoke's recommended moveset always comes first). PvPoke tracks fast-move and charged-move usage
separately, so it cannot say which charged moves go with which fast move. "Most used" comes from PvPoke's
simulations, not from real battle logs. Regional forms use the in-game name ("Alolan Marowak"); other forms
are also reachable by the plain species name when that is unambiguous.

## Run the tests

Open the project in Android Studio and run the `core` tests, or from a terminal:

    ./gradlew :core:test

(On Windows: `gradlew.bat :core:test`.)

## Later

- Optimal charged-move throw timing against the opponent's fast-move length.
- Auto-transfer helper for the Pokémon storage (needs a decision on how much to automate).
