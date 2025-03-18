using DashboardApp.Config;
using DashboardApp.Models;
using System;
using System.Drawing;
using System.Windows.Forms;

namespace DashboardApp.Views
{
    public partial class OptionManager
    {
        private readonly Panel mainPanel;
        private Label titleLabel;
        private TextBox rootFolderTextBox;
        private TextBox imagesPerPageTextBox;
        private TextBox thumbnailSizeTextBox;
        private Button browseButton;
        private Button saveButton;
        private Button cancelButton;
        private FlowLayoutPanel flowPanel;

        public OptionManager(Panel panel, int width)
        {
            mainPanel = panel;
            panel.Width = width;
        }

        public void CreateOptionView()
        {
            mainPanel.Controls.Clear();
            SetupOptionView(mainPanel);
        }

        private void SetupOptionView(Control parent)
        {
            // Conteneur principal
            Panel container = new Panel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                Padding = new Padding(20),
            };
            parent.Controls.Add(container);
            
            // Titre
            titleLabel = UIHelper.CreateTitleLabel("Options de l'application");
            container.Controls.Add(titleLabel);

            // FlowLayoutPanel pour une disposition fluide
            flowPanel = new FlowLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                Padding = new Padding(10)
            };

            // Ajout des champs de paramètres
            rootFolderTextBox = CreateSettingInput("Dossier racine des images:", AppSettings.DEFAULT_ROOT_FOLDER);
            imagesPerPageTextBox = CreateSettingInput("Images par page:", AppSettings.IMAGES_PER_PAGE.ToString());
            thumbnailSizeTextBox = CreateSettingInput("Taille des vignettes (px):", AppSettings.THUMBNAIL_SIZE.ToString());

            container.Controls.Add(flowPanel);

            // Panel pour les boutons Save/Cancel (invisible au début)
            FlowLayoutPanel buttonPanel = new FlowLayoutPanel
            {
                Dock = DockStyle.Bottom,
                Height = 50,
                FlowDirection = FlowDirection.RightToLeft,
                Padding = new Padding(0, 10, 0, 0),
                Visible = false // Caché au départ
            };

            saveButton = new Button
            {
                Text = "Enregistrer",
                Width = 100,
                BackColor = Color.LightGreen,
                FlatStyle = FlatStyle.Flat
            };
            saveButton.Click += SaveButton_Click;

            cancelButton = new Button
            {
                Text = "Annuler",
                Width = 100,
                BackColor = Color.LightCoral,
                FlatStyle = FlatStyle.Flat,
                Margin = new Padding(10, 0, 0, 0)
            };
            cancelButton.Click += CancelButton_Click;

            buttonPanel.Controls.Add(saveButton);
            buttonPanel.Controls.Add(cancelButton);

            container.Controls.Add(buttonPanel);

            // Ajouter un event handler aux TextBoxes pour afficher les boutons Save/Cancel
            rootFolderTextBox.TextChanged += (s, e) => buttonPanel.Visible = true;
            imagesPerPageTextBox.TextChanged += (s, e) => buttonPanel.Visible = true;
            thumbnailSizeTextBox.TextChanged += (s, e) => buttonPanel.Visible = true;
        }

        private TextBox CreateSettingInput(string label, string defaultValue)
        {
            Panel container = new Panel
            {
                Width = 500,
                Height = 50,
                Padding = new Padding(0, 5, 0, 5),
                AutoSize = true
            };

            Label settingLabel = new Label
            {
                Text = label,
                Font = new Font("Segoe UI", 12),
                ForeColor = Color.Black,
                AutoSize = true
            };

            TextBox inputBox = new TextBox
            {
                Text = defaultValue,
                Font = new Font("Segoe UI", 12),
                Width = 250,
                BorderStyle = BorderStyle.FixedSingle
            };

            container.Controls.Add(settingLabel);
            container.Controls.Add(inputBox);
            settingLabel.Location = new Point(0, 5);
            inputBox.Location = new Point(200, 5);

            flowPanel.Controls.Add(container);

            return inputBox;
        }

        private void SaveButton_Click(object sender, EventArgs e)
        {
            // Simulation de sauvegarde des paramètres
            MessageBox.Show("Paramètres enregistrés !");
            saveButton.Parent.Visible = false; // Cacher les boutons après sauvegarde
        }

        private void CancelButton_Click(object sender, EventArgs e)
        {
            // Réinitialiser les valeurs et cacher les boutons
            rootFolderTextBox.Text = AppSettings.DEFAULT_ROOT_FOLDER;
            imagesPerPageTextBox.Text = AppSettings.IMAGES_PER_PAGE.ToString();
            thumbnailSizeTextBox.Text = AppSettings.THUMBNAIL_SIZE.ToString();

            saveButton.Parent.Visible = false; // Cacher les boutons
        }
    }
}
