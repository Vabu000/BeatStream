param(
    [string]$BaseUrl = "https://sonarcloud.io",
    [string]$ProjectKey = "Vabu000_BeatStream",
    [string[]]$ComponentPaths = @(
        "app/src/main/java/com/example/myapplication7/data/ConcertTariffPolicy.kt",
        "app/src/main/java/com/example/myapplication7/data/AppRepository.kt",
        "app/src/main/java/com/example/myapplication7/quality/ConcertValidator.kt"
    ),
    [string]$OutputDirectory = "build/reports/sonar"
)

$ErrorActionPreference = "Stop"
$token = $env:SONAR_TOKEN
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "SONAR_TOKEN is not set. Set it in the local environment or GitHub Secrets; do not put it in source control."
}

$base = $BaseUrl.TrimEnd("/")
$headers = @{
    Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${token}:"))
}
$requestedMetrics = @(
    "complexity",
    "cognitive_complexity",
    "ncloc",
    "class_complexity",
    "coupling_between_objects",
    "lcom4",
    "dit",
    "rfc",
    "mhf",
    "ahf",
    "cof"
)

$metricsResponse = Invoke-RestMethod -Method Get -Uri "$base/api/metrics/search?ps=500" -Headers $headers
$availableMetrics = @($metricsResponse.metrics | ForEach-Object { $_.key })
$supportedMetrics = @($requestedMetrics | Where-Object { $_ -in $availableMetrics })
$unsupportedMetrics = @($requestedMetrics | Where-Object { $_ -notin $availableMetrics })
if ($supportedMetrics.Count -eq 0) {
    throw "Sonar returned none of the requested metrics. Check project permissions and server compatibility."
}

$out = Join-Path (Get-Location) $OutputDirectory
New-Item -ItemType Directory -Force -Path $out | Out-Null
$components = @()
foreach ($path in $ComponentPaths) {
    $componentKey = "${ProjectKey}:$path"
    $query = "component=$([Uri]::EscapeDataString($componentKey))&metricKeys=$([Uri]::EscapeDataString(($supportedMetrics -join ',')))"
    $uri = "$base/api/measures/component?$query"
    $response = Invoke-RestMethod -Method Get -Uri $uri -Headers $headers
    if (-not $response.component) {
        throw "Sonar returned no component for '$componentKey'. Confirm that this file was included in a successful analysis."
    }

    $safeName = [IO.Path]::GetFileNameWithoutExtension($path)
    $filePath = Join-Path $out "$safeName.json"
    $response | ConvertTo-Json -Depth 20 | Set-Content -Path $filePath -Encoding UTF8
    $components += [pscustomobject]@{
        component = $response.component.key
        measures = $response.component.measures
        raw_response = [IO.Path]::GetFileName($filePath)
    }
}

$report = [pscustomobject]@{
    generated_at_utc = [DateTime]::UtcNow.ToString("o")
    sonar_url = $base
    project_key = $ProjectKey
    supported_requested_metrics = $supportedMetrics
    unsupported_requested_metrics = $unsupportedMetrics
    components = $components
    note = "Unavailable CK/MOOD metrics are reported as unsupported and are not inferred from other measures."
}
$summaryPath = Join-Path $out "sonar-metrics-summary.json"
$report | ConvertTo-Json -Depth 20 | Set-Content -Path $summaryPath -Encoding UTF8
Write-Host "SonarCloud metrics written to $summaryPath"
if ($unsupportedMetrics.Count -gt 0) {
    Write-Warning ("The server does not expose these requested metric keys: " + ($unsupportedMetrics -join ", "))
}
