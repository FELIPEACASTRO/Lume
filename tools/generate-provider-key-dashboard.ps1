param(
    [string]$CsvPath = "reports/providers-missing-api-keys.csv",
    [string]$SourcePath = "backend/src/main/java/com/lume/workspace/service/ProviderCatalogService.java",
    [string]$OutputPath = "reports/providers-missing-api-keys.svg"
)

$ErrorActionPreference = "Stop"

function Escape-Xml {
    param([string]$Text)

    if ($null -eq $Text) {
        return ""
    }

    return [System.Security.SecurityElement]::Escape($Text)
}

function Wrap-Text {
    param(
        [string]$Text,
        [int]$MaxLen
    )

    if ([string]::IsNullOrWhiteSpace($Text)) {
        return @("")
    }

    $remaining = $Text.Trim()
    $lines = New-Object System.Collections.Generic.List[string]

    while ($remaining.Length -gt $MaxLen) {
        $cut = $remaining.LastIndexOf(" ", [Math]::Min($MaxLen, $remaining.Length - 1))
        if ($cut -lt 1) {
            $cut = $remaining.LastIndexOf(";", [Math]::Min($MaxLen, $remaining.Length - 1))
        }
        if ($cut -lt 1) {
            $cut = $MaxLen
        }

        $lines.Add($remaining.Substring(0, $cut).Trim())
        $remaining = $remaining.Substring($cut).TrimStart(" ", ";")
    }

    if ($remaining.Length -gt 0) {
        $lines.Add($remaining)
    }

    return @($lines.ToArray())
}

function Add-Text {
    param(
        [System.Text.StringBuilder]$Builder,
        [string]$CssClass,
        [int]$X,
        [int]$Y,
        [string]$Value
    )

    [void]$Builder.AppendLine(("  <text class='{0}' x='{1}' y='{2}'>{3}</text>" -f $CssClass, $X, $Y, (Escape-Xml $Value)))
}

function Add-BoxLabel {
    param(
        [System.Text.StringBuilder]$Builder,
        [int]$X,
        [int]$Y,
        [string]$Label
    )

    [void]$Builder.AppendLine(("  <rect class='check-box' x='{0}' y='{1}' width='12' height='12' rx='2' />" -f $X, $Y))
    [void]$Builder.AppendLine(("  <text class='check-label' x='{0}' y='{1}'>{2}</text>" -f ($X + 18), ($Y + 11), (Escape-Xml $Label)))
}

if (-not (Test-Path $CsvPath)) {
    throw "CSV nao encontrado: $CsvPath"
}

if (-not (Test-Path $SourcePath)) {
    throw "Fonte do catalogo nao encontrada: $SourcePath"
}

$rows = Import-Csv -Path $CsvPath
$source = Get-Content -Raw -Path $SourcePath
$start = $source.IndexOf("private Map<String, ProviderDefinition> buildProviders()")
$end = $source.IndexOf("private Map<String, ModelDefinition> buildModels()")

if ($start -lt 0 -or $end -le $start) {
    throw "Nao foi possivel localizar buildProviders no catalogo."
}

$section = $source.Substring($start, $end - $start)
$providerMatches = [regex]::Matches(
    $section,
    'register\(providers,\s*provider\("(?<code>[^"]+)",\s*"(?<name>[^"]+)",\s*"(?<category>[^"]+)"(?<body>[\s\S]*?)\)\);'
)

$providerMeta = @{}
foreach ($match in $providerMatches) {
    $providerMeta[$match.Groups["code"].Value] = [PSCustomObject]@{
        code = $match.Groups["code"].Value
        name = $match.Groups["name"].Value
        category = $match.Groups["category"].Value
    }
}

$categoryLabels = @{
    "text-runtime" = "Texto e chat"
    "vector-runtime" = "Embeddings e rerank"
    "research-search" = "Busca e pesquisa"
    "media-audio" = "Midia, audio e OCR"
    "enterprise-gateway" = "Gateways enterprise"
    "threat-intel" = "Threat intel"
}

