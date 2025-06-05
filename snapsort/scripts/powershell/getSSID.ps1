# Get the SSID of the Mobile Hotspot
try {
    $hostedNetwork = Get-NetAdapter | Where-Object {$_.Name -like "*Mobile*" -or $_.Description -like "*Mobile*"} | Select-Object -First 1
    if ($hostedNetwork) {
        $connectionProfile = [Windows.Networking.Connectivity.NetworkInformation,Windows.Networking.Connectivity,ContentType=WindowsRuntime]::GetInternetConnectionProfile()
        $tetheringManager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager,Windows.Networking.NetworkOperators,ContentType=WindowsRuntime]::CreateFromConnectionProfile($connectionProfile)
        $ssid = $tetheringManager.TetheringOperationalState -eq 1 ? $tetheringManager.NetworkName : "Not Active"
        Write-Output "SSID: $ssid"
    } else {
        Write-Output "SSID: Mobile Hotspot adapter not found"
    }
} catch {
    Write-Output "SSID: Error retrieving SSID: $_"
}