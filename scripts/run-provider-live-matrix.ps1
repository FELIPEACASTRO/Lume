param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [string]$Email = "operator@lume.local",
    [string]$Password = "Admin@123456",
    [string]$OutputJson = "reports/provider-live-matrix.json",
    [string]$OutputCsv = "reports/provider-live-matrix.csv"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Read-HttpError {
    param(
        [Parameter(Mandatory = $true)]
        [System.Management.Automation.ErrorRecord]$ErrorRecord
    )

    $statusCode = $null
    $body = $null
    $message = $ErrorRecord.Exception.Message

    if ($null -ne $ErrorRecord.Exception -and $null -ne $ErrorRecord.Exception.Response) {
        try {
            $statusCode = [int]$ErrorRecord.Exception.Response.StatusCode.value__
        } catch {
            $statusCode = $null
        }

        try {
            $stream = $ErrorRecord.Exception.Response.GetResponseStream()
            if ($null -ne $stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $body = $reader.ReadToEnd()
                $reader.Dispose()
            }
        } catch {
            $body = $null
        }
    }

    [pscustomobject]@{
        StatusCode = $statusCode
        Body       = $body
        Message    = $message
    }
}

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet("GET", "POST")]
        [string]$Method,
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session,
        [object]$Payload = $null,
        [int]$TimeoutSec = 120
    )

    $uri = "$BaseUrl$Path"
    $started = Get-Date
    try {
        if ($null -eq $Payload) {
            $response = Invoke-RestMethod -Method $Method -Uri $uri -WebSession $Session -TimeoutSec $TimeoutSec
        } else {
            $json = $Payload | ConvertTo-Json -Depth 20 -Compress
            $response = Invoke-RestMethod -Method $Method -Uri $uri -WebSession $Session -ContentType "application/json" -Body $json -TimeoutSec $TimeoutSec
        }

        $latencyMs = [int]((Get-Date) - $started).TotalMilliseconds
        [pscustomobject]@{
            Ok         = $true
            StatusCode = 200
            LatencyMs  = $latencyMs
            Body       = $response
            ErrorBody  = $null
            Error      = $null
        }
    } catch {
        $latencyMs = [int]((Get-Date) - $started).TotalMilliseconds
        $httpError = Read-HttpError -ErrorRecord $_
        [pscustomobject]@{
            Ok         = $false
            StatusCode = $httpError.StatusCode
            LatencyMs  = $latencyMs
            Body       = $null
            ErrorBody  = $httpError.Body
            Error      = $httpError.Message
        }
    }
}

function New-CapabilitySpec {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$Provider
    )

    $code = [string]$Provider.providerCode
    $category = [string]$Provider.category

    switch ($code) {
        "assemblyai" {
            return [pscustomobject]@{
                Endpoint = "/v1/audio/stt"
                Payload  = @{
                    providerCode = $code
                    audioUrl     = "https://www.w3schools.com/html/horse.mp3"
                    languageCode = "en"
                }
                Blocked  = $null
            }
        }
        "elevenlabs" {
            return [pscustomobject]@{
                Endpoint = "/v1/audio/tts"
                Payload  = @{
                    providerCode = $code
                    text         = "Teste de integracao de voz"
                    voice        = "Rachel"
                    format       = "mp3_44100_128"
                }
                Blocked  = $null
            }
        }
        default {
            switch ($category) {
                "text-runtime" {
                    return [pscustomobject]@{
                        Endpoint = "/v1/chat"
                        Payload  = @{
                            providerCode = $code
                            prompt       = "Responda exatamente: teste OK"
                        }
                        Blocked  = $null
                    }
                }
                "vector-runtime" {
                    return [pscustomobject]@{
                        Endpoint = "/v1/embeddings"
                        Payload  = @{
                            providerCode = $code
                            input        = "teste de embeddings no Lume"
                        }
                        Blocked  = $null
                    }
                }
                "research-search" {
                    return [pscustomobject]@{
                        Endpoint = "/v1/search"
                        Payload  = @{
                            providerCode = $code
                            query        = "stoicism"
                            limit        = 3
                        }
                        Blocked  = $null
                    }
                }
                "media-audio" {
                    return [pscustomobject]@{
                        Endpoint = $null
                        Payload  = $null
                        Blocked  = "blocked_asset_missing"
                    }
                }
                default {
                    return [pscustomobject]@{
                        Endpoint = $null
                        Payload  = $null
                        Blocked  = "unsupported_category"
                    }
                }
            }
        }
    }
}

