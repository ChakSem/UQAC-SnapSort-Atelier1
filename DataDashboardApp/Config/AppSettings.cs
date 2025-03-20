using System;
using System.Configuration;

namespace DashboardApp.Config
{
    public class AppSettings
    {
        private static AppSettings instance;

        // Valeurs par défaut basées sur Properties.Settings
        private static readonly string DEFAULT_ROOT_FOLDER = $@"C:\Users\{Environment.UserName}\Pictures";
        private static readonly int DEFAULT_IMAGES_PER_PAGE = 10;
        private static readonly int DEFAULT_THUMBNAIL_SIZE = 200;

        public string RootFolder { get; set; }
        public int ImagesPerPage { get; set; }
        public int ThumbnailSize { get; set; }

        private AppSettings()
        {
            LoadSettings();
        }

        public static AppSettings Instance
        {
            get
            {
                if (instance == null)
                    instance = new AppSettings();
                return instance;
            }
        }

        public void LoadSettings()
        {
            RootFolder = !string.IsNullOrEmpty(Properties.Settings.Default.RootFolder)
                ? Properties.Settings.Default.RootFolder
                : DEFAULT_ROOT_FOLDER;

            ImagesPerPage = Properties.Settings.Default.ImagesPerPage > 0
                ? Properties.Settings.Default.ImagesPerPage
                : DEFAULT_IMAGES_PER_PAGE;

            ThumbnailSize = Properties.Settings.Default.ThumbnailSize > 0
                ? Properties.Settings.Default.ThumbnailSize
                : DEFAULT_THUMBNAIL_SIZE;
        }

        public void SaveSettings()
        {
            Properties.Settings.Default.RootFolder = RootFolder;
            Properties.Settings.Default.ImagesPerPage = ImagesPerPage;
            Properties.Settings.Default.ThumbnailSize = ThumbnailSize;
            Properties.Settings.Default.Save();
        }

        public void ResetToDefault()
        {
            RootFolder = DEFAULT_ROOT_FOLDER;
            ImagesPerPage = DEFAULT_IMAGES_PER_PAGE;
            ThumbnailSize = DEFAULT_THUMBNAIL_SIZE;
            SaveSettings(); // Sauvegarde après réinitialisation
        }
    }
}
