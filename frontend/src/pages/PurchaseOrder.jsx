import React, { useState, useEffect, useCallback } from 'react';
import { Table, Button, Modal, Form, Input, Select, Space, message, Card, Tag, DatePicker, Upload, Tabs } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ShoppingOutlined, UploadOutlined, DownloadOutlined, SendOutlined } from '@ant-design/icons';
import { purchaseOrderApi } from '../services/purchaseOrderApi';
import { supplierOrderApi } from '../services/supplierOrderApi';
import axios from 'axios';
import dayjs from 'dayjs';

const { Option } = Select; const PAGE_SIZE = 10;

export default function PurchaseOrder() {
  const [activeTab, setActiveTab] = useState('purchase');
  // Purchase Order (注文書) state
  const [data, setData] = useState([]); const [loading, setLoading] = useState(false); const [total, setTotal] = useState(0); const [page, setPage] = useState(1);
  const [modalVisible, setModalVisible] = useState(false); const [editingRecord, setEditingRecord] = useState(null);
  const [formLoading, setFormLoading] = useState(false); const [customers, setCustomers] = useState([]);
  const [selectedOrder, setSelectedOrder] = useState(null); const [orderDetails, setOrderDetails] = useState([]);
  const [details, setDetails] = useState([]); const [form] = Form.useForm();

  // Supplier Order (発注書) state
  const [soData, setSoData] = useState([]); const [soLoading, setSoLoading] = useState(false); const [soTotal, setSoTotal] = useState(0); const [soPage, setSoPage] = useState(1);
  const [soModalVisible, setSoModalVisible] = useState(false); const [soEditingRecord, setSoEditingRecord] = useState(null);
  const [soFormLoading, setSoFormLoading] = useState(false); const [soForm] = Form.useForm();

  // ---- Purchase Order ----
  const fetchData = useCallback(async () => {
    setLoading(true);
    try { const r = await purchaseOrderApi.list({ page: page-1, size: PAGE_SIZE }); setData(r.data.data.content||[]); setTotal(r.data.data.totalElements||0); }
    catch { message.error('データ取得失敗'); } finally { setLoading(false); }
  }, [page]);
  useEffect(() => { fetchData(); }, [fetchData]);
  useEffect(() => { axios.get('/api/customer?size=200').then(r => setCustomers(r.data.data.content||[])).catch(()=>{}); }, []);

  const handleSelect = async (r) => { setSelectedOrder(r); try { const res = await purchaseOrderApi.getById(r.id); setOrderDetails(res.data.data.details||[]); } catch {} };
  const handleCreate = () => { setEditingRecord(null); setDetails([]); form.resetFields(); form.setFieldsValue({ orderDate: dayjs(), taxRate: 10 }); setModalVisible(true); };
  const handleEdit = async (r) => { setEditingRecord(r); form.setFieldsValue({ ...r, orderDate: r.orderDate?dayjs(r.orderDate):null, deliveryDate: r.deliveryDate?dayjs(r.deliveryDate):null }); try { const res = await purchaseOrderApi.getById(r.id); setDetails(res.data.data.details||[]); } catch {} setModalVisible(true); };

  const handleSubmit = async () => {
    try { const v = await form.validateFields(); setFormLoading(true);
      const payload = { ...v, orderDate: v.orderDate.format('YYYY-MM-DD'), deliveryDate: v.deliveryDate ? v.deliveryDate.format('YYYY-MM-DD') : null, details,
        amount: details.reduce((s,d)=>s+((d.quantity||0)*(d.unitPrice||0)), 0) };
      if (editingRecord) { await purchaseOrderApi.update(editingRecord.id, payload); message.success('更新しました'); }
      else { await purchaseOrderApi.create(payload); message.success('登録しました'); }
      setModalVisible(false); fetchData();
    } catch (e) { if (e.response?.data?.error) message.error(e.response.data.error); } finally { setFormLoading(false); }
  };
  const handleDelete = (r) => Modal.confirm({ title:'削除', content:`注文書 ${r.orderNumber} を削除しますか？`, okText:'削除', cancelText:'キャンセル', okType:'danger', centered:true,
    onOk: async () => { try { await purchaseOrderApi.delete(r.id); setSelectedOrder(null); fetchData(); } catch {} } });
  const addDetail = () => setDetails([...details, { employeeName: '', itemName: '', quantity: 1, unitPrice: 0, amount: 0 }]);
  const updDetail = (i, f, v) => { const d=[...details]; d[i][f]=v; if(f==='quantity'||f==='unitPrice') d[i].amount=(d[i].quantity||0)*(d[i].unitPrice||0); setDetails(d); };
  const remDetail = (i) => setDetails(details.filter((_,j)=>j!==i));

  // ---- Supplier Order ----
  const fetchSoData = useCallback(async () => {
    setSoLoading(true);
    try { const r = await supplierOrderApi.list({ page: soPage-1, size: PAGE_SIZE }); setSoData(r.data.data.content||[]); setSoTotal(r.data.data.totalElements||0); }
    catch { message.error('データ取得失敗'); } finally { setSoLoading(false); }
  }, [soPage]);
  useEffect(() => { fetchSoData(); }, [fetchSoData]);

  const handleSoCreate = () => { setSoEditingRecord(null); soForm.resetFields(); soForm.setFieldsValue({ orderDate: dayjs(), taxRate: 10 }); setSoModalVisible(true); };
  const handleSoEdit = (r) => { setSoEditingRecord(r); soForm.setFieldsValue({ ...r, orderDate: r.orderDate?dayjs(r.orderDate):null, deliveryDate: r.deliveryDate?dayjs(r.deliveryDate):null }); setSoModalVisible(true); };
  const handleSoSubmit = async () => {
    try { const v = await soForm.validateFields(); setSoFormLoading(true);
      const payload = { ...v, orderDate: v.orderDate.format('YYYY-MM-DD'), deliveryDate: v.deliveryDate ? v.deliveryDate.format('YYYY-MM-DD') : null };
      if (soEditingRecord) { await supplierOrderApi.update(soEditingRecord.id, payload); message.success('更新しました'); }
      else { await supplierOrderApi.create(payload); message.success('登録しました'); }
      setSoModalVisible(false); fetchSoData();
    } catch (e) { if (e.response?.data?.error) message.error(e.response.data.error); } finally { setSoFormLoading(false); }
  };
  const handleSoDelete = (r) => Modal.confirm({ title:'削除', content:`発注書 ${r.orderNumber} を削除しますか？`, okText:'削除', cancelText:'キャンセル', okType:'danger', centered:true,
    onOk: async () => { try { await supplierOrderApi.delete(r.id); fetchSoData(); } catch {} } });

  const poCols = [
    { title:'注文番号', dataIndex:'orderNumber', width:150 }, { title:'件名', dataIndex:'subject', width:180, ellipsis:true },
    { title:'発注先', dataIndex:'customerName', width:160, ellipsis:true }, { title:'注文日', dataIndex:'orderDate', width:100 },
    { title:'納品期限', dataIndex:'deliveryDate', width:100 }, { title:'金額', dataIndex:'totalWithTax', width:120, render:v=>v?.toLocaleString() },
    { title:'状態', dataIndex:'status', width:80, render:t=>(<Tag color={t==='下書き'?'gold':t==='発注済'?'blue':t==='納品済'?'green':'purple'}>{t}</Tag>) },
    { title:'操作', width:140, fixed:'right', render:(_,r)=><Space><Button type="link" icon={<EditOutlined/>} onClick={()=>handleEdit(r)}>編集</Button><Button type="link" danger icon={<DeleteOutlined/>} onClick={()=>handleDelete(r)}>削除</Button></Space> },
  ];
  const soCols = [
    { title:'発注番号', dataIndex:'orderNumber', width:150 }, { title:'件名', dataIndex:'subject', width:180, ellipsis:true },
    { title:'発注先', dataIndex:'supplierName', width:160 }, { title:'発注日', dataIndex:'orderDate', width:100 },
    { title:'納品期限', dataIndex:'deliveryDate', width:100 }, { title:'税込金額', dataIndex:'totalWithTax', width:120, render:v=>v?.toLocaleString() },
    { title:'状態', dataIndex:'status', width:80, render:t=>(<Tag color={t==='下書き'?'gold':t==='発注済'?'blue':t==='納品済'?'green':'purple'}>{t}</Tag>) },
    { title:'操作', width:140, fixed:'right', render:(_,r)=><Space><Button type="link" icon={<EditOutlined/>} onClick={()=>handleSoEdit(r)}>編集</Button><Button type="link" danger icon={<DeleteOutlined/>} onClick={()=>handleSoDelete(r)}>削除</Button></Space> },
  ];

  const tabItems = [
    { key:'purchase', label:<span><ShoppingOutlined/> 注文書</span> },
    { key:'supplier', label:<span><SendOutlined/> 発注書</span> },
  ];

  return (<div>
    <h2 style={{marginBottom:20}}><ShoppingOutlined /> 注文書・発注管理</h2>
    <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />

    {activeTab === 'purchase' && <>
      <Card extra={<Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>新規登録</Button>}>
        <Table columns={poCols} dataSource={data} rowKey="id" loading={loading} scroll={{x:'max-content'}}
          onRow={r=>({onClick:()=>handleSelect(r),style:{background:selectedOrder?.id===r.id?'#e6f7ff':undefined,cursor:'pointer'}})}
          pagination={{current:page,pageSize:PAGE_SIZE,total,showSizeChanger:false,showTotal:t=>`全 ${t} 件`,onChange:p=>setPage(p)}} />
      </Card>
      {selectedOrder && <Card title={`📎 注文書詳細: ${selectedOrder.orderNumber}`} style={{marginTop:16}}
        extra={<Space>
          <Upload showUploadList={false} beforeUpload={async (f) => { const fd=new FormData(); fd.append('file',f); try { await axios.post(`/api/purchase-order/${selectedOrder.id}/upload`,fd); message.success('アップロードしました'); } catch { message.error('失敗'); } return false; }}>
            <Button icon={<UploadOutlined />}>発注書添付</Button>
          </Upload>
          {selectedOrder.attachmentPath && <Button icon={<DownloadOutlined />} onClick={() => window.open(`/api/purchase-order/${selectedOrder.id}/download`)}>DL</Button>}
        </Space>}>
        <div style={{display:'flex',gap:16,marginBottom:16}}>
          <Card size="small" style={{flex:1}} title="📋 基本情報">
            <p><strong>発注先:</strong> {selectedOrder.customerName}</p>
            {selectedOrder.recipientDept && <p><strong>部署:</strong> {selectedOrder.recipientDept}</p>}
            {selectedOrder.recipientName && <p><strong>担当:</strong> {selectedOrder.recipientName}</p>}
            <p><strong>注文日:</strong> {selectedOrder.orderDate}</p>
            {selectedOrder.deliveryDate && <p><strong>納品期限:</strong> {selectedOrder.deliveryDate}</p>}
          </Card>
          <Card size="small" style={{flex:1}} title="💰 金額">
            <p>税抜金額: <strong>¥{selectedOrder.amount?.toLocaleString()}</strong></p>
            <p>消費税({selectedOrder.taxRate||10}%): <strong>¥{selectedOrder.taxAmount?.toLocaleString()}</strong></p>
            <p style={{fontSize:16}}>税込合計: <strong style={{color:'#cf1322'}}>¥{selectedOrder.totalWithTax?.toLocaleString()}</strong></p>
          </Card>
        </div>
        <Table dataSource={orderDetails} rowKey="id" size="small" pagination={false} locale={{emptyText:'明細はありません'}}
          columns={[{title:'担当者',dataIndex:'employeeName',width:100},{title:'項目',dataIndex:'itemName',ellipsis:true},{title:'数量',dataIndex:'quantity',width:80},{title:'単価',dataIndex:'unitPrice',width:90,render:v=>v?.toLocaleString()},{title:'金額',dataIndex:'amount',width:100,render:v=>v?.toLocaleString()}]} />
      </Card>}
      <Modal title={editingRecord?'注文書編集':'注文書新規登録'} open={modalVisible} onOk={handleSubmit} onCancel={()=>setModalVisible(false)}
        confirmLoading={formLoading} okText="保存" cancelText="キャンセル" destroyOnClose centered width={900}>
        <Form form={form} layout="vertical" style={{marginTop:8}}>
          {editingRecord ? (<><Form.Item name="orderNumber" hidden><Input /></Form.Item><Form.Item label="注文番号"><Input value={editingRecord.orderNumber} disabled /></Form.Item></>) : <Form.Item label="注文番号"><Input value="自動採番（PO-YYYY-NNNN）" disabled /></Form.Item>}
          <Form.Item name="subject" label="件名"><Input placeholder="例: システム開発一式" maxLength={500}/></Form.Item>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:'0 16px'}}>
            <Form.Item name="customerId" label="発注先" rules={[{required:true}]}><Select placeholder="選択" showSearch filterOption={(i,o)=>o.children.toLowerCase().includes(i.toLowerCase())}>{customers.map(c=><Option key={c.id} value={c.id}>{c.companyName} ({c.customerCode})</Option>)}</Select></Form.Item>
            <Form.Item name="status" label="状態">{editingRecord?<Select><Option value="下書き">下書き</Option><Option value="発注済">発注済</Option><Option value="納品済">納品済</Option><Option value="検収済">検収済</Option></Select>:<Input value="下書き" disabled/>}</Form.Item>
            <Form.Item name="orderDate" label="注文日" rules={[{required:true}]}><DatePicker style={{width:'100%'}}/></Form.Item>
            <Form.Item name="deliveryDate" label="納品期限"><DatePicker style={{width:'100%'}}/></Form.Item>
            <Form.Item name="issuerName" label="発注元担当者"><Input placeholder="自社担当者" maxLength={100}/></Form.Item>
            <Form.Item name="issuerDept" label="発注元部署"><Input placeholder="自社部署" maxLength={100}/></Form.Item></div>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:'0 16px'}}>
            <Form.Item label="税抜金額"><Input value={details.reduce((s,d)=>s+((d.quantity||0)*(d.unitPrice||0)),0).toLocaleString()} disabled/></Form.Item>
            <Form.Item name="taxRate" label="消費税率(%)"><Input type="number" min={0} max={100} placeholder="10"/></Form.Item>
            <Form.Item label="税込合計"><Input value={(()=>{const sub=details.reduce((s,d)=>s+((d.quantity||0)*(d.unitPrice||0)),0);const r=Number(form.getFieldValue('taxRate'))||10;return (sub+Math.round(sub*r)/100).toLocaleString();})()} disabled/></Form.Item></div>
          <div style={{borderTop:'2px solid #722ed1',paddingTop:12,marginTop:8}}>
            <h4 style={{color:'#722ed1',marginBottom:8}}>● 注文明細</h4>
            <Button type="dashed" onClick={addDetail} style={{marginBottom:8}}>＋ 明細行追加</Button>
            {details.length>0 && <table style={{width:'100%',borderCollapse:'collapse',marginBottom:8}}><thead><tr style={{background:'#fafafa'}}><th style={{padding:4}}>担当者</th><th style={{padding:4}}>項目</th><th style={{padding:4,width:70}}>数量</th><th style={{padding:4,width:90}}>単価</th><th style={{padding:4,width:90}}>金額</th><th style={{padding:4,width:50}}></th></tr></thead><tbody>
              {details.map((d,i)=><tr key={i}><td style={{padding:2}}><Input value={d.employeeName} onChange={e=>updDetail(i,'employeeName',e.target.value)} size="small" placeholder="担当者"/></td><td style={{padding:2}}><Input value={d.itemName} onChange={e=>updDetail(i,'itemName',e.target.value)} size="small" placeholder="品名"/></td><td style={{padding:2}}><Input value={d.quantity} onChange={e=>updDetail(i,'quantity',Number(e.target.value))} size="small" type="number"/></td><td style={{padding:2}}><Input value={d.unitPrice} onChange={e=>updDetail(i,'unitPrice',Number(e.target.value))} size="small" type="number"/></td><td style={{padding:2,textAlign:'right'}}>{(d.amount||0).toLocaleString()}</td><td style={{padding:2}}><Button type="link" danger size="small" onClick={()=>remDetail(i)}>✕</Button></td></tr>)}</tbody></table>}
          </div>
          <Form.Item name="remark" label="備考"><Input.TextArea rows={2}/></Form.Item>
        </Form>
      </Modal>
    </>}

    {activeTab === 'supplier' && <>
      <Card extra={<Button type="primary" icon={<PlusOutlined/>} onClick={handleSoCreate}>新規登録</Button>}>
        <Table columns={soCols} dataSource={soData} rowKey="id" loading={soLoading} scroll={{x:'max-content'}}
          pagination={{current:soPage,pageSize:PAGE_SIZE,total:soTotal,showSizeChanger:false,showTotal:t=>`全 ${t} 件`,onChange:p=>setSoPage(p)}} />
      </Card>
      <Modal title={soEditingRecord?'発注書編集':'発注書新規登録'} open={soModalVisible} onOk={handleSoSubmit} onCancel={()=>setSoModalVisible(false)}
        confirmLoading={soFormLoading} okText="保存" cancelText="キャンセル" destroyOnClose centered width={600}>
        <Form form={soForm} layout="vertical" style={{marginTop:8}}>
          <Form.Item name="orderNumber" label="発注番号" rules={[{required:true}]}><Input maxLength={50}/></Form.Item>
          <Form.Item name="supplierName" label="発注先" rules={[{required:true}]}><Input maxLength={200}/></Form.Item>
          <Form.Item name="subject" label="件名"><Input maxLength={500}/></Form.Item>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:'0 16px'}}>
            <Form.Item name="orderDate" label="発注日" rules={[{required:true}]}><DatePicker style={{width:'100%'}}/></Form.Item>
            <Form.Item name="deliveryDate" label="納品期限"><DatePicker style={{width:'100%'}}/></Form.Item>
            <Form.Item name="amount" label="税抜金額(円)" rules={[{required:true}]}><Input type="number" min={0}/></Form.Item>
            <Form.Item name="taxRate" label="消費税率(%)"><Input type="number" min={0} max={100} placeholder="10"/></Form.Item></div>
          {soEditingRecord && <Form.Item name="status" label="状態"><Select><Option value="下書き">下書き</Option><Option value="発注済">発注済</Option><Option value="納品済">納品済</Option><Option value="検収済">検収済</Option></Select></Form.Item>}
          <Form.Item name="remark" label="備考"><Input.TextArea rows={2}/></Form.Item>
        </Form>
      </Modal>
    </>}
  </div>);
}
