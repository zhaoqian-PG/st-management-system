import React, { useState, useEffect, useCallback } from 'react';
import { Table, Button, Modal, Form, Input, Select, Space, message, Card, Tag, DatePicker } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SendOutlined } from '@ant-design/icons';
import { supplierOrderApi } from '../services/supplierOrderApi';
import dayjs from 'dayjs';

const { Option } = Select; const PAGE_SIZE = 10;

export default function SupplierOrder() {
  const [data, setData] = useState([]); const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0); const [page, setPage] = useState(1);
  const [modalVisible, setModalVisible] = useState(false); const [editingRecord, setEditingRecord] = useState(null);
  const [formLoading, setFormLoading] = useState(false); const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try { const r = await supplierOrderApi.list({ page: page-1, size: PAGE_SIZE }); setData(r.data.data.content||[]); setTotal(r.data.data.totalElements||0); }
    catch { message.error('データの取得に失敗しました'); } finally { setLoading(false); }
  }, [page]);
  useEffect(() => { fetchData(); }, [fetchData]);

  const handleCreate = () => { setEditingRecord(null); form.resetFields(); form.setFieldsValue({ orderDate: dayjs(), taxRate: 10 }); setModalVisible(true); };
  const handleEdit = (r) => { setEditingRecord(r); form.setFieldsValue({ ...r, orderDate: r.orderDate?dayjs(r.orderDate):null, deliveryDate: r.deliveryDate?dayjs(r.deliveryDate):null }); setModalVisible(true); };

  const handleSubmit = async () => {
    try { const v = await form.validateFields(); setFormLoading(true);
      const payload = { ...v, orderDate: v.orderDate.format('YYYY-MM-DD'), deliveryDate: v.deliveryDate ? v.deliveryDate.format('YYYY-MM-DD') : null };
      if (editingRecord) { await supplierOrderApi.update(editingRecord.id, payload); message.success('更新しました'); }
      else { await supplierOrderApi.create(payload); message.success('登録しました'); }
      setModalVisible(false); fetchData();
    } catch (e) { if (e.response?.data?.error) message.error(e.response.data.error); } finally { setFormLoading(false); }
  };

  const handleDelete = (r) => Modal.confirm({ title:'削除', content:`発注書 ${r.orderNumber} を削除しますか？`, okText:'削除', cancelText:'キャンセル', okType:'danger', centered:true,
    onOk: async () => { try { await supplierOrderApi.delete(r.id); fetchData(); } catch {} } });

  const columns = [
    { title:'発注番号', dataIndex:'orderNumber', width:150 }, { title:'件名', dataIndex:'subject', width:180, ellipsis:true },
    { title:'発注先', dataIndex:'supplierName', width:160 }, { title:'発注日', dataIndex:'orderDate', width:100 },
    { title:'納品期限', dataIndex:'deliveryDate', width:100 }, { title:'税込金額', dataIndex:'totalWithTax', width:120, render:v=>v?.toLocaleString() },
    { title:'状態', dataIndex:'status', width:80, render:t=>(<Tag color={t==='下書き'?'gold':t==='発注済'?'blue':t==='納品済'?'green':'purple'}>{t}</Tag>) },
    { title:'操作', width:140, fixed:'right', render:(_,r)=><Space><Button type="link" icon={<EditOutlined/>} onClick={()=>handleEdit(r)}>編集</Button><Button type="link" danger icon={<DeleteOutlined/>} onClick={()=>handleDelete(r)}>削除</Button></Space> },
  ];

  return (<div>
    <h2 style={{marginBottom:20}}><SendOutlined /> 発注管理</h2>
    <Card extra={<Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>新規登録</Button>}>
      <Table columns={columns} dataSource={data} rowKey="id" loading={loading} scroll={{x:'max-content'}}
        pagination={{current:page,pageSize:PAGE_SIZE,total,showSizeChanger:false,showTotal:t=>`全 ${t} 件`,onChange:p=>setPage(p)}} />
    </Card>
    <Modal title={editingRecord?'発注書編集':'発注書新規登録'} open={modalVisible} onOk={handleSubmit} onCancel={()=>setModalVisible(false)}
      confirmLoading={formLoading} okText="保存" cancelText="キャンセル" destroyOnClose centered width={600}>
      <Form form={form} layout="vertical" style={{marginTop:8}}>
        <Form.Item name="orderNumber" label="発注番号" rules={[{required:true}]}><Input placeholder="PO-SUP-2026-0001" maxLength={50}/></Form.Item>
        <Form.Item name="supplierName" label="発注先" rules={[{required:true}]}><Input placeholder="例: 株式会社〇〇" maxLength={200}/></Form.Item>
        <Form.Item name="subject" label="件名"><Input placeholder="例: サーバー機器一式" maxLength={500}/></Form.Item>
        <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:'0 16px'}}>
          <Form.Item name="orderDate" label="発注日" rules={[{required:true}]}><DatePicker style={{width:'100%'}}/></Form.Item>
          <Form.Item name="deliveryDate" label="納品期限"><DatePicker style={{width:'100%'}}/></Form.Item>
          <Form.Item name="amount" label="税抜金額(円)" rules={[{required:true}]}><Input type="number" min={0}/></Form.Item>
          <Form.Item name="taxRate" label="消費税率(%)"><Input type="number" min={0} max={100} placeholder="10"/></Form.Item></div>
        {editingRecord && <Form.Item name="status" label="状態"><Select><Option value="下書き">下書き</Option><Option value="発注済">発注済</Option><Option value="納品済">納品済</Option><Option value="検収済">検収済</Option></Select></Form.Item>}
        <Form.Item name="remark" label="備考"><Input.TextArea rows={2}/></Form.Item>
      </Form>
    </Modal>
  </div>);
}
