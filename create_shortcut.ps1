# Create Windows Desktop / Explorer shortcut with official UniDesk icon
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $ScriptDir) { $ScriptDir = Get-Location }

$WshShell = New-Object -ComObject WScript.Shell
$LnkPath = Join-Path $ScriptDir "UniDesk.lnk"
$JarPath = Join-Path $ScriptDir "UniDesk.jar"
$IcoPath = Join-Path $ScriptDir "UniDesk.ico"

$Shortcut = $WshShell.CreateShortcut($LnkPath)
$Shortcut.TargetPath = "javaw.exe"
$Shortcut.Arguments = "-jar `"$JarPath`""
$Shortcut.WorkingDirectory = "$ScriptDir"
if (Test-Path $IcoPath) {
    $Shortcut.IconLocation = "$IcoPath,0"
}
$Shortcut.Description = "UniDesk Student Information System"
$Shortcut.Save()

Write-Host "[SUCCESS] Created Windows Shortcut with custom icon at $LnkPath"
