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

        // TODO : Racine a adapter en fonction de chacun pour la demo / test des affichages 
        //NB: Le probleme sera régle automatiquement par le fait que dans la VF ça sera un dossier qui est dans l'arbo de l'application 
        private string rootImageFolder = @"C:\Users\alaac\Pictures";
        //private string rootImageFolder = @"C:\Users\vigou\Pictures";
        private List<string> imageFiles = new List<string>();
        private int currentIndex = 0;
        private const int imagesPerPage = 10;

        public AlbumManager(Panel panel)
        {
            mainPanel = panel;
        }

        public void CreateAlbumView()
        {
            SplitContainer contentSplitContainer = new SplitContainer
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

            parent.Controls.Add(imagePanel);
            parent.Controls.Add(buttonPanel);
        }

        private void LoadAlbumTree()
        {
            if (!Directory.Exists(rootImageFolder))
            {
                MessageBox.Show("Le dossier n'existe pas.", "Erreur", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return;
            }

            albumTreeView.Nodes.Clear();
            TreeNode rootNode = new TreeNode("Albums") { Tag = rootImageFolder };
            albumTreeView.Nodes.Add(rootNode);
            LoadDirectory(rootImageFolder, rootNode);
            rootNode.Expand();
        }

        private void LoadDirectory(string path, TreeNode parentNode)
        {
            foreach (string dir in Directory.GetDirectories(path))
            {
                TreeNode dirNode = new TreeNode(Path.GetFileName(dir)) { Tag = dir };
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
            imagePanel.Controls.Clear();
            imageFiles.Clear();
            currentIndex = 0;

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
            PictureBox pictureBox = new PictureBox
            {
                Width = 150,
                Height = 150,
                SizeMode = PictureBoxSizeMode.Zoom,
                Margin = new Padding(5),
                BorderStyle = BorderStyle.FixedSingle,
                Image = Image.FromFile(imagePath)
            };

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
