param(
    [ValidateSet("status", "replay")]
    [string]$Action = "status",

    [ValidateRange(1, 100)]
    [int]$MaxMessages = 10,

    [string]$EnvFile = ".env"
)

$ErrorActionPreference = "Stop"
$composeArgs = @(
    "compose",
    "--env-file", $EnvFile,
    "-f", "infrastructure/docker-compose.yml"
)
$dltTopic = "delivery-compensation-dlt"
$sourceTopic = "delivery-compensation"

if ($Action -eq "status") {
    Write-Host "DLT end offsets:"
    & docker @composeArgs exec -T kafka /opt/kafka/bin/kafka-get-offsets.sh --bootstrap-server kafka:29092 --topic $dltTopic
    exit 0
}

$offsetLines = @(
    & docker @composeArgs exec -T kafka /opt/kafka/bin/kafka-get-offsets.sh `
        --bootstrap-server kafka:29092 `
        --topic $dltTopic
)
$offsetExitCode = $LASTEXITCODE
if ($offsetExitCode -ne 0) {
    throw "Failed to read DLT offsets. exitCode=$offsetExitCode"
}

$availableMessages = ($offsetLines | ForEach-Object {
    $parts = $_ -split ":"
    if ($parts.Length -eq 3) { [long]$parts[2] } else { 0 }
} | Measure-Object -Sum).Sum

if (-not $availableMessages) {
    Write-Host "No DLT messages to replay."
    exit 0
}

$replayCount = [Math]::Min([long]$MaxMessages, [long]$availableMessages)
Write-Host "Replaying $replayCount message(s): $dltTopic -> $sourceTopic"

# Consumer Group을 사용하지 않아 Producer 실패 시 DLT 원본과 offset이 유지됩니다.
$messages = @(
    & docker @composeArgs exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh `
        --bootstrap-server kafka:29092 `
        --topic $dltTopic `
        --from-beginning `
        --max-messages $replayCount
)
$consumerExitCode = $LASTEXITCODE
if ($consumerExitCode -ne 0) {
    throw "DLT consumer failed. exitCode=$consumerExitCode"
}

if ($messages.Count -ne $replayCount) {
    throw "Expected $replayCount DLT messages but read $($messages.Count)."
}

$messages |
    & docker @composeArgs exec -T kafka /opt/kafka/bin/kafka-console-producer.sh `
        --bootstrap-server kafka:29092 `
        --topic $sourceTopic

$producerExitCode = $LASTEXITCODE
if ($producerExitCode -ne 0) {
    throw "DLT producer failed. DLT messages remain available for retry. exitCode=$producerExitCode"
}

Write-Host "Replay completed. DLT originals were retained, so rerunning may produce duplicates."
