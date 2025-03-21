using DashboardApp.Config;
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.IO;
using System.Linq;
using System.Windows.Forms;

namespace DashboardApp.Views
{
    public partial class SortingConfigurationForm
    {
        private readonly Panel mainPanel;

        // Contrôles
        private Panel containerPanel;
        private TableLayoutPanel mainLayout;

        private Button addFolderButton;
        private TreeView folderTreeView;

        private Label nameLabel;
        private TextBox nameTextBox;
        private Label locationLabel;
        private TextBox locationTextBox;

        private Label dateRangeLabel;
        private DateTimePicker startDatePicker;
        private DateTimePicker endDatePicker;

        private Label subfolderLabel;
        private TrackBar subfolderTrackBar;
        private Label subfolderMinLabel;
        private Label subfolderMaxLabel;

        private Label minImagesLabel;
        private NumericUpDown minImagesNumeric;

        private CheckBox acceptConditionsCheckBox;
        private Button sortButton;

        public SortingConfigurationForm(Panel panel, int width)
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
            // PANEL de fond (pour le dégradé)
            containerPanel = new Panel
            {
                Dock = DockStyle.Fill,
                Padding = new Padding(20),
                BackColor = ColorTranslator.FromHtml("#f6fbff") // Couleur de fond
            };
            // Gestion du dégradé
            containerPanel.Paint += (s, e) =>
            {
                using (var brush = new LinearGradientBrush(
                    containerPanel.ClientRectangle,
                    Color.FromArgb(240, 248, 255),
                    Color.White,
                    LinearGradientMode.Vertical))
                {
                    e.Graphics.FillRectangle(brush, containerPanel.ClientRectangle);
                }
            };
            parent.Controls.Add(containerPanel);

