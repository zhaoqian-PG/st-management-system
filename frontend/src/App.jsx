import React, { useState, useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import { Spin } from 'antd';
import axios from 'axios';
import Layout from './components/Layout';
import PlaceholderPage from './components/PlaceholderPage';
import Employee from './pages/Employee';
import Customer from './pages/Customer';
import BankAccount from './pages/BankAccount';
import Attendance from './pages/Attendance';
import './App.css';

const API_BASE = '/api';

function App() {
  const [health, setHealth] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    axios.get(`${API_BASE}/health`).then(res => setHealth(res.data)).catch(err => setError(err.message));
    // Set default role if not logged in
    if (!localStorage.getItem('userRole')) {
      localStorage.setItem('userRole', 'ADMIN');
      localStorage.setItem('username', 'admin');
    }
  }, []);

  if (!health && !error) return <div className="app-loading"><Spin size="large" tip="システム起動中..." /></div>;
  if (error) return <div className="app-error"><h1>バックエンド接続エラー</h1><p>{error}</p></div>;

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Attendance />} />
        <Route path="/employee" element={<Employee />} />
        <Route path="/customer" element={<Customer />} />
        <Route path="/attendance" element={<Attendance />} />
        <Route path="/invoice" element={<PlaceholderPage name="請求書・注文書管理" />} />
        <Route path="/bank-accounts" element={<BankAccount />} />
      </Route>
    </Routes>
  );
}

export default App;
