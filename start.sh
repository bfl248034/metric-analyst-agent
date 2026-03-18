#!/bin/bash
cd /root/.openclaw/workspace/metric-analyst-agent
mvn spring-boot:run > /tmp/app.log 2>&1 &
echo $! > /tmp/app.pid
echo "Started with PID: $(cat /tmp/app.pid)"