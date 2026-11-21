#!/bin/sh
# Launch a ForgeGradle dev run from a captured argv, without Gradle.
# Usage: launch_1201.sh <args-file> <working-dir> <log-file>
set -e
ARGS="$1"; WORKDIR="$2"; LOGFILE="$3"
cd /home/hatch/workspace/mekanism-pipe-tweaks-1201
export MOD_CLASSES="mekanism_pipe_tweaks%%$PWD/build/resources/main:mekanism_pipe_tweaks%%$PWD/build/classes/java/main"
unset JAVA_TOOL_OPTIONS
exec python3 verification/run_from_args.py "$ARGS" "$WORKDIR" "$LOGFILE"
