#!/usr/bin/env bash
# BridgePlay VPS Kernel and Network Optimizer (Helsinki, Finland Node)
# Targets: Ubuntu 22.04 / 24.04 LTS / Debian 12

set -euo pipefail

echo "=========================================================="
echo " BridgePlay Edge Relay Linux Kernel Network Optimizer     "
echo "=========================================================="

# 1. Update system packages
apt-get update && apt-get install -y python3 curl ufw

# 2. Kernel Socket and Buffer Tuning for Low-Latency Gaming UDP
cat <<EOF > /etc/sysctl.d/99-bridgeplay-gaming.conf
# Maximum receive and send socket buffers (16MB)
net.core.rmem_max = 16777216
net.core.wmem_max = 16777216
net.core.rmem_default = 2097152
net.core.wmem_default = 2097152

# Low-latency UDP queue limits
net.ipv4.udp_rmem_min = 8192
net.ipv4.udp_wmem_min = 8192

# Enable BBR Congestion Control and Fair Queueing
net.core.default_qdisc = fq
net.ipv4.tcp_congestion_control = bbr

# Increase network interface packet backlog
net.core.netdev_max_backlog = 10000

# Fast socket recycling
net.ipv4.ip_local_port_range = 1024 65535
EOF

sysctl --system

# 3. Configure Firewall for BridgePlay UDP port 8910
ufw allow 8910/udp comment 'BridgePlay Multipath UDP Relay'
ufw reload || true

echo "=========================================================="
echo " Kernel optimization complete.                           "
echo " To run standalone Python relay:                         "
echo " python3 server/python/relay_server.py --listen-port 8910 "
echo "=========================================================="
