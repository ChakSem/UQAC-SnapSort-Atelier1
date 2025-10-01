import { NavLink, Link } from 'react-router-dom';
import '../styles/components.css';

import logo from '../assets/logo.png';
import iconAlbums from '../assets/icon_albums.png';
import iconSettings from '../assets/icon_settings.png';
import iconUser from '../assets/icon_user.png';
import iconUnsortedImages from '../assets/icon_unsorted_images.png';
import iconWifi from '../assets/icon_wifi.png';
import iconAllImages from '../assets/icon_all_images.png';

import { NavBarItemProps, BottomItemProps } from '../types/interfaces';

const NavBarItem: React.FC<NavBarItemProps> = ({ label, imageUrl, redirectTo }) => {
  return (
    <NavLink
      to={redirectTo}
      className={({ isActive }) =>
        "sidebar-item" + (isActive ? " active" : "")
      }
    >
      <img src={imageUrl} alt={label} className="sidebar-icon" />
      <span>{label}</span>
    </NavLink>
  );
};

const BottomItem: React.FC<BottomItemProps> = ({ imageUrl, redirectTo }) => {
  return (
    <Link to={redirectTo} className="bottom-item">
      <img src={imageUrl} alt="logo" className="bottom-icon" />
    </Link>
  );
};

const Sidebar = () => {
  return (
    <div className='sidebar'>
        <div className="top">
          <img src={logo} className="sidebar-logo" alt="logo" />
        </div>
        <div className="mid">
          <NavBarItem
            label="Albums"
            imageUrl={iconAlbums}
            redirectTo="/albums"
          />
          <NavBarItem
            label="Toutes les images"
            imageUrl={iconAllImages}
            redirectTo="/all-images"
          />
          <NavBarItem
            label="Images transférées"
            imageUrl={iconUnsortedImages}
            redirectTo="/unsorted-images"
          />
          <NavBarItem
            label="Connexion"
            imageUrl={iconWifi}
            redirectTo="/connexion"
          />
        </div>
        <div className="bottom">
          <BottomItem
            imageUrl={iconSettings}
            redirectTo="/settings"
          />
          <BottomItem
            imageUrl={iconUser}
            redirectTo="/profile"
          />
        </div>
    </div>
  )
}

export default Sidebar