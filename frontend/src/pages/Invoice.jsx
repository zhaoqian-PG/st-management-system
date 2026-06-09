import React, { useState, useEffect, useCallback } from 'react';
import { Table, Button, Modal, Form, Input, Select, Space, message, Card, Tag, Upload, DatePicker } from 'antd';
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
  const [year, setYear] = useState(null);
  const [month, setMonth] = useState(null);
  const [customerId, setCustomerId] = useState(null);
  const [customers, setCustomers] = useState([]);
  const [details, setDetails] = useState([]);
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [attendanceSummary, setAttendanceSummary] = useState(null);
  const [customerBanks, setCustomerBanks] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await invoiceApi.list({ page: page - 1, size: PAGE_SIZE, year: year || undefined, month: month || undefined, customerId: customerId || undefined });
      setData(res.data.data.content || []); setTotal(res.data.data.totalElements || 0);
    } catch { message.error('データの取得に失敗しました'); }
    finally { setLoading(false); }
  }, [page, year, month, customerId]);

  useEffect(() => { fetchData(); }, [fetchData]);
  useEffect(() => { axios.get('/api/customer?size=200').then(r => setCustomers(r.data.data.content || [])).catch(() => {}); }, []);
  useEffect(() => { axios.get('/api/employee?size=200').then(r => setEmployees(r.data.data.content || [])).catch(() => {}); }, []);

  const handleSelect = async (record) => {
    setSelectedInvoice(record);
    try { const r = await invoiceApi.getById(record.id); setDocuments(r.data.data.documents || []); } catch { setDocuments([]); }
    // Fetch related attendance summary for this customer/month
    try { const r = await axios.get('/api/attendance/summary', { params: { year: record.year, month: record.month, employeeId: record.customerId } }); setAttendanceSummary(r.data.data); } catch { setAttendanceSummary(null); }
    // Fetch customer bank accounts
    try { const r = await axios.get(`/api/bank-accounts/customer/${record.customerId}`); setCustomerBanks(r.data.data || []); } catch { setCustomerBanks([]); }
  };

  const handleCreate = () => { setEditingRecord(null); setDetails([]); setCustomerBanks([]); form.resetFields(); form.setFieldsValue({ year, month, taxRate: 10 }); setModalVisible(true); };
  const handleEdit = async (r) => { setEditingRecord(r); form.setFieldsValue({ ...r, invoiceDate: r.invoiceDate ? dayjs(r.invoiceDate) : null, dueDate: r.dueDate ? dayjs(r.dueDate) : null }); try { const res = await invoiceApi.getById(r.id); setDetails(res.data.data.details || []); } catch { setDetails([]); } setModalVisible(true); };

  const handleSubmit = async () => {
    try { const v = await form.validateFields(); setFormLoading(true);
      const payload = { ...v, invoiceDate: v.invoiceDate ? v.invoiceDate.format('YYYY-MM-DD') : null, dueDate: v.dueDate ? v.dueDate.format('YYYY-MM-DD') : null, details };
      // Auto-calc amount from details
      const sub = details.reduce((s, d) => s + (d.quantity||0)*(d.unitPrice||0), 0);
      payload.amount = sub;
      const rate = v.taxRate != null ? Number(v.taxRate) : 10;
      payload.taxRate = rate; payload.taxAmount = Math.round(sub * rate) / 100; payload.totalWithTax = sub + Math.round(sub * rate) / 100;
      if (editingRecord) { await invoiceApi.update(editingRecord.id, payload); message.success('更新しました'); }
      else { await invoiceApi.create(payload); message.success('登録しました'); }
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

  const addDetail = () => setDetails([...details, { employeeName: '', description: '', quantity: 1, unitPrice: 0, amount: 0, isOvertime: false }]);

  const importFromAttendance = async () => {
    if (!form.getFieldValue('year') || !form.getFieldValue('month')) { message.warning('年月を選択してください'); return; }
    try {
      const r = await axios.get('/api/attendance/monthly-summary', { params: { year: form.getFieldValue('year'), month: form.getFieldValue('month') } });
      const rows = (r.data.data || []).map(e => ({
        employeeName: e.employeeName, employeeId: e.employeeId,
        description: `${e.employeeName} 勤務分`, quantity: e.workHours, unitPrice: 0,
        amount: 0, isOvertime: false,
      }));
      const overtimeRows = (r.data.data || []).filter(e => e.overtimeHours > 0).map(e => ({
        employeeName: e.employeeName, employeeId: e.employeeId,
        description: `${e.employeeName} 残業分`, quantity: e.overtimeHours, unitPrice: 0,
        amount: 0, isOvertime: true,
      }));
      setDetails([...details, ...rows, ...overtimeRows]); message.success('勤務データを取込しました');
    } catch { message.error('取込に失敗しました'); }
  };
  const updateDetail = (i, field, value) => { const d = [...details]; d[i][field] = value; if (field === 'quantity' || field === 'unitPrice') d[i].amount = (d[i].quantity||0)*(d[i].unitPrice||0); setDetails(d); };
  const removeDetail = (i) => setDetails(details.filter((_, j) => j !== i));

  const handleDeleteDoc = (docId) => Modal.confirm({ title: '削除', content: 'このファイルを削除しますか？', okText: '削除', cancelText: 'キャンセル', centered: true,
    onOk: async () => { try { await invoiceApi.deleteDocument(docId); setDocuments(documents.filter(d => d.id !== docId)); } catch {} } });

  const columns = [
    { title: '請求書番号', dataIndex: 'invoiceNumber', width: 150 }, { title: '件名', dataIndex: 'subject', width: 180, ellipsis: true },
    { title: '顧客名', dataIndex: 'customerName', width: 160, ellipsis: true },
    { title: '請求日', dataIndex: 'invoiceDate', width: 100 },
    { title: '支払期限', dataIndex: 'dueDate', width: 100 },
    { title: '税抜金額', dataIndex: 'amount', width: 110, render: v => v?.toLocaleString() },
    { title: '消費税', dataIndex: 'taxAmount', width: 90, render: v => v?.toLocaleString() },
    { title: '税込合計', dataIndex: 'totalWithTax', width: 120, render: v => <strong>{v?.toLocaleString()}</strong> },
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
        {selectedInvoice.subject && <p style={{ marginBottom: 8 }}><strong>件名:</strong> {selectedInvoice.subject}</p>}
        {selectedInvoice.customerName && <p style={{ marginBottom: 8 }}><strong>請求先:</strong> {selectedInvoice.customerName}</p>}
        <div style={{ display: 'flex', gap: 16, marginBottom: 12 }}>
          <Card size="small" style={{ flex: 1 }} title="📅 関連勤務実績">
            {attendanceSummary ? <div>
              <p>総勤務時間: <strong>{attendanceSummary.workHours}h</strong></p>
              <p>総残業時間: <strong>{attendanceSummary.overtimeHours}h</strong></p>
              <p>勤務日数: <strong>{attendanceSummary.workDays}日</strong></p>
              <p>総労働時間: <strong>{attendanceSummary.totalHours}h</strong></p>
            </div> : <span style={{ color: 'rgba(0,0,0,0.25)' }}>勤務データなし</span>}
          </Card>
          <Card size="small" style={{ flex: 2 }} title="💰 請求金額">
            <p>税抜金額: <strong>¥{selectedInvoice.amount?.toLocaleString()}</strong></p>
            <p>消費税({selectedInvoice.taxRate || 10}%): <strong>¥{selectedInvoice.taxAmount?.toLocaleString()}</strong></p>
            <p style={{ fontSize: 16 }}>税込合計: <strong style={{ color: '#cf1322' }}>¥{selectedInvoice.totalWithTax?.toLocaleString()}</strong></p>
          </Card>
        </div>
        {customerBanks.filter(b => b.isDefault).length > 0 && (
          <Card size="small" title="🏦 振込先口座（データベース参照）" style={{ marginBottom: 12 }}>
            {customerBanks.filter(b => b.isDefault).map((b, i) => (
              <p key={b.id} style={{ marginBottom: 4 }}>
                {i + 1}. <strong>{b.bankName}</strong> {b.branchCode}支店
                　{b.accountType} {b.accountNumber}　{b.accountHolder}
                {b.isDefault && <Tag color="gold" style={{ marginLeft: 8 }}>主カード</Tag>}
              </p>
            ))}
          </Card>
        )}
        {customerBanks.filter(b => b.isDefault).length === 0 && selectedInvoice.customerId && <p style={{ color: 'rgba(0,0,0,0.25)', marginBottom: 12 }}>🏦 振込先口座情報は登録されていません</p>}
        <Upload showUploadList={false} beforeUpload={(f) => { handleUpload(f, selectedInvoice.id); return false; }}>
          <Button icon={<UploadOutlined />} style={{ marginBottom: 12 }}>＋ 注文書アップロード</Button>
        </Upload>
        <Table columns={docCols} dataSource={documents} rowKey="id" size="small" pagination={false} locale={{ emptyText: '注文書はありません' }} />
      </Card>
    )}

    <Modal title={editingRecord ? '請求書編集' : '請求書新規登録'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)}
      confirmLoading={formLoading} okText="保存" cancelText="キャンセル" destroyOnClose centered width={900}>
      <Form form={form} layout="vertical" style={{ marginTop: 8 }}>
        <Form.Item name="invoiceNumber" label="請求書番号" rules={[{ required: true }]}>
          <Input placeholder="例: INV-2026-0601" maxLength={50} /></Form.Item>
        <Form.Item name="subject" label="件名">
          <Input placeholder="例: システム開発費用" maxLength={500} /></Form.Item>
        <Form.Item name="customerId" label="請求先" rules={[{ required: true }]}>
          <Select placeholder="選択" showSearch filterOption={(i,o)=>o.children.toLowerCase().includes(i.toLowerCase())}
            onChange={async (val) => { try { const r = await axios.get(`/api/bank-accounts/customer/${val}`); setCustomerBanks(r.data.data || []); } catch { setCustomerBanks([]); } }}>
            {customers.map(c => <Option key={c.id} value={c.id}>{c.companyName} ({c.customerCode})</Option>)}</Select></Form.Item>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
          <Form.Item name="year" label="年度" rules={[{ required: true }]}>
            <Select>{[2024,2025,2026,2027].map(y => <Option key={y} value={y}>{y}</Option>)}</Select></Form.Item>
          <Form.Item name="month" label="月度" rules={[{ required: true }]}>
            <Select>{Array.from({length:12},(_,i)=>i+1).map(m => <Option key={m} value={m}>{m}</Option>)}</Select></Form.Item></div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
          <Form.Item name="invoiceDate" label="請求日"><DatePicker style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="dueDate" label="支払期限"><DatePicker style={{ width: '100%' }} /></Form.Item></div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0 16px' }}>
          <Form.Item name="amount" label="税抜金額(円)" rules={[{ required: true }]}>
            <Input type="number" min={0} /></Form.Item>
          <Form.Item name="taxRate" label="消費税率(%)"><Input type="number" min={0} max={100} placeholder="10" /></Form.Item>
          <Form.Item label="税込合計"><Input value={(() => { const a = Number(form.getFieldValue('amount')) || 0; const r = Number(form.getFieldValue('taxRate')) || 10; const t = Math.round(a * r) / 100; return (a + t).toLocaleString(); })()} disabled /></Form.Item></div>
        <div style={{ borderTop: '2px solid #1890ff', paddingTop: 12, marginTop: 8 }}>
          <h4 style={{ color: '#1890ff', marginBottom: 8 }}>● 請求明細</h4>
          <Space style={{ marginBottom: 8 }}>
            <Button type="dashed" onClick={addDetail}>＋ 明細行追加</Button>
            <Button onClick={importFromAttendance}>📅 勤務データ取込</Button>
          </Space>
          {details.length > 0 && <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 8 }}><thead><tr style={{ background: '#fafafa' }}>
            <th style={{ padding: 4 }}>担当者</th><th style={{ padding: 4 }}>項目</th><th style={{ padding: 4, width: 60 }}>数量</th><th style={{ padding: 4, width: 90 }}>単価</th><th style={{ padding: 4, width: 40 }}>残業</th><th style={{ padding: 4, width: 90 }}>金額</th><th style={{ padding: 4, width: 50 }}></th></tr></thead><tbody>
            {details.map((d, i) => <tr key={i}>
              <td style={{ padding: 2 }}><Input value={d.employeeName} onChange={e => updateDetail(i, 'employeeName', e.target.value)} size="small" placeholder="例: 山田 太郎（BP）" /></td>
              <td style={{ padding: 2 }}><Input value={d.description} onChange={e => updateDetail(i, 'description', e.target.value)} size="small" placeholder="例: 基本設計" /></td>
              <td style={{ padding: 2 }}><Input value={d.quantity} onChange={e => updateDetail(i, 'quantity', Number(e.target.value))} size="small" type="number" /></td>
              <td style={{ padding: 2 }}><Input value={d.unitPrice} onChange={e => updateDetail(i, 'unitPrice', Number(e.target.value))} size="small" type="number" placeholder="時給" /></td>
              <td style={{ padding: 2, textAlign: 'center' }}><input type="checkbox" checked={d.isOvertime} onChange={e => updateDetail(i, 'isOvertime', e.target.checked)} /></td>
              <td style={{ padding: 2, textAlign: 'right' }}>{(d.amount||0).toLocaleString()}</td>
              <td style={{ padding: 2 }}><Button type="link" danger size="small" onClick={() => removeDetail(i)}>✕</Button></td></tr>)}</tbody></table>}
          <div style={{ textAlign: 'right', marginBottom: 8 }}>小計: <strong>{details.reduce((s,d) => s + (d.amount||0), 0).toLocaleString()}</strong> 円</div>
        </div>
        {editingRecord && <Form.Item name="status" label="状態"><Select><Option value="下書き">下書き</Option><Option value="送付済">送付済</Option><Option value="入金済">入金済</Option></Select></Form.Item>}
        <div style={{ borderTop: '2px solid #f0f0f0', paddingTop: 8, marginTop: 8 }}>
          {customerBanks.filter(b => b.isDefault).length > 0 ? (
            <Card size="small" title="🏦 振込先口座（参照のみ）" style={{ marginBottom: 8 }}>
              {customerBanks.filter(b => b.isDefault).map((b, i) => (
                <p key={b.id} style={{ marginBottom: 2, fontSize: 12 }}>
                  {i + 1}. <strong>{b.bankName}</strong> {b.branchCode}支店　{b.accountType} {b.accountNumber}　{b.accountHolder}
                  {b.isDefault && <Tag color="gold" style={{ marginLeft: 4 }}>主</Tag>}
                </p>))}
            </Card>
          ) : <p style={{ fontSize: 12, color: 'rgba(0,0,0,0.25)', marginBottom: 8 }}>🏦 振込先口座情報は登録されていません（顧客選択後に表示）</p>}
        </div>
        <Form.Item name="remark" label="備考"><Input.TextArea rows={2} /></Form.Item>
      </Form>
    </Modal>
  </div>);
}
