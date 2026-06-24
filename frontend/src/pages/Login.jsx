import React, { useState } from 'react';
import { Form, Input, Button, Card, message, Alert } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import api from '../api';

export default function Login({ onLogin }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (values) => {
    setLoading(true); setError(null);
    try {
      const res = await api.post('/api/auth/login', values);
      const { username, role, employeeId } = res.data.data;
      localStorage.setItem('username', username);
      localStorage.setItem('userRole', role);
      if (employeeId) localStorage.setItem('employeeId', employeeId);
      message.success('ログインしました');
      onLogin(role);
    } catch (err) {
      setError(err.response?.data?.error || 'サーバーに接続できません');
    } finally { setLoading(false); }
  };

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    }}>
      <Card style={{ width: 400, padding: '40px 32px', borderRadius: 8, boxShadow: '0 8px 24px rgba(0,0,0,0.15)' }}>
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div style={{ width: 56, height: 56, background: '#1890ff', borderRadius: 12, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 28, fontWeight: 'bold' }}>ST</div>
          <h1 style={{ fontSize: 22, marginTop: 12, color: 'rgba(0,0,0,0.85)' }}>ST管理システム</h1>
          <p style={{ fontSize: 13, color: 'rgba(0,0,0,0.45)', marginTop: 4 }}>社員・顧客・勤務・請求書の統合管理</p>
        </div>

        {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 20 }} closable onClose={() => setError(null)} />}

        <Form onFinish={handleSubmit} size="large">
          <Form.Item name="username" rules={[{ required: true, message: 'ユーザー名を入力してください' }]}>
            <Input prefix={<UserOutlined />} placeholder="ユーザー名" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: 'パスワードを入力してください' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="パスワード" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading}>ログイン</Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
