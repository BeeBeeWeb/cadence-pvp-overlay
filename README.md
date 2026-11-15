# Cadence

An Android overlay for Pokémon GO trainer battles. It reads the opponent from the battle screen
and shows how many fast moves, and how many turns, they need to reach each charged move.

Not affiliated with or endorsed by Niantic, Scopely, Nintendo or The Pokémon Company.
The app only reads what is on screen; it does not touch the game's code, memory or servers.

## Status

Early work. Great League first.

- [x] `core`: count and turn maths, species-name matching, moveset lookup (pure Kotlin, unit-tested)
- [ ] Great League data (movesets, energy, turns)
- [ ] Android app: screen capture, overlay, opponent name reading
- [ ] Shields and remaining-Pokémon detection
- [ ] Matchup and backline prediction

## How counts work

For each charged move: `count = ceil(energy cost / fast move energy gain)`, and
`turns = count x fast move turns`. Counts assume the Pokémon starts at zero energy.

Example: Bubble gives 11 energy over 3 turns, so Ice Beam (55 energy) takes 5 Bubbles = 15 turns.

## Run the tests

Open the project in Android Studio and run the `core` tests, or from a terminal:

    ./gradlew :core:test

(On Windows: `gradlew.bat :core:test`.)

## Later

- Optimal charged-move throw timing against the opponent's fast-move length.
- Auto-transfer helper for the Pokémon storage (needs a decision on how much to automate).
