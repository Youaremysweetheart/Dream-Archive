param(
    [string]$Model = "ensemble",
    [int]$Epochs = 30
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Train-One {
    param(
        [string]$Name
    )
    Write-Host "Training model: $Name (epochs=$Epochs)"
    python train.py --model $Name --epochs $Epochs
}

if ($Model -eq "ensemble") {
    Train-One -Name "cnn"
    Train-One -Name "rnn"
} elseif ($Model -eq "bert") {
    Write-Host "Training model: bert (epochs=$Epochs)"
    python train_bert.py --epochs $Epochs
} else {
    Train-One -Name $Model
}

Write-Host "Model update completed. New checkpoints are in ./checkpoints"
