import React, { useState, useEffect, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, Select, Space, message,
  Card, Tag, Tooltip,
} from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined,
  ShopOutlined, MinusCircleOutlined, StarFilled,
} from '@ant-design/icons';
import { customerApi } from '../services/customerApi';
import { bankAccountApi } from '../services/bankAccountApi';

const PAGE_SIZE = 10;

const SectionTitle = ({ children }) => (
  <h4 style={{
    margin: '16px 0 12px', paddingBottom: 6,
    borderBottom: '2px solid #1890ff',
    color: '#1890ff', fontWeight: 600, fontSize: 14,
  }}>{children}</h4>
);

// Common validation rules
const rules = {
  required: (label) => [{ required: true, message: `${label}は必須です` }],
  max: (len) => [{ max: len, message: `${len}文字以内で入力してください` }],
  phone: [
    { max: 20, message: '20文字以内で入力してください' },
    { pattern: /^[0-9\-+() ]*$/, message: '正しい電話番号形式で入力してください' },
  ],
  email: [
    { max: 255, message: '255文字以内で入力してください' },
    { type: 'email', message: '正しいメールアドレス形式で入力してください' },
  ],
  url: [
    { max: 500, message: '500文字以内で入力してください' },
    { pattern: /^(https?:\/\/)?[\w.-]+\.[a-z]{2,}(\/\S*)?$/, message: '正しいURL形式で入力してください' },
  ],
};

