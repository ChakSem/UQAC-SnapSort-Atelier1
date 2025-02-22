using System;
using System.Drawing;
using System.Windows.Forms;
using System.IO;
using DashboardApp.Models;

namespace DashboardApp
{
    public partial class Form1 : Form
    {
        private readonly NavigationManager navigationManager;
        private readonly ConnectionManager connectionManager;

        public Form1()
        {
            InitializeComponent();
            navigationManager = new NavigationManager(mainPanel);
            connectionManager = new ConnectionManager(loginButton, statusLabel);
            CustomizeDesign();
            SetupEventHandlers();
        }

        private void CustomizeDesign()
        {
            this.FormBorderStyle = FormBorderStyle.None;
            this.Padding = new Padding(2);
            this.BackColor = Color.FromArgb(41, 128, 185);
            mainPanel.BackColor = Color.White;
        }

        private void SetupEventHandlers()
        {
            logoPanel.MouseDown += UIHelper.HandleWindowDrag(this);
            loginButton.Click += (s, e) => connectionManager.ToggleConnection();
            albumsButton.Click += (s, e) => navigationManager.NavigateTo("Albums");
            imagesButton.Click += (s, e) => navigationManager.NavigateTo("Images");
            settingsButton.Click += (s, e) => navigationManager.NavigateTo("Parametres");
        }
    }
}