"""Build The Twilight Forest with the pack's completion of the Final Castle, from TeamTwilight's own sources.

  python build.py [--src DIR] [--install]

1. Clones https://github.com/TeamTwilight/twilightforest into DIR (default: _tools/twilightforest-src, outside the
   repository), checks out BASE (the commit the pack's TF 4.8.3345 was built from) on a fresh branch and applies
   patches/*.patch with `git am`.
2. Builds it with Gradle (the wrapper of the TF repository) under a JDK 21, with the version TF's CI gave it
   (CIRevision=4.8.3345), so other mods see the same Twilight Forest version.
3. --install puts the jar, as wilds_twilightforest-1.21.1-4.8.3345-<REV>.jar, into client/mods, server/mods and
   testserver/mods, renaming the original TF jar there to *.jar.replaced (kept, never deleted).

The code is LGPL 2.1 and the art CC BY-NC-SA 4.0, like TF itself: see README.md. The jar is rebuilt, never edited.
"""
import argparse, glob, hashlib, os, shutil, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
REPO = "https://github.com/TeamTwilight/twilightforest.git"
BRANCH = "1.21.1"
BASE = "008085c660f1f9fc9aad6376ea0dc8080b3c0629"   # "fix #2578", 22.03.2026: the commit TF 4.8.3345 was built from
CI_REVISION = "4.8.3345"
REV = "w1"   # the pack's revision of the completion: raise it when the patches change
JAR = f"wilds_twilightforest-1.21.1-{CI_REVISION}-{REV}.jar"
ORIGINAL = f"twilightforest-1.21.1-{CI_REVISION}-universal.jar"
TARGETS = ["client/mods", "server/mods", "testserver/mods"]


def run(cmd, cwd, env=None):
    print(">", " ".join(cmd))
    subprocess.run(cmd, cwd=cwd, env=env, check=True)


def jdk21():
    """A JDK 21: JAVA21_HOME, or the one Gradle downloaded for TF's toolchain, or JAVA_HOME if it is 21."""
    for home in [os.environ.get("JAVA21_HOME")] + sorted(glob.glob(os.path.expanduser("~/.gradle/jdks/*21*"))) + [os.environ.get("JAVA_HOME")]:
        if home and os.path.exists(os.path.join(home, "bin", "javac" + (".exe" if os.name == "nt" else ""))):
            out = subprocess.run([os.path.join(home, "bin", "java"), "-version"], capture_output=True, text=True).stderr
            if '"21' in out:
                return home
    sys.exit("no JDK 21 found: set JAVA21_HOME (Gradle and TF's Beanification plugin need 21, a JRE is not enough)")


def prepare(src):
    if not os.path.isdir(os.path.join(src, ".git")):
        # LF as in TF's repository: the jar's text files must match TF's own
        run(["git", "clone", "-c", "core.autocrlf=false", "--branch", BRANCH, "--single-branch", REPO, src], ROOT)
    run(["git", "config", "core.autocrlf", "false"], src)
    run(["git", "fetch", "origin", BRANCH], src)
    run(["git", "checkout", "-q", "-f", "-B", "wilds-completion", BASE], src)
    # rewrite every file, so a checkout made with other line-ending settings comes out exactly as TF has it
    run(["git", "rm", "-q", "-r", "--cached", "."], src)
    run(["git", "reset", "-q", "--hard", BASE], src)
    patches = sorted(glob.glob(os.path.join(HERE, "patches", "*.patch")))
    run(["git", "-c", "user.name=wilds-build", "-c", "user.email=build@localhost", "am", "-q", "--keep-cr", *patches], src)


def build(src):
    env = dict(os.environ, JAVA_HOME=jdk21())
    env["PATH"] = os.path.join(env["JAVA_HOME"], "bin") + os.pathsep + env["PATH"]
    wrapper = os.path.join(src, "gradlew.bat" if os.name == "nt" else "gradlew")
    # TF's build tasks sometimes trip over their own class transformer on Windows (a missing .class): try again
    for attempt in range(3):
        if subprocess.run([wrapper, "--no-daemon", f"-PCIRevision={CI_REVISION}", "build", "-x", "test"], cwd=src, env=env).returncode == 0:
            return os.path.join(src, "build", "libs", f"twilightforest-1.21.1-{CI_REVISION}-universal.jar")
    sys.exit("gradle build failed")


def install(jar):
    digest = hashlib.sha256(open(jar, "rb").read()).hexdigest()
    for t in TARGETS:
        d = os.path.join(ROOT, t)
        if not os.path.isdir(d):
            continue
        original = os.path.join(d, ORIGINAL)
        if os.path.exists(original):
            os.replace(original, original + ".replaced")
        for old in glob.glob(os.path.join(d, "wilds_twilightforest-*.jar")):
            if os.path.basename(old) != JAR:
                os.replace(old, old + ".replaced")
        shutil.copy2(jar, os.path.join(d, JAR))
        print("installed", os.path.join(t, JAR))
    print("sha256", digest)


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--src", default=os.path.join(ROOT, "_tools", "twilightforest-src"), help="where to keep TF's sources")
    ap.add_argument("--install", action="store_true")
    ap.add_argument("--no-prepare", action="store_true", help="build the checkout as it is (while working on the patches)")
    o = ap.parse_args()
    if not o.no_prepare:
        prepare(o.src)
    jar = build(o.src)
    print("built", jar)
    if o.install:
        install(jar)


if __name__ == "__main__":
    main()
