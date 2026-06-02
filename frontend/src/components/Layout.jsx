import React, { useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Layout as AntLayout, Menu, Button, message } from 'antd';
import {
  UserOutlined,
  ShopOutlined,
  CalendarOutlined,
  FileTextOutlined,
  SafetyOutlined,
  LogoutOutlined,
} from '@ant-design/icons';

const { Header, Sider, Content } = AntLayout;

const menuItems = [
  { key: '/employee', icon: <UserOutlined />, label: '社員管理' },
  { key: '/customer', icon: <ShopOutlined />, label: '顧客管理' },
  { key: '/attendance', icon: <CalendarOutlined />, label: '勤務管理' },
  { key: '/invoice', icon: <FileTextOutlined />, label: '請求書・注文書管理' },
  { key: '/bank-accounts', icon: <SafetyOutlined />, label: '銀行口座管理' },
];

export default function Layout() {
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  const selectedKey = '/' + location.pathname.split('/')[1];

  const handleMenuClick = ({ key }) => {
    navigate(key);
  };

  const handleLogout = () => {
    message.info('ログアウトしました');
  };

  return (
    <AntLayout style={{ minHeight: '100vh' }}>
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

      <AntLayout>
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
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  );
}
