using DashboardApp.Config;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Windows.Forms;

namespace DashboardApp.Models
{
    public class ImageGallery
    {
        private readonly Panel mainPanel;
        private FlowLayoutPanel imagePanel;
        private Button sortButton;
        private Label messageLabel;
        private List<string> imageFiles = new List<string>();
        private string imagesFolder;

        public ImageGallery(Panel panel)
        {
            mainPanel = panel;
            imagesFolder = Path.Combine(AppSettings.Instance.RootFolder, "NonSortedImages");
        }

        public void CreateImageGallery()
        {
            mainPanel.Controls.Clear();

            messageLabel = new Label
            {
                Dock = DockStyle.Top,
                Font = new Font("Segoe UI", 14, FontStyle.Bold),
                ForeColor = Color.Black,
                TextAlign = ContentAlignment.MiddleCenter,
                Height = 40
            };

            imagePanel = new FlowLayoutPanel
            {
                Dock = DockStyle.Fill,
                AutoScroll = true,
                Padding = new Padding(10),
                WrapContents = true
            };

            sortButton = new Button
            {
                Text = "Trier",
                Dock = DockStyle.Bottom,
                Height = 40
            };
            sortButton.Click += SortImages;

            mainPanel.Controls.Add(messageLabel);
            mainPanel.Controls.Add(imagePanel);
            mainPanel.Controls.Add(sortButton);

            LoadImages();
        }

        private void LoadImages()
        {
            imagePanel.Controls.Clear();
            imageFiles.Clear();

            if (!Directory.Exists(imagesFolder))
            {
                messageLabel.Text = "Vous n’avez pas d’images, veuillez les exporter.";
                return;
            }

            string[] extensions = { "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp" };
            foreach (string ext in extensions)
            {
                imageFiles.AddRange(Directory.GetFiles(imagesFolder, ext));
            }

            if (imageFiles.Count == 0)
            {
                messageLabel.Text = "Vous n’avez pas d’images, veuillez les exporter.";
                return;
            }

            messageLabel.Text = "";
            DisplayImages();
        }

        private void DisplayImages()
        {
            imagePanel.Controls.Clear();
            Random rnd = new Random();

            foreach (string imagePath in imageFiles.OrderBy(x => rnd.Next()))
            {
                AddImageToPanel(imagePath);
            }
        }

        private void AddImageToPanel(string imagePath)
        {
            var pictureBox = new PictureBox
            {
                Width = 100,
                Height = 100,
                SizeMode = PictureBoxSizeMode.Zoom,
                Margin = new Padding(10),
                BorderStyle = BorderStyle.FixedSingle,
                Image = Image.FromFile(imagePath)
            };

            new ToolTip().SetToolTip(pictureBox, Path.GetFileName(imagePath));
            imagePanel.Controls.Add(pictureBox);
        }

        private void SortImages(object sender, EventArgs e)
        {
            //On Charge la page des options avancées pour trier é
        }
    }
}
