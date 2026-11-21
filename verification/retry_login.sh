#!/bin/sh
# Keep relaunching the dev client until the Forge login handshake gets through.
#
# Forge 47.4.0's HandshakeHandler keeps its pending acknowledgements in a plain ArrayList
# that is mutated from both the server thread (tickServer) and the netty IO thread
# (handleIndexedMessage); the two removeIf passes can collide, throwing a
# ConcurrentModificationException and dropping the login. It is purely timing dependent, so a
# retry loop is the only reliable way to get a session running. The RCON driver is already
# running and resets its file offset when the client log is recreated.
#
# Usage: verification/retry_login.sh [attempts]
cd /home/hatch/workspace/mekanism-pipe-tweaks-1201
ATTEMPTS=${1:-8}
SERVER_LOG=run-test-server/logs/latest.log
baseline=$(grep -c "Internal Exception" "$SERVER_LOG" 2>/dev/null || echo 0)

attempt=1
while [ "$attempt" -le "$ATTEMPTS" ]; do
    pkill -f forgeclientuserdev 2>/dev/null || true
    sleep 3
    rm -f run-test-client/logs/latest.log
    echo "[retry_login] attempt $attempt at $(date +%H:%M:%S)"
    DISPLAY=:99 setsid nohup ./verification/launch_1201.sh /tmp/client-args-1201.txt \
        run-test-client /tmp/client-1201.log > /tmp/client-1201.out 2>&1 < /dev/null &
    waited=0
    while [ "$waited" -lt 260 ]; do
        if grep -q "MPT_AUTOTEST world_ready" /tmp/client-1201.log 2>/dev/null; then
            echo "[retry_login] joined on attempt $attempt"
            exit 0
        fi
        now=$(grep -c "Internal Exception" "$SERVER_LOG" 2>/dev/null || echo 0)
        if [ "$now" -gt "$baseline" ]; then
            echo "[retry_login] attempt $attempt hit the login CME"
            baseline=$now
            break
        fi
        sleep 2
        waited=$((waited + 2))
    done
    attempt=$((attempt + 1))
done
echo "[retry_login] gave up after $ATTEMPTS attempts"
exit 1
