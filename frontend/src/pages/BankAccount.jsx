import React, { useState, useEffect, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, Select, Space, message,
  Card, Tabs, Tag,
} from 'antd';
import {
  PlusOutlined, EyeOutlined, EyeInvisibleOutlined,
  EditOutlined, DeleteOutlined, SafetyOutlined,
} from '@ant-design/icons';
import { bankAccountApi } from '../services/bankAccountApi';

const { Option } = Select;
const PAGE_SIZE = 10;

export default function BankAccount() {
  const [activeTab, setActiveTab] = useState('CUSTOMER');
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);
  const [formLoading, setFormLoading] = useState(false);
  const [revealedAccounts, setRevealedAccounts] = useState({});
  const [revealTimers, setRevealTimers] = useState({});
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await bankAccountApi.list({ page: page - 1, size: PAGE_SIZE, category: activeTab });
      const r = res.data.data;
      setData(r.content || []);
      setTotal(r.totalElements || 0);
    } catch { message.error('データの取得に失敗しました'); }
    finally { setLoading(false); }
  }, [page, activeTab]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleCreate = () => {
    setEditingRecord(null);
    form.resetFields();
    form.setFieldsValue({ category: activeTab });
    setModalVisible(true);
  };

  const handleEdit = (record) => {
    setEditingRecord(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setFormLoading(true);
      if (editingRecord) {
        await bankAccountApi.update(editingRecord.id, values);
        message.success('銀行口座を更新しました');
      } else {
        await bankAccountApi.create(values);
        message.success('銀行口座を登録しました');
      }
      setModalVisible(false);
      fetchData();
    } catch (err) {
      if (err.response?.data?.error) message.error(err.response.data.error);
    } finally { setFormLoading(false); }
  };

  const handleDeleteConfirm = (record) => {
    Modal.confirm({
      title: '削除確認',
      content: `取引番号 ${record.torihikiNo} を削除しますか？`,
      okText: '削除', cancelText: 'キャンセル', okType: 'danger', centered: true,
      onOk: async () => {
        try { await bankAccountApi.delete(record.id); message.success('削除しました'); fetchData(); }
        catch (err) { message.error(err.response?.data?.error || 'エラー'); }
      },
    });
  };

  const toggleReveal = (id, fullNumber) => {
    if (revealedAccounts[id]) {
      const nr = { ...revealedAccounts }; delete nr[id]; setRevealedAccounts(nr);
      if (revealTimers[id]) { clearTimeout(revealTimers[id]); const nt = { ...revealTimers }; delete nt[id]; setRevealTimers(nt); }
    } else {
      Modal.confirm({
        title: '口座番号を表示', content: 'この操作はログに記録されます。続行しますか？',
        okText: '表示', cancelText: 'キャンセル', centered: true,
        onOk: () => {
          setRevealedAccounts({ ...revealedAccounts, [id]: true });
          const t = setTimeout(() => { setRevealedAccounts((prev) => { const n = { ...prev }; delete n[id]; return n; }); }, 30000);
          setRevealTimers((prev) => ({ ...prev, [id]: t }));
        },
      });
    }
  };

  const renderAccountNumber = (text, record) => {
    const isRevealed = revealedAccounts[record.id];
    return (
      <Space size={4}>
        <span style={{ fontFamily: 'monospace', letterSpacing: 1 }}>{isRevealed ? text : '****' + (text ? text.slice(-4) : '****')}</span>
        <Button type="link" size="small" icon={isRevealed ? <EyeInvisibleOutlined /> : <EyeOutlined />}
          onClick={() => toggleReveal(record.id, text)} style={{ fontSize: 11 }}>
          {isRevealed ? '隠す' : '表示'}
        </Button>
      </Space>
    );
  };

  const columns = [
    { title: '取引番号', key: 'torihiki', width: 140,
      render: (_, r) => <strong>{r.torihikiNo}-{r.branchNo}</strong> },
    { title: '支店番号', dataIndex: 'branchCode', width: 90 },
    { title: '銀行名称', dataIndex: 'bankName', width: 150 },
    { title: '口座種類', dataIndex: 'accountType', width: 90, render: (t) => <Tag color={t === '普通' ? 'blue' : 'orange'}>{t}</Tag> },
    { title: '口座番号', dataIndex: 'accountNumber', width: 200, render: renderAccountNumber },
    { title: '口座名義', dataIndex: 'accountHolder', width: 180 },
    {
      title: '操作', width: 140, fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>編集</Button>
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDeleteConfirm(record)}>削除</Button>
        </Space>
      ),
    },
  ];

  const tabItems = [
    { key: 'CUSTOMER', label: '🏢 顧客用' },
    { key: 'EMPLOYEE', label: '👤 社員用' },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 20 }}><SafetyOutlined /> 銀行口座管理</h2>
      <Tabs activeKey={activeTab} onChange={(k) => { setActiveTab(k); setPage(1); }} items={tabItems} />
      <Card
        title={`銀行口座一覧（${activeTab === 'CUSTOMER' ? '顧客用' : '社員用'}）`}
        extra={<Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>新規登録</Button>}
      >
        <Table columns={columns} dataSource={data} rowKey="id" loading={loading}
          pagination={{ current: page, pageSize: PAGE_SIZE, total, showSizeChanger: false,
            showTotal: (t) => `全 ${t} 件`, onChange: (p) => setPage(p) }} />
      </Card>

      <Modal title={editingRecord ? '銀行口座を編集' : '銀行口座を追加'}
        open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)}
        confirmLoading={formLoading} okText="保存" cancelText="キャンセル"
        destroyOnClose centered width={640}>
        <Form form={form} layout="vertical" style={{ marginTop: 8 }}>
          <Form.Item name="category" label="区分"
            rules={[{ required: true, message: '区分は必須です' }]}>
            <Select>
              <Option value="CUSTOMER">🏢 顧客用</Option>
              <Option value="EMPLOYEE">👤 社員用</Option>
            </Select>
          </Form.Item>
          <Form.Item name="bankName" label="銀行名称"
            rules={[{ required: true, message: '銀行名称は必須です' }, { max: 200, message: '200文字以内' }]}>
            <Input placeholder="例: 三菱UFJ銀行" maxLength={200} />
          </Form.Item>
          <Form.Item name="branchCode" label="支店番号"
            rules={[{ required: true, message: '支店番号は必須です' }, { max: 10, message: '10文字以内' }]}>
            <Input placeholder="例: 038" maxLength={10} />
          </Form.Item>
          <Form.Item name="accountType" label="口座種類"
            rules={[{ required: true, message: '口座種類を選択してください' }]}>
            <Select placeholder="選択してください">
              <Option value="普通">普通</Option><Option value="当座">当座</Option>
            </Select>
          </Form.Item>
          <Form.Item name="accountNumber" label="口座番号"
            rules={[{ required: true, message: '口座番号は必須です' }, { max: 50 }, { pattern: /^[0-9]+$/, message: '半角数字で入力' }]}>
            <Input.Password placeholder="例: 1234567" maxLength={50} autoComplete="off" />
          </Form.Item>
          <Form.Item name="accountHolder" label="口座名義"
            rules={[{ required: true, message: '口座名義は必須です' }, { max: 100 }]}>
            <Input placeholder="例: カ）テクノトウキョウ" maxLength={100} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
