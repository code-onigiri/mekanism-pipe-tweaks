#!/usr/bin/env python3
"""Launch a ForgeGradle dev run from a captured argv, without Gradle.

Two Gradle `run*` tasks cannot share one GRADLE_USER_HOME (journal lock), so after
the first Gradle launch the other side is started straight from its captured
command line.

MOD_CLASSES uses Forge's `modid%%/path` entry syntax; it is built by plain
concatenation here (do not use printf-style formatting, which collapses `%%`).

Usage: launch_direct.py <args-file> <working-dir> <log-file>
"""
import os
import subprocess
import sys


def main():
    args_file, workdir, log_file = sys.argv[1], sys.argv[2], sys.argv[3]
    args = [line for line in open(args_file).read().split("\n") if line]
    env = dict(os.environ)
    env.pop("JAVA_TOOL_OPTIONS", None)
    project = os.path.abspath(os.path.join(workdir, os.pardir))
    env["MOD_CLASSES"] = (
        "mekanism_pipe_tweaks%%" + project + "/build/resources/main"
        + os.pathsep
        + "mekanism_pipe_tweaks%%" + project + "/build/classes/java/main"
    )
    log = open(log_file, "wb")
    proc = subprocess.Popen(args, cwd=workdir, stdout=log, stderr=subprocess.STDOUT, env=env)
    print("started pid", proc.pid, flush=True)
    sys.exit(proc.wait())


if __name__ == "__main__":
    main()
