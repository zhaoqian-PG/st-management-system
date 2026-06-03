import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Table, Button, Modal, Form, Input, Select, Space, message, Card, Tag, DatePicker, Upload, Tooltip } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, UserOutlined, UploadOutlined, DownloadOutlined, MinusCircleOutlined, StarOutlined, StarFilled } from '@ant-design/icons';
import { employeeApi } from '../services/employeeApi';
import { bankAccountApi } from '../services/bankAccountApi';
import dayjs from 'dayjs';

const { Option } = Select;
const PAGE_SIZE = 10;

const SectionTitle = ({ children }) => (
  <h4 style={{ margin: '16px 0 12px', paddingBottom: 6, borderBottom: '2px solid #1890ff', color: '#1890ff', fontWeight: 600, fontSize: 14 }}>{children}</h4>
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
  const [attachments, setAttachments] = useState([]);
  const [pendingFiles, setPendingFiles] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [importing, setImporting] = useState(false);
  const [form] = Form.useForm();
  const fileRef = useRef(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await employeeApi.list({ page: page - 1, size: PAGE_SIZE, keyword: keyword || undefined });
      setData(res.data.data.content || []); setTotal(res.data.data.totalElements || 0);
    } catch { message.error('データの取得に失敗しました'); }
    finally { setLoading(false); }
  }, [page, keyword]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const refreshDetail = async (empId) => {
    try { const r = await employeeApi.getById(empId); setAttachments(r.data.data.attachments || []); setLinkedBanks(r.data.data.bankAccounts || []); }
    catch { setAttachments([]); setLinkedBanks([]); }
  };

  const fetchAvailableBanks = async () => {
    try {
      const r = await bankAccountApi.list({ size: 200 });
      setAvailableBanks((r.data.data.content || []).filter(a => a.category === 'EMPLOYEE' && a.employeeId === null));
    } catch { setAvailableBanks([]); }
  };

  const handleCreate = () => {
    setEditingRecord(null); setLinkedBanks([]); setAttachments([]); setPendingFiles([]); setSelectedBankId(null);
    form.resetFields(); setModalVisible(true); fetchAvailableBanks();
  };

  const handleEdit = async (record) => {
    setEditingRecord(record); setPendingFiles([]); setSelectedBankId(null);
    form.setFieldsValue({ ...record, joinDate: record.joinDate ? dayjs(record.joinDate) : null, birthDate: record.birthDate ? dayjs(record.birthDate) : null });
    setModalVisible(true);
    await refreshDetail(record.id);
    await fetchAvailableBanks();
  };

  const doUpload = async (empId, files) => {
    if (!files || files.length === 0) return;
    const fd = new FormData();
    files.forEach(f => fd.append('files', f));
    await employeeApi.upload(empId, fd);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setFormLoading(true);
      const payload = { ...values, joinDate: values.joinDate ? values.joinDate.format('YYYY-MM-DD') : null, birthDate: values.birthDate ? values.birthDate.format('YYYY-MM-DD') : null };
      let empId = editingRecord?.id;

      if (editingRecord) {
        await employeeApi.update(empId, payload);
        if (pendingFiles.length > 0) await doUpload(empId, pendingFiles);
        message.success('更新しました');
      } else {
        const r = await employeeApi.create(payload);
        empId = r.data.data.id;
        if (pendingFiles.length > 0) await doUpload(empId, pendingFiles);
        if (linkedBanks.length > 0) {
          for (const acc of linkedBanks) { try { await bankAccountApi.bindToEmployee(acc.id, empId); } catch (e) {} }
        }
        message.success('登録しました');
      }
      setModalVisible(false); fetchData();
    } catch (err) { if (err.response?.data?.error) message.error(err.response.data.error); }
    finally { setFormLoading(false); }
  };

  const handleDeleteConfirm = (r) => {
    Modal.confirm({ title: '削除確認', content: `${r.employeeCode} ${r.name} を削除しますか？`, okText: '削除', cancelText: 'キャンセル', okType: 'danger', centered: true,
      onOk: async () => { try { await employeeApi.delete(r.id); fetchData(); } catch {} } });
  };

  const handleBindBank = () => {
    if (!selectedBankId) return;
    const acc = availableBanks.find(a => a.id === selectedBankId);
    if (!acc) return;
    setLinkedBanks([...linkedBanks, acc]); setAvailableBanks(availableBanks.filter(a => a.id !== selectedBankId)); setSelectedBankId(null);
  };

  const handleUnbindBank = (bank) => {
    if (editingRecord) {
      Modal.confirm({ title: '紐付け解除', content: '解除しますか？', okText: '解除', cancelText: 'キャンセル', centered: true,
        onOk: async () => { try { await bankAccountApi.unbindFromEmployee(bank.id); await refreshDetail(editingRecord.id); await fetchAvailableBanks(); } catch {} } });
    } else {
      setLinkedBanks(linkedBanks.filter(a => a.id !== bank.id)); setAvailableBanks([...availableBanks, bank]);
    }
  };

  const handleSetDefault = async (bankId) => {
    if (!editingRecord) return;
    try { await bankAccountApi.setDefaultForEmployee(bankId, editingRecord.id); await refreshDetail(editingRecord.id); message.success('既定口座を設定しました'); }
    catch { message.error('失敗'); }
  };

  const handleUploadClick = () => {
    if (!editingRecord?.id) return;
    setUploading(true);
    const files = fileRef.current?.input?.files;
    if (!files || files.length === 0) { setUploading(false); return; }
    doUpload(editingRecord.id, Array.from(files)).then(() => {
      return employeeApi.getById(editingRecord.id);
    }).then(r => {
      setAttachments(r.data.data.attachments || []); message.success('アップロードしました');
    }).catch(() => message.error('失敗')).finally(() => setUploading(false));
  };

  const handleDownload = (att) => { window.open(`/api/employee/attachments/${att.id}/download`); };

  const handleDeleteAtt = (attId) => {
    Modal.confirm({ title: '削除', content: 'このファイルを削除しますか？', okText: '削除', cancelText: 'キャンセル', centered: true,
      onOk: async () => { try { await employeeApi.deleteAttachment(attId); setAttachments(attachments.filter(a => a.id !== attId)); } catch {} } });
  };

  const handleBatchImport = async (file) => {
    setImporting(true);
    try { const fd = new FormData(); fd.append('file', file); const r = await employeeApi.batchImport(fd); message.success(r.data.message); fetchData(); }
    catch { message.error('失敗'); }
    finally { setImporting(false); }
  };

  const handleExportAll = () => { window.open('/api/employee/export'); };

  const columns = [
    { title: '社員番号', dataIndex: 'employeeCode', width: 110 },
    { title: '氏名', dataIndex: 'name', width: 110 },
    { title: '部署', dataIndex: 'department', width: 80 },
    { title: '役職', dataIndex: 'position', width: 80 },
    { title: 'メール', dataIndex: 'email', width: 180, ellipsis: true },
    { title: '日本電話', dataIndex: 'phone', width: 120 },
    { title: '銀行口座', dataIndex: 'torihikiNo', width: 180, ellipsis: true, render: t => t || <span style={{ color: 'rgba(0,0,0,0.25)' }}>未設定</span> },
    { title: '操作', width: 220, fixed: 'right',
      render: (_, r) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(r)}>編集</Button>
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDeleteConfirm(r)}>削除</Button>
        </Space>
      ),
    },
  ];

  const bankCols = [
    { title: '既定', dataIndex: 'isDefault', width: 50,
      render: (v, r) => v ? <StarFilled style={{ color: '#faad14' }} /> : (
        <Tooltip title="既定口座に設定"><Button type="link" size="small" icon={<StarOutlined />} onClick={() => handleSetDefault(r.id)} /></Tooltip>
      ),
    },
    { title: '取引番号', dataIndex: 'torihikiNo', width: 100, render: t => <strong>{t}</strong> },
    { title: '銀行名称', dataIndex: 'bankName', width: 120 },
    { title: '支店番号', dataIndex: 'branchCode', width: 80 },
    { title: '口座種類', dataIndex: 'accountType', width: 80, render: t => <Tag color={t === '普通' ? 'blue' : 'orange'}>{t}</Tag> },
    { title: '口座番号', dataIndex: 'accountNumber', width: 100, render: t => '****' + (t ? t.slice(-4) : '****') },
    { title: '口座名義', dataIndex: 'accountHolder', width: 130 },
    { title: '操作', width: 70, render: (_, r) => <Button type="link" danger icon={<MinusCircleOutlined />} onClick={() => handleUnbindBank(r)}>解除</Button> },
  ];

  const attCols = [
    { title: 'ファイル名', dataIndex: 'fileName', ellipsis: true },
    { title: 'サイズ', dataIndex: 'fileSize', width: 100, render: s => s ? (s / 1024).toFixed(1) + ' KB' : '-' },
    { title: '操作', width: 120, render: (_, a) => (
        <Space>
          <Button type="link" icon={<DownloadOutlined />} onClick={() => handleDownload(a)}>DL</Button>
          <Button type="link" danger onClick={() => handleDeleteAtt(a.id)}>削除</Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 20 }}><UserOutlined /> 社員管理</h2>
      <Card>
        <div style={{ display: 'flex', gap: 12, marginBottom: 16, alignItems: 'center', flexWrap: 'wrap' }}>
          <Input placeholder="社員番号・氏名で検索" value={keyword} onChange={e => { setKeyword(e.target.value); setPage(1); }} style={{ width: 240 }} allowClear />
          <span style={{ flex: 1 }} />
          <Button icon={<DownloadOutlined />} onClick={handleExportAll}>一括DL</Button>
          <Upload accept=".csv" showUploadList={false} beforeUpload={f => { handleBatchImport(f); return false; }}>
            <Button icon={<UploadOutlined />} loading={importing}>一括登録</Button>
          </Upload>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>新規登録</Button>
        </div>
        <Table columns={columns} dataSource={data} rowKey="id" loading={loading}
          pagination={{ current: page, pageSize: PAGE_SIZE, total, showSizeChanger: false, showTotal: t => `全 ${t} 件`, onChange: p => setPage(p) }} />
      </Card>

      <Modal title={editingRecord ? '社員情報編集' : '社員新規登録'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)}
        confirmLoading={formLoading} okText="保存" cancelText="キャンセル" destroyOnClose centered width={800}>
        <Form form={form} layout="vertical" style={{ marginTop: 8 }}>
          <SectionTitle>● 基本情報</SectionTitle>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            {editingRecord ? (<><Form.Item name="employeeCode" hidden><Input /></Form.Item><Form.Item label="社員番号"><Input value={editingRecord.employeeCode} disabled /></Form.Item></>) : (<Form.Item label="社員番号"><Input value="自動採番（EMP0001〜）" disabled /></Form.Item>)}
            <Form.Item name="name" label="氏名" rules={[{ required: true, message: '必須' }, { max: 100 }]}><Input maxLength={100} /></Form.Item>
            <Form.Item name="email" label="メール（日本）" rules={[{ max: 255 }, { type: 'email' }]}><Input maxLength={255} /></Form.Item>
            <Form.Item name="phone" label="電話番号（日本）" rules={[{ max: 20 }]}><Input maxLength={20} /></Form.Item>
            <Form.Item name="joinDate" label="入社日"><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="birthDate" label="生年月日"><DatePicker style={{ width: '100%' }} /></Form.Item>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            <Form.Item name="department" label="部署"><Select allowClear><Option value="営業部">営業部</Option><Option value="技術部">技術部</Option><Option value="経理部">経理部</Option><Option value="人事部">人事部</Option></Select></Form.Item>
            <Form.Item name="position" label="役職"><Select allowClear><Option value="部長">部長</Option><Option value="課長">課長</Option><Option value="係長">係長</Option><Option value="一般社員">一般社員</Option></Select></Form.Item>
          </div>
          <Form.Item name="japanAddress" label="日本住所"><Input maxLength={500} /></Form.Item>

          <SectionTitle>● 中国連絡情報</SectionTitle>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            <Form.Item name="chinaPhone" label="中国電話番号"><Input maxLength={20} /></Form.Item>
            <Form.Item name="chinaEmergencyContact" label="緊急連絡先"><Input maxLength={100} /></Form.Item>
          </div>
          <Form.Item name="chinaAddress" label="中国住所"><Input maxLength={500} /></Form.Item>

          <SectionTitle>● 銀行口座（給与振込用・複数可）</SectionTitle>
          <Table columns={bankCols} dataSource={linkedBanks} rowKey="id" size="small" pagination={false}
            locale={{ emptyText: 'なし' }} style={{ marginBottom: 12 }} />
          <Space>
            <Select placeholder="＋ 口座を追加" value={selectedBankId} onChange={setSelectedBankId} style={{ minWidth: 280 }} allowClear showSearch
              filterOption={(inp, opt) => opt.children.toLowerCase().includes(inp.toLowerCase())}>
              {availableBanks.map(a => (<Select.Option key={a.id} value={a.id}>{a.torihikiNo} — {a.bankName} ({a.branchCode})</Select.Option>))}
            </Select>
            <Button onClick={handleBindBank} disabled={!selectedBankId}>追加</Button>
          </Space>

          <SectionTitle>● 添付ファイル</SectionTitle>
          {editingRecord ? (
            <>
              <Table columns={attCols} dataSource={attachments} rowKey="id" size="small" pagination={false}
                locale={{ emptyText: 'なし' }} style={{ marginBottom: 8 }} />
              <Space>
                <input type="file" multiple ref={fileRef} style={{ display: 'none' }} id="empFileInput" onChange={(e) => { if (e.target.files.length > 0) message.info(e.target.files.length + '件選択中'); }} />
                <Button onClick={() => document.getElementById('empFileInput').click()}>ファイル選択（複数可）</Button>
                <Button type="primary" icon={<UploadOutlined />} onClick={handleUploadClick} loading={uploading}>アップロード</Button>
              </Space>
            </>
          ) : (
            <>
              {pendingFiles.length > 0 && (
                <Table dataSource={pendingFiles.map((f, i) => ({ key: i, name: f.name, size: f.size }))} rowKey="key" size="small" pagination={false}
                  columns={[
                    { title: 'ファイル名', dataIndex: 'name' },
                    { title: 'サイズ', dataIndex: 'size', width: 100, render: s => (s / 1024).toFixed(1) + ' KB' },
                    { title: '操作', width: 60, render: (_, __, i) => <Button type="link" danger onClick={() => setPendingFiles(pendingFiles.filter((_, j) => j !== i))}>削除</Button> },
                  ]} style={{ marginBottom: 8 }} />
              )}
              <Upload multiple beforeUpload={(f, fl) => { setPendingFiles([...pendingFiles, f]); return false; }} showUploadList={false}>
                <Button icon={<UploadOutlined />}>ファイル選択（複数可・保存時に登録）</Button>
              </Upload>
            </>
          )}
        </Form>
      </Modal>
    </div>
  );
}
