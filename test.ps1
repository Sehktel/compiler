$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$outDir = ".\out\$timestamp"

if (-not (Test-Path -Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force
}

Get-ChildItem -Path ".\test\c4ast\*.c" | ForEach-Object {
    $baseName = $_.BaseName
    $inputFile = $_.FullName
    $outFile = Join-Path $outDir "$baseName.out"
    
    Write-Host "Processing: $($_.Name) -> $outFile" -ForegroundColor Green
    
    lein run -- --debug --trace $inputFile | Out-File -FilePath $outFile -Encoding utf8
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Success: Output saved to $outFile" -ForegroundColor Green
    } else {
        Write-Host "Error processing $($_.Name)" -ForegroundColor Red
    }
    
    Write-Host ("-" * 50)
}

Write-Host "All files processed in $outDir!" -ForegroundColor Cyan
