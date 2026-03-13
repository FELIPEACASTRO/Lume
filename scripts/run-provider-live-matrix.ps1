param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [string]$Email = "operator@lume.local",
    [string]$Password = "lume12345",
    [string[]]$FallbackPasswords = @("lume123", "Admin@123456"),
    [string]$OutputJson = "reports/provider-live-test-results.json",
    [string]$OutputCsv = "reports/provider-live-test-results.csv",
    [int]$RetryCount = 2,
    [int]$RetryWaitSeconds = 20,
    [switch]$SkipComposeUp,
    [switch]$SkipBootstrap,
    [switch]$DisableCircuitRestart
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$SuccessStatuses = @("completed", "succeeded", "submitted", "running", "ready")
$FailureLikeStatuses = @("provider_error", "missing_credentials", "failed", "error", "compliance_blocked", "blocked", "unsupported")
$SampleAudioUrl = "https://raw.githubusercontent.com/anars/blank-audio/master/1-second-of-silence.mp3"
$SampleImageUrl = "https://placehold.co/1024x1024/png?text=Lume+Sample"
$SampleDocumentUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"

function Ensure-DirectoryForFile {
    param([Parameter(Mandatory = $true)][string]$Path)
    $directory = Split-Path -Path $Path -Parent
    if ($directory -and -not (Test-Path $directory)) {
        New-Item -ItemType Directory -Path $directory | Out-Null
    }
}

function Get-NowIso {
    return (Get-Date).ToString("s")
}

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

        [pscustomobject]@{
            Ok         = $true
            StatusCode = 200
            LatencyMs  = [int]((Get-Date) - $started).TotalMilliseconds
            Body       = $response
            ErrorBody  = $null
            Error      = $null
        }
    } catch {
        $httpError = Read-HttpError -ErrorRecord $_
        [pscustomobject]@{
            Ok         = $false
            StatusCode = $httpError.StatusCode
            LatencyMs  = [int]((Get-Date) - $started).TotalMilliseconds
            Body       = $null
            ErrorBody  = $httpError.Body
            Error      = $httpError.Message
        }
    }
}

function Get-CandidatePasswords {
    $candidates = New-Object System.Collections.Generic.List[string]
    if ($Password -and $Password.Trim().Length -gt 0) {
        $candidates.Add($Password.Trim())
    }

    foreach ($fallback in $FallbackPasswords) {
        if ($fallback -and $fallback.Trim().Length -gt 0 -and -not $candidates.Contains($fallback.Trim())) {
            $candidates.Add($fallback.Trim())
        }
    }

    return $candidates.ToArray()
}

function Try-Authenticate {
    param([Parameter(Mandatory = $true)][string[]]$CandidatePasswords)

    foreach ($candidate in $CandidatePasswords) {
        $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
        $loginPayload = @{ email = $Email; password = $candidate }
        $loginResult = Invoke-Api -Method POST -Path "/v1/auth/login" -Session $session -Payload $loginPayload -TimeoutSec 30
        if ($loginResult.Ok) {
            return [pscustomobject]@{
                Session      = $session
                PasswordUsed = $candidate
            }
        }
    }

    throw "Falha no login. Nenhuma senha candidata autenticou o usuário '$Email'."
}

function Wait-BackendHealthy {
    param([int]$Attempts = 45, [int]$IntervalSeconds = 2)
    for ($i = 1; $i -le $Attempts; $i++) {
        try {
            $health = Invoke-RestMethod -Method Get -Uri "$BaseUrl/health" -TimeoutSec 10
            if ($null -ne $health) {
                return
            }
        } catch {
            Start-Sleep -Seconds $IntervalSeconds
        }
    }

    throw "Backend não ficou saudável em $BaseUrl/health dentro do timeout esperado."
}

