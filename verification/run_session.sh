#!/bin/sh
# Run one verification session: start the RCON driver, launch the dev client, and let the
# harness in AutoTest drive every scene. The dedicated server must already be running and
# its world is reset to the flat rig baseline before the client connects.
#
# The login handshake can intermittently fail with a ConcurrentModificationException thrown
# inside Forge's HandshakeHandler (its sentMessages ArrayList is mutated from both the server
# thread and the netty IO thread). That is a Forge race, not mod code, so a session simply
# retries the connection until the handshake gets through.
#
# Usage: verification/run_session.sh [attempts]
set -e
cd /home/hatch/workspace/mekanism-pipe-tweaks-1201
ATTEMPTS=${1:-6}

reset_world() {
python3 - <<'PY'
import sys
sys.path.insert(0, 'verification')
from rcon_driver import Rcon
r = Rcon(host='127.0.0.1', port=25576)
for command in [
    'fill 8 -60 8 40 -48 28 minecraft:air',
    'fill 8 -61 8 40 -61 28 minecraft:grass_block',
    'setblock 14 -60 19 minecraft:stone',
    'setblock 14 -60 20 minecraft:air',
    'setblock 14 -60 21 minecraft:air',
    'gamemode creative Dev',
]:
    r.command(command)
PY
}

pkill -f forgeclientuserdev 2>/dev/null || true
pkill -f rcon_driver_1201 2>/dev/null || true
sleep 2
rm -f verification/.rcon-driver.lock
rm -f verification/screenshot-1201-*.png
rm -f verification/logs/rcon-driver-1201.log verification/logs/rcon-driver-1201.out
rm -f run-test-client/logs/latest.log /tmp/client-1201.log
reset_world

attempt=1
while [ "$attempt" -le "$ATTEMPTS" ]; do
    echo "login attempt $attempt"
    rm -f run-test-client/logs/latest.log /tmp/client-1201.log
    nohup python3 verification/rcon_driver_1201.py run-test-client/logs/latest.log \
        verification/logs/rcon-driver-1201.log >> verification/logs/rcon-driver-1201.out 2>&1 &
    driver_pid=$!
    sleep 2
    DISPLAY=:99 nohup ./verification/launch_1201.sh /tmp/client-args-1201.txt run-test-client \
        /tmp/client-1201.log > /tmp/client-1201.out 2>&1 &
    # The dev client needs ~2 min to boot under llvmpipe before it can join.
    stepped=0
    while [ "$stepped" -lt 300 ]; do
        if grep -q "MPT_AUTOTEST world_ready" /tmp/client-1201.log 2>/dev/null; then
            break
        fi
        sleep 2
        stepped=$((stepped + 2))
    done
    if grep -q "MPT_AUTOTEST world_ready" /tmp/client-1201.log 2>/dev/null; then
        echo "joined on attempt $attempt"
        exit 0
    fi
    echo "attempt $attempt did not join; retrying"
    pkill -f forgeclientuserdev 2>/dev/null || true
    pkill -f rcon_driver_1201 2>/dev/null || true
    sleep 3
    rm -f verification/.rcon-driver.lock
    reset_world
    attempt=$((attempt + 1))
done
echo "no successful login after $ATTEMPTS attempts"
exit 1
