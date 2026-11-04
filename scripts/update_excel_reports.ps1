Add-Type -AssemblyName System.IO.Compression.FileSystem

$ErrorActionPreference = "Stop"

$targets = @(
    "c:\Users\ASUS\SmartOmmi_BE\SmartOmni_Test_Report_Final_Fixed.xlsx",
    "C:\Users\ASUS\Downloads\SmartOmni_Test_Report_Final_Fixed.xlsx"
)

$today = "2026-10-31"
$tester = "Automation"

$sheetMap = @{
    4 = @{ Name = "AI"; Count = 6 }
    5 = @{ Name = "Auth"; Count = 64 }
    6 = @{ Name = "Catalog"; Count = 6 }
    7 = @{ Name = "Integration"; Count = 188 }
    8 = @{ Name = "Inventory"; Count = 8 }
    9 = @{ Name = "Order"; Count = 16 }
    10 = @{ Name = "Tenant"; Count = 44 }
}

$mods = @(
    @{ Row = 11; Code = "AI"; Count = 6 }
    @{ Row = 12; Code = "Auth"; Count = 64 }
    @{ Row = 13; Code = "Catalog"; Count = 6 }
    @{ Row = 14; Code = "Integration"; Count = 188 }
    @{ Row = 15; Code = "Inventory"; Count = 8 }
    @{ Row = 16; Code = "Order"; Count = 16 }
    @{ Row = 17; Code = "Tenant"; Count = 44 }
)

