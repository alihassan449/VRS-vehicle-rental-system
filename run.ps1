$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force -Path bin | Out-Null
$sources = (Get-ChildItem -Recurse src -Filter *.java).FullName
javac -cp "lib/*" -d bin $sources
java -cp "bin;lib/*" com.rental.Main @args
