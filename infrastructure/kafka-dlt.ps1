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
    Write-Host "Replay consumer lag:"
    & docker @composeArgs exec -T kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:29092 --group delivery-compensation-dlt-replay --describe
    exit 0
}

Write-Host "Replaying up to $MaxMessages message(s): $dltTopic -> $sourceTopic"

& docker @composeArgs exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:29092 --topic $dltTopic --group delivery-compensation-dlt-replay --from-beginning --max-messages $MaxMessages |
    & docker @composeArgs exec -T kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:29092 --topic $sourceTopic

if ($LASTEXITCODE -ne 0) {
    throw "DLT replay failed."
}

Write-Host "Replay request completed. Check Delivery Service logs and DLT status."
