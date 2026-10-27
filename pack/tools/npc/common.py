"""Helpers for the wandering quest givers' stories (quests_*.py). One Q(...) is one quest giver and their quest.

Q(key, short quest title (<= 26 chars), name, who (title under the name), skin, item in hand,
  task: a quest-book task spec ("item:minecraft:poppy*12", "kill:minecraft:zombie*10", "dim:...", "stat:...*n", "struct:..."),
  reward: dict(items=[...], xp=, skill=, loot="wilds:reward/..."),
  story: list of paragraphs (who they are, how they got here, why they cannot do it themselves),
  where: dict(words=[structure id parts], dims=[...], min_day=n),
  questions: [(question, answer), ...],
  lines: greet, ask, accept, decline, remind, done, thanks, lie_ok, lie_fail, haggle_ok, haggle_fail, dead, after,
         task_text (the task as the NPC words it, instead of the generated one))
skin: one of Minecraft's own: steve alex ari efe kai makena noor sunny zuri, "/slim" for slim arms.
"""

SKINS = {"steve", "alex", "ari", "efe", "kai", "makena", "noor", "sunny", "zuri"}

VILLAGE = ["village", "town", "colony", "hamlet", "tavern", "farm", "market"]
WILDS = ["ruin", "remnant", "camp", "tower", "outpost", "well", "house", "hut", "cabin", "lodge", "statue", "windmill", "barn", "rubble", "shrine"]
UNDER = ["mineshaft", "dungeon", "crypt", "catacomb", "ancient_city", "stronghold", "cave", "underground", "mines", "trial", "den"]
SEA = ["ship", "ocean", "sunken", "pirate", "wreck", "lighthouse", "atlantis", "seapost", "lily_pad", "whale", "raft", "corsair", "galleon", "frigate", "junk", "yacht", "monument"]


def S(key, title, task, reward, story, questions=(), lie=True, haggle=True, **lines):
    """A follow-up quest of a giver from quests_*.py (see quests_chains.py): the giver's name, look and place are
    filled in by gen_npc.py from the chain's persona."""
    return Q(key, title, "", "", "steve", "minecraft:air", task, reward, story, None, questions, lie, haggle, **lines) | {"follow_up": True}


def Q(key, title, name, who, skin, item, task, reward, story, where=None, questions=(), lie=True, haggle=True, **lines):
    if "task_text" in lines:
        lines["task"] = lines.pop("task_text")
    base, _, slim = skin.partition("/")
    assert base in SKINS, skin
    kind = task.split(":", 1)[0]
    check = {"item": "item", "kill": "kill"}.get(kind, "other")
    ident, count = "", 1
    if kind in ("item", "kill"):
        rest = task.split(":", 1)[1]
        ident, _, n = rest.partition("*")
        count = int(n or 1)
    return {
        "key": key, "title": title,
        "npc": {"name": name, "title": who, "skin": f"minecraft:textures/entity/player/{'slim' if slim else 'wide'}/{base}.png",
                "slim": bool(slim), "item": item},
        "where": dict(where or {"words": VILLAGE}),
        "task_spec": task,
        "task": {"type": check, "id": ident, "count": count},
        "reward": {"items": reward.get("items", []), "xp": reward.get("xp", 0), "skill_xp": reward.get("skill", 0), "loot": reward.get("loot")},
        "story": list(story),
        "questions": [{"q": q, "a": a} for q, a in questions],
        "lines": lines, "lie": lie, "haggle": haggle,
    }
