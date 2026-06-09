import React, { useState, useEffect, useCallback } from 'react';
import { Table, Button, Modal, Form, Input, Select, Space, message, Card, Tag, Upload } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, FileTextOutlined, DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import { invoiceApi } from '../services/invoiceApi';
import axios from 'axios';
import dayjs from 'dayjs';

const { Option } = Select;
const PAGE_SIZE = 10;

export default function Invoice() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);
  const [formLoading, setFormLoading] = useState(false);
  const [year, setYear] = useState(dayjs().year());
  const [month, setMonth] = useState(dayjs().month() + 1);
  const [customerId, setCustomerId] = useState(null);
  const [customers, setCustomers] = useState([]);
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await invoiceApi.list({ page: page - 1, size: PAGE_SIZE, year, month, customerId: customerId || undefined });
      setData(res.data.data.content || []); setTotal(res.data.data.totalElements || 0);
    } catch { message.error('データの取得に失敗しました'); }
    finally { setLoading(false); }
  }, [page, year, month, customerId]);

  useEffect(() => { fetchData(); }, [fetchData]);
  useEffect(() => { axios.get('/api/customer?size=200').then(r => setCustomers(r.data.data.content || [])).catch(() => {}); }, []);

  const handleSelect = async (record) => {
    setSelectedInvoice(record);
    try { const r = await invoiceApi.getById(record.id); setDocuments(r.data.data.documents || []); } catch { setDocuments([]); }
  };

  const handleCreate = () => { setEditingRecord(null); form.resetFields(); form.setFieldsValue({ year, month }); setModalVisible(true); };
  const handleEdit = (r) => { setEditingRecord(r); form.setFieldsValue(r); setModalVisible(true); };

  const handleSubmit = async () => {
    try { const v = await form.validateFields(); setFormLoading(true);
      if (editingRecord) { await invoiceApi.update(editingRecord.id, v); message.success('更新しました'); }
      else { await invoiceApi.create(v); message.success('登録しました'); }
      setModalVisible(false); fetchData();
    } catch (e) { if (e.response?.data?.error) message.error(e.response.data.error); }
    finally { setFormLoading(false); }
  };

  const handleDelete = (r) => Modal.confirm({ title: '削除', content: `請求書 ${r.invoiceNumber} を削除しますか？`, okText: '削除', cancelText: 'キャンセル', okType: 'danger', centered: true,
    onOk: async () => { try { await invoiceApi.delete(r.id); setSelectedInvoice(null); setDocuments([]); fetchData(); } catch {} } });

  const handleUpload = async (file, invId) => {
    const fd = new FormData(); fd.append('file', file);
    try { await invoiceApi.uploadDocument(invId, fd); const r = await invoiceApi.getById(invId); setDocuments(r.data.data.documents || []); message.success('アップロードしました'); }
    catch { message.error('失敗'); }
    return false;
  };

  const handleDeleteDoc = (docId) => Modal.confirm({ title: '削除', content: 'このファイルを削除しますか？', okText: '削除', cancelText: 'キャンセル', centered: true,
    onOk: async () => { try { await invoiceApi.deleteDocument(docId); setDocuments(documents.filter(d => d.id !== docId)); } catch {} } });

  const columns = [
    { title: '請求書番号', dataIndex: 'invoiceNumber', width: 150 }, { title: '顧客名', dataIndex: 'customerName', width: 180, ellipsis: true },
    { title: '年月', key: 'ym', width: 100, render: (_, r) => `${r.year}/${r.month}` },
    { title: '金額(円)', dataIndex: 'amount', width: 120, render: v => v?.toLocaleString() },
    { title: '状態', dataIndex: 'status', width: 90, render: t => {
      const m = { '下書き': ['gold', '下書き'], '送付済': ['blue', '送付済'], '入金済': ['green', '入金済'] };
      return <Tag color={m[t]?.[0]}>{m[t]?.[1] || t}</Tag>;
    }},
    { title: '操作', width: 140, fixed: 'right', render: (_, r) => <Space><Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(r)}>編集</Button><Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(r)}>削除</Button></Space> },
  ];

  const docCols = [
    { title: 'ファイル名', dataIndex: 'fileName', ellipsis: true },
    { title: 'サイズ', dataIndex: 'fileSize', width: 100, render: s => s ? (s/1024).toFixed(1)+' KB' : '-' },
    { title: '操作', width: 140, render: (_, d) => <Space>
      <Button type="link" icon={<DownloadOutlined />} onClick={() => window.open(`/api/invoice/documents/${d.id}/download`)}>DL</Button>
      <Button type="link" danger onClick={() => handleDeleteDoc(d.id)}>削除</Button></Space> },
  ];

  return (<div>
    <h2 style={{ marginBottom: 20 }}><FileTextOutlined /> 請求書・注文書管理</h2>
    <Card>
      <div style={{ display: 'flex', gap: 12, marginBottom: 16, alignItems: 'center', flexWrap: 'wrap' }}>
        <Select value={year} onChange={setYear} style={{ width: 100 }}>{[2024,2025,2026,2027].map(y => <Option key={y} value={y}>{y}</Option>)}</Select><span>年</span>
        <Select value={month} onChange={setMonth} style={{ width: 70 }}>{Array.from({length:12},(_,i)=>i+1).map(m => <Option key={m} value={m}>{m}</Option>)}</Select><span>月</span>
        <Select placeholder="全顧客" allowClear value={customerId} onChange={setCustomerId} style={{ width: 220 }} showSearch filterOption={(i,o)=>o.children.toLowerCase().includes(i.toLowerCase())}>
          {customers.map(c => <Option key={c.id} value={c.id}>{c.companyName} ({c.customerCode})</Option>)}</Select>
        <span style={{ flex: 1 }} /><Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>新規登録</Button>
      </div>
      <Table columns={columns} dataSource={data} rowKey="id" loading={loading}
        onRow={r => ({ onClick: () => handleSelect(r), style: { background: selectedInvoice?.id === r.id ? '#e6f7ff' : undefined, cursor: 'pointer' } })}
        pagination={{ current: page, pageSize: PAGE_SIZE, total, showSizeChanger: false, showTotal: t => `全 ${t} 件`, onChange: p => setPage(p) }} />
    </Card>

    {selectedInvoice && (
      <Card title={`📎 請求書詳細: ${selectedInvoice.invoiceNumber}`} style={{ marginTop: 16 }}>
        <Upload showUploadList={false} beforeUpload={(f) => { handleUpload(f, selectedInvoice.id); return false; }}>
          <Button icon={<UploadOutlined />} style={{ marginBottom: 12 }}>＋ 注文書アップロード</Button>
        </Upload>
        <Table columns={docCols} dataSource={documents} rowKey="id" size="small" pagination={false} locale={{ emptyText: '注文書はありません' }} />
      </Card>
    )}

    <Modal title={editingRecord ? '請求書編集' : '請求書新規登録'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)}
      confirmLoading={formLoading} okText="保存" cancelText="キャンセル" destroyOnClose centered width={550}>
      <Form form={form} layout="vertical" style={{ marginTop: 8 }}>
        <Form.Item name="invoiceNumber" label="請求書番号" rules={[{ required: true }]}>
          <Input placeholder="例: INV-2026-0601" maxLength={50} /></Form.Item>
        <Form.Item name="customerId" label="顧客" rules={[{ required: true }]}>
          <Select placeholder="選択" showSearch filterOption={(i,o)=>o.children.toLowerCase().includes(i.toLowerCase())}>
            {customers.map(c => <Option key={c.id} value={c.id}>{c.companyName} ({c.customerCode})</Option>)}</Select></Form.Item>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
          <Form.Item name="year" label="年度" rules={[{ required: true }]}>
            <Select>{[2024,2025,2026,2027].map(y => <Option key={y} value={y}>{y}</Option>)}</Select></Form.Item>
          <Form.Item name="month" label="月度" rules={[{ required: true }]}>
            <Select>{Array.from({length:12},(_,i)=>i+1).map(m => <Option key={m} value={m}>{m}</Option>)}</Select></Form.Item></div>
        <Form.Item name="amount" label="金額(円)" rules={[{ required: true }]}>
          <Input type="number" min={0} /></Form.Item>
        {editingRecord && <Form.Item name="status" label="状態"><Select><Option value="下書き">下書き</Option><Option value="送付済">送付済</Option><Option value="入金済">入金済</Option></Select></Form.Item>}
        <Form.Item name="remark" label="備考"><Input.TextArea rows={2} /></Form.Item>
      </Form>
    </Modal>
  </div>);
}
