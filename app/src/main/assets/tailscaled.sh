#!/system/bin/sh
TAILSCALE_FILE_DIR=/storage/emulated/legacy/Download ./tailscaled --tun=tun --state=/data/local/tmp/tailscaled.state --socket=/data/local/tmp/tailscaled.sock
