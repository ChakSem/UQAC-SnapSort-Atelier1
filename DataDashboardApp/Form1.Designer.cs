using System.Windows.Forms;

namespace DashboardApp
{
    partial class Form1
    {
        private System.ComponentModel.IContainer components = null;
        private System.Windows.Forms.Panel sidePanel;
        private System.Windows.Forms.Panel logoPanel;
        private System.Windows.Forms.Label logoLabel;
        private System.Windows.Forms.Panel mainPanel;
        private System.Windows.Forms.Panel navigationPanel;
        private System.Windows.Forms.Button albumsButton;
        private System.Windows.Forms.Button imagesButton;
        private System.Windows.Forms.Button loginButton;
        private System.Windows.Forms.Button settingsButton;
        private System.Windows.Forms.Panel statusPanel;
        private System.Windows.Forms.Label statusLabel;
        private System.Windows.Forms.PictureBox statusIcon;
        private System.Windows.Forms.ImageList imageList;

        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        private void InitializeComponent()
        {
            this.components = new System.ComponentModel.Container();
            this.sidePanel = new System.Windows.Forms.Panel();
            this.statusPanel = new System.Windows.Forms.Panel();
            this.statusLabel = new System.Windows.Forms.Label();
            this.statusIcon = new System.Windows.Forms.PictureBox();
            this.navigationPanel = new System.Windows.Forms.Panel();
            this.settingsButton = new System.Windows.Forms.Button();
            this.loginButton = new System.Windows.Forms.Button();
            this.imagesButton = new System.Windows.Forms.Button();
            this.albumsButton = new System.Windows.Forms.Button();
            this.logoPanel = new System.Windows.Forms.Panel();
            this.logoLabel = new System.Windows.Forms.Label();
            this.mainPanel = new System.Windows.Forms.Panel();

            // Form1
            this.AutoScaleDimensions = new System.Drawing.SizeF(8F, 16F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(1200, 800);
            this.Controls.Add(this.mainPanel);
            this.Controls.Add(this.sidePanel);
            this.Name = "Form1";
            this.Text = "Modern Dashboard";
            this.StartPosition = System.Windows.Forms.FormStartPosition.CenterScreen;

            // sidePanel
            this.sidePanel.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(41)))), ((int)(((byte)(128)))), ((int)(((byte)(185)))));
            this.sidePanel.Dock = System.Windows.Forms.DockStyle.Left;
            this.sidePanel.Width = 250;
            this.sidePanel.Controls.Add(this.statusPanel);
            this.sidePanel.Controls.Add(this.navigationPanel);
            this.sidePanel.Controls.Add(this.logoPanel);

            // logoPanel
            this.logoPanel.Dock = System.Windows.Forms.DockStyle.Top;
            this.logoPanel.Height = 100;
            this.logoPanel.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(52)))), ((int)(((byte)(152)))), ((int)(((byte)(219)))));
            this.logoPanel.Controls.Add(this.logoLabel);

            // logoLabel
            this.logoLabel.Text = "SnapSort";
            this.logoLabel.Font = new System.Drawing.Font("Segoe UI", 24F, System.Drawing.FontStyle.Bold);
            this.logoLabel.ForeColor = System.Drawing.Color.White;
            this.logoLabel.Dock = System.Windows.Forms.DockStyle.Fill;
            this.logoLabel.TextAlign = System.Drawing.ContentAlignment.MiddleCenter;

            // navigationPanel
            this.navigationPanel.Dock = System.Windows.Forms.DockStyle.Fill;
            this.navigationPanel.Padding = new System.Windows.Forms.Padding(10);

      
            this.navigationPanel.Controls.Add(this.albumsButton);
            this.navigationPanel.Controls.Add(this.imagesButton);
            this.navigationPanel.Controls.Add(this.loginButton);
            this.navigationPanel.Controls.Add(this.settingsButton);

            // albumsButton
            this.albumsButton.Dock = System.Windows.Forms.DockStyle.Top;
            this.albumsButton.Height = 60;
            this.albumsButton.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
            this.albumsButton.FlatAppearance.BorderSize = 0;
            this.albumsButton.ForeColor = System.Drawing.Color.White;
            this.albumsButton.TextAlign = System.Drawing.ContentAlignment.MiddleLeft;
            this.albumsButton.Padding = new System.Windows.Forms.Padding(20, 0, 0, 0);

            // imagesButton
            this.imagesButton.Dock = System.Windows.Forms.DockStyle.Top;
            this.imagesButton.Height = 60;
            this.imagesButton.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
            this.imagesButton.FlatAppearance.BorderSize = 0;
            this.imagesButton.ForeColor = System.Drawing.Color.White;
            this.imagesButton.TextAlign = System.Drawing.ContentAlignment.MiddleLeft;
            this.imagesButton.Padding = new System.Windows.Forms.Padding(20, 0, 0, 0);



            // loginButton
            this.loginButton.Dock = System.Windows.Forms.DockStyle.Top;
            this.loginButton.Height = 60;
            this.loginButton.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
            this.loginButton.FlatAppearance.BorderSize = 0;
            this.loginButton.ForeColor = System.Drawing.Color.White;
            this.loginButton.TextAlign = System.Drawing.ContentAlignment.MiddleLeft;
            this.loginButton.Padding = new System.Windows.Forms.Padding(20, 0, 0, 0);

            // settingsButton
            this.settingsButton.Dock = System.Windows.Forms.DockStyle.Top;
            this.settingsButton.Height = 60;
            this.settingsButton.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
            this.settingsButton.FlatAppearance.BorderSize = 0;
            this.settingsButton.ForeColor = System.Drawing.Color.White;
            this.settingsButton.TextAlign = System.Drawing.ContentAlignment.MiddleLeft;
            this.settingsButton.Padding = new System.Windows.Forms.Padding(20, 0, 0, 0);


            // Définition des textes après instanciation
            albumsButton.Text = "Albums";
            imagesButton.Text = "Images non triées";
            loginButton.Text = "Se connecter";
            settingsButton.Text = "Options";

            // statusPanel
            this.statusPanel.Dock = System.Windows.Forms.DockStyle.Bottom;
            this.statusPanel.Height = 40;
            this.statusPanel.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(41)))), ((int)(((byte)(128)))), ((int)(((byte)(185)))));
            this.statusPanel.Controls.Add(this.statusLabel);
            this.statusPanel.Controls.Add(this.statusIcon);

            // statusLabel
            this.statusLabel.Text = "Connexion";
            this.statusLabel.ForeColor = System.Drawing.Color.White;
            this.statusLabel.Dock = System.Windows.Forms.DockStyle.Fill;
            this.statusLabel.TextAlign = System.Drawing.ContentAlignment.MiddleLeft;
            this.statusLabel.Padding = new System.Windows.Forms.Padding(40, 0, 0, 0);

            // statusIcon
            this.statusIcon.Size = new System.Drawing.Size(24, 24);
            this.statusIcon.Location = new System.Drawing.Point(10, 8);
            this.statusIcon.SizeMode = System.Windows.Forms.PictureBoxSizeMode.StretchImage;

            // mainPanel
            this.mainPanel.Dock = System.Windows.Forms.DockStyle.Fill;
            this.mainPanel.BackColor = System.Drawing.Color.WhiteSmoke;
            this.mainPanel.Padding = new System.Windows.Forms.Padding(20);

            // Ajout de l'ImageList pour les icônes de l'arborescence
            this.imageList = new System.Windows.Forms.ImageList(this.components);
            this.imageList.ImageSize = new System.Drawing.Size(16, 16);
            
            // Configuration du formulaire principal
            this.AutoScaleDimensions = new System.Drawing.SizeF(8F, 16F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(1200, 800);
            this.MinimumSize = new System.Drawing.Size(800, 600);
        }

    }
}