$categoryOrder = @(
    "text-runtime",
    "vector-runtime",
    "research-search",
    "media-audio",
    "enterprise-gateway",
    "threat-intel"
)

$priorityByCategory = @{
    "text-runtime" = "Alta"
    "vector-runtime" = "Alta"
    "research-search" = "Alta"
    "media-audio" = "Media"
    "enterprise-gateway" = "Media"
    "threat-intel" = "Restrita"
}

$categoryColor = @{
    "text-runtime" = "#d9ecff"
    "vector-runtime" = "#dff5e4"
    "research-search" = "#fdf0d6"
    "media-audio" = "#f7e0f0"
    "enterprise-gateway" = "#e5e7ff"
    "threat-intel" = "#ffe0e0"
}

$priorityColor = @{
    "Alta" = "#b42318"
    "Media" = "#b54708"
    "Restrita" = "#6941c6"
}

function Resolve-Note {
    param(
        [string]$ProviderCode,
        [string]$Category
    )

    switch -Regex ($ProviderCode) {
        "^google-" { return "Uma credencial shared cobre todos os servicos Google Cloud desta familia." }
        "^aws-bedrock$" { return "Nao e API key unica. Requer IAM access key e regiao valida." }
        "^azure-openai$" { return "Precisa endpoint, deployment e api-version alem da chave." }
        "^darkowl$|^darknetsearch$|^flare$|^fullhunt$|^onion-search-engine$|^twingly$" { return "Uso restrito. Exige compliance, aprovacao e testes separados." }
        "^github-models$" { return "PAT precisa escopo compatvel com GitHub Models." }
        "^dashscope-qwen$" { return "Validar regiao gratuita antes de promover para uso live." }
        default {
            if ($Category -eq "threat-intel") {
                return "Bloquear ate credencial, compliance e autorizacao estarem fechados."
            }
            return "Preencher .env e rodar connectivity-test apos criar a credencial."
        }
    }
}

$enrichedRows = foreach ($row in $rows) {
    $meta = $providerMeta[$row.provider_code]
    if ($null -eq $meta) {
        continue
    }

    $category = $meta.category
    [PSCustomObject]@{
        provider_code = $row.provider_code
        product_name = $row.product_name
        required_envs = $row.required_envs -replace ", ", "; "
        credential_portal_url = $row.credential_portal_url
        category = $category
        category_label = $categoryLabels[$category]
        priority = $priorityByCategory[$category]
        note = Resolve-Note -ProviderCode $row.provider_code -Category $category
    }
}

$grouped = @{}
foreach ($category in $categoryOrder) {
    $grouped[$category] = @($enrichedRows | Where-Object { $_.category -eq $category } | Sort-Object product_name)
}

$totalCount = $enrichedRows.Count
$highCount = @($enrichedRows | Where-Object { $_.priority -eq "Alta" }).Count
$restrictedCount = @($enrichedRows | Where-Object { $_.priority -eq "Restrita" }).Count
$googleCount = @($enrichedRows | Where-Object { $_.provider_code -like "google-*" }).Count

$layout = @{
    marginLeft = 36
    marginTop = 34
    cardGap = 16
    cardWidth = 270
    cardHeight = 88
    sectionGap = 22
    sectionHeaderHeight = 34
    tableHeaderHeight = 34
    rowPadding = 12
    lineHeight = 15
}

$columns = @(
    [PSCustomObject]@{ key = "index"; label = "#"; width = 40; type = "plain" },
    [PSCustomObject]@{ key = "product"; label = "Produto"; width = 250; type = "product" },
    [PSCustomObject]@{ key = "env"; label = "ENV obrigatoria"; width = 330; type = "env" },
    [PSCustomObject]@{ key = "portal"; label = "Portal / criacao"; width = 380; type = "portal" },
    [PSCustomObject]@{ key = "control"; label = "Controle"; width = 280; type = "control" },
    [PSCustomObject]@{ key = "note"; label = "Observacao"; width = 420; type = "note" }
)

$tableWidth = 0
foreach ($column in $columns) {
    $tableWidth += $column.width
}

$currentY = $layout.marginTop + 120
$sectionRows = New-Object System.Collections.Generic.List[object]
$globalIndex = 1

