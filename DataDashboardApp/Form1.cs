using System;
using System.Drawing;
using System.Windows.Forms;
using System.IO;

namespace DashboardApp
{
    public partial class Form1 : Form
    {
        private bool isConnected = false;
        private TreeView albumTreeView;
        private FlowLayoutPanel imagePanel;
        // TODO : Racine a adapter en fonction de chacun pour la demo / test des affichages 
        //NB: Le probleme sera régle automatiquement par le fait que dans la VF ça sera un dossier qui est dans l'arbo de l'application 
        //private string rootImageFolder = @"C:\Users\alaac\Pictures";
        private string rootImageFolder = @"C:\Users\vigou\Pictures";

        public Form1()
        {
            InitializeComponent();
            CustomizeDesign();
            SetupEventHandlers();
        }

        private void CustomizeDesign()
        {
            // Configuration de base du formulaire
            this.FormBorderStyle = FormBorderStyle.None;
            this.Padding = new Padding(2);
            this.BackColor = Color.FromArgb(41, 128, 185);

            // Configuration du panneau principal
            mainPanel.BackColor = Color.White;
            mainPanel.Padding = new Padding(0);

            // Configuration des effets de survol des boutons
            foreach (Control ctrl in navigationPanel.Controls)
            {
                if (ctrl is Button btn)
                {
                    btn.MouseEnter += Button_MouseEnter;
                    btn.MouseLeave += Button_MouseLeave;
                }
            }

            UpdateConnectionStatus();
            ShowWelcomeMessage();
        }

        private void Button_MouseEnter(object sender, EventArgs e)
        {
            if (sender is Button btn)
            {
                btn.BackColor = Color.FromArgb(52, 152, 219);
            }
        }

        private void Button_MouseLeave(object sender, EventArgs e)
        {
            if (sender is Button btn)
            {
                btn.BackColor = Color.Transparent;
            }
        }

        private void SetupEventHandlers()
        {
            logoPanel.MouseDown += (s, e) => {
                if (e.Button == MouseButtons.Left)
                {
                    ReleaseCapture();
                    SendMessage(Handle, WM_NCLBUTTONDOWN, HT_CAPTION, 0);
                }
            };

            albumsButton.Click += (s, e) => NavigateTo("Albums");
            imagesButton.Click += (s, e) => NavigateTo("Images");
            loginButton.Click += (s, e) => ToggleConnection();
            settingsButton.Click += (s, e) => NavigateTo("Parametres");
        }

        private void NavigateTo(string section)
        {
            mainPanel.Controls.Clear();

            if (section == "Albums")
            {
                CreateAlbumView();
            }
            else
            {
                CreateDefaultView(section);
            }
        }

        private void CreateDefaultView(string title)
        {
            Label titleLabel = new Label
            {
                Text = title,
                Font = new Font("Segoe UI", 24, FontStyle.Bold),
                ForeColor = Color.FromArgb(52, 73, 94),
                Dock = DockStyle.Top,
                Height = 60
            };
            mainPanel.Controls.Add(titleLabel);

            Panel contentPanel = new Panel
            {
                Dock = DockStyle.Fill,
                Padding = new Padding(20),
                AutoScroll = true
            };
            mainPanel.Controls.Add(contentPanel);
        }

        private void CreateAlbumView()
        {
            // SplitContainer principal pour le titre
            SplitContainer mainSplitContainer = new SplitContainer
            {
                Dock = DockStyle.Fill,
                Orientation = Orientation.Horizontal,
                SplitterDistance = 40,
                IsSplitterFixed = true,
                FixedPanel = FixedPanel.Panel1
            };
            mainPanel.Controls.Add(mainSplitContainer);

            // Titre
            Label titleLabel = new Label
            {
                Text = "Albums",
                Font = new Font("Segoe UI", 24, FontStyle.Bold),
                ForeColor = Color.FromArgb(52, 73, 94),
                Dock = DockStyle.Fill,
                TextAlign = ContentAlignment.MiddleLeft,
                Padding = new Padding(10, 0, 0, 0)
            };
            mainSplitContainer.Panel1.Controls.Add(titleLabel);

            // SplitContainer pour l'arborescence et les images
            SplitContainer contentSplitContainer = new SplitContainer
            {
                Dock = DockStyle.Fill,
                Orientation = Orientation.Horizontal,
                SplitterDistance = 200
            };
            mainSplitContainer.Panel2.Controls.Add(contentSplitContainer);

            // Configuration de l'arborescence
            SetupTreeView(contentSplitContainer.Panel1);

            // Configuration du panneau d'images
            SetupImagePanel(contentSplitContainer.Panel2);

            // Chargement initial
            LoadAlbumTree();
        }

