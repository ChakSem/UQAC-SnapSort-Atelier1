using DashboardApp.Config;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Windows.Forms;

namespace DashboardApp.Models
{
    public class AlbumManager
    {
        private readonly Panel mainPanel;
        private TreeView albumTreeView;
        private FlowLayoutPanel imagePanel;
        private Button loadMoreButton;
        private Button loadPreviousButton;
        private Label folderTitleLabel;

        // Valeurs récupérées depuis la configuration
        private string rootImageFolder;
        private List<string> imageFiles = new List<string>();
        private int currentIndex = 0;
        private int imagesPerPage;
        private int thumbnailSize;

        public AlbumManager(Panel panel, int width)
        {
            mainPanel = panel;
            panel.Width = width;
            // Récupération initiale des paramètres depuis AppSettings
            RefreshSettings();
        }

        /// <summary>
        /// Recharge les paramètres depuis AppSettings.
        /// </summary>
        private void RefreshSettings()
        {
            rootImageFolder = AppSettings.Instance.RootFolder;
            imagesPerPage = AppSettings.Instance.ImagesPerPage;
            thumbnailSize = AppSettings.Instance.ThumbnailSize;
        }

        public void CreateAlbumView()
        {
            var contentSplitContainer = new SplitContainer
            {
                Dock = DockStyle.Fill,
                Orientation = Orientation.Vertical,
                SplitterDistance = 250
            };
            mainPanel.Controls.Add(contentSplitContainer);

            SetupTreeView(contentSplitContainer.Panel1);
            SetupImagePanel(contentSplitContainer.Panel2);
            LoadAlbumTree();
        }

        private void SetupTreeView(Control parent)
        {
            albumTreeView = new TreeView
            {
                Dock = DockStyle.Fill,
                Font = new Font("Segoe UI", 12),
                BorderStyle = BorderStyle.None,
                BackColor = Color.WhiteSmoke
            };
            albumTreeView.AfterSelect += AlbumTreeView_AfterSelect;
            parent.Controls.Add(albumTreeView);
        }

        private void SetupImagePanel(Control parent)
        {
            folderTitleLabel = new Label
            {
                Dock = DockStyle.Top,
                Font = new Font("Segoe UI", 14, FontStyle.Bold),
                ForeColor = Color.Black,
                TextAlign = ContentAlignment.MiddleCenter,
                Height = 40,
                Text = "Sélectionnez un album"
            };

            imagePanel = new FlowLayoutPanel
            {
                Dock = DockStyle.Fill,
                AutoScroll = true,
                Padding = new Padding(10),
                WrapContents = true
            };

            Panel buttonPanel = new Panel
            {
                Dock = DockStyle.Bottom,
                Height = 40
            };

            loadPreviousButton = new Button
            {
                Text = "Charger Précédent",
                Enabled = false,
                Dock = DockStyle.Left,
                Width = 150
            };
            loadPreviousButton.Click += LoadPreviousImages;

            loadMoreButton = new Button
            {
                Text = "Charger Plus",
                Enabled = false,
                Dock = DockStyle.Right,
                Width = 150
            };
            loadMoreButton.Click += LoadMoreImages;

            buttonPanel.Controls.Add(loadPreviousButton);
            buttonPanel.Controls.Add(loadMoreButton);

            parent.Controls.Add(folderTitleLabel);
            parent.Controls.Add(imagePanel);
            parent.Controls.Add(buttonPanel);

            // Ajustement optionnel de la hauteur (à adapter si nécessaire)
            parent.Height = parent.Parent.Width + 1000;
        }

        private void LoadAlbumTree()
        {
            RefreshSettings();
            if (!Directory.Exists(rootImageFolder))
            {
                MessageBox.Show("Le dossier n'existe pas.", "Erreur", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return;
            }

            albumTreeView.Nodes.Clear();
            var rootNode = new TreeNode("Albums") { Tag = rootImageFolder };
            albumTreeView.Nodes.Add(rootNode);
            LoadDirectory(rootImageFolder, rootNode);
            rootNode.Expand();
        }

        private void LoadDirectory(string path, TreeNode parentNode)
        {
            foreach (string dir in Directory.GetDirectories(path))
            {
                var dirNode = new TreeNode(Path.GetFileName(dir)) { Tag = dir };
                parentNode.Nodes.Add(dirNode);
                LoadDirectory(dir, dirNode);
            }
        }

        private void AlbumTreeView_AfterSelect(object sender, TreeViewEventArgs e)
        {
            if (e.Node?.Tag != null)
            {
                LoadImagesFromFolder(e.Node.Tag.ToString());
            }
        }

        private void LoadImagesFromFolder(string folderPath)
        {
            // Actualisation des paramètres avant de charger les images
            RefreshSettings();

            imagePanel.Controls.Clear();
            imageFiles.Clear();
            currentIndex = 0;

            folderTitleLabel.Text = $"Album : {Path.GetFileName(folderPath)}";

            string[] extensions = { "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp" };
            foreach (string ext in extensions)
            {
                imageFiles.AddRange(Directory.GetFiles(folderPath, ext));
            }

            imageFiles = imageFiles.OrderBy(f => f).ToList();
            UpdateImageNavigation();
            DisplayImages();
        }

        private void DisplayImages()
        {
            imagePanel.Controls.Clear();
            for (int i = currentIndex; i < Math.Min(currentIndex + imagesPerPage, imageFiles.Count); i++)
            {
                AddImageToPanel(imageFiles[i]);
            }
            UpdateImageNavigation();
        }

        private void AddImageToPanel(string imagePath)
        {
            var pictureBox = new PictureBox
            {
                Width = thumbnailSize,
                Height = thumbnailSize,
                SizeMode = PictureBoxSizeMode.Zoom,
                Margin = new Padding(10),
                BorderStyle = BorderStyle.FixedSingle
            };

            try
            {
                pictureBox.Image = Image.FromFile(imagePath);
            }
            catch (Exception ex)
            {
                // Gestion de l'erreur au cas où le fichier ne pourrait être chargé
                MessageBox.Show($"Erreur lors du chargement de l'image : {Path.GetFileName(imagePath)}\n{ex.Message}", "Erreur", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }

            new ToolTip().SetToolTip(pictureBox, Path.GetFileName(imagePath));
            imagePanel.Controls.Add(pictureBox);
        }

        private void LoadMoreImages(object sender, EventArgs e)
        {
            if (currentIndex + imagesPerPage < imageFiles.Count)
            {
                currentIndex += imagesPerPage;
                DisplayImages();
            }
        }

        private void LoadPreviousImages(object sender, EventArgs e)
        {
            if (currentIndex - imagesPerPage >= 0)
            {
                currentIndex -= imagesPerPage;
                DisplayImages();
            }
        }

        private void UpdateImageNavigation()
        {
            loadPreviousButton.Enabled = currentIndex > 0;
            loadMoreButton.Enabled = currentIndex + imagesPerPage < imageFiles.Count;
        }
    }
}