function Get-ProviderHttpStatusFromText {
    param([string]$Text)
    if (-not $Text) {
        return $null
    }

    if ($Text -match "(?i)\b(?:erro\s*)?http\s*(\d{3})\b") {
        return [int]$Matches[1]
    }

    if ($Text -match "(?i)\bstatus(?:\s*code)?\s*(\d{3})\b") {
        return [int]$Matches[1]
    }

    if ($Text -match "(?i)\b(\d{3})\s+(forbidden|unauthorized|bad request|not found|too many requests|payment required|internal server error|service unavailable)\b") {
        return [int]$Matches[1]
    }

    return $null
}

function Get-ProviderHttpStatus {
    param(
        [Parameter(Mandatory = $true)][pscustomobject]$PrimaryResult,
        [pscustomobject]$SecondaryResult = $null
    )

    $texts = New-Object System.Collections.Generic.List[string]
    $results = @($PrimaryResult)
    if ($null -ne $SecondaryResult) {
        $results += $SecondaryResult
    }

    foreach ($result in $results) {
        if ($null -eq $result) {
            continue
        }

        if ($result.Error) {
            $texts.Add([string]$result.Error)
        }

        if ($result.ErrorBody) {
            $texts.Add([string]$result.ErrorBody)
        }

        if ($result.Body) {
            $body = $result.Body
            foreach ($field in @("error", "message", "detail", "status")) {
                if ($body.PSObject.Properties.Name -contains $field) {
                    $value = [string]$body.$field
                    if ($value) {
                        $texts.Add($value)
                    }
                }
            }

            if ($body.PSObject.Properties.Name -contains "attemptChain" -and $null -ne $body.attemptChain) {
                foreach ($attempt in $body.attemptChain) {
                    if ($attempt.error) {
                        $texts.Add([string]$attempt.error)
                    }
                }
            }
        }
    }

    foreach ($text in $texts) {
        $code = Get-ProviderHttpStatusFromText -Text $text
        if ($null -ne $code) {
            return $code
        }
    }

    if ($PrimaryResult.StatusCode -and [int]$PrimaryResult.StatusCode -ge 400) {
        return [int]$PrimaryResult.StatusCode
    }

    if ($null -ne $SecondaryResult -and $SecondaryResult.StatusCode -and [int]$SecondaryResult.StatusCode -ge 400) {
        return [int]$SecondaryResult.StatusCode
    }

    return $null
}

function Test-ContainsCircuitOpen {
    param(
        [pscustomobject]$ConnectivityResult,
        [pscustomobject]$CapabilityResult
    )

    $fragments = New-Object System.Collections.Generic.List[string]
    foreach ($result in @($ConnectivityResult, $CapabilityResult)) {
        if ($null -eq $result) {
            continue
        }

        if ($result.Error) {
            $fragments.Add([string]$result.Error)
        }

        if ($result.ErrorBody) {
            $fragments.Add([string]$result.ErrorBody)
        }

        if ($result.Body) {
            foreach ($field in @("error", "message", "detail", "status")) {
                if ($result.Body.PSObject.Properties.Name -contains $field) {
                    $value = [string]$result.Body.$field
                    if ($value) {
                        $fragments.Add($value)
                    }
                }
            }
        }
    }

    foreach ($fragment in $fragments) {
        if ($fragment -match "(?i)circuit[_\-\s]?open|circuit\s*breaker") {
            return $true
        }
    }

    return $false
}

function Test-TransientFailure {
    param(
        [pscustomobject]$ConnectivityResult,
        [pscustomobject]$CapabilityResult,
        [Nullable[int]]$ProviderHttpStatus
    )

    if ($ProviderHttpStatus -eq 503) {
        return $true
    }

    foreach ($result in @($ConnectivityResult, $CapabilityResult)) {
        if ($null -eq $result) {
            continue
        }

        if ($result.StatusCode -eq 503) {
            return $true
        }

        foreach ($text in @([string]$result.Error, [string]$result.ErrorBody)) {
            if ($text -match "(?i)timeout|timed out|temporarily unavailable|upstream unavailable|connection reset") {
                return $true
            }
        }
    }

    return $false
}