        private void SetupTreeView(Control parent)
        {
            albumTreeView = new TreeView
            {
                Dock = DockStyle.Fill,
                Font = new Font("Segoe UI", 12),
                BorderStyle = BorderStyle.None,
                BackColor = Color.WhiteSmoke,
                Indent = 20,
                ItemHeight = 25
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
                BackColor = Color.White,
                Padding = new Padding(10),
                WrapContents = true
            };
            parent.Controls.Add(imagePanel);
        }

        private void LoadAlbumTree()
        {
            if (!Directory.Exists(rootImageFolder))
            {
                MessageBox.Show("Le dossier racine des images n'existe pas.", "Erreur", 
                    MessageBoxButtons.OK, MessageBoxIcon.Error);
                return;
            }

            albumTreeView.Nodes.Clear();
            TreeNode rootNode = new TreeNode("Albums")
            {
                Tag = rootImageFolder,
                ImageKey = "folder",
                SelectedImageKey = "folder"
            };
            albumTreeView.Nodes.Add(rootNode);
            LoadDirectory(rootImageFolder, rootNode);
            rootNode.Expand();
        }

        private void LoadDirectory(string path, TreeNode parentNode)
        {
            try
            {
                foreach (string dir in Directory.GetDirectories(path))
                {
                    DirectoryInfo dirInfo = new DirectoryInfo(dir);
                    TreeNode dirNode = new TreeNode(dirInfo.Name)
                    {
                        Tag = dir,
                        ImageKey = "folder",
                        SelectedImageKey = "folder"
                    };
                    parentNode.Nodes.Add(dirNode);
                    LoadDirectory(dir, dirNode);
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Erreur lors du chargement du dossier : {ex.Message}");
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
            imagePanel.SuspendLayout();
            imagePanel.Controls.Clear();

            try
            {
                string[] supportedExtensions = { "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp" };
                foreach (string extension in supportedExtensions)
                {
                    foreach (string imagePath in Directory.GetFiles(folderPath, extension))
                    {
                        AddImageToPanel(imagePath);
                    }
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Erreur lors du chargement des images : {ex.Message}");
            }

            imagePanel.ResumeLayout();
        }

        private void AddImageToPanel(string imagePath)
        {
            try
            {
                using (Image fullsizeImage = Image.FromFile(imagePath))
                {
                    PictureBox pictureBox = new PictureBox
                    {
                        Width = 150,
                        Height = 150,
                        SizeMode = PictureBoxSizeMode.Zoom,
                        Margin = new Padding(5),
                        BorderStyle = BorderStyle.FixedSingle,
                        Image = new Bitmap(fullsizeImage)
                    };

                    new ToolTip().SetToolTip(pictureBox, Path.GetFileName(imagePath));
                    imagePanel.Controls.Add(pictureBox);
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors du chargement de l'image {imagePath}: {ex.Message}");
            }
        }

        private void ShowWelcomeMessage()
        {
            Label welcomeLabel = new Label
            {
                Text = "Tu préfères profiter de ta vie plutôt que de la ranger, SnapSort est là !",
                Font = new Font("Segoe UI", 12, FontStyle.Bold),
                ForeColor = Color.FromArgb(52, 73, 94),
                AutoSize = true,
                TextAlign = ContentAlignment.MiddleCenter
            };

            welcomeLabel.Location = new Point(
                (mainPanel.Width - welcomeLabel.Width) / 3,
                (mainPanel.Height - welcomeLabel.Height) / 2
            );
            welcomeLabel.Anchor = AnchorStyles.None;

            mainPanel.Controls.Add(welcomeLabel);
        }

        private void UpdateConnectionStatus()
        {
            loginButton.Text = isConnected ? "Déconnexion" : "Connexion";
            loginButton.BackColor = isConnected ? Color.Red : Color.Green;
            statusLabel.Text = isConnected ? "Connecté" : "Déconnecté";
        }

        private void ToggleConnection()
        {
            isConnected = !isConnected;
            UpdateConnectionStatus();
        }

        // Windows API calls for window dragging
        public const int WM_NCLBUTTONDOWN = 0xA1;
        public const int HT_CAPTION = 0x2;

        [System.Runtime.InteropServices.DllImport("user32.dll")]
        public static extern int SendMessage(IntPtr hWnd, int Msg, int wParam, int lParam);

        [System.Runtime.InteropServices.DllImport("user32.dll")]
        public static extern bool ReleaseCapture();
    }
}