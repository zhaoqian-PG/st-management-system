import React, { useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Layout as AntLayout, Menu, Button, message } from 'antd';
import { UserOutlined, ShopOutlined, CalendarOutlined, FileTextOutlined, SafetyOutlined, ShoppingOutlined, LogoutOutlined } from '@ant-design/icons';

const { Header, Sider, Content } = AntLayout;

const allMenuItems = [
  { key: '/employee', icon: <UserOutlined />, label: '社員管理', adminOnly: true },
  { key: '/customer', icon: <ShopOutlined />, label: '顧客管理', adminOnly: true },
  { key: '/attendance', icon: <CalendarOutlined />, label: '勤務管理', adminOnly: false },
  { key: '/invoice', icon: <FileTextOutlined />, label: '請求書管理', adminOnly: true },
  { key: '/purchase-order', icon: <ShoppingOutlined />, label: '注文書管理', adminOnly: true },
  { key: '/bank-accounts', icon: <SafetyOutlined />, label: '銀行口座管理', adminOnly: true },
];

export default function Layout() {
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  const role = localStorage.getItem('userRole') || 'USER';
  const menuItems = role === 'ADMIN'
    ? allMenuItems
    : allMenuItems.filter(m => !m.adminOnly);

  const selectedKey = '/' + location.pathname.split('/')[1];

  const handleMenuClick = ({ key }) => { navigate(key); };

  const handleLogout = () => {
    localStorage.removeItem('userRole');
    message.info('ログアウトしました');
    navigate('/login');
  };

  return (
    <AntLayout style={{ minHeight: '100vh' }}>
      <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed} theme="dark" width={220}>
        <div className="logo-area">
          <div className="logo-icon">ST</div>
          {!collapsed && <span className="logo-text">ST管理システム</span>}
        </div>
        <Menu theme="dark" mode="inline" selectedKeys={[selectedKey]} items={menuItems} onClick={handleMenuClick} />
      </Sider>
      <AntLayout>
        <Header className="app-header">
          <div className="header-right">
            <span className="user-name">👤 {role === 'ADMIN' ? '管理者' : '一般'} ({localStorage.getItem('username') || 'user'})</span>
            <Button size="small" icon={<LogoutOutlined />} onClick={handleLogout}>ログアウト</Button>
          </div>
        </Header>
        <Content className="app-content"><Outlet /></Content>
      </AntLayout>
    </AntLayout>
  );
}
