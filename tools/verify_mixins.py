#!/usr/bin/env python3
"""
Checks every mixin target against the compiled Minecraft classes before the jar ships.

A mixin whose target signature has drifted does not fail at build time — it fails at
launch, as a crash on the loading screen, which is the worst possible place to find out.
This reads the @Mixin targets and @Inject/@Invoker/@Accessor method selectors straight out
of the mixin sources and confirms each one exists in the jar the mod was compiled against.

    python3 tools/verify_mixins.py

Exits non-zero if anything is missing, so it can gate a release.
"""

import glob
import json
import os
import re
import subprocess
import sys
import zipfile

MIXIN_SRC = "src/main/java/com/smmorpg/mixin"
CONFIG = "src/main/resources/smmorpg.mixins.json"


def find_minecraft_jar():
    candidates = [p for p in glob.glob("build/moddev/artifacts/*minecraft*.jar")
                  if "sources" not in p]
    if not candidates:
        print("No compiled Minecraft jar found. Run a build first (gradle build).",
              file=sys.stderr)
        sys.exit(2)
    return candidates[0]


def methods_of(jar, internal_name):
    """Returns {(name, descriptor)} for one class, read with javap."""
    with zipfile.ZipFile(jar) as z:
        entry = internal_name + ".class"
        if entry not in z.namelist():
            return None
        data = z.read(entry)

    tmp = os.path.join("build", "tmp", "verify_mixins")
    os.makedirs(tmp, exist_ok=True)
    path = os.path.join(tmp, internal_name.replace("/", ".") + ".class")
    with open(path, "wb") as f:
        f.write(data)

    out = subprocess.run(["javap", "-p", "-s", path],
                         capture_output=True, text=True).stdout

    found = set()
    pending = None
    for line in out.splitlines():
        line = line.strip()
        m = re.match(r"descriptor: (.+)$", line)
        if m and pending:
            found.add((pending, m.group(1)))
            pending = None
            continue
        m = re.search(r"([A-Za-z_$<][\w$<>]*)\s*\(", line)
        if m:
            pending = m.group(1)
    return found


def targets_from_source(path):
    """(target class, [(selector, kind)]) for one mixin source file."""
    src = open(path, encoding="utf-8").read()

    m = re.search(r"@Mixin\(\s*([\w.]*?)([\w]+)\.class\s*\)", src)
    if not m:
        return None, []
    simple = m.group(2)

    # Resolve the simple name through the file's own imports.
    fq = None
    if m.group(1):
        fq = (m.group(1) + simple).replace(".class", "")
    else:
        imp = re.search(r"^import ([\w.]+\.%s);" % re.escape(simple), src, re.M)
        if imp:
            fq = imp.group(1)
    if not fq:
        return None, []

    selectors = []
    for kind, pattern in (("inject", r'@Inject\((?:.|\n)*?method\s*=\s*((?:"[^"]*"\s*\+?\s*)+)'),
                          ("invoker", r'@Invoker\("([^"]+)"\)'),
                          ("accessor", r'@Accessor\("([^"]+)"\)')):
        for hit in re.finditer(pattern, src):
            raw = hit.group(1)
            # Java string concatenation in the annotation is normal for long descriptors.
            value = "".join(re.findall(r'"([^"]*)"', raw)) if kind == "inject" else raw
            selectors.append((value.strip(), kind))
    return fq.replace(".", "/"), selectors


def main():
    jar = find_minecraft_jar()
    print(f"Verifying against {jar}\n")

    enabled = set()
    if os.path.exists(CONFIG):
        cfg = json.load(open(CONFIG))
        for key in ("mixins", "client", "server"):
            enabled.update(cfg.get(key, []))

    failures = []
    checked = 0

    for path in sorted(glob.glob(os.path.join(MIXIN_SRC, "*.java"))):
        name = os.path.basename(path)[:-5]
        if enabled and name not in enabled:
            print(f"  {name}: not listed in smmorpg.mixins.json, skipping")
            continue

        target, selectors = targets_from_source(path)
        if not target:
            print(f"  {name}: no @Mixin target found, skipping")
            continue

        available = methods_of(jar, target)
        if available is None:
            failures.append(f"{name}: target class {target} is not in the jar")
            continue

        for selector, kind in selectors:
            checked += 1
            if "(" in selector:
                sel_name, desc = selector.split("(", 1)
                desc = "(" + desc
                ok = (sel_name, desc) in available
                shown = f"{sel_name}{desc}"
            else:
                sel_name = selector.rstrip("*")
                ok = any(n == sel_name for n, _ in available)
                shown = sel_name
                overloads = [d for n, d in available if n == sel_name]
                if len(overloads) > 1:
                    print(f"  ! {name}: '{shown}' matches {len(overloads)} overloads in "
                          f"{target}; pin the full descriptor")
            status = "ok " if ok else "MISSING"
            print(f"  {status} {name}: {target}#{shown}")
            if not ok:
                failures.append(f"{name}: {target}#{shown} not found")

    print()
    if failures:
        print(f"{len(failures)} problem(s):", file=sys.stderr)
        for f in failures:
            print("  " + f, file=sys.stderr)
        return 1
    print(f"All {checked} mixin targets resolve.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