foreach ($category in $categoryOrder) {
    $rowsForCategory = $grouped[$category]
    if ($rowsForCategory.Count -eq 0) {
        continue
    }

    $sectionInfo = [PSCustomObject]@{
        category = $category
        label = $categoryLabels[$category]
        color = $categoryColor[$category]
        startY = $currentY
        rows = New-Object System.Collections.Generic.List[object]
    }

    $currentY += $layout.sectionHeaderHeight + $layout.tableHeaderHeight

    foreach ($row in $rowsForCategory) {
        $envLines = Wrap-Text -Text $row.required_envs -MaxLen 34
        $portalLines = Wrap-Text -Text $row.credential_portal_url -MaxLen 42
        $noteLines = Wrap-Text -Text $row.note -MaxLen 48
        $productLines = Wrap-Text -Text $row.product_name -MaxLen 28

        $maxLines = [Math]::Max(
            [Math]::Max($envLines.Count, $portalLines.Count),
            [Math]::Max($noteLines.Count, $productLines.Count)
        )

        $rowHeight = ($maxLines * $layout.lineHeight) + 54

        $sectionInfo.rows.Add([PSCustomObject]@{
            y = $currentY
            height = $rowHeight
            index = $globalIndex
            row = $row
            envLines = $envLines
            portalLines = $portalLines
            noteLines = $noteLines
            productLines = $productLines
        })

        $currentY += $rowHeight
        $globalIndex++
    }

    $sectionRows.Add($sectionInfo)
    $currentY += $layout.sectionGap
}

$svgWidth = $layout.marginLeft + $tableWidth + 36
$svgHeight = $currentY + 24
$builder = New-Object System.Text.StringBuilder

[void]$builder.AppendLine(("<?xml version='1.0' encoding='UTF-8'?>"))
[void]$builder.AppendLine(("<svg xmlns='http://www.w3.org/2000/svg' width='{0}' height='{1}' viewBox='0 0 {0} {1}' role='img' aria-labelledby='title desc'>" -f $svgWidth, $svgHeight))
[void]$builder.AppendLine("  <title id='title'>Painel de controle de criacao de API keys pendentes</title>")
[void]$builder.AppendLine("  <desc id='desc'>Dashboard SVG para acompanhar criacao de credenciais faltantes, preenchimento do .env e validacao tecnica provider por provider.</desc>")
[void]$builder.AppendLine("  <style>")
[void]$builder.AppendLine("    .bg { fill: #f5f7fb; }")
[void]$builder.AppendLine("    .frame { fill: #ffffff; stroke: #d6dde7; stroke-width: 1; }")
[void]$builder.AppendLine("    .title { font: 700 28px 'Segoe UI', Arial, sans-serif; fill: #10243e; }")
[void]$builder.AppendLine("    .subtitle { font: 400 13px 'Segoe UI', Arial, sans-serif; fill: #587086; }")
[void]$builder.AppendLine("    .card-title { font: 700 12px 'Segoe UI', Arial, sans-serif; fill: #5b6f84; text-transform: uppercase; }")
[void]$builder.AppendLine("    .card-value { font: 700 28px 'Segoe UI', Arial, sans-serif; fill: #10243e; }")
[void]$builder.AppendLine("    .card-note { font: 400 12px 'Segoe UI', Arial, sans-serif; fill: #587086; }")
[void]$builder.AppendLine("    .section-title { font: 700 16px 'Segoe UI', Arial, sans-serif; fill: #10243e; }")
[void]$builder.AppendLine("    .header-cell { font: 700 12px 'Segoe UI', Arial, sans-serif; fill: #ffffff; }")
[void]$builder.AppendLine("    .body-text { font: 12px 'Segoe UI', Arial, sans-serif; fill: #132238; }")
[void]$builder.AppendLine("    .mono { font: 11px 'Consolas', 'Courier New', monospace; fill: #132238; }")
[void]$builder.AppendLine("    .link { font: 11px 'Consolas', 'Courier New', monospace; fill: #0a4ea3; }")
[void]$builder.AppendLine("    .muted { font: 11px 'Segoe UI', Arial, sans-serif; fill: #587086; }")
[void]$builder.AppendLine("    .priority { font: 700 10px 'Segoe UI', Arial, sans-serif; fill: #ffffff; }")
[void]$builder.AppendLine("    .check-box { fill: #ffffff; stroke: #8da2b8; stroke-width: 1; }")
[void]$builder.AppendLine("    .check-label { font: 11px 'Segoe UI', Arial, sans-serif; fill: #29435b; }")
[void]$builder.AppendLine("    .grid { stroke: #dce4ee; stroke-width: 1; }")
[void]$builder.AppendLine("  </style>")
[void]$builder.AppendLine(("  <rect class='bg' x='0' y='0' width='{0}' height='{1}' />" -f $svgWidth, $svgHeight))

