import React, { useState, useEffect, useCallback } from 'react';
import { Table, Button, Modal, Form, Input, Select, Space, message, Card, Tag, DatePicker, Popconfirm, Tooltip } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SendOutlined, FilePdfOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { supplierOrderApi } from '../services/supplierOrderApi';
import axios from 'axios';
import dayjs from 'dayjs';

const { Option } = Select; const PAGE_SIZE = 10;

export default function SupplierOrder() {
  const [data, setData] = useState([]); const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0); const [page, setPage] = useState(1);
  const [modalVisible, setModalVisible] = useState(false); const [editingRecord, setEditingRecord] = useState(null);
  const [formLoading, setFormLoading] = useState(false); const [form] = Form.useForm();
  const [exportingId, setExportingId] = useState(null); const [employees, setEmployees] = useState([]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try { const r = await supplierOrderApi.list({ page: page-1, size: PAGE_SIZE }); setData(r.data.data.content||[]); setTotal(r.data.data.totalElements||0); }
    catch { message.error('データの取得に失敗しました'); } finally { setLoading(false); }
  }, [page]);
  useEffect(() => { fetchData(); }, [fetchData]);
  useEffect(() => { axios.get('/api/employee?size=200').then(r => setEmployees(r.data.data.content||[])).catch(()=>{}); }, []);

  const onEmployeeSelect = (empId) => {
    const emp = employees.find(e => e.id === empId);
    if (emp) form.setFieldsValue({ issuerName: emp.name, issuerDept: emp.department, issuerTel: emp.phone });
  };

  const handleCreate = () => {
    setEditingRecord(null);
    form.resetFields();
    form.setFieldsValue({ orderDate: dayjs(), deliveryDate: null, taxRate: 10, issuerName: '', issuerDept: '', issuerTel: '', supplierContact: '', supplierDept: '', supplierTel: '', supplierAddr: '', details: [{ employeeName: '', itemName: '', quantity: 1, unitPrice: 0 }] });
    setModalVisible(true);
  };

  const handleEdit = async (r) => {
    setEditingRecord(r);
    setFormLoading(true);
    try {
      const res = await supplierOrderApi.getById(r.id);
      const rec = res.data.data;
      form.setFieldsValue({
        ...rec,
        orderDate: rec.orderDate ? dayjs(rec.orderDate) : null,
        deliveryDate: rec.deliveryDate ? dayjs(rec.deliveryDate) : null,
        details: rec.details && rec.details.length > 0 ? rec.details : [{ employeeName: '', itemName: '', quantity: 1, unitPrice: 0 }],
      });
    } catch { message.error('データの取得に失敗しました'); }
    setFormLoading(false);
    setModalVisible(true);
  };

  const handleSubmit = async () => {
    try { const v = await form.validateFields(); setFormLoading(true);
      const payload = { ...v, orderDate: v.orderDate.format('YYYY-MM-DD'), deliveryDate: v.deliveryDate ? v.deliveryDate.format('YYYY-MM-DD') : null };
      if (editingRecord) { await supplierOrderApi.update(editingRecord.id, payload); message.success('更新しました'); }
      else { await supplierOrderApi.create(payload); message.success('登録しました'); }
      setModalVisible(false); fetchData();
    } catch (e) { if (e.response?.data?.error) message.error(e.response.data.error); else if (e.errorFields) message.error('必須項目を入力してください'); else message.error('登録に失敗しました'); } finally { setFormLoading(false); }
  };

  const handleDelete = async (r) => {
    try { await supplierOrderApi.delete(r.id); message.success('削除しました'); fetchData(); } catch { message.error('削除に失敗しました'); }
  };

  const handleExportPdf = async (r) => {
    setExportingId(r.id);
    try { await supplierOrderApi.downloadPdf(r.id); message.success('PDFをダウンロードしました'); }
    catch { message.error('PDF出力に失敗しました'); }
    finally { setExportingId(null); }
  };

  const columns = [
    { title:'発注番号', dataIndex:'orderNumber', width:150 },
    { title:'件名', dataIndex:'subject', width:180, ellipsis:true },
    { title:'受注方', dataIndex:'supplierName', width:160 },
    { title:'発注日', dataIndex:'orderDate', width:100 },
    { title:'納品期限', dataIndex:'deliveryDate', width:100 },
    { title:'税込金額', dataIndex:'totalWithTax', width:120, render:v=>v?.toLocaleString() },
    { title:'状態', dataIndex:'status', width:80, render:t=>(<Tag color={t==='下書き'?'gold':t==='発注済'?'blue':t==='納品済'?'green':'purple'}>{t}</Tag>) },
    { title:'操作', width:220, fixed:'right',
      render:(_,r)=><Space>
        <Tooltip title="PDF出力"><Button type="link" icon={<FilePdfOutlined/>} loading={exportingId===r.id} onClick={()=>handleExportPdf(r)}>PDF</Button></Tooltip>
        <Button type="link" icon={<EditOutlined/>} onClick={()=>handleEdit(r)}>編集</Button>
        <Popconfirm title="削除しますか？" onConfirm={()=>handleDelete(r)} okText="削除" cancelText="キャンセル"><Button type="link" danger icon={<DeleteOutlined/>}>削除</Button></Popconfirm>
      </Space> },
  ];

  return (<div>
    <h2 style={{marginBottom:20}}><SendOutlined /> 発注管理（我社→他社）</h2>
    <Card extra={<Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>新規登録</Button>}>
      <Table columns={columns} dataSource={data} rowKey="id" loading={loading} scroll={{x:'max-content'}}
        pagination={{current:page,pageSize:PAGE_SIZE,total,showSizeChanger:false,showTotal:t=>`全 ${t} 件`,onChange:p=>setPage(p)}} />
    </Card>
    <Modal title={editingRecord?'発注書編集':'発注書新規登録'} open={modalVisible} onOk={handleSubmit} onCancel={()=>setModalVisible(false)}
      confirmLoading={formLoading} okText="保存" cancelText="キャンセル" destroyOnClose centered width={800}>
      <Form form={form} layout="vertical" style={{marginTop:8}}>
        <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:'0 16px'}}>
          {editingRecord ? (<><Form.Item name="orderNumber" hidden><Input /></Form.Item><Form.Item label="発注番号"><Input value={editingRecord.orderNumber} disabled /></Form.Item></>) : <Form.Item label="発注番号"><Input value="自動採番（PO-SUP-YYYY-NNNN）" disabled /></Form.Item>}
          <Form.Item name="supplierName" label="受注方（会社名）" rules={[{required:true,message:'必須です'}]}><Input placeholder="例: 株式会社〇〇" maxLength={200}/></Form.Item>
        </div>
        <Form.Item name="subject" label="件名"><Input placeholder="例: サーバー機器一式" maxLength={500}/></Form.Item>
        <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:'0 16px'}}>
          <Form.Item name="orderDate" label="発注日" rules={[{required:true,message:'必須です'}]}><DatePicker style={{width:'100%'}}/></Form.Item>
          <Form.Item name="deliveryDate" label="納品期限" rules={[{required:true, message:'必須です'}]}><DatePicker style={{width:'100%'}}/></Form.Item>
        </div>

        <Card size="small" title="発注元（当社）" style={{marginBottom:16}}>
          <Form.Item label="担当者選択（社員DBより）" style={{marginBottom:8}}>
            <Select placeholder="社員を選択（自動補完）" allowClear showSearch
              filterOption={(i,o)=>o.children.toLowerCase().includes(i.toLowerCase())}
              onChange={(val) => { if (val) onEmployeeSelect(val); else form.setFieldsValue({issuerName:'',issuerDept:'',issuerTel:''}); }}>
              {employees.map(e=><Option key={e.id} value={e.id}>{e.name} ({e.department||'--'})</Option>)}
            </Select>
          </Form.Item>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:'0 16px'}}>
            <Form.Item name="issuerDept" label="部署"><Input placeholder="自動補完" maxLength={100}/></Form.Item>
            <Form.Item name="issuerName" label="担当者"><Input placeholder="自動補完" maxLength={100}/></Form.Item>
            <Form.Item name="issuerTel" label="TEL"><Input placeholder="自動補完" maxLength={20}/></Form.Item>
          </div>
        </Card>

        <Card size="small" title="受注方（発注先）" style={{marginBottom:16}}>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr 1fr',gap:'0 16px'}}>
            <Form.Item name="supplierDept" label="部署"><Input placeholder="先方部署" maxLength={100}/></Form.Item>
            <Form.Item name="supplierContact" label="担当者"><Input placeholder="先方担当者" maxLength={100}/></Form.Item>
            <Form.Item name="supplierTel" label="TEL"><Input placeholder="先方TEL" maxLength={20}/></Form.Item>
            <Form.Item name="supplierAddr" label="住所"><Input placeholder="先方住所" maxLength={500}/></Form.Item>
          </div>
        </Card>

        <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:'0 16px'}}>
          <Form.Item name="amount" label="税抜金額(円)" rules={[{required:true,message:'必須です'}]}><Input type="number" min={0}/></Form.Item>
          <Form.Item name="taxRate" label="消費税率(%)"><Input type="number" min={0} max={100} placeholder="10"/></Form.Item>
        </div>

        {editingRecord && <Form.Item name="status" label="状態">
          <Select><Option value="下書き">下書き</Option><Option value="発注済">発注済</Option><Option value="納品済">納品済</Option><Option value="検収済">検収済</Option></Select>
        </Form.Item>}

        <Form.Item name="remark" label="備考"><Input.TextArea rows={2}/></Form.Item>

        <Card size="small" title="発注明細" style={{marginBottom:16}}>
          <Form.List name="details">
            {(fields, { add, remove }) => (
              <>
                {fields.map(({ key, name, ...restField }) => (
                  <div key={key} style={{display:'grid',gridTemplateColumns:'1.5fr 2.5fr 0.8fr 1.2fr 1.2fr 40px',gap:'0 8px',marginBottom:8,alignItems:'start'}}>
                    <Form.Item {...restField} name={[name,'employeeName']} style={{marginBottom:0}}><Input placeholder="担当者" maxLength={100}/></Form.Item>
                    <Form.Item {...restField} name={[name,'itemName']} rules={[{required:true,message:'必須'}]} style={{marginBottom:0}}><Input placeholder="品名/項目" maxLength={500}/></Form.Item>
                    <Form.Item {...restField} name={[name,'quantity']} style={{marginBottom:0}}><Input type="number" min={0} placeholder="数量"/></Form.Item>
                    <Form.Item {...restField} name={[name,'unitPrice']} style={{marginBottom:0}}><Input type="number" min={0} placeholder="単価(円)"/></Form.Item>
                    <Form.Item {...restField} name={[name,'amount']} style={{marginBottom:0}}><Input type="number" min={0} placeholder="金額(円)" disabled/></Form.Item>
                    <MinusCircleOutlined onClick={()=>remove(name)} style={{marginTop:8,color:'#ff4d4f',cursor:'pointer'}}/>
                  </div>
                ))}
                <Button type="dashed" onClick={()=>add({employeeName:'',itemName:'',quantity:1,unitPrice:0})} block icon={<PlusOutlined/>}>明細行を追加</Button>
              </>
            )}
          </Form.List>
        </Card>
      </Form>
    </Modal>
  </div>);
}
