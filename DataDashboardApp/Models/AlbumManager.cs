using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace DashboardApp.Models
{
    public class AlbumManager
    {
        private readonly Panel mainPanel;
        private TreeView albumTreeView;
        private FlowLayoutPanel imagePanel;
        // TODO : Racine a adapter en fonction de chacun pour la demo / test des affichages 
        //NB: Le probleme sera régle automatiquement par le fait que dans la VF ça sera un dossier qui est dans l'arbo de l'application 
        private string rootImageFolder = @"C:\Users\alaac\Pictures";
        //private string rootImageFolder = @"C:\Users\vigou\Pictures";

        public AlbumManager(Panel panel)
        {
            mainPanel = panel;
        }

        public void CreateAlbumView()
        {
            SplitContainer contentSplitContainer = new SplitContainer
            {
                Dock = DockStyle.Fill,
                Orientation = Orientation.Horizontal,
                SplitterDistance = 200
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
            parent.Controls.Add(imagePanel);
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
                DisplayImagesFromFolder(e.Node.Tag.ToString());
            }
        }

        private void DisplayImagesFromFolder(string folderPath)
        {
            imagePanel.Controls.Clear();
            string[] extensions = { "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp" };

            foreach (string ext in extensions)
            {
                foreach (string file in Directory.GetFiles(folderPath, ext))
                {
                    AddImageToPanel(file);
                }
            }
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
    }
}
