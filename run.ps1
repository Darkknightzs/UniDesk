# Refresh PATH for current PowerShell session
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

$javaBin = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin"
if (Test-Path "$javaBin\java.exe") {
    $env:Path = "$javaBin;$env:Path"
}

Write-Host "Compiling project..." -ForegroundColor Cyan
javac -cp "lib/*" -d out sis\*.java

if ($LASTEXITCODE -eq 0) {
    Write-Host "Launching Student Information System..." -ForegroundColor Green
    Start-Process java -ArgumentList "-cp", "`"out;lib/*`"", "sis.Main"
} else {
    Write-Host "Compilation failed with exit code $LASTEXITCODE" -ForegroundColor Red
}