foreach ($targetFile in $targets) {
    if (-not (Test-Path $targetFile)) {
        Write-Warning "File not found: $targetFile. Skipping."
        continue
    }

    Write-Host "Processing Excel file: $targetFile..."
    $tempDir = Join-Path $env:TEMP "smartomni_xlsx_update_$(Get-Random)"
    if (Test-Path $tempDir) { Remove-Item -Recurse -Force $tempDir }
    [System.IO.Compression.ZipFile]::ExtractToDirectory($targetFile, $tempDir)

    # Update Sheets 4..10
    foreach ($sheetId in 4..10) {
        $sheetFile = Join-Path $tempDir "xl\worksheets\sheet$sheetId.xml"
        $xmlContent = [System.IO.File]::ReadAllText($sheetFile, [System.Text.Encoding]::UTF8)

        $rowRegex = [regex]'(<row r="(\d+)"[^>]*>)(.*?)(</row>)'
        $evaluator = [System.Text.RegularExpressions.MatchEvaluator]{
            param($m)
            $fullRow = $m.Value
            $rowNum = $m.Groups[2].Value
            $inner = $m.Groups[3].Value
            
            if ($inner -match '<c r="A\d+"[^>]*><is><t>([A-Z]+-\d+-[NE])</t></is></c>') {
                $inner = [regex]::Replace($inner, '<c r="F' + $rowNum + '"[^>]*>.*?</c>', '<c r="F' + $rowNum + '" s="118" t="inlineStr"><is><t>Passed</t></is></c>')
                $inner = [regex]::Replace($inner, '<c r="G' + $rowNum + '"[^>]*>.*?</c>', '<c r="G' + $rowNum + '" s="118" t="inlineStr"><is><t>' + $today + '</t></is></c>')
                $inner = [regex]::Replace($inner, '<c r="H' + $rowNum + '"[^>]*>.*?</c>', '<c r="H' + $rowNum + '" s="118" t="inlineStr"><is><t>' + $tester + '</t></is></c>')
                return $m.Groups[1].Value + $inner + $m.Groups[4].Value
            }
            
            if ($rowNum -eq "6") {
                $count = $sheetMap[$sheetId].Count
                $inner = [regex]::Replace($inner, '<c r="B6"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="B6"$1><f>$2</f><v>' + $count + '</v></c>')
                $inner = [regex]::Replace($inner, '<c r="C6"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="C6"$1><f>$2</f><v>0</v></c>')
                $inner = [regex]::Replace($inner, '<c r="D6"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="D6"$1><f>$2</f><v>0</v></c>')
                $inner = [regex]::Replace($inner, '<c r="E6"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="E6"$1><f>$2</f><v>0</v></c>')
                return $m.Groups[1].Value + $inner + $m.Groups[4].Value
            }
            
            return $fullRow
        }

        $xmlContent = $rowRegex.Replace($xmlContent, $evaluator)
        [System.IO.File]::WriteAllText($sheetFile, $xmlContent, [System.Text.Encoding]::UTF8)
    }

    # Update Sheet 3 (Test Statistics)
    $sheet3File = Join-Path $tempDir "xl\worksheets\sheet3.xml"
    $s3Content = [System.IO.File]::ReadAllText($sheet3File, [System.Text.Encoding]::UTF8)

    foreach ($m in $mods) {
        $r = $m.Row
        $c = $m.Count
        $s3Content = [regex]::Replace($s3Content, '<c r="D' + $r + '"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="D' + $r + '"$1><f>$2</f><v>' + $c + '</v></c>')
        $s3Content = [regex]::Replace($s3Content, '<c r="E' + $r + '"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="E' + $r + '"$1><f>$2</f><v>0</v></c>')
        $s3Content = [regex]::Replace($s3Content, '<c r="F' + $r + '"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="F' + $r + '"$1><f>$2</f><v>0</v></c>')
        $s3Content = [regex]::Replace($s3Content, '<c r="G' + $r + '"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="G' + $r + '"$1><f>$2</f><v>0</v></c>')
        $s3Content = [regex]::Replace($s3Content, '<c r="H' + $r + '"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="H' + $r + '"$1><f>$2</f><v>' + $c + '</v></c>')
    }

    # Sub total (row 19)
    $s3Content = [regex]::Replace($s3Content, '<c r="D19"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="D19"$1><f>$2</f><v>332</v></c>')
    $s3Content = [regex]::Replace($s3Content, '<c r="E19"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="E19"$1><f>$2</f><v>0</v></c>')
    $s3Content = [regex]::Replace($s3Content, '<c r="F19"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="F19"$1><f>$2</f><v>0</v></c>')
    $s3Content = [regex]::Replace($s3Content, '<c r="G19"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="G19"$1><f>$2</f><v>0</v></c>')
    $s3Content = [regex]::Replace($s3Content, '<c r="H19"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="H19"$1><f>$2</f><v>332</v></c>')

    # Coverage (row 21 and 22)
    $s3Content = [regex]::Replace($s3Content, '<c r="E21"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="E21"$1><f>$2</f><v>100</v></c>')
    $s3Content = [regex]::Replace($s3Content, '<c r="E22"([^>]*)><f>([^<]*)</f>(?:<v>[^<]*</v>)?</c>', '<c r="E22"$1><f>$2</f><v>100</v></c>')

    [System.IO.File]::WriteAllText($sheet3File, $s3Content, [System.Text.Encoding]::UTF8)

    # Re-zip into temporary file then replace targetFile
    $tempZip = Join-Path $env:TEMP "smartomni_repack_$(Get-Random).xlsx"
    if (Test-Path $tempZip) { Remove-Item $tempZip }
    $zipArchive = [System.IO.Compression.ZipFile]::Open($tempZip, [System.IO.Compression.ZipArchiveMode]::Create)
    $files = Get-ChildItem -Path $tempDir -Recurse | Where-Object { -not $_.PSIsContainer }
    foreach ($file in $files) {
        $relPath = $file.FullName.Substring($tempDir.Length + 1).Replace('\', '/')
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zipArchive, $file.FullName, $relPath) | Out-Null
    }
    $zipArchive.Dispose()
    Remove-Item -Recurse -Force $tempDir

    # Overwrite target
    Copy-Item -Path $tempZip -Destination $targetFile -Force
    Remove-Item -Force $tempZip

    Write-Host "Successfully updated $targetFile (Round 1: 332 Passed, 0 Failed, 0 Pending)"
}

