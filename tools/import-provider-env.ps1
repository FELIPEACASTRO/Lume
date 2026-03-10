param(
    [Parameter(Mandatory = $true)]
    [string]$InputPath,
    [string]$TargetEnvPath = ".env",
    [string]$CatalogSourcePath = "backend/src/main/java/com/lume/workspace/service/ProviderCatalogService.java"
)

$ErrorActionPreference = "Stop"

function Parse-EnvFile {
    param([string]$Path)

    $entries = New-Object System.Collections.ArrayList
    if (-not (Test-Path $Path)) {
        return $entries
    }

    foreach ($line in Get-Content $Path) {
        if ($line -match '^\s*$') {
            [void]$entries.Add([PSCustomObject]@{ Kind = "blank"; Raw = $line })
            continue
        }

        if ($line -match '^\s*#') {
            [void]$entries.Add([PSCustomObject]@{ Kind = "comment"; Raw = $line })
            continue
        }

        $index = $line.IndexOf("=")
        if ($index -lt 1) {
            [void]$entries.Add([PSCustomObject]@{ Kind = "raw"; Raw = $line })
            continue
        }

        [void]$entries.Add([PSCustomObject]@{
                Kind  = "pair"
                Key   = $line.Substring(0, $index).Trim()
                Value = $line.Substring($index + 1)
            })
    }

    return $entries
}

function Collect-RecognizedEnvVars {
    param([string]$CatalogPath)

    if (-not (Test-Path $CatalogPath)) {
        throw "Catalogo nao encontrado em $CatalogPath"
    }

    $raw = Get-Content -Raw $CatalogPath
    $matches = [regex]::Matches($raw, 'cred\("[^"]+",\s*"[^"]+",\s*"(?<env>[A-Z0-9_]+)"')
    $envs = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($match in $matches) {
        [void]$envs.Add($match.Groups["env"].Value)
    }
    return $envs
}

function Parse-IncomingEnv {
    param(
        [string]$Path,
        [System.Collections.Generic.HashSet[string]]$AllowedEnvVars
    )

    $incoming = [ordered]@{}
    foreach ($line in Get-Content $Path) {
        if ($line -match '^\s*$' -or $line -match '^\s*#') {
            continue
        }

        $index = $line.IndexOf("=")
        if ($index -lt 1) {
            continue
        }

        $key = $line.Substring(0, $index).Trim()
        $value = $line.Substring($index + 1)

        if (-not $AllowedEnvVars.Contains($key)) {
            continue
        }

        if ([string]::IsNullOrWhiteSpace($value)) {
            continue
        }

        $incoming[$key] = $value
    }

    return $incoming
}

function Test-GoogleCredentialsJson {
    param([string]$Value)

    try {
        $json = $Value | ConvertFrom-Json
        return -not [string]::IsNullOrWhiteSpace($json.project_id)
    }
    catch {
        return $false
    }
}

function Test-CloudflareAccountId {
    param([string]$Value)

    return $Value -match '^[a-fA-F0-9]{32}$'
}

function Validate-EnvValue {
    param(
        [string]$Key,
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return [PSCustomObject]@{ Valid = $false; Reason = "empty" }
    }

    if ($Value -match '[A-Z0-9_]+=') {
        return [PSCustomObject]@{ Valid = $false; Reason = "embedded_env_assignment" }
    }

    switch ($Key) {
        "GOOGLE_CLOUD_CREDENTIALS_JSON" {
            if (Test-GoogleCredentialsJson -Value $Value) {
                return [PSCustomObject]@{ Valid = $true; Reason = "ok" }
            }
            return [PSCustomObject]@{ Valid = $false; Reason = "invalid_google_credentials_json" }
        }
        "CLOUDFLARE_ACCOUNT_ID" {
            if (Test-CloudflareAccountId -Value $Value) {
                return [PSCustomObject]@{ Valid = $true; Reason = "ok" }
            }
            return [PSCustomObject]@{ Valid = $false; Reason = "invalid_cloudflare_account_id" }
        }
        default {
            return [PSCustomObject]@{ Valid = $true; Reason = "ok" }
        }
    }
}

if (-not (Test-Path $InputPath)) {
    throw "Arquivo de entrada nao encontrado em $InputPath"
}

$recognizedEnvVars = Collect-RecognizedEnvVars -CatalogPath $CatalogSourcePath
$existingEntries = [System.Collections.ArrayList](Parse-EnvFile -Path $TargetEnvPath)
$incoming = Parse-IncomingEnv -Path $InputPath -AllowedEnvVars $recognizedEnvVars

$existingValues = @{}
$existingIndexes = @{}
for ($i = 0; $i -lt $existingEntries.Count; $i++) {
    $entry = $existingEntries[$i]
    if ($entry.Kind -eq "pair") {
        $existingValues[$entry.Key] = $entry.Value
        $existingIndexes[$entry.Key] = $i
    }
}

$added = New-Object System.Collections.Generic.List[string]
$updated = New-Object System.Collections.Generic.List[string]
$keptExisting = New-Object System.Collections.Generic.List[string]
$blocked = New-Object System.Collections.Generic.List[string]
$sanitizedExisting = New-Object System.Collections.Generic.List[string]

foreach ($key in @($existingIndexes.Keys)) {
    $index = $existingIndexes[$key]
    $entry = $existingEntries[$index]
    if ($entry.Kind -ne "pair") {
        continue
    }

    $validation = Validate-EnvValue -Key $key -Value $entry.Value
    if ($validation.Valid) {
        continue
    }

    if ($validation.Reason -in @("invalid_google_credentials_json", "invalid_cloudflare_account_id", "embedded_env_assignment")) {
        $existingEntries[$index] = [PSCustomObject]@{
            Kind  = "pair"
            Key   = $key
            Value = ""
        }
        $existingValues[$key] = ""
        [void]$sanitizedExisting.Add($key)
    }
}

foreach ($key in $incoming.Keys) {
    $value = $incoming[$key]

    $validation = Validate-EnvValue -Key $key -Value $value
    if (-not $validation.Valid) {
        [void]$blocked.Add($key)
        continue
    }

    if ($existingIndexes.ContainsKey($key)) {
        $currentValue = $existingValues[$key]
        if (-not [string]::IsNullOrWhiteSpace($currentValue)) {
            [void]$keptExisting.Add($key)
            continue
        }

        $index = $existingIndexes[$key]
        $existingEntries[$index] = [PSCustomObject]@{
            Kind  = "pair"
            Key   = $key
            Value = $value
        }
        [void]$updated.Add($key)
        continue
    }

    [void]$existingEntries.Add([PSCustomObject]@{
            Kind  = "pair"
            Key   = $key
            Value = $value
        })
    [void]$added.Add($key)
}

$outputLines = foreach ($entry in $existingEntries) {
    switch ($entry.Kind) {
        "pair" { "{0}={1}" -f $entry.Key, $entry.Value }
        default { $entry.Raw }
    }
}

Set-Content -Path $TargetEnvPath -Value $outputLines -Encoding UTF8

[PSCustomObject]@{
    added        = @($added | Sort-Object)
    updated      = @($updated | Sort-Object)
    keptExisting = @($keptExisting | Sort-Object)
    blocked      = @($blocked | Sort-Object)
    sanitized    = @($sanitizedExisting | Sort-Object)
} | ConvertTo-Json -Depth 4
