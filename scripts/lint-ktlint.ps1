param(
  [Parameter(Mandatory = $true, Position = 0)]
  [ValidateSet("check", "fix")]
  [string] $Action,

  [Parameter(Mandatory = $true, Position = 1)]
  [string] $FilesBase64
)

$json = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($FilesBase64))
$files = @(ConvertFrom-Json -InputObject $json | Where-Object { $_ })
if (-not $files -or $files.Count -eq 0) {
  Write-Output "lint:ktlint: no files"
  exit 0
}

$ktlintArgs = @()
if ($Action -eq "fix") {
  $ktlintArgs += "-F"
}
$ktlintArgs += $files
& ktlint @ktlintArgs
exit $LASTEXITCODE
