import React, { useState, useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import { Spin } from 'antd';
import axios from 'axios';
import Layout from './components/Layout';
import PlaceholderPage from './components/PlaceholderPage';
import Employee from './pages/Employee';
import Customer from './pages/Customer';
import BankAccount from './pages/BankAccount';
import './App.css';

const API_BASE = '/api';

function App() {
  const [health, setHealth] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    axios
      .get(`${API_BASE}/health`)
      .then((res) => setHealth(res.data))
      .catch((err) => setError(err.message));
  }, []);

  if (!health && !error) {
    return (
      <div className="app-loading">
        <Spin size="large" tip="システム起動中..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="app-error">
        <h1>バックエンド接続エラー</h1>
        <p>{error}</p>
      </div>
    );
  }

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<PlaceholderPage name="社員管理" />} />
        <Route path="/employee" element={<Employee />} />
        <Route path="/customer" element={<Customer />} />
        <Route path="/attendance" element={<PlaceholderPage name="勤務管理" />} />
        <Route path="/invoice" element={<PlaceholderPage name="請求書・注文書管理" />} />
        <Route path="/bank-accounts" element={<BankAccount />} />
      </Route>
    </Routes>
  );
}

export default App;
