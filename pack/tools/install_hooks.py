"""Installs the repository's git hooks on this computer (hooks are not cloned with the repository).

  python pack/tools/install_hooks.py

pre-commit: refuses a commit when PROJECT_MAP.md does not match the repository (pack/tools/gen_project_map.py --check);
post-commit: rebuilds the code graph in graphify-out/ when graphify is installed (pip install graphifyy), so the graph
follows every commit without an LLM. Existing hooks are kept: these lines are added once, at their end.
"""
import os
import stat
import subprocess

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
HOOKS = {
    "pre-commit": ("# wilds: project map", 'python pack/tools/gen_project_map.py --check || { echo "PROJECT_MAP.md is out of date: python pack/tools/gen_project_map.py, then add it"; exit 1; }'),
    "post-commit": ("# wilds: graphify", "command -v graphify >/dev/null 2>&1 && graphify update . >/dev/null 2>&1 &"),
}


def main():
    git_dir = subprocess.run(["git", "rev-parse", "--git-dir"], cwd=ROOT, capture_output=True, text=True, check=True).stdout.strip()
    hooks = os.path.join(ROOT, git_dir, "hooks")
    os.makedirs(hooks, exist_ok=True)
    for name, (mark, line) in HOOKS.items():
        p = os.path.join(hooks, name)
        old = open(p, encoding="utf-8").read() if os.path.exists(p) else "#!/bin/sh\n"
        if mark in old:
            print(f"{name}: already installed")
            continue
        open(p, "w", encoding="utf-8", newline="\n").write(old.rstrip("\n") + f"\n{mark}\n{line}\n")
        os.chmod(p, os.stat(p).st_mode | stat.S_IEXEC)
        print(f"{name}: installed")


if __name__ == "__main__":
    main()