function Resolve-Classification {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$Provider,
        [Parameter(Mandatory = $true)]
        [string]$ConnectivityStatus,
        [Parameter(Mandatory = $true)]
        [string]$CapabilityStatus
    )

    if (-not [bool]$Provider.executionSupported) {
        if ([string]$Provider.implementationStatus -eq "blocked") {
            return "BLOCKED_COMPLIANCE"
        }
        if ([string]$Provider.readinessStatus -eq "manual" -or [string]$Provider.catalogState -eq "manual") {
            return "MANUAL_ONLY"
        }
        return "NOT_EXECUTION_SUPPORTED"
    }

    if (-not [bool]$Provider.configured) {
        return "BLOCKED_CREDENTIAL"
    }

    if ($ConnectivityStatus -eq "BLOCKED_CREDENTIAL") {
        return "BLOCKED_CREDENTIAL"
    }

    if ($ConnectivityStatus -eq "BLOCKED_BILLING") {
        return "BLOCKED_BILLING"
    }

    if ($ConnectivityStatus -eq "BLOCKED_PROVIDER_CONFIG") {
        return "BLOCKED_PROVIDER_CONFIG"
    }

    if ($ConnectivityStatus -ne "PASS") {
        return "FAIL"
    }

    if ($CapabilityStatus -eq "BLOCKED_ASSET") {
        return "BLOCKED_ASSET"
    }

    if ($CapabilityStatus -eq "PASS") {
        return "PASS"
    }

    return "FAIL"
}

if (-not (Test-Path "reports")) {
    New-Item -ItemType Directory -Path "reports" | Out-Null
}

$loginPayload = @{ email = $Email; password = $Password } | ConvertTo-Json -Compress
try {
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/v1/auth/login" -ContentType "application/json" -Body $loginPayload -SessionVariable webSession | Out-Null
} catch {
    $errorInfo = Read-HttpError -ErrorRecord $_
    throw "Falha no login para executar testes de providers. status=$($errorInfo.StatusCode) message=$($errorInfo.Message)"
}

$setupStatus = Invoke-RestMethod -Method Get -Uri "$BaseUrl/v1/setup/status" -WebSession $webSession
if ([bool]$setupStatus.setupRequired) {
    throw "Setup ainda obrigatorio. Execute bootstrap antes de rodar a matriz de providers."
}

$providersStatus = Invoke-RestMethod -Method Get -Uri "$BaseUrl/v1/providers/status" -WebSession $webSession

$results = New-Object System.Collections.Generic.List[object]