Add-Text -Builder $builder -CssClass "title" -X $layout.marginLeft -Y 44 -Value "Controle de criacao de API keys pendentes"
Add-Text -Builder $builder -CssClass "subtitle" -X $layout.marginLeft -Y 68 -Value "Leitura do ambiente local atual. Use esta tabela para controlar portal, criacao da chave, preenchimento do .env e teste por provider."
Add-Text -Builder $builder -CssClass "subtitle" -X $layout.marginLeft -Y 88 -Value "Regra pratica: so promova um provider depois de concluir os quatro passos do bloco Controle."

$cards = @(
    [PSCustomObject]@{ x = $layout.marginLeft; title = "Pendentes"; value = "$totalCount"; note = "Providers sem credencial obrigatoria." },
    [PSCustomObject]@{ x = $layout.marginLeft + ($layout.cardWidth + $layout.cardGap); title = "Prioridade alta"; value = "$highCount"; note = "Texto, vetorial e busca." },
    [PSCustomObject]@{ x = $layout.marginLeft + (2 * ($layout.cardWidth + $layout.cardGap)); title = "Google shared"; value = "$googleCount"; note = "Uma credencial cobre 5 produtos." },
    [PSCustomObject]@{ x = $layout.marginLeft + (3 * ($layout.cardWidth + $layout.cardGap)); title = "Restritos"; value = "$restrictedCount"; note = "Threat intel e fluxos sensiveis." }
)

foreach ($card in $cards) {
    [void]$builder.AppendLine(("  <rect class='frame' x='{0}' y='{1}' width='{2}' height='{3}' rx='14' />" -f $card.x, 104, $layout.cardWidth, $layout.cardHeight))
    Add-Text -Builder $builder -CssClass "card-title" -X ($card.x + 16) -Y 128 -Value $card.title
    Add-Text -Builder $builder -CssClass "card-value" -X ($card.x + 16) -Y 162 -Value $card.value
    Add-Text -Builder $builder -CssClass "card-note" -X ($card.x + 16) -Y 184 -Value $card.note
}

