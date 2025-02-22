
using System.Drawing;
using System.Windows.Forms;

namespace DashboardApp.Models
{
    public class ConnectionManager
    {
        private bool isConnected = false;
        private readonly Button loginButton;
        private readonly Label statusLabel;

        public ConnectionManager(Button button, Label label)
        {
            loginButton = button;
            statusLabel = label;
            UpdateConnectionStatus();
        }

        public void ToggleConnection()
        {
            isConnected = !isConnected;
            UpdateConnectionStatus();
        }

        private void UpdateConnectionStatus()
        {
            loginButton.Text = isConnected ? "Déconnexion" : "Connexion";
            loginButton.BackColor = isConnected ? Color.Red : Color.Green;
            statusLabel.Text = isConnected ? "Connecté" : "Déconnecté";
        }
    }

}
