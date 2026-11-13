#!/usr/bin/env bash

set -euo pipefail

PID=78263

jcmd $PID Thread.print -l > thread-1.txt
sleep 1
jcmd $PID Thread.print -l > thread-2.txt
sleep 1
jcmd $PID Thread.print -l > thread-3.txt
