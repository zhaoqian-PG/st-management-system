import React, { useState, useEffect } from 'react';
import { Routes, Route, Link, useLocation, useNavigate } from 'react-router-dom';
import { Layout, Menu, Button, Spin, message } from 'antd';
import {
  UserOutlined,
  ShopOutlined,
  CalendarOutlined,
  FileTextOutlined,
  SafetyOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import axios from 'axios';
import BankAccount from './pages/BankAccount';
import './App.css';

const { Header, Sider, Content } = Layout;
const API_BASE = '/api';

const menuItems = [
  { key: '/employee', icon: <UserOutlined />, label: '社員管理' },
  { key: '/customer', icon: <ShopOutlined />, label: '顧客管理' },
  { key: '/attendance', icon: <CalendarOutlined />, label: '勤務管理' },
  { key: '/invoice', icon: <FileTextOutlined />, label: '請求書・注文書管理' },
  { key: '/bank-accounts', icon: <SafetyOutlined />, label: '銀行口座管理' },
];

function App() {
  const [health, setHealth] = useState(null);
  const [error, setError] = useState(null);
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    axios
      .get(`${API_BASE}/health`)
      .then((res) => setHealth(res.data))
      .catch((err) => setError(err.message));
  }, []);

  const selectedKey = '/' + location.pathname.split('/')[1];

  const handleMenuClick = ({ key }) => {
    navigate(key);
  };

  const handleLogout = () => {
    message.info('ログアウトしました');
  };

  // Health check display
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
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        theme="dark"
        width={220}
      >
        <div className="logo-area">
          <div className="logo-icon">ST</div>
          {!collapsed && <span className="logo-text">ST管理システム</span>}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>

      <Layout>
        <Header className="app-header">
          <div className="header-right">
            <span className="user-name">👤 管理者 (admin)</span>
            <Button
              size="small"
              icon={<LogoutOutlined />}
              onClick={handleLogout}
            >
              ログアウト
            </Button>
          </div>
        </Header>

        <Content className="app-content">
          <Routes>
            <Route path="/" element={<PlaceholderPage name="社員管理" />} />
            <Route path="/employee" element={<PlaceholderPage name="社員管理" />} />
            <Route path="/customer" element={<PlaceholderPage name="顧客管理" />} />
            <Route path="/attendance" element={<PlaceholderPage name="勤務管理" />} />
            <Route path="/invoice" element={<PlaceholderPage name="請求書・注文書管理" />} />
            <Route path="/bank-accounts" element={<BankAccount />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  );
}

function PlaceholderPage({ name }) {
  return (
    <div>
      <h2>{name}</h2>
      <p style={{ color: 'rgba(0,0,0,0.45)', marginTop: 16 }}>
        🚧 このページは開発中です
      </p>
    </div>
  );
}

export default App;
