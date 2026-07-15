#!/usr/bin/env bash
# Start Serena MCP server for the photosorter project
# Dashboard will be available at http://127.0.0.1:24282/dashboard/

# Kill any existing instance
pkill -f "serena start-mcp-server" 2>/dev/null || true
sleep 1

nohup serena start-mcp-server --context=codex --project-from-cwd > /tmp/serena_start.log 2>&1 &