export default function Customer() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);
  const [formLoading, setFormLoading] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [linkedAccounts, setLinkedAccounts] = useState([]);
  const [availableAccounts, setAvailableAccounts] = useState([]);
  const [selectedBankId, setSelectedBankId] = useState(null);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page: page - 1, size: PAGE_SIZE };
      if (keyword) params.keyword = keyword;
      const res = await customerApi.list(params);
      const result = res.data.data;
      setData(result.content || []);
      setTotal(result.totalElements || 0);
    } catch (err) {
      message.error('データの取得に失敗しました');
    } finally {
      setLoading(false);
    }
  }, [page, keyword]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const fetchLinkedAccounts = async (customerId) => {
    try {
      const res = await bankAccountApi.getByCustomerId(customerId);
      setLinkedAccounts(res.data.data || []);
    } catch { setLinkedAccounts([]); }
  };

  const fetchAvailableAccounts = async () => {
    try {
      const res = await bankAccountApi.list({ size: 100 });
      const all = res.data.data.content || [];
      setAvailableAccounts(all.filter((a) => a.category === 'CUSTOMER' && a.customerId === null));
    } catch { setAvailableAccounts([]); }
  };

  const handleCreate = () => {
    setEditingRecord(null);
    setLinkedAccounts([]);
    setSelectedBankId(null);
    form.resetFields();
    setModalVisible(true);
    fetchAvailableAccounts();
  };

  const handleEdit = async (record) => {
    setEditingRecord(record);
    setSelectedBankId(null);
    form.setFieldsValue(record);
    setModalVisible(true);
    await fetchLinkedAccounts(record.id);
    await fetchAvailableAccounts();
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setFormLoading(true);
      let customerId = editingRecord?.id;
      if (editingRecord) {
        await customerApi.update(customerId, values);
        message.success('顧客情報を更新しました');
      } else {
        const res = await customerApi.create(values);
        customerId = res.data.data.id;
        message.success('顧客を登録しました');
      }
      if (linkedAccounts.length > 0 && customerId) {
        for (const acc of linkedAccounts) {
          if (acc.customerId !== customerId) {
            try { await bankAccountApi.bindToCustomer(acc.id, customerId); } catch (e) {}
          }
        }
      }
      setModalVisible(false);
      fetchData();
    } catch (err) {
      if (err.response?.data?.error) message.error(err.response.data.error);
    } finally {
      setFormLoading(false);
    }
  };

  const handleDeleteConfirm = (record) => {
    Modal.confirm({
      title: '削除確認',
      content: `顧客 ${record.customerCode} ${record.companyName} を削除してもよろしいですか？`,
      okText: '削除', cancelText: 'キャンセル', okType: 'danger', centered: true,
      onOk: async () => {
        try { await customerApi.delete(record.id); message.success('削除しました'); fetchData(); }
        catch (err) { message.error(err.response?.data?.error || '削除に失敗しました'); }
      },
    });
  };

  const handleBindBank = () => {
    if (!selectedBankId) return;
    const acc = availableAccounts.find((a) => a.id === selectedBankId);
    if (!acc) return;
    setLinkedAccounts([...linkedAccounts, acc]);
    setAvailableAccounts(availableAccounts.filter((a) => a.id !== selectedBankId));
    setSelectedBankId(null);
  };

  const handleUnbindBank = (bank) => {
    if (editingRecord) {
      Modal.confirm({
        title: '紐付け解除確認', content: 'この銀行口座の紐付けを解除してもよろしいですか？',
        okText: '解除', cancelText: 'キャンセル', centered: true,
        onOk: async () => {
          try {
            await bankAccountApi.unbindFromCustomer(bank.id);
            message.success('紐付けを解除しました');
            await fetchLinkedAccounts(editingRecord.id);
            await fetchAvailableAccounts();
          } catch (err) { message.error('解除に失敗しました'); }
        },
      });
    } else {
      setLinkedAccounts(linkedAccounts.filter((a) => a.id !== bank.id));
      setAvailableAccounts([...availableAccounts, bank]);
    }
  };

  const columns = [
    { title: '顧客番号', dataIndex: 'customerCode', width: 100 },
    { title: '会社名', dataIndex: 'companyName', width: 180, ellipsis: true },
    { title: '社長名', dataIndex: 'presidentName', width: 100 },
    { title: '営業担当', dataIndex: 'salesRepName', width: 100 },
    { title: '事務担当', dataIndex: 'adminRepName', width: 100 },
    {
      title: '銀行口座', dataIndex: 'torihikiNo', width: 280, ellipsis: true,
      render: (text) => {
        if (!text) return <span style={{ color: 'rgba(0,0,0,0.25)' }}>未設定</span>;
        const items = text.split('、');
        return (
          <Tooltip title={text}>
            <span style={{ fontSize: 12 }}>
              {items.length <= 2 ? text : `${items[0]}、${items[1]} 他${items.length - 2}件`}
            </span>
          </Tooltip>
        );
      },
    },
    {
      title: 'Webサイト', dataIndex: 'website', width: 160, ellipsis: true,
      render: (text) => text ? <a href={`https://${text}`} target="_blank" rel="noreferrer">{text}</a> : '-',
    },
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

  const bankColumns = [
    { title: '主カード', dataIndex: 'isDefault', width: 60,
      render: (v) => v ? <StarFilled style={{ color: '#faad14' }} /> : null },
    { title: '銀行名称', dataIndex: 'bankName', width: 130 },
    { title: '支店番号', dataIndex: 'branchCode', width: 80 },
    { title: '口座種類', dataIndex: 'accountType', width: 80, render: (t) => <Tag color={t === '普通' ? 'blue' : 'orange'}>{t}</Tag> },
    { title: '口座番号', dataIndex: 'accountNumber', width: 120, render: (t) => '****' + (t ? t.slice(-4) : '****') },
    { title: '口座名義', dataIndex: 'accountHolder', width: 150 },
    {
      title: '操作', width: 70,
      render: (_, record) => (
        <Button type="link" danger icon={<MinusCircleOutlined />} onClick={() => handleUnbindBank(record)}>解除</Button>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 20 }}><ShopOutlined /> 顧客管理</h2>
      <Card>
        <div style={{ display: 'flex', gap: 12, marginBottom: 16, alignItems: 'center', flexWrap: 'wrap' }}>
          <Input
            placeholder="顧客番号・会社名で検索"
            value={keyword}
            onChange={(e) => { setKeyword(e.target.value); setPage(1); }}
            style={{ width: 260 }}
            allowClear
          />
          <span style={{ flex: 1 }} />
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>新規登録</Button>
        </div>
        <Table
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          pagination={{
            current: page, pageSize: PAGE_SIZE, total,
            showSizeChanger: false,
            showTotal: (t) => `全 ${t} 件`,
            onChange: (p) => setPage(p),
          }}
        />
      </Card>

      <Modal
        title={editingRecord ? '顧客情報編集' : '顧客新規登録'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        confirmLoading={formLoading}
        okText="保存" cancelText="キャンセル"
        destroyOnClose centered width={800}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 8 }}>
          <SectionTitle>● 会社情報</SectionTitle>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            {editingRecord ? (
              <>
                <Form.Item name="customerCode" hidden><Input /></Form.Item>
                <Form.Item label="顧客番号">
                  <Input value={editingRecord.customerCode} disabled style={{ color: 'rgba(0,0,0,0.65)' }} />
                </Form.Item>
              </>
            ) : (
              <Form.Item label="顧客番号">
                <Input value="自動採番（CUS0001〜）" disabled style={{ color: 'rgba(0,0,0,0.45)' }} />
              </Form.Item>
            )}
            <Form.Item name="companyName" label="会社名"
              rules={[...rules.required('会社名'), ...rules.max(200)]}>
              <Input placeholder="例: 株式会社〇〇" maxLength={200} />
            </Form.Item>
            <Form.Item name="presidentName" label="社長名" rules={rules.max(100)}>
              <Input placeholder="例: 田中 太郎" maxLength={100} />
            </Form.Item>
            <Form.Item name="website" label="会社Webサイト" rules={rules.url}>
              <Input placeholder="例: www.example.co.jp" maxLength={500} />
            </Form.Item>
          </div>
          <Form.Item name="address" label="住所" rules={rules.max(500)}>
            <Input placeholder="例: 東京都千代田区〇〇" maxLength={500} />
          </Form.Item>

          <SectionTitle>● 営業担当</SectionTitle>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0 16px' }}>
            <Form.Item name="salesRepName" label="氏名" rules={rules.max(100)}>
              <Input placeholder="例: 中村 博" maxLength={100} />
            </Form.Item>
            <Form.Item name="salesRepPhone" label="連絡先" rules={rules.phone}>
              <Input placeholder="例: 090-1234-5678" maxLength={20} />
            </Form.Item>
            <Form.Item name="salesRepEmail" label="メール" rules={rules.email}>
              <Input placeholder="例: sales@example.co.jp" maxLength={255} />
            </Form.Item>
          </div>

          <SectionTitle>● 事務担当</SectionTitle>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0 16px' }}>
            <Form.Item name="adminRepName" label="氏名" rules={rules.max(100)}>
              <Input placeholder="例: 斉藤 雅子" maxLength={100} />
            </Form.Item>
            <Form.Item name="adminRepPhone" label="連絡先" rules={rules.phone}>
              <Input placeholder="例: 03-1111-2223" maxLength={20} />
            </Form.Item>
            <Form.Item name="adminRepEmail" label="メール" rules={rules.email}>
              <Input placeholder="例: admin@example.co.jp" maxLength={255} />
            </Form.Item>
          </div>

          <SectionTitle>● 銀行口座一覧</SectionTitle>
          <Space style={{ marginBottom: 12 }}>
            <Select
              placeholder="＋ 口座を追加（未紐付MASTERから選択）"
              value={selectedBankId}
              onChange={setSelectedBankId}
              style={{ minWidth: 300 }}
              allowClear showSearch
              filterOption={(input, option) => option.children.toLowerCase().includes(input.toLowerCase())}
            >
              {availableAccounts.map((a) => (
                <Select.Option key={a.id} value={a.id}>
                  {a.torihikiNo} — {a.bankName} ({a.branchCode})
                </Select.Option>
              ))}
            </Select>
            <Button onClick={handleBindBank} disabled={!selectedBankId}>追加</Button>
          </Space>
          {linkedAccounts.length > 0 && (
            <Table
              columns={bankColumns}
              dataSource={linkedAccounts}
              rowKey="id" size="small" pagination={false}
              style={{ marginBottom: 12 }}
            />
          )}
        </Form>
      </Modal>
    </div>
  );
}
