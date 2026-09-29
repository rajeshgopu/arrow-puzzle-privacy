@echo off
rem Launches the Arrow Puzzle level viewer. Pass -Dump or -Smoke through as arguments.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0LevelViewer.ps1" %*
