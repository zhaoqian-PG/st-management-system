import React, { useState, useEffect } from 'react';
import { Routes, Route, useNavigate } from 'react-router-dom';
import { Spin } from 'antd';
import axios from 'axios';
import Layout from './components/Layout';
import PlaceholderPage from './components/PlaceholderPage';
import Employee from './pages/Employee';
import Customer from './pages/Customer';
import BankAccount from './pages/BankAccount';
import Attendance from './pages/Attendance';
import Invoice from './pages/Invoice';
import Login from './pages/Login';
import './App.css';

const API_BASE = '/api';

function App() {
  const [health, setHealth] = useState(null);
  const [error, setError] = useState(null);
  const [loggedIn, setLoggedIn] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    axios.get(`${API_BASE}/health`).then(res => setHealth(res.data)).catch(err => setError(err.message));
    if (localStorage.getItem('userRole')) setLoggedIn(true);
  }, []);

  const handleLogin = () => {
    setLoggedIn(true);
    navigate('/');
  };

  if (!health && !error) return <div className="app-loading"><Spin size="large" tip="システム起動中..." /></div>;
  if (error) return <div className="app-error"><h1>バックエンド接続エラー</h1><p>{error}</p></div>;
  if (!loggedIn) return <Login onLogin={handleLogin} />;

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Attendance />} />
        <Route path="/employee" element={<Employee />} />
        <Route path="/customer" element={<Customer />} />
        <Route path="/attendance" element={<Attendance />} />
        <Route path="/invoice" element={<Invoice />} />
        <Route path="/bank-accounts" element={<BankAccount />} />
      </Route>
    </Routes>
  );
}

export default App;