function Get-RandomPrompt {
    $prompts = @(
        "Explique o estoicismo em 2 frases.",
        "Liste 3 benefícios de backlog grooming para equipes B2B.",
        "Sugira uma resposta curta para um incidente de atraso em projeto.",
        "Resuma em uma frase o conceito de FinOps."
    )
    return (Get-Random -InputObject $prompts)
}

function Get-ProviderDetail {
    param(
        [Parameter(Mandatory = $true)][string]$ProviderCode,
        [Parameter(Mandatory = $true)][Microsoft.PowerShell.Commands.WebRequestSession]$Session
    )

    $detailResult = Invoke-Api -Method GET -Path "/v1/providers/$ProviderCode" -Session $Session -TimeoutSec 45
    if ($detailResult.Ok -and $null -ne $detailResult.Body) {
        return $detailResult.Body
    }

    return $null
}

function Get-CapabilitySpec {
    param(
        [Parameter(Mandatory = $true)][pscustomobject]$Provider,
        [pscustomobject]$ProviderDetail
    )

    $code = [string]$Provider.providerCode
    $category = [string]$Provider.category
    $capabilities = @()
    if ($null -ne $ProviderDetail -and $ProviderDetail.PSObject.Properties.Name -contains "capabilities" -and $null -ne $ProviderDetail.capabilities) {
        $capabilities = @($ProviderDetail.capabilities | ForEach-Object { ([string]$_).ToLowerInvariant() })
    }

    if ($category -eq "text-runtime" -or $category -eq "enterprise-gateway") {
        return [pscustomobject]@{
            capabilityTested = "chat"
            endpoint         = "/v1/chat"
            payload          = @{
                providerCode = $code
                prompt       = Get-RandomPrompt
            }
        }
    }

    if ($category -eq "research-search") {
        return [pscustomobject]@{
            capabilityTested = "search"
            endpoint         = "/v1/search"
            payload          = @{
                providerCode = $code
                query        = "finops governance workspace"
                limit        = 3
            }
        }
    }

    if ($category -eq "vector-runtime") {
        return [pscustomobject]@{
            capabilityTested = "embeddings"
            endpoint         = "/v1/embeddings"
            payload          = @{
                providerCode = $code
                input        = "teste de embeddings no Lume"
            }
        }
    }

    if ($category -eq "threat-intel") {
        return [pscustomobject]@{
            capabilityTested = "threat-intel-search"
            endpoint         = "/v1/threat-intel/search"
            payload          = @{
                providerCode  = $code
                query         = "credential exposure"
                limit         = 3
                justification = "teste-integrado-autorizado"
            }
        }
    }

    if ($category -eq "media-audio") {
        if ($capabilities -contains "translation") {
            return [pscustomobject]@{
                capabilityTested = "translation"
                endpoint         = "/v1/translation/text"
                payload          = @{
                    providerCode       = $code
                    text               = "Ola, mundo"
                    targetLanguageCode = "en"
                    sourceLanguageCode = "pt"
                }
            }
        }

        if (($capabilities -contains "nlp") -or ($capabilities -contains "entities") -or ($capabilities -contains "classification") -or ($capabilities -contains "sentiment")) {
            return [pscustomobject]@{
                capabilityTested = "nlp"
                endpoint         = "/v1/nlp/analyze"
                payload          = @{
                    providerCode = $code
                    text         = "Lume melhorou a produtividade do time sem aumentar custo."
                    analysisType = "entities"
                    languageCode = "pt"
                }
            }
        }

        if (($capabilities -contains "stt") -or ($capabilities -contains "speech-to-text")) {
            return [pscustomobject]@{
                capabilityTested = "audio-stt"
                endpoint         = "/v1/audio/stt"
                payload          = @{
                    providerCode = $code
                    audioUrl     = $SampleAudioUrl
                    languageCode = "en"
                }
            }
        }

        if (($capabilities -contains "tts") -or ($capabilities -contains "text-to-speech") -or ($capabilities -contains "voice")) {
            return [pscustomobject]@{
                capabilityTested = "audio-tts"
                endpoint         = "/v1/audio/tts"
                payload          = @{
                    providerCode = $code
                    text         = "Teste de sintese de voz no Lume."
                    voice        = "alloy"
                    format       = "mp3"
                }
            }
        }

        if ($capabilities -contains "ocr") {
            return [pscustomobject]@{
                capabilityTested = "ocr"
                endpoint         = "/v1/ocr"
                payload          = @{
                    providerCode = $code
                    imageUrl     = $SampleImageUrl
                    documentUrl  = $SampleDocumentUrl
                    languageCode = "en"
                }
            }
        }

        if ($capabilities -contains "video") {
            return [pscustomobject]@{
                capabilityTested = "video-generate"
                endpoint         = "/v1/videos/generate"
                payload          = @{
                    providerCode    = $code
                    prompt          = "A short city timelapse at dawn."
                    inputImageUrl   = $SampleImageUrl
                    durationSeconds = 4
                    aspectRatio     = "16:9"
                }
            }
        }

        if ($capabilities -contains "image") {
            return [pscustomobject]@{
                capabilityTested = "image-generate"
                endpoint         = "/v1/images/generate"
                payload          = @{
                    providerCode   = $code
                    prompt         = "Minimal clean workspace desk in natural light."
                    size           = "1024x1024"
                    numberOfImages = 1
                }
            }
        }

        switch ($code) {
            "assemblyai" {
                return [pscustomobject]@{
                    capabilityTested = "audio-stt"
                    endpoint         = "/v1/audio/stt"
                    payload          = @{
                        providerCode = $code
                        audioUrl     = $SampleAudioUrl
                        languageCode = "en"
                    }
                }
            }
            "elevenlabs" {
                return [pscustomobject]@{
                    capabilityTested = "audio-tts"
                    endpoint         = "/v1/audio/tts"
                    payload          = @{
                        providerCode = $code
                        text         = "Teste de sintese de voz no Lume."
                        voice        = "alloy"
                        format       = "mp3"
                    }
                }
            }
            "google-translation" {
                return [pscustomobject]@{
                    capabilityTested = "translation"
                    endpoint         = "/v1/translation/text"
                    payload          = @{
                        providerCode       = $code
                        text               = "Ola, mundo"
                        targetLanguageCode = "en"
                        sourceLanguageCode = "pt"
                    }
                }
            }
            "google-natural-language" {
                return [pscustomobject]@{
                    capabilityTested = "nlp"
                    endpoint         = "/v1/nlp/analyze"
                    payload          = @{
                        providerCode = $code
                        text         = "Lume melhorou a produtividade do time sem aumentar custo."
                        analysisType = "entities"
                        languageCode = "pt"
                    }
                }
            }
            "google-vision" {
                return [pscustomobject]@{
                    capabilityTested = "ocr"
                    endpoint         = "/v1/ocr"
                    payload          = @{
                        providerCode = $code
                        imageUrl     = $SampleImageUrl
                        documentUrl  = $SampleDocumentUrl
                        languageCode = "en"
                    }
                }
            }
            "runway" {
                return [pscustomobject]@{
                    capabilityTested = "video-generate"
                    endpoint         = "/v1/videos/generate"
                    payload          = @{
                        providerCode    = $code
                        prompt          = "A short city timelapse at dawn."
                        inputImageUrl   = $SampleImageUrl
                        durationSeconds = 4
                        aspectRatio     = "16:9"
                    }
                }
            }
            default {
                return [pscustomobject]@{
                    capabilityTested = "image-generate"
                    endpoint         = "/v1/images/generate"
                    payload          = @{
                        providerCode   = $code
                        prompt         = "Minimal clean workspace desk in natural light."
                        size           = "1024x1024"
                        numberOfImages = 1
                    }
                }
            }
        }
    }

    return [pscustomobject]@{
        capabilityTested = "chat"
        endpoint         = "/v1/chat"
        payload          = @{
            providerCode = $code
            prompt       = Get-RandomPrompt
        }
    }
}

