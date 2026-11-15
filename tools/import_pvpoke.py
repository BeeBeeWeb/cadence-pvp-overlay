#!/usr/bin/env python3
"""Build data/greatleague.json from a PvPoke checkout.

PvPoke (https://github.com/pvpoke/pvpoke) is MIT licensed. This script reads its move table and
Great League rankings and writes a small file in Cadence's own format, so the app does not depend
on PvPoke's file layout.

Usage:
    python tools/import_pvpoke.py --pvpoke /path/to/pvpoke --out data/greatleague.json

Each species gets up to three movesets (one fast move plus up to two charged moves):
  1. PvPoke's recommended moveset for the league, then
  2. combinations of its two most used fast moves and four most used charged moves, ranked by
     fast-move usage share x charged-move usage share.
Usage numbers come from PvPoke's league simulations, so "likely" means "used most in simulations",
not "seen most often in real battles".
"""
import argparse
import itertools
import json
import re
import subprocess
import sys
from datetime import date
from pathlib import Path

MAX_FAST = 2
MAX_CHARGED = 4
MAX_MOVESETS = 3
REGIONAL = ("Alolan", "Galarian", "Hisuian", "Paldean")


def read_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def classify_moves(moves):
    """Return (fast, charged) dicts keyed by moveId, skipping unlisted or unusable moves."""
    fast, charged = {}, {}
    for m in moves:
        if m.get("unlisted"):
            continue
        move_id = m["moveId"]
        if m.get("energyGain", 0) > 0 and m.get("energy", 0) == 0:
            turns = m.get("turns") or max(1, round(m["cooldown"] / 500))
            fast[move_id] = {"name": m["name"], "gain": int(m["energyGain"]), "turns": int(turns)}
        elif m.get("energy", 0) > 0 and m.get("energyGain", 0) == 0:
            charged[move_id] = {"name": m["name"], "cost": int(m["energy"])}
    return fast, charged


def usage_shares(entries, valid):
    """Usage share per move id among the valid moves, highest first."""
    usable = [e for e in entries if e["moveId"] in valid and (e.get("uses") or 0) > 0]
    total = sum(e["uses"] for e in usable)
    if total == 0:
        return []
    return sorted(((e["moveId"], e["uses"] / total) for e in usable), key=lambda x: -x[1])


def build_movesets(entry, fast_table, charged_table):
    moves = entry["moves"]
    fast = usage_shares(moves.get("fastMoves", []), fast_table)[:MAX_FAST]
    charged = usage_shares(moves.get("chargedMoves", []), charged_table)[:MAX_CHARGED]
    charged_share = dict(charged)

    candidates = []
    if fast and charged:
        pair_size = 2 if len(charged) >= 2 else 1
        for (fast_id, f_share), pair in itertools.product(fast, itertools.combinations(charged, pair_size)):
            score = f_share * sum(share for _, share in pair)
            candidates.append((score, fast_id, [c for c, _ in pair]))
        candidates.sort(key=lambda c: -c[0])

    ordered = []
    recommended = entry.get("moveset", [])
    if recommended and recommended[0] in fast_table:
        rec_charged = [m for m in recommended[1:] if m in charged_table]
        if rec_charged:
            ordered.append((recommended[0], rec_charged))
    ordered.extend((fast_id, pair) for _, fast_id, pair in candidates)

    result, seen = [], set()
    for fast_id, pair in ordered:
        key = (fast_id, frozenset(pair))
        if key in seen:
            continue
        seen.add(key)
        result.append({"fast": fast_id, "charged": pair})
        if len(result) == MAX_MOVESETS:
            break
    return result


def base_name(name):
    return re.sub(r"\s*\(Shadow\)$", "", name)


def display_and_aliases(pvpoke_name):
    """Regional forms read 'Alolan Marowak' in game; PvPoke writes 'Marowak (Alolan)'."""
    m = re.fullmatch(r"(.+?) \((.+)\)", pvpoke_name)
    if not m:
        return pvpoke_name, []
    species, form = m.groups()
    if form in REGIONAL:
        return f"{form} {species}", [pvpoke_name]
    return pvpoke_name, [species]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--pvpoke", required=True, type=Path, help="path to a PvPoke checkout")
    parser.add_argument("--out", required=True, type=Path, help="output file, e.g. data/greatleague.json")
    args = parser.parse_args()

    data = args.pvpoke / "src" / "data"
    moves = read_json(data / "gamemaster" / "moves.json")
    rankings = read_json(data / "rankings" / "all" / "overall" / "rankings-1500.json")
    fast_table, charged_table = classify_moves(moves)

    # One entry per species: prefer the normal version over its Shadow version, keep file order.
    chosen = {}
    for entry in rankings:
        key = base_name(entry["speciesName"])
        is_shadow = entry["speciesName"] != key
        if key not in chosen or (chosen[key][1] and not is_shadow):
            chosen[key] = (entry, is_shadow)

    def norm(text):
        return re.sub(r"\W", "", text.lower())

    # Real display names are reserved first so an alias never shadows a species with that name.
    display = {key: display_and_aliases(key) for key in chosen}
    used_names = {norm(name) for name, _ in display.values()}

    species, used_fast, used_charged = [], set(), set()
    for key, (entry, _) in chosen.items():
        movesets = build_movesets(entry, fast_table, charged_table)
        if not movesets:
            print(f"skipping {key}: no usable movesets", file=sys.stderr)
            continue
        name, candidate_aliases = display[key]
        aliases = []
        for alias in candidate_aliases:
            if norm(alias) not in used_names:  # first species to claim an alias keeps it
                used_names.add(norm(alias))
                aliases.append(alias)
        for ms in movesets:
            used_fast.add(ms["fast"])
            used_charged.update(ms["charged"])
        item = {"id": entry["speciesId"], "name": name, "movesets": movesets}
        if aliases:
            item["aliases"] = aliases
        species.append(item)

    commit = "unknown"
    try:
        commit = subprocess.check_output(
            ["git", "-C", str(args.pvpoke), "rev-parse", "HEAD"], text=True, stderr=subprocess.DEVNULL
        ).strip()
    except Exception:
        pass

    output = {
        "version": 1,
        "league": "great",
        "source": "PvPoke https://github.com/pvpoke/pvpoke (MIT License, Copyright (c) 2019 pvpoke)",
        "sourceCommit": commit,
        "generated": date.today().isoformat(),
        "fastMoves": {k: fast_table[k] for k in sorted(used_fast)},
        "chargedMoves": {k: charged_table[k] for k in sorted(used_charged)},
        "species": species,
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, separators=(",", ":"))
        f.write("\n")
    print(f"wrote {args.out}: {len(species)} species, {len(used_fast)} fast moves, {len(used_charged)} charged moves")


if __name__ == "__main__":
    main()
