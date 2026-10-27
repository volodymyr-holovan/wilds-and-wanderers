"""Rewrite Nomadic Tents recipe advancements into the 1.21 format (they fail to parse as shipped).

Item predicates {"tag": X} become {"items": "#X"}, and requirements list every criterion once
(unlock when any of them is met, which is what recipe advancements are meant to do).
"""
import glob, json, os, sys, zipfile

jar = glob.glob(os.path.join(sys.argv[1], "nomadictents*.jar"))[0]
out_root = os.path.join(sys.argv[2], "kubejs", "data")
z = zipfile.ZipFile(jar)
count = 0
for name in z.namelist():
    if "/advancement/recipes/" not in name or not name.endswith(".json"):
        continue
    adv = json.loads(z.read(name))
    for crit in adv.get("criteria", {}).values():
        items = crit.get("conditions", {}).get("items")
        if isinstance(items, list):
            fixed = []
            for pred in items:
                if "tag" in pred:
                    fixed.append({"items": "#" + pred["tag"]})
                elif "item" in pred:
                    fixed.append({"items": pred["item"]})
                else:
                    fixed.append(pred)
            crit["conditions"]["items"] = fixed
    adv["requirements"] = [list(adv.get("criteria", {}).keys())]
    target = os.path.join(out_root, name[len("data/"):])
    os.makedirs(os.path.dirname(target), exist_ok=True)
    json.dump(adv, open(target, "w", encoding="utf-8"), indent=1)
    count += 1
print("rewrote", count, "advancements")