function Test-CapabilitySuccess {
    param(
        [Parameter(Mandatory = $true)][string]$CapabilityTested,
        [Parameter(Mandatory = $true)][pscustomobject]$CapabilityResult
    )

    if (-not $CapabilityResult.Ok -or $null -eq $CapabilityResult.Body) {
        return $false
    }

    $body = $CapabilityResult.Body

    if ($body.PSObject.Properties.Name -contains "error") {
        $errorValue = [string]$body.error
        if ($errorValue -and $errorValue.Trim().Length -gt 0) {
            return $false
        }
    }

    $status = $null
    if ($body.PSObject.Properties.Name -contains "status" -and $null -ne $body.status) {
        $status = ([string]$body.status).ToLowerInvariant()
    }

    if ($status) {
        if ($SuccessStatuses -contains $status) {
            return $true
        }

        if ($FailureLikeStatuses -contains $status) {
            return $false
        }
    }

    switch ($CapabilityTested) {
        "embeddings" {
            if ($body.PSObject.Properties.Name -contains "embeddings" -and $null -ne $body.embeddings) {
                return ($body.embeddings.Count -gt 0)
            }
            return $false
        }
        default {
            return $true
        }
    }
}

function Get-ConnectivityStatus {
    param([pscustomobject]$ConnectivityResult)

    if ($null -eq $ConnectivityResult) {
        return "not_executed"
    }

    if ($ConnectivityResult.Ok -and $null -ne $ConnectivityResult.Body -and $ConnectivityResult.Body.PSObject.Properties.Name -contains "status") {
        return [string]$ConnectivityResult.Body.status
    }

    if ($ConnectivityResult.Ok) {
        return "completed"
    }

    if ($ConnectivityResult.StatusCode) {
        return "http_$($ConnectivityResult.StatusCode)"
    }

    return "failed"
}

