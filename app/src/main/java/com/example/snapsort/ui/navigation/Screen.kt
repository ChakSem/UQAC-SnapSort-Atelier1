// ui/navigation/Screen.kt
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Tutorial : Screen("tutorial")
    object Connection : Screen("connection")
    object ImageSelection : Screen("image_selection")
    object TransferProgress : Screen("transfer_progress")
}