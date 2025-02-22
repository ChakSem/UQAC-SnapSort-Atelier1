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

        public NavigationManager(Panel panel)
        {
            mainPanel = panel;
            albumManager = new AlbumManager(mainPanel);
        }

        public void NavigateTo(string section)
        {
            mainPanel.Controls.Clear();

            if (section == "Albums")
            {
                albumManager.CreateAlbumView();
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