            // TableLayoutPanel principal
            mainLayout = new TableLayoutPanel
            {
                Dock = DockStyle.Top,
                ColumnCount = 3,
                RowCount = 8,
                Padding = new Padding(10),
                AutoSize = true
            };
            // Ajustement des largeurs de colonnes
            mainLayout.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 200f));  // 1ère colonne
            mainLayout.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100f));   // 2e colonne (largeur dynamique)
            mainLayout.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 150f));  // 3e colonne
            // Ajustement des hauteurs de lignes
            for (int i = 0; i < mainLayout.RowCount; i++)
            {
                mainLayout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
            }
            containerPanel.Controls.Add(mainLayout);

            // Titre
            Label titleLabel = new Label
            {
                Text = "Configuration du tri",
                Font = new Font("Segoe UI", 18, FontStyle.Bold),
                ForeColor = Color.FromArgb(33, 150, 243),
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 20)
            };
            // Sur la 1ère ligne, on va occuper toute la largeur
            mainLayout.SetColumnSpan(titleLabel, 3);
            mainLayout.Controls.Add(titleLabel, 0, 0);

            // 1) Bouton “Ajouter un dossier”
            addFolderButton = new Button
            {
                Text = "Ajouter un dossier",
                BackColor = Color.FromArgb(33, 150, 243),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Width = 130,
                Height = 40,
                Margin = new Padding(5),
                Cursor = Cursors.Hand
            };
            addFolderButton.FlatAppearance.BorderSize = 0;
            addFolderButton.Click += AddFolderButton_Click;
            mainLayout.Controls.Add(addFolderButton, 0, 1);

            // 2) TreeView pour l’arborescence
            folderTreeView = new TreeView
            {
                Dock = DockStyle.Fill,
                Margin = new Padding(5),
                Font = new Font("Segoe UI", 10)
            };
            mainLayout.SetColumnSpan(folderTreeView, 2); // Occupe 2 colonnes (col 2 et 3)
            mainLayout.Controls.Add(folderTreeView, 1, 1);

            // 3) Champ “Nom”
            nameLabel = new Label
            {
                Text = "Nom :",
                AutoSize = true,
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                Margin = new Padding(5),
                TextAlign = ContentAlignment.MiddleLeft
            };
            mainLayout.Controls.Add(nameLabel, 0, 2);

            nameTextBox = new TextBox
            {
                Font = new Font("Segoe UI", 10),
                Width = 200,
                Margin = new Padding(5)
            };
            mainLayout.Controls.Add(nameTextBox, 1, 2);

            // 4) Champ “Localisation”
            locationLabel = new Label
            {
                Text = "Localisation :",
                AutoSize = true,
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                Margin = new Padding(5),
                TextAlign = ContentAlignment.MiddleLeft
            };
            mainLayout.Controls.Add(locationLabel, 0, 3);

            locationTextBox = new TextBox
            {
                Font = new Font("Segoe UI", 10),
                Width = 200,
                Margin = new Padding(5)
            };
            mainLayout.Controls.Add(locationTextBox, 1, 3);

            // 5) Période (DateTimePickers)
            dateRangeLabel = new Label
            {
                Text = "Période :",
                AutoSize = true,
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                Margin = new Padding(5),
                TextAlign = ContentAlignment.MiddleLeft
            };
            mainLayout.Controls.Add(dateRangeLabel, 0, 4);

            startDatePicker = new DateTimePicker
            {
                Format = DateTimePickerFormat.Short,
                Margin = new Padding(5)
            };
            endDatePicker = new DateTimePicker
            {
                Format = DateTimePickerFormat.Short,
                Margin = new Padding(5)
            };
            // Panel pour regrouper les 2 pickers
            Panel datePanel = new Panel
            {
                Dock = DockStyle.Fill
            };
            datePanel.Controls.Add(startDatePicker);
            datePanel.Controls.Add(endDatePicker);

            // Placement des pickers dans le panel
            startDatePicker.Location = new Point(0, 0);
            endDatePicker.Location = new Point(120, 0);

            mainLayout.Controls.Add(datePanel, 1, 4);

            // 6) Autoriser les sous-dossiers (TrackBar)
            subfolderLabel = new Label
            {
                Text = "Autoriser les sous-dossiers :",
                AutoSize = true,
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                Margin = new Padding(5),
                TextAlign = ContentAlignment.MiddleLeft
            };
            mainLayout.Controls.Add(subfolderLabel, 0, 5);

            subfolderTrackBar = new TrackBar
            {
                Minimum = 0,
                Maximum = 10,
                TickStyle = TickStyle.None,
                Value = 5,
                Width = 150,
                Margin = new Padding(5)
            };
            subfolderMinLabel = new Label
            {
                Text = "Peu détaillé",
                AutoSize = true,
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.Gray,
                Location = new Point(0, 35)
            };
            subfolderMaxLabel = new Label
            {
                Text = "Très détaillé",
                AutoSize = true,
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.Gray,
                Location = new Point(100, 35)
            };
            Panel trackPanel = new Panel
            {
                Dock = DockStyle.Fill,
                Height = 60
            };
            trackPanel.Controls.Add(subfolderTrackBar);
            trackPanel.Controls.Add(subfolderMinLabel);
            trackPanel.Controls.Add(subfolderMaxLabel);

            mainLayout.Controls.Add(trackPanel, 1, 5);

            // 7) Nombre minimum d’images/dossier
            minImagesLabel = new Label
            {
                Text = "Nombre minimum d’images/dossier :",
                AutoSize = true,
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                Margin = new Padding(5),
                TextAlign = ContentAlignment.MiddleLeft
            };
            mainLayout.Controls.Add(minImagesLabel, 0, 6);

            minImagesNumeric = new NumericUpDown
            {
                Minimum = 0,
                Maximum = 9999,
                Value = 10,
                Margin = new Padding(5),
                Width = 60
            };
            mainLayout.Controls.Add(minImagesNumeric, 1, 6);

            // 8) Case à cocher “J’accepte les conditions d’utilisation”
            acceptConditionsCheckBox = new CheckBox
            {
                Text = "J’accepte les conditions d’utilisation",
                Font = new Font("Segoe UI", 10),
                AutoSize = true,
                Margin = new Padding(5)
            };
            mainLayout.Controls.Add(acceptConditionsCheckBox, 1, 7);

            // 9) Bouton “Trier”
            sortButton = new Button
            {
                Text = "Trier",
                BackColor = Color.FromArgb(33, 150, 243),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Width = 120,
                Height = 40,
                Margin = new Padding(5),
                Cursor = Cursors.Hand
            };
            sortButton.FlatAppearance.BorderSize = 0;
            sortButton.Click += SortButton_Click;
            mainLayout.Controls.Add(sortButton, 2, 7);
        }

        // Événement "Ajouter un dossier"
        private void AddFolderButton_Click(object sender, EventArgs e)
        {
            using (var dialog = new FolderBrowserDialog())
            {
                if (dialog.ShowDialog() == DialogResult.OK)
                {
                    // Ajout du dossier dans le TreeView
                    var node = new TreeNode(System.IO.Path.GetFileName(dialog.SelectedPath))
                    {
                        Tag = dialog.SelectedPath
                    };
                    folderTreeView.Nodes.Add(node);
                }
            }
        }

        // Événement du bouton “Trier”
        private void SortButton_Click(object sender, EventArgs e)
        {
            // Vérification de la case “J’accepte les conditions...”
            if (!acceptConditionsCheckBox.Checked)
            {
                MessageBox.Show(
                    "Vous devez accepter les conditions d’utilisation avant de lancer le tri.",
                    "Information",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Warning
                );
                return;
            }

            // TODO : Logique de tri selon :
            // - Les dossiers ajoutés (folderTreeView)
            // - Les dates sélectionnées (startDatePicker.Value, endDatePicker.Value)
            // - Le niveau de détail (subfolderTrackBar.Value)
            // - Le nombre minimum d’images (minImagesNumeric.Value)
            // etc.

            MessageBox.Show("Tri en cours…", "Trier", MessageBoxButtons.OK, MessageBoxIcon.Information);
        }
    }
}