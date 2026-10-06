$ErrorActionPreference = 'SilentlyContinue'

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$runtimeDll = if ([Environment]::Is64BitProcess) {
    "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\System.Runtime.WindowsRuntime.dll"
} else {
    "C:\Windows\Microsoft.NET\Framework\v4.0.30319\System.Runtime.WindowsRuntime.dll"
}
if (Test-Path $runtimeDll) {
    Add-Type -Path $runtimeDll -ErrorAction SilentlyContinue
}

[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime] | Out-Null
[Windows.Media.Control.GlobalSystemMediaTransportControlsSession, Windows.Media.Control, ContentType = WindowsRuntime] | Out-Null

$asTaskGeneric = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
} | Select-Object -First 1

$asStreamMethod = [System.IO.WindowsRuntimeStreamExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsStream' -and $_.GetParameters().Count -eq 1
} | Select-Object -First 1

$coverDir = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), 'fluxclient_covers')
if (-not (Test-Path $coverDir)) {
    try { New-Item -ItemType Directory -Path $coverDir -Force | Out-Null } catch {}
}

function Await-WinRT($asyncOp, $type) {
    if ($asyncOp -eq $null -or $asTaskGeneric -eq $null) { return $null }
    try {
        $method = $asTaskGeneric.MakeGenericMethod($type)
        $task = $method.Invoke($null, @($asyncOp))
        if ($task.Wait(1200)) {
            return $task.Result
        }
    } catch {}
    return $null
}

function Get-Manager {
    try {
        $op = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()
        return Await-WinRT $op ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
    } catch {
        return $null
    }
}

$mgr = Get-Manager
if ($mgr -eq $null) {
    Write-Output "STATUS|NO_MGR"
    [Console]::Out.Flush()
} else {
    Write-Output "STATUS|READY"
    [Console]::Out.Flush()
}

$lastOut = ""
$lastCoverTrack = ""
$currentCoverPath = ""

while ($true) {
    try {
        if ($mgr -eq $null) {
            $mgr = Get-Manager
        }

        $bestSession = $null
        $bestStatus = "Unknown"
        $bestProps = $null

        if ($mgr -ne $null) {
            $sessions = $null
            try { $sessions = $mgr.GetSessions() } catch { $mgr = $null }

            if ($sessions -ne $null -and $sessions.Count -gt 0) {
                # 1. Look for actively playing session with a title
                foreach ($s in $sessions) {
                    try {
                        $info = $s.GetPlaybackInfo()
                        $st = if ($info -ne $null) { $info.PlaybackStatus.ToString() } else { "Unknown" }
                        $pr = Await-WinRT ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
                        if ($pr -ne $null -and -not [string]::IsNullOrWhiteSpace($pr.Title)) {
                            if ($st -eq "Playing") {
                                $bestSession = $s
                                $bestStatus = $st
                                $bestProps = $pr
                                break
                            } elseif ($bestSession -eq $null) {
                                $bestSession = $s
                                $bestStatus = $st
                                $bestProps = $pr
                            }
                        }
                    } catch {}
                }

                # 2. If none with title found, try GetCurrentSession
                if ($bestSession -eq $null) {
                    try {
                        $curr = $mgr.GetCurrentSession()
                        if ($curr -ne $null) {
                            $bestSession = $curr
                            $info = $curr.GetPlaybackInfo()
                            $bestStatus = if ($info -ne $null) { $info.PlaybackStatus.ToString() } else { "Unknown" }
                            $bestProps = Await-WinRT ($curr.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
                        }
                    } catch {}
                }
            }
        }

        if ($bestSession -ne $null) {
            $title = if ($bestProps -ne $null -and $bestProps.Title -ne $null) { $bestProps.Title } else { "" }
            $artist = if ($bestProps -ne $null -and $bestProps.Artist -ne $null) { $bestProps.Artist } else { "" }
            $app = if ($bestSession.SourceAppId -ne $null) { $bestSession.SourceAppId } else { "" }

            # Extract real cover art from WinRT session when track changes
            $trackKey = ($title + "|||" + $artist)
            if ($trackKey -ne $lastCoverTrack -and -not [string]::IsNullOrWhiteSpace($title)) {
                $lastCoverTrack = $trackKey
                $currentCoverPath = ""
                if ($bestProps -ne $null -and $bestProps.Thumbnail -ne $null -and $asStreamMethod -ne $null) {
                    try {
                        $stream = Await-WinRT ($bestProps.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
                        if ($stream -ne $null) {
                            $hash = [Math]::Abs($trackKey.GetHashCode())
                            $covFile = [System.IO.Path]::Combine($coverDir, "cov_" + $hash + ".jpg")
                            $netStream = $asStreamMethod.Invoke($null, @($stream))
                            $fs = [System.IO.File]::Create($covFile)
                            $netStream.CopyTo($fs)
                            $fs.Dispose()
                            $netStream.Dispose()
                            $currentCoverPath = $covFile
                        }
                    } catch {}
                }
            } elseif ([string]::IsNullOrEmpty($currentCoverPath) -and $bestProps -ne $null -and $bestProps.Thumbnail -ne $null -and -not [string]::IsNullOrWhiteSpace($title) -and $asStreamMethod -ne $null) {
                try {
                    $stream = Await-WinRT ($bestProps.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
                    if ($stream -ne $null) {
                        $hash = [Math]::Abs($trackKey.GetHashCode())
                        $covFile = [System.IO.Path]::Combine($coverDir, "cov_" + $hash + ".jpg")
                        $netStream = $asStreamMethod.Invoke($null, @($stream))
                        $fs = [System.IO.File]::Create($covFile)
                        $netStream.CopyTo($fs)
                        $fs.Dispose()
                        $netStream.Dispose()
                        $currentCoverPath = $covFile
                    }
                } catch {}
            }

            $posMs = 0
            $endMs = 0
            try {
                $tl = $bestSession.GetTimelineProperties()
                if ($tl -ne $null) {
                    $basePos = if ($tl.Position -ne $null) { [long]$tl.Position.TotalMilliseconds } else { 0 }
                    $now = [DateTimeOffset]::UtcNow
                    $last = if ($tl.LastUpdatedTime -ne $null) { $tl.LastUpdatedTime.ToUniversalTime() } else { $now }
                    $elapsed = if ($bestStatus -eq "Playing") { ($now - $last).TotalMilliseconds } else { 0 }
                    if ($elapsed -lt 0) { $elapsed = 0 }
                    $posMs = [long]($basePos + $elapsed)
                    if ($tl.EndTime -ne $null) { $endMs = [long]$tl.EndTime.TotalMilliseconds }
                    if ($endMs -gt 0 -and $posMs -gt $endMs) { $posMs = $endMs }
                }
            } catch {}

            $titleB64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($title))
            $artistB64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($artist))
            $appB64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($app))
            $coverB64 = if (-not [string]::IsNullOrEmpty($currentCoverPath)) { [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($currentCoverPath)) } else { "" }

            $line = "B64TRACK|$bestStatus|$titleB64|$artistB64|$appB64|$posMs|$endMs|$coverB64"
            if ($line -ne $lastOut) {
                $lastOut = $line
                Write-Output $line
                [Console]::Out.Flush()
            }
        } else {
            $lastCoverTrack = ""
            $currentCoverPath = ""
            if ($lastOut -ne "NO_TRACK") {
                $lastOut = "NO_TRACK"
                Write-Output "NO_TRACK"
                [Console]::Out.Flush()
            }
        }
    } catch {
        # Catch any unexpected error and keep worker running
    }

    Start-Sleep -Milliseconds 300
}
