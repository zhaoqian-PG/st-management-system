import React, { useState, useEffect, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, Select, Space, message, Card, Tag,
  DatePicker, Upload,
} from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, UserOutlined,
  UploadOutlined, MinusCircleOutlined,
} from '@ant-design/icons';
import { employeeApi } from '../services/employeeApi';
import { bankAccountApi } from '../services/bankAccountApi';
import dayjs from 'dayjs';

const { Option } = Select;
const PAGE_SIZE = 10;

const SectionTitle = ({ children }) => (
  <h4 style={{ margin: '16px 0 12px', paddingBottom: 6,
    borderBottom: '2px solid #1890ff', color: '#1890ff', fontWeight: 600, fontSize: 14 }}>
    {children}
  </h4>
);

export default function Employee() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);
  const [formLoading, setFormLoading] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [linkedBanks, setLinkedBanks] = useState([]);
  const [availableBanks, setAvailableBanks] = useState([]);
  const [selectedBankId, setSelectedBankId] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [importing, setImporting] = useState(false);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page: page - 1, size: PAGE_SIZE };
      if (keyword) params.keyword = keyword;
      const res = await employeeApi.list(params);
      const r = res.data.data;
      setData(r.content || []);
      setTotal(r.totalElements || 0);
    } catch { message.error('データの取得に失敗しました'); }
    finally { setLoading(false); }
  }, [page, keyword]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const fetchLinkedBanks = async (empId) => {
    try {
      const res = await bankAccountApi.getByEmployeeId(empId);
      setLinkedBanks(res.data.data || []);
    } catch { setLinkedBanks([]); }
  };

  const fetchAvailableBanks = async () => {
    try {
      const res = await bankAccountApi.list({ size: 200 });
      setAvailableBanks((res.data.data.content || []).filter(
        (a) => a.category === 'EMPLOYEE' && a.employeeId === null
      ));
    } catch { setAvailableBanks([]); }
  };

  const handleCreate = () => {
    setEditingRecord(null);
    setLinkedBanks([]);
    setSelectedBankId(null);
    form.resetFields();
    setModalVisible(true);
    fetchAvailableBanks();
  };

  const handleEdit = async (record) => {
    setEditingRecord(record);
    setSelectedBankId(null);
    form.setFieldsValue({
      ...record,
      joinDate: record.joinDate ? dayjs(record.joinDate) : null,
      birthDate: record.birthDate ? dayjs(record.birthDate) : null,
    });
    setModalVisible(true);
    await fetchLinkedBanks(record.id);
    await fetchAvailableBanks();
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setFormLoading(true);
      const payload = {
        ...values,
        joinDate: values.joinDate ? values.joinDate.format('YYYY-MM-DD') : null,
        birthDate: values.birthDate ? values.birthDate.format('YYYY-MM-DD') : null,
      };
      let empId = editingRecord?.id;
      if (editingRecord) {
        await employeeApi.update(empId, payload);
        message.success('社員情報を更新しました');
      } else {
        const res = await employeeApi.create(payload);
        empId = res.data.data.id;
        message.success('社員を登録しました');
      }
      if (linkedBanks.length > 0 && empId) {
        for (const acc of linkedBanks) {
          if (acc.employeeId !== empId) {
            try { await bankAccountApi.bindToEmployee(acc.id, empId); } catch (e) {}
          }
        }
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
      content: `社員 ${record.employeeCode} ${record.name} を削除しますか？`,
      okText: '削除', cancelText: 'キャンセル', okType: 'danger', centered: true,
      onOk: async () => {
        try { await employeeApi.delete(record.id); message.success('削除しました'); fetchData(); }
        catch (err) { message.error(err.response?.data?.error || 'エラー'); }
      },
    });
  };

  const handleBindBank = () => {
    if (!selectedBankId) return;
    const acc = availableBanks.find((a) => a.id === selectedBankId);
    if (!acc) return;
    setLinkedBanks([...linkedBanks, acc]);
    setAvailableBanks(availableBanks.filter((a) => a.id !== selectedBankId));
    setSelectedBankId(null);
  };

  const handleUnbindBank = (bank) => {
    if (editingRecord) {
      Modal.confirm({
        title: '紐付け解除', content: 'この口座の紐付けを解除しますか？',
        okText: '解除', cancelText: 'キャンセル', centered: true,
        onOk: async () => {
          try { await bankAccountApi.unbindFromEmployee(bank.id); await fetchLinkedBanks(editingRecord.id); await fetchAvailableBanks(); }
          catch (err) { message.error('解除に失敗しました'); }
        },
      });
    } else {
      setLinkedBanks(linkedBanks.filter((a) => a.id !== bank.id));
      setAvailableBanks([...availableBanks, bank]);
    }
  };

  const handleUpload = async (file, empId) => {
    setUploading(true);
    try {
      const fd = new FormData();
      fd.append('file', file);
      await employeeApi.upload(empId, fd);
      message.success('ファイルをアップロードしました');
      return false;
    } catch { message.error('アップロードに失敗しました'); }
    finally { setUploading(false); }
  };

  const handleBatchImport = async (file) => {
    setImporting(true);
    try {
      const fd = new FormData();
      fd.append('file', file);
      const res = await employeeApi.batchImport(fd);
      message.success(res.data.message);
      fetchData();
    } catch { message.error('インポートに失敗しました'); }
    finally { setImporting(false); }
  };

  const columns = [
    { title: '社員番号', dataIndex: 'employeeCode', width: 110 },
    { title: '氏名', dataIndex: 'name', width: 120 },
    { title: '部署', dataIndex: 'department', width: 90 },
    { title: '役職', dataIndex: 'position', width: 90 },
    { title: 'メール', dataIndex: 'email', width: 200, ellipsis: true },
    { title: '日本電話', dataIndex: 'phone', width: 130 },
    { title: '中国電話', dataIndex: 'chinaPhone', width: 130 },
    {
      title: '銀行口座', dataIndex: 'torihikiNo', width: 200, ellipsis: true,
      render: (t) => t || <span style={{ color: 'rgba(0,0,0,0.25)' }}>未設定</span>,
    },
    {
      title: '操作', width: 200, fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>編集</Button>
          {record.attachmentPath && (
            <Button type="link" onClick={async () => {
              try { const r = await employeeApi.download(record.id);
                const url = URL.createObjectURL(r.data);
                const a = document.createElement('a'); a.href = url; a.download = record.attachmentPath.split('\\').pop(); a.click();
                URL.revokeObjectURL(url); } catch (e) {}
            }}>📥</Button>
          )}
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDeleteConfirm(record)}>削除</Button>
        </Space>
      ),
    },
  ];

  const bankColumns = [
    { title: '取引番号', dataIndex: 'torihikiNo', width: 110, render: (t) => <strong>{t}</strong> },
    { title: '銀行名称', dataIndex: 'bankName', width: 130 },
    { title: '支店番号', dataIndex: 'branchCode', width: 80 },
    { title: '口座種類', dataIndex: 'accountType', width: 80, render: (t) => <Tag color={t === '普通' ? 'blue' : 'orange'}>{t}</Tag> },
    { title: '口座番号', dataIndex: 'accountNumber', width: 120, render: (t) => '****' + (t ? t.slice(-4) : '****') },
    { title: '口座名義', dataIndex: 'accountHolder', width: 150 },
    { title: '操作', width: 70,
      render: (_, record) => (
        <Button type="link" danger icon={<MinusCircleOutlined />} onClick={() => handleUnbindBank(record)}>解除</Button>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 20 }}><UserOutlined /> 社員管理</h2>
      <Card>
        <div style={{ display: 'flex', gap: 12, marginBottom: 16, alignItems: 'center', flexWrap: 'wrap' }}>
          <Input placeholder="社員番号・氏名で検索" value={keyword}
            onChange={(e) => { setKeyword(e.target.value); setPage(1); }}
            style={{ width: 260 }} allowClear />
          <span style={{ flex: 1 }} />
          <Upload accept=".csv" showUploadList={false} beforeUpload={(f) => { handleBatchImport(f); return false; }}>
            <Button icon={<UploadOutlined />} loading={importing}>CSV一括登録</Button>
          </Upload>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>新規登録</Button>
        </div>
        <Table columns={columns} dataSource={data} rowKey="id" loading={loading}
          pagination={{ current: page, pageSize: PAGE_SIZE, total, showSizeChanger: false,
            showTotal: (t) => `全 ${t} 件`, onChange: (p) => setPage(p) }} />
      </Card>

      <Modal
        title={editingRecord ? '社員情報編集' : '社員新規登録'}
        open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)}
        confirmLoading={formLoading} okText="保存" cancelText="キャンセル"
        destroyOnClose centered width={800}>
        <Form form={form} layout="vertical" style={{ marginTop: 8 }}>
          <SectionTitle>● 基本情報</SectionTitle>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            {editingRecord ? (
              <>
                <Form.Item name="employeeCode" hidden><Input /></Form.Item>
                <Form.Item label="社員番号">
                  <Input value={editingRecord.employeeCode} disabled style={{ color: 'rgba(0,0,0,0.65)' }} />
                </Form.Item>
              </>
            ) : (
              <Form.Item label="社員番号">
                <Input value="自動採番（EMP0001〜）" disabled style={{ color: 'rgba(0,0,0,0.45)' }} />
              </Form.Item>
            )}
            <Form.Item name="name" label="氏名"
              rules={[{ required: true, message: '氏名は必須です' }, { max: 100, message: '100文字以内' }]}>
              <Input placeholder="例: 山田 太郎" maxLength={100} />
            </Form.Item>
            <Form.Item name="email" label="メール（日本）"
              rules={[{ max: 255 }, { type: 'email', message: '正しい形式で入力してください' }]}>
              <Input placeholder="例: yamada@example.com" maxLength={255} />
            </Form.Item>
            <Form.Item name="phone" label="電話番号（日本）"
              rules={[{ max: 20 }, { pattern: /^[0-9\-+() ]*$/, message: '半角数字・ハイフンで入力' }]}>
              <Input placeholder="例: 090-1111-2222" maxLength={20} />
            </Form.Item>
            <Form.Item name="joinDate" label="入社日">
              <DatePicker style={{ width: '100%' }} placeholder="選択してください" />
            </Form.Item>
            <Form.Item name="birthDate" label="生年月日">
              <DatePicker style={{ width: '100%' }} placeholder="選択してください" />
            </Form.Item>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            <Form.Item name="department" label="部署" rules={[{ max: 100 }]}>
              <Select placeholder="選択" allowClear>
                <Option value="営業部">営業部</Option><Option value="技術部">技術部</Option>
                <Option value="経理部">経理部</Option><Option value="人事部">人事部</Option>
              </Select>
            </Form.Item>
            <Form.Item name="position" label="役職" rules={[{ max: 100 }]}>
              <Select placeholder="選択" allowClear>
                <Option value="部長">部長</Option><Option value="課長">課長</Option>
                <Option value="係長">係長</Option><Option value="一般社員">一般社員</Option>
              </Select>
            </Form.Item>
          </div>
          <Form.Item name="japanAddress" label="日本住所" rules={[{ max: 500 }]}>
            <Input placeholder="例: 東京都新宿区..." maxLength={500} />
          </Form.Item>

          <SectionTitle>● 中国連絡情報</SectionTitle>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            <Form.Item name="chinaPhone" label="中国電話番号"
              rules={[{ max: 20 }, { pattern: /^[0-9\-+() ]*$/, message: '半角数字・ハイフンで入力' }]}>
              <Input placeholder="例: 138-0001-0001" maxLength={20} />
            </Form.Item>
            <Form.Item name="chinaEmergencyContact" label="緊急連絡先" rules={[{ max: 100 }]}>
              <Input placeholder="例: 山田 花子（妻）" maxLength={100} />
            </Form.Item>
          </div>
          <Form.Item name="chinaAddress" label="中国住所" rules={[{ max: 500 }]}>
            <Input placeholder="例: 上海市浦东新区..." maxLength={500} />
          </Form.Item>

          <SectionTitle>● 銀行口座（給与振込用）</SectionTitle>
          <Table columns={bankColumns} dataSource={linkedBanks} rowKey="id" size="small"
            pagination={false} locale={{ emptyText: '紐付く銀行口座はありません' }}
            style={{ marginBottom: 12 }} />
          <Space>
            <Select placeholder="＋ 口座を追加（未紐付から選択）" value={selectedBankId}
              onChange={setSelectedBankId} style={{ minWidth: 300 }} allowClear showSearch
              filterOption={(input, option) => option.children.toLowerCase().includes(input.toLowerCase())}>
              {availableBanks.map((a) => (
                <Select.Option key={a.id} value={a.id}>{a.torihikiNo} — {a.bankName} ({a.branchCode})</Select.Option>
              ))}
            </Select>
            <Button onClick={handleBindBank} disabled={!selectedBankId}>追加</Button>
          </Space>

          {editingRecord && (
            <>
              <SectionTitle>● 添付ファイル</SectionTitle>
              <Upload beforeUpload={(f) => handleUpload(f, editingRecord.id)} showUploadList={false}>
                <Button icon={<UploadOutlined />} loading={uploading}>ファイルをアップロード</Button>
              </Upload>
              {editingRecord.attachmentPath && (
                <div style={{ marginTop: 8, fontSize: 12, color: 'rgba(0,0,0,0.45)' }}>
                  📎 {editingRecord.attachmentPath.split('\\').pop()}
                </div>
              )}
            </>
          )}
        </Form>
      </Modal>
    </div>
  );
}