function Get-DetailMessage {
    param(
        [pscustomobject]$CapabilityResult,
        [pscustomobject]$ConnectivityResult
    )

    $fragments = New-Object System.Collections.Generic.List[string]
    foreach ($result in @($CapabilityResult, $ConnectivityResult)) {
        if ($null -eq $result) {
            continue
        }

        if ($result.Body) {
            foreach ($field in @("error", "message", "detail", "status")) {
                if ($result.Body.PSObject.Properties.Name -contains $field) {
                    $value = [string]$result.Body.$field
                    if ($value) {
                        $fragments.Add($value)
                    }
                }
            }
        }

        if ($result.ErrorBody) {
            $fragments.Add([string]$result.ErrorBody)
        }

        if ($result.Error) {
            $fragments.Add([string]$result.Error)
        }
    }

    $detail = ($fragments | Select-Object -First 1)
    if (-not $detail) {
        $detail = "Sem detalhe retornado."
    }

    if ($detail.Length -gt 420) {
        return $detail.Substring(0, 420)
    }

    return $detail
}

Ensure-DirectoryForFile -Path $OutputCsv
Ensure-DirectoryForFile -Path $OutputJson

if (-not $SkipComposeUp) {
    Write-Host "[preflight] Subindo stack docker compose (postgres/backend/frontend)..."
    & docker compose up -d postgres backend frontend
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao executar 'docker compose up -d postgres backend frontend'."
    }
}

Write-Host "[preflight] Aguardando backend saudável..."
Wait-BackendHealthy

$bootstrapSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$setupStatusResult = Invoke-Api -Method GET -Path "/v1/setup/status" -Session $bootstrapSession -TimeoutSec 30
if (-not $setupStatusResult.Ok -or $null -eq $setupStatusResult.Body) {
    throw "Falha ao consultar setup status."
}

$setupRequired = $false
if ($setupStatusResult.Body.PSObject.Properties.Name -contains "setupRequired") {
    $setupRequired = [bool]$setupStatusResult.Body.setupRequired
}

