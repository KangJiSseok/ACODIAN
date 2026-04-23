param(
    [string]$Title = "Codex OMX",
    [string]$Message = "Response completed."
)

try {
    [void][reflection.assembly]::LoadWithPartialName('System.Windows.Forms')
    [void][reflection.assembly]::LoadWithPartialName('System.Drawing')

    $notify = New-Object System.Windows.Forms.NotifyIcon
    $notify.Icon = [System.Drawing.SystemIcons]::Information
    $notify.BalloonTipIcon = [System.Windows.Forms.ToolTipIcon]::Info
    $notify.BalloonTipTitle = $Title
    $notify.BalloonTipText = $Message
    $notify.Visible = $true
    $notify.ShowBalloonTip(5000)

    try { [System.Media.SystemSounds]::Asterisk.Play() } catch {}

    Start-Sleep -Seconds 6
    $notify.Dispose()
    exit 0
} catch {
    try {
        $wshell = New-Object -ComObject WScript.Shell
        $null = $wshell.Popup($Message, 3, $Title, 64)
        exit 0
    } catch {
        try {
            [console]::beep(1200,300)
            exit 0
        } catch {
            exit 1
        }
    }
}
