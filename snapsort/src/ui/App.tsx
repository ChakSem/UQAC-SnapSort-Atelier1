import { HashRouter as Router, Routes, Route } from 'react-router-dom';
import Home from './pages/Home';
import Settings from './pages/Settings';
import UnsortedImages from './pages/UnsortedImages';
import MainLayout from "./layouts/MainLayout";
import Connexion from './pages/Connexion';
import Albums from './pages/Albums';
import AllImages from './pages/AllImages';

import './App.css'
import './styles/global.css'
import './styles/themes.css'
import "@flaticon/flaticon-uicons/css/all/all.css";

function App() {
  return (
    <Router>
      <Routes>
        {/* Pages sans Sidebar/Navbar (ex: login, signup) */}

        {/* Pages avec Sidebar/Navbar */}
        <Route element={<MainLayout />}>
          <Route path="/" element={<Home />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="/all-images" element={<AllImages />} />
          <Route path="/unsorted-images" element={<UnsortedImages />} />
          <Route path="/connexion" element={<Connexion />} />
          <Route path="/albums" element={<Albums />} />
        </Route>
      </Routes>
    </Router>
  );
}

export default App;