if ($setupRequired -and -not $SkipBootstrap) {
    Write-Host "[preflight] Setup requerido. Executando bootstrap..."
    $bootstrapPayload = @{
        organizationName = "Lume Local"
        workspaceName    = "Workspace Operacional"
        adminName        = "Operador Principal"
        adminEmail       = $Email
        password         = $Password
        primaryUseCase   = "operations"
        workStyle        = "team"
        selectedPlan     = "core"
    }
    $bootstrapResult = Invoke-Api -Method POST -Path "/v1/setup/bootstrap" -Session $bootstrapSession -Payload $bootstrapPayload -TimeoutSec 60
    if (-not $bootstrapResult.Ok) {
        throw "Falha no bootstrap inicial: $($bootstrapResult.Error)"
    }
}

Write-Host "[auth] Autenticando sessão..."
$auth = Try-Authenticate -CandidatePasswords (Get-CandidatePasswords)
$webSession = $auth.Session

$providersResult = Invoke-Api -Method GET -Path "/v1/providers/status" -Session $webSession -TimeoutSec 60
if (-not $providersResult.Ok -or $null -eq $providersResult.Body) {
    throw "Falha ao obter providers/status."
}

$allProviders = @($providersResult.Body)
$targetProviders = @($allProviders | Where-Object { [bool]$_.executionSupported } | Sort-Object providerCode)
Write-Host "[matrix] Providers alvo (executionSupported=true): $($targetProviders.Count)"

$csvRows = New-Object System.Collections.Generic.List[object]
$jsonRows = New-Object System.Collections.Generic.List[object]

$consecutiveCircuitOpen = 0
$maxAttempts = 1 + [Math]::Max($RetryCount, 0)

