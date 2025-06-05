try {
  Add-Type -AssemblyName System.Runtime.WindowsRuntime
  $asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
  
  # Accès aux types WinRT
  $netInformationType = [Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]
  $tetheringManagerType = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]
  
  # Obtenir le profil de connexion
  $connectionProfile = $netInformationType::GetInternetConnectionProfile()
  
  if ($connectionProfile -ne $null) {
    $tetheringManager = $tetheringManagerType::CreateFromConnectionProfile($connectionProfile)
    
    if ($tetheringManager -ne $null) {
      $tetheringConfig = $tetheringManager.GetCurrentAccessPointConfiguration()
      
      if ($tetheringConfig -ne $null) {
        Write-Output "ssid: $($tetheringConfig.Ssid)"
        Write-Output "password: $($tetheringConfig.Passphrase)"
      } else {
        Write-Output "Erreur: Impossible de récupérer la configuration"
      }
    } else {
      Write-Output "Erreur: Impossible de créer TetheringManager"
    }
  } else {
    Write-Output "Erreur: Aucun profil de connexion Internet trouvé"
  }
} catch {
  Write-Output "Exception: $($_.Exception.Message)"
}