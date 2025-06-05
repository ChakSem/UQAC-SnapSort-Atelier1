# Get the security key of the Mobile Hotspot
try {
    $connectionProfile = [Windows.Networking.Connectivity.NetworkInformation,Windows.Networking.Connectivity,ContentType=WindowsRuntime]::GetInternetConnectionProfile()
    $tetheringManager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager,Windows.Networking.NetworkOperators,ContentType=WindowsRuntime]::CreateFromConnectionProfile($connectionProfile)
    $key = $tetheringManager.TetheringOperationalState -eq 1 ? $tetheringManager.GetCurrentPasswordAsync().GetAwaiter().GetResult() : "Not Active"
    Write-Output "Key: $key"
} catch {
    Write-Output "Key: Error retrieving security key: $_"
}