foreach ($section in $sectionRows) {
    [void]$builder.AppendLine(("  <rect x='{0}' y='{1}' width='{2}' height='{3}' rx='12' fill='{4}' />" -f $layout.marginLeft, $section.startY, $tableWidth, $layout.sectionHeaderHeight, $section.color))
    Add-Text -Builder $builder -CssClass "section-title" -X ($layout.marginLeft + 16) -Y ($section.startY + 22) -Value $section.label

    $headerY = $section.startY + $layout.sectionHeaderHeight
    [void]$builder.AppendLine(("  <rect x='{0}' y='{1}' width='{2}' height='{3}' rx='0' fill='#16324f' />" -f $layout.marginLeft, $headerY, $tableWidth, $layout.tableHeaderHeight))

    $columnX = $layout.marginLeft
    foreach ($column in $columns) {
        Add-Text -Builder $builder -CssClass "header-cell" -X ($columnX + 10) -Y ($headerY + 22) -Value $column.label
        $columnX += $column.width
    }

    foreach ($entry in $section.rows) {
        [void]$builder.AppendLine(("  <rect class='frame' x='{0}' y='{1}' width='{2}' height='{3}' rx='0' />" -f $layout.marginLeft, $entry.y, $tableWidth, $entry.height))

        $columnX = $layout.marginLeft
        foreach ($column in $columns) {
            if ($column.key -ne "index") {
                [void]$builder.AppendLine(("  <line class='grid' x1='{0}' y1='{1}' x2='{0}' y2='{2}' />" -f $columnX, $entry.y, ($entry.y + $entry.height)))
            }
            $columnX += $column.width
        }
        [void]$builder.AppendLine(("  <line class='grid' x1='{0}' y1='{1}' x2='{2}' y2='{1}' />" -f $layout.marginLeft, ($entry.y + $entry.height), ($layout.marginLeft + $tableWidth)))

        $contentTop = $entry.y + 18
        Add-Text -Builder $builder -CssClass "mono" -X ($layout.marginLeft + 10) -Y ($contentTop + 6) -Value ([string]$entry.index)

        $productX = $layout.marginLeft + $columns[0].width + 10
        $lineY = $contentTop + 6
        foreach ($line in $entry.productLines) {
            Add-Text -Builder $builder -CssClass "body-text" -X $productX -Y $lineY -Value $line
            $lineY += $layout.lineHeight
        }
        Add-Text -Builder $builder -CssClass "muted" -X $productX -Y ($entry.y + $entry.height - 14) -Value $entry.row.provider_code

        $priorityText = $entry.row.priority
        $priorityWidth = if ($priorityText.Length -gt 6) { 78 } else { 56 }
        $priorityX = $layout.marginLeft + $columns[0].width + $columns[1].width - $priorityWidth - 12
        [void]$builder.AppendLine(("  <rect x='{0}' y='{1}' width='{2}' height='18' rx='9' fill='{3}' />" -f $priorityX, ($entry.y + 12), $priorityWidth, $priorityColor[$priorityText]))
        Add-Text -Builder $builder -CssClass "priority" -X ($priorityX + 10) -Y ($entry.y + 25) -Value $priorityText

        $envX = $layout.marginLeft + $columns[0].width + $columns[1].width + 10
        $lineY = $contentTop + 6
        foreach ($line in $entry.envLines) {
            Add-Text -Builder $builder -CssClass "mono" -X $envX -Y $lineY -Value $line
            $lineY += $layout.lineHeight
        }

        $portalX = $layout.marginLeft + $columns[0].width + $columns[1].width + $columns[2].width + 10
        $lineY = $contentTop + 6
        foreach ($line in $entry.portalLines) {
            Add-Text -Builder $builder -CssClass "link" -X $portalX -Y $lineY -Value $line
            $lineY += $layout.lineHeight
        }

        $controlX = $layout.marginLeft + $columns[0].width + $columns[1].width + $columns[2].width + $columns[3].width + 14
        Add-Text -Builder $builder -CssClass "muted" -X $controlX -Y ($contentTop + 6) -Value "Checklist manual"
        Add-BoxLabel -Builder $builder -X $controlX -Y ($entry.y + 34) -Label "Portal"
        Add-BoxLabel -Builder $builder -X ($controlX + 80) -Y ($entry.y + 34) -Label "Criar"
        Add-BoxLabel -Builder $builder -X ($controlX + 154) -Y ($entry.y + 34) -Label ".env"
        Add-BoxLabel -Builder $builder -X ($controlX + 214) -Y ($entry.y + 34) -Label "Teste"
        Add-Text -Builder $builder -CssClass "muted" -X $controlX -Y ($entry.y + $entry.height - 14) -Value "Status atual: pendente"

        $noteX = $layout.marginLeft + $columns[0].width + $columns[1].width + $columns[2].width + $columns[3].width + $columns[4].width + 10
        $lineY = $contentTop + 6
        foreach ($line in $entry.noteLines) {
            Add-Text -Builder $builder -CssClass "body-text" -X $noteX -Y $lineY -Value $line
            $lineY += $layout.lineHeight
        }
    }
}

[void]$builder.AppendLine("</svg>")

Set-Content -Path $OutputPath -Value $builder.ToString() -Encoding UTF8
Write-Output $OutputPath
