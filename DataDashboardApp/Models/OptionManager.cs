using DashboardApp.Config;
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
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

        private readonly AppSettings settingsManager;

        public OptionManager(Panel panel, int width)
        {
            mainPanel = panel;
            panel.Width = width;
            settingsManager = AppSettings.Instance;
        }

        public void CreateOptionView()
        {
            mainPanel.Controls.Clear();
            SetupOptionView(mainPanel);
        }

        private void SetupOptionView(Control parent)
        {
            settingsManager.LoadSettings(); // Charger les valeurs actuelles

            // Conteneur principal avec dégradé de fond
            Panel container = new Panel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                Padding = new Padding(20),
                BackColor = ColorTranslator.FromHtml("#f6fbff")
            };
            container.Paint += (s, e) =>
            {
                // Dégradé vertical du bleu très clair vers le blanc
                using (var brush = new LinearGradientBrush(container.ClientRectangle,
                    Color.FromArgb(240, 248, 255), Color.White, LinearGradientMode.Vertical))
                {
                    e.Graphics.FillRectangle(brush, container.ClientRectangle);
                }
            };
            parent.Controls.Add(container);

            titleLabel = new Label
            {
                Text = "Options de l'application",
                Font = new Font("Segoe UI", 18, FontStyle.Bold),
                ForeColor = Color.FromArgb(33, 150, 243),
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 20)
            };
            container.Controls.Add(titleLabel);

            flowPanel = new FlowLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                Padding = new Padding(10),
                BackColor = Color.Transparent,
                Margin = new Padding(0, 0, 0, 20)
            };

            // Création des champs de saisie modernisés
            rootFolderTextBox = CreateSettingInput("Dossier racine des images :", settingsManager.RootFolder);
            imagesPerPageTextBox = CreateSettingInput("Images par page :", settingsManager.ImagesPerPage.ToString());
            thumbnailSizeTextBox = CreateSettingInput("Taille des vignettes (px) :", settingsManager.ThumbnailSize.ToString());

            container.Controls.Add(flowPanel);

            FlowLayoutPanel buttonPanel = new FlowLayoutPanel
            {
                Dock = DockStyle.Bottom,
                Height = 50,
                FlowDirection = FlowDirection.RightToLeft,
                Padding = new Padding(0, 10, 0, 0),
                Visible = false,
                BackColor = Color.Transparent
            };

            saveButton = new Button
            {
                Text = "Enregistrer",
                Width = 150,    // Largeur ajustée
                Height = 40,    // Hauteur définie
                BackColor = Color.FromArgb(76, 175, 80),
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                ForeColor = Color.White,
                Cursor = Cursors.Hand,
                Margin = new Padding(5)
            };
            saveButton.FlatAppearance.BorderSize = 0;
            saveButton.Click += SaveButton_Click;

            cancelButton = new Button
            {
                Text = "Annuler",
                Width = 150,    // Largeur ajustée
                Height = 40,    // Hauteur définie
                BackColor = Color.FromArgb(244, 67, 54),
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                ForeColor = Color.White,
                Cursor = Cursors.Hand,
                Margin = new Padding(5)
            };
            cancelButton.FlatAppearance.BorderSize = 0;
            cancelButton.Click += CancelButton_Click;


            buttonPanel.Controls.Add(saveButton);
            buttonPanel.Controls.Add(cancelButton);

            container.Controls.Add(buttonPanel);

            // Affichage du panneau de boutons dès modification
            rootFolderTextBox.TextChanged += (s, e) => buttonPanel.Visible = true;
            imagesPerPageTextBox.TextChanged += (s, e) => buttonPanel.Visible = true;
            thumbnailSizeTextBox.TextChanged += (s, e) => buttonPanel.Visible = true;
        }

        // Méthode pour créer des inputs avec style moderne
        private TextBox CreateSettingInput(string label, string defaultValue)
        {
            Panel container = new Panel
            {
                Width = 600,
                Height = 60,
                Padding = new Padding(10),
                Margin = new Padding(0, 0, 0, 15),
                BackColor = Color.White,
                BorderStyle = BorderStyle.None
            };

            // Ajout d'une bordure fine en bas pour l'effet "underline"
            container.Paint += (s, e) =>
            {
                using (var pen = new Pen(Color.FromArgb(200, 200, 200), 2))
                {
                    e.Graphics.DrawLine(pen, 10, container.Height - 2, container.Width - 10, container.Height - 2);
                }
            };

            Label settingLabel = new Label
            {
                Text = label,
                Font = new Font("Segoe UI", 12),
                ForeColor = Color.FromArgb(66, 66, 66),
                AutoSize = false,
                Width = 250,
                TextAlign = ContentAlignment.MiddleLeft
            };

            TextBox inputBox = new TextBox
            {
                Text = defaultValue,
                Font = new Font("Segoe UI", 12),
                Width = 300,
                BorderStyle = BorderStyle.None,
                ForeColor = Color.FromArgb(33, 33, 33)
            };

            container.Controls.Add(settingLabel);
            container.Controls.Add(inputBox);
            settingLabel.Location = new Point(10, 15);
            inputBox.Location = new Point(270, 15);

            flowPanel.Controls.Add(container);

            return inputBox;
        }

        private void SaveButton_Click(object sender, EventArgs e)
        {
            settingsManager.RootFolder = rootFolderTextBox.Text;
            settingsManager.ImagesPerPage = int.TryParse(imagesPerPageTextBox.Text, out int imgPerPage) && imgPerPage > 0
                ? imgPerPage
                : 10;
            settingsManager.ThumbnailSize = int.TryParse(thumbnailSizeTextBox.Text, out int thumbSize) && thumbSize > 0
                ? thumbSize
                : 200;

            settingsManager.SaveSettings();
            MessageBox.Show("Paramètres enregistrés !", "Confirmation", MessageBoxButtons.OK, MessageBoxIcon.Information);
            saveButton.Parent.Visible = false;
        }

        private void CancelButton_Click(object sender, EventArgs e)
        {
            settingsManager.LoadSettings();
            rootFolderTextBox.Text = settingsManager.RootFolder;
            imagesPerPageTextBox.Text = settingsManager.ImagesPerPage.ToString();
            thumbnailSizeTextBox.Text = settingsManager.ThumbnailSize.ToString();
            saveButton.Parent.Visible = false;
        }
    }
}