foreach ($provider in ($providersStatus | Sort-Object providerCode)) {
    $code = [string]$provider.providerCode
    $name = [string]$provider.providerName
    $category = [string]$provider.category

    $connectivityStatus = "SKIPPED"
    $connectivityHttpStatus = $null
    $connectivityLatencyMs = $null
    $connectivityMessage = $null
    $capabilityEndpoint = $null
    $capabilityStatus = "SKIPPED"
    $capabilityHttpStatus = $null
    $errorSummary = $null
    $blockedReason = $null

    if ([bool]$provider.executionSupported -and [bool]$provider.configured) {
        $connectivityResult = Invoke-Api -Method POST -Path "/v1/providers/$code/connectivity-test" -Session $webSession -TimeoutSec 120
        $connectivityHttpStatus = $connectivityResult.StatusCode
        $connectivityLatencyMs = $connectivityResult.LatencyMs

        if ($connectivityResult.Ok -and $null -ne $connectivityResult.Body) {
            $connectivityMessage = [string]$connectivityResult.Body.message
            $rawConnectivityStatus = [string]$connectivityResult.Body.status
            if ($rawConnectivityStatus -in @("completed", "submitted", "running", "processing", "manual_validation_recommended", "succeeded")) {
                $connectivityStatus = "PASS"
            } else {
                $errorSummary = [string]$connectivityResult.Body.message
                if ($errorSummary -match "Autenticacao recusada|Unauthorized|401") {
                    $connectivityStatus = "BLOCKED_CREDENTIAL"
                } elseif ($errorSummary -match "402|positive balance|credit balance|Plans \\& Billing|insufficient") {
                    $connectivityStatus = "BLOCKED_BILLING"
                } elseif ($errorSummary -match "model not found|model is not available|model is not supported|not deployed|scaled to zero|do not have access to it|not supported") {
                    $connectivityStatus = "BLOCKED_PROVIDER_CONFIG"
                } else {
                    $connectivityStatus = "FAIL"
                }
            }
        } else {
            $errorSummary = if ($null -ne $connectivityResult.ErrorBody -and $connectivityResult.ErrorBody.Length -gt 0) {
                $connectivityResult.ErrorBody
            } else {
                $connectivityResult.Error
            }
            if ($errorSummary -match "Autenticacao recusada|Unauthorized|401") {
                $connectivityStatus = "BLOCKED_CREDENTIAL"
            } elseif ($errorSummary -match "402|positive balance|credit balance|Plans \\& Billing|insufficient") {
                $connectivityStatus = "BLOCKED_BILLING"
            } elseif ($errorSummary -match "model not found|model is not available|model is not supported|not deployed|scaled to zero|do not have access to it|not supported") {
                $connectivityStatus = "BLOCKED_PROVIDER_CONFIG"
            } else {
                $connectivityStatus = "FAIL"
            }
        }

        if ($connectivityStatus -eq "PASS") {
            $capabilitySpec = New-CapabilitySpec -Provider $provider
            $capabilityEndpoint = $capabilitySpec.Endpoint

            if ($null -ne $capabilitySpec.Blocked) {
                $capabilityStatus = "BLOCKED_ASSET"
                $blockedReason = [string]$capabilitySpec.Blocked
            } else {
                $capabilityResult = Invoke-Api -Method POST -Path $capabilitySpec.Endpoint -Payload $capabilitySpec.Payload -Session $webSession -TimeoutSec 180
                $capabilityHttpStatus = $capabilityResult.StatusCode

                if ($capabilityResult.Ok -and $null -ne $capabilityResult.Body) {
                    $rawStatus = $null
                    if ($capabilityResult.Body.PSObject.Properties.Name -contains "status") {
                        $rawStatus = [string]$capabilityResult.Body.status
                    }

                    if ($rawStatus -in @("completed", "succeeded", "running", "submitted")) {
                        $capabilityStatus = "PASS"
                    } elseif ($rawStatus -in @("provider_error", "failed", "error")) {
                        $capabilityStatus = "FAIL"
                        $errorSummary = [string]$capabilityResult.Body.error
                    } else {
                        $capabilityStatus = "PASS"
                    }
                } else {
                    $capabilityStatus = "FAIL"
                    $errorSummary = if ($null -ne $capabilityResult.ErrorBody -and $capabilityResult.ErrorBody.Length -gt 0) {
                        $capabilityResult.ErrorBody
                    } else {
                        $capabilityResult.Error
                    }
                }
            }
        } else {
            $capabilityStatus = "SKIPPED"
        }
    } else {
        if (-not [bool]$provider.executionSupported) {
            $blockedReason = "execution_not_supported"
        } elseif (-not [bool]$provider.configured) {
            $blockedReason = "missing_credentials"
        }
    }

    if ($null -ne $errorSummary -and $errorSummary.Length -gt 400) {
        $errorSummary = $errorSummary.Substring(0, 400)
    }

    $classification = Resolve-Classification -Provider $provider -ConnectivityStatus $connectivityStatus -CapabilityStatus $capabilityStatus

    $results.Add([pscustomobject]@{
        timestamp               = (Get-Date).ToString("s")
        providerCode            = $code
        providerName            = $name
        category                = $category
        configured              = [bool]$provider.configured
        executionSupported      = [bool]$provider.executionSupported
        readinessStatus         = [string]$provider.readinessStatus
        implementationStatus    = [string]$provider.implementationStatus
        catalogState            = [string]$provider.catalogState
        missingCredentialEnvVars = [string](($provider.missingCredentialEnvVars -join ";"))
        connectivityStatus      = $connectivityStatus
        connectivityHttpStatus  = $connectivityHttpStatus
        connectivityLatencyMs   = $connectivityLatencyMs
        connectivityMessage     = $connectivityMessage
        capabilityEndpoint      = $capabilityEndpoint
        capabilityStatus        = $capabilityStatus
        capabilityHttpStatus    = $capabilityHttpStatus
        classification          = $classification
        blockedReason           = $blockedReason
        errorSummary            = $errorSummary
    })
}

$results | ConvertTo-Json -Depth 8 | Set-Content $OutputJson
$results | Export-Csv -Path $OutputCsv -NoTypeInformation -Encoding UTF8

$summary = $results | Group-Object classification | Sort-Object Name | Select-Object Name, Count
$summary | Format-Table -AutoSize | Out-String
