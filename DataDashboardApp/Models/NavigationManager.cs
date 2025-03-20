using DashboardApp.Views;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace DashboardApp.Models
{
    public class NavigationManager
    {
        private readonly Panel mainPanel;
        private readonly AlbumManager albumManager;
        private readonly OptionManager optionManager;
        private readonly ImageGallery gallery;

        public NavigationManager(Panel panel)
        {
            mainPanel = panel;
            albumManager = new AlbumManager(mainPanel, 900);
            optionManager = new OptionManager(mainPanel, 900);
            gallery = new ImageGallery(mainPanel);

        }

        public void NavigateTo(string section)
        {
            mainPanel.Controls.Clear();

            if (section == "Albums")
            {
                albumManager.CreateAlbumView();
            }
            else if (section == "Options")
            {
                optionManager.CreateOptionView();
            }
            else if (section == "Images non triées")
            {
                gallery.CreateImageGallery();
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
        }
    }
}