foreach ($provider in $targetProviders) {
    $providerCode = [string]$provider.providerCode
    $category = [string]$provider.category
    $configured = [bool]$provider.configured
    $executionSupported = [bool]$provider.executionSupported
    $providerDetail = Get-ProviderDetail -ProviderCode $providerCode -Session $webSession
    $capabilitySpec = Get-CapabilitySpec -Provider $provider -ProviderDetail $providerDetail

    Write-Host "[provider] $providerCode ($category) => $($capabilitySpec.capabilityTested)"

    $attempts = New-Object System.Collections.Generic.List[object]
    $hadRejectedAttempt = $false

    for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
        $connectivityResult = Invoke-Api -Method POST -Path "/v1/providers/$providerCode/connectivity-test" -Session $webSession -TimeoutSec 120
        $capabilityResult = Invoke-Api -Method POST -Path $capabilitySpec.endpoint -Session $webSession -Payload $capabilitySpec.payload -TimeoutSec 180

        $providerHttpStatus = Get-ProviderHttpStatus -PrimaryResult $capabilityResult -SecondaryResult $connectivityResult
        $successOperational = Test-CapabilitySuccess -CapabilityTested $capabilitySpec.capabilityTested -CapabilityResult $capabilityResult

        $attemptStatus = "FAIL_REJECTED"
        if ($successOperational) {
            $attemptStatus = "PASS_ACCEPTED"
        } elseif ($providerHttpStatus -eq 403) {
            $attemptStatus = "PASS_ACCEPTED_403"
        } else {
            $hadRejectedAttempt = $true
        }

        $isTransient = Test-TransientFailure -ConnectivityResult $connectivityResult -CapabilityResult $capabilityResult -ProviderHttpStatus $providerHttpStatus
        $containsCircuitOpen = Test-ContainsCircuitOpen -ConnectivityResult $connectivityResult -CapabilityResult $capabilityResult
        $connectivityStatus = Get-ConnectivityStatus -ConnectivityResult $connectivityResult
        $detail = Get-DetailMessage -CapabilityResult $capabilityResult -ConnectivityResult $connectivityResult

        $attempts.Add([pscustomobject]@{
            attempt             = $attempt
            timestamp           = Get-NowIso
            capabilityTested    = $capabilitySpec.capabilityTested
            endpoint            = $capabilitySpec.endpoint
            connectivityStatus  = $connectivityStatus
            connectivityHttp    = $connectivityResult.StatusCode
            capabilityHttp      = $capabilityResult.StatusCode
            providerHttpStatus  = $providerHttpStatus
            latencyMs           = $capabilityResult.LatencyMs
            attemptStatus       = $attemptStatus
            acceptedByRule      = ($attemptStatus -ne "FAIL_REJECTED")
            transientFailure    = $isTransient
            circuitOpenDetected = $containsCircuitOpen
            detail              = $detail
        })

        if ($attemptStatus -eq "FAIL_REJECTED" -and $isTransient -and $attempt -lt $maxAttempts) {
            Write-Host "  [retry] $providerCode tentativa $attempt falhou por condição transiente. Aguardando $RetryWaitSeconds s..."
            Start-Sleep -Seconds $RetryWaitSeconds
            continue
        }

        break
    }

    $finalAttempt = $attempts[$attempts.Count - 1]
    $finalStatus = [string]$finalAttempt.attemptStatus
    if ($hadRejectedAttempt) {
        $finalStatus = "FAIL_REJECTED"
    }

    $acceptedByRule = ($finalStatus -ne "FAIL_REJECTED")
    $providerHttpStatusFinal = $finalAttempt.providerHttpStatus
    $connectivityStatusFinal = [string]$finalAttempt.connectivityStatus
    $detailFinal = [string]$finalAttempt.detail
    $latencyFinal = $finalAttempt.latencyMs

    $csvRows.Add([pscustomobject]@{
        providerCode       = $providerCode
        category           = $category
        configured         = $configured
        executionSupported = $executionSupported
        capabilityTested   = $capabilitySpec.capabilityTested
        connectivityStatus = $connectivityStatusFinal
        providerHttpStatus = $providerHttpStatusFinal
        acceptedByRule     = $acceptedByRule
        finalStatus        = $finalStatus
        detail             = $detailFinal
        latencyMs          = $latencyFinal
        timestamp          = Get-NowIso
    })

    $jsonRows.Add([pscustomobject]@{
        providerCode             = $providerCode
        providerName             = [string]$provider.providerName
        category                 = $category
        configured               = $configured
        executionSupported       = $executionSupported
        implementationStatus     = [string]$provider.implementationStatus
        readinessStatus          = [string]$provider.readinessStatus
        missingCredentialEnvVars = @($provider.missingCredentialEnvVars)
        capabilityTested         = $capabilitySpec.capabilityTested
        endpoint                 = $capabilitySpec.endpoint
        providerHttpStatus       = $providerHttpStatusFinal
        acceptedByRule           = $acceptedByRule
        finalStatus              = $finalStatus
        detail                   = $detailFinal
        attempts                 = $attempts
    })

    if ($finalAttempt.circuitOpenDetected) {
        $consecutiveCircuitOpen++
    } else {
        $consecutiveCircuitOpen = 0
    }

    if ($consecutiveCircuitOpen -ge 2 -and -not $DisableCircuitRestart) {
        Write-Host "[circuit] Detectado circuit_open em sequência. Reiniciando backend para evitar falso negativo..."
        & docker compose restart backend
        if ($LASTEXITCODE -eq 0) {
            Start-Sleep -Seconds 20
            try {
                Wait-BackendHealthy
                $auth = Try-Authenticate -CandidatePasswords (Get-CandidatePasswords)
                $webSession = $auth.Session
            } catch {
                Write-Host "[circuit] Reinício executado, mas houve falha para recuperar sessão: $($_.Exception.Message)"
            }
        }
        $consecutiveCircuitOpen = 0
    }
}

$csvRows | Export-Csv -Path $OutputCsv -NoTypeInformation -Encoding UTF8
$jsonRows | ConvertTo-Json -Depth 20 | Set-Content -Path $OutputJson -Encoding UTF8

Write-Host ""
Write-Host "=== RESUMO FINAL ==="
$csvRows | Group-Object finalStatus | Sort-Object Name | ForEach-Object {
    Write-Host ("{0}: {1}" -f $_.Name, $_.Count)
}
Write-Host "CSV:  $OutputCsv"
Write-Host "JSON: $OutputJson"
