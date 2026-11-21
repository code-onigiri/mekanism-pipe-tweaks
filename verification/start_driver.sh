#!/bin/sh
# Start the RCON driver detached and record its PID.
set -e
cd /home/hatch/workspace/mekanism-pipe-tweaks-1201
rm -f verification/logs/rcon-driver-1201.log
python3 verification/rcon_driver_1201.py run-test-client/logs/latest.log \
    verification/logs/rcon-driver-1201.log > verification/logs/rcon-driver-1201.out 2>&1 &
echo $! > /tmp/rcon-driver.pid
echo "driver pid $(cat /tmp/rcon-driver.pid)"
