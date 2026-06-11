import React, { useState, useEffect, useCallback } from 'react';
import { Table, Button, Modal, Form, Input, Select, Space, message, Card, Tag, DatePicker, Upload, Tabs } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ShoppingOutlined, UploadOutlined, DownloadOutlined, SendOutlined, PrinterOutlined, FileTextOutlined } from '@ant-design/icons';
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
  const [soDetails, setSoDetails] = useState([]); const [soSelectedOrder, setSoSelectedOrder] = useState(null);
  const [soOrderDetails, setSoOrderDetails] = useState([]); const [employees, setEmployees] = useState([]);

  // ---- Purchase Order ----
  const fetchData = useCallback(async () => {
    setLoading(true);
    try { const r = await purchaseOrderApi.list({ page: page-1, size: PAGE_SIZE }); setData(r.data.data.content||[]); setTotal(r.data.data.totalElements||0); }
    catch { message.error('データの取得に失敗しました'); } finally { setLoading(false); }
  }, [page]);
  useEffect(() => { fetchData(); }, [fetchData]);
  useEffect(() => { axios.get('/api/customer?size=200').then(r => setCustomers(r.data.data.content||[])).catch(()=>{}); }, []);
  useEffect(() => { axios.get('/api/employee?size=200').then(r => setEmployees(r.data.data.content||[])).catch(()=>{}); }, []);

  const onEmployeeSelect = (empId, target) => {
    const emp = employees.find(e => e.id === empId);
    if (!emp) return;
    const vals = {};
    if (target === 'recipient') {
      vals.recipientName = emp.name; vals.recipientDept = emp.department;
      vals.recipientTel = emp.phone; vals.recipientAddr = emp.japanAddress;
    } else if (target === 'issuerPo') {
      vals.issuerName = emp.name; vals.issuerDept = emp.department; vals.issuerTel = emp.phone;
    } else if (target === 'issuerSo') {
      soForm.setFieldsValue({ issuerName: emp.name, issuerDept: emp.department, issuerTel: emp.phone });
      return;
    }
    form.setFieldsValue(vals);
  };
  const onSoEmployeeSelect = (empId) => onEmployeeSelect(empId, 'issuerSo');
  const handleDeleteAttachment = async (attachId) => { try { await axios.delete(`/api/purchase-order/attachment/${attachId}`); message.success('添付ファイルを削除しました'); const res = await purchaseOrderApi.getById(selectedOrder.id); setSelectedOrder(res.data.data); setOrderDetails(res.data.data.details||[]); } catch { message.error('削除に失敗しました'); } };
  const handleSelect = async (r) => { setSelectedOrder(r); try { const res = await purchaseOrderApi.getById(r.id); const full = res.data.data; setSelectedOrder(full); setOrderDetails(full.details||[]); } catch {} };
  const handleCreate = () => { setEditingRecord(null); setDetails([]); form.resetFields(); form.setFieldsValue({ orderDate: dayjs(), deliveryDate: null, taxRate: 10, issuerName: '', issuerDept: '', issuerTel: '', recipientName: '', recipientDept: '', recipientTel: '', recipientAddr: '' }); setModalVisible(true); };
  const handleEdit = async (r) => { setEditingRecord(r); form.setFieldsValue({ ...r, orderDate: r.orderDate?dayjs(r.orderDate):null, deliveryDate: r.deliveryDate?dayjs(r.deliveryDate):null }); try { const res = await purchaseOrderApi.getById(r.id); setDetails(res.data.data.details||[]); } catch {} setModalVisible(true); };

  const handleSubmit = async () => {
    try { const v = await form.validateFields(); setFormLoading(true);
      const payload = { ...v, orderDate: v.orderDate.format('YYYY-MM-DD'), deliveryDate: v.deliveryDate ? v.deliveryDate.format('YYYY-MM-DD') : null, details,
        amount: details.reduce((s,d)=>s+((d.quantity||0)*(d.unitPrice||0)), 0) };
      if (editingRecord) { await purchaseOrderApi.update(editingRecord.id, payload); message.success('更新しました'); }
      else { await purchaseOrderApi.create(payload); message.success('登録しました'); }
      setModalVisible(false); fetchData();
    } catch (e) { if (e.response?.data?.error) message.error(e.response.data.error); else if (e.errorFields) message.error('必須項目を入力してください'); else message.error('登録に失敗しました'); } finally { setFormLoading(false); }
  };
  const handleDelete = (r) => Modal.confirm({ title:'削除', content:`受注書 ${r.orderNumber} を削除しますか？`, okText:'削除', cancelText:'キャンセル', okType:'danger', centered:true,
    onOk: async () => { try { await purchaseOrderApi.delete(r.id); setSelectedOrder(null); fetchData(); } catch {} } });
  const addDetail = () => setDetails([...details, { employeeName: '', itemName: '', quantity: 1, unitPrice: 0, amount: 0 }]);
  const updDetail = (i, f, v) => { const d=[...details]; d[i][f]=v; if(f==='quantity'||f==='unitPrice') d[i].amount=(d[i].quantity||0)*(d[i].unitPrice||0); setDetails(d); };
  const remDetail = (i) => setDetails(details.filter((_,j)=>j!==i));

  // ---- Supplier Order ----
  const fetchSoData = useCallback(async () => {
    setSoLoading(true);
    try { const r = await supplierOrderApi.list({ page: soPage-1, size: PAGE_SIZE }); setSoData(r.data.data.content||[]); setSoTotal(r.data.data.totalElements||0); }
    catch { message.error('データの取得に失敗しました'); } finally { setSoLoading(false); }
  }, [soPage]);
  useEffect(() => { fetchSoData(); }, [fetchSoData]);

  const handleSoSelect = async (r) => {
    setSoSelectedOrder(r);
    try { const res = await supplierOrderApi.getById(r.id); setSoOrderDetails(res.data.data.details||[]); }
    catch { setSoOrderDetails([]); }
  };
  const handleSoCreate = () => { setSoEditingRecord(null); setSoDetails([]); soForm.resetFields(); soForm.setFieldsValue({ orderDate: dayjs(), deliveryDate: null, taxRate: 10, issuerName: '', issuerDept: '', issuerTel: '', supplierContact: '', supplierDept: '', supplierTel: '', supplierAddr: '' }); setSoModalVisible(true); };
  const handleSoEdit = async (r) => { setSoEditingRecord(r); soForm.setFieldsValue({ ...r, orderDate: r.orderDate?dayjs(r.orderDate):null, deliveryDate: r.deliveryDate?dayjs(r.deliveryDate):null }); try { const res = await supplierOrderApi.getById(r.id); setSoDetails(res.data.data.details||[]); } catch { setSoDetails([]); } setSoModalVisible(true); };
  const handleSoSubmit = async () => {
    try { const v = await soForm.validateFields(); setSoFormLoading(true);
      const payload = { ...v, orderDate: v.orderDate.format('YYYY-MM-DD'), deliveryDate: v.deliveryDate ? v.deliveryDate.format('YYYY-MM-DD') : null, details: soDetails,
        amount: soDetails.reduce((s,d)=>s+((d.quantity||0)*(d.unitPrice||0)), 0) };
      if (soEditingRecord) { await supplierOrderApi.update(soEditingRecord.id, payload); message.success('更新しました'); }
      else { await supplierOrderApi.create(payload); message.success('登録しました'); }
      setSoModalVisible(false); fetchSoData();
    } catch (e) { if (e.response?.data?.error) message.error(e.response.data.error); else if (e.errorFields) message.error('必須項目を入力してください'); else message.error('登録に失敗しました'); } finally { setSoFormLoading(false); }
  };
  const addSoDetail = () => setSoDetails([...soDetails, { employeeName: '', itemName: '', quantity: 1, unitPrice: 0, amount: 0 }]);
  const updSoDetail = (i, f, v) => { const d=[...soDetails]; d[i][f]=v; if(f==='quantity'||f==='unitPrice') d[i].amount=(d[i].quantity||0)*(d[i].unitPrice||0); setSoDetails(d); };
  const remSoDetail = (i) => setSoDetails(soDetails.filter((_,j)=>j!==i));
  const handleSoDelete = (r) => Modal.confirm({ title:'削除', content:`発注書 ${r.orderNumber} を削除しますか？`, okText:'削除', cancelText:'キャンセル', okType:'danger', centered:true,
    onOk: async () => { try { await supplierOrderApi.delete(r.id); setSoSelectedOrder(null); setSoOrderDetails([]); fetchSoData(); } catch {} } });

  const poCols = [
    { title:'受注番号', dataIndex:'orderNumber', width:150 }, { title:'件名', dataIndex:'subject', width:180, ellipsis:true },
    { title:'発注元', dataIndex:'customerName', width:160, ellipsis:true }, { title:'受注日', dataIndex:'orderDate', width:100 },
    { title:'納品期限', dataIndex:'deliveryDate', width:100 }, { title:'金額', dataIndex:'totalWithTax', width:120, render:v=>v?.toLocaleString() },
    { title:'状態', dataIndex:'status', width:80, render:t=>(<Tag color={t==='下書き'?'gold':t==='発注済'?'blue':t==='納品済'?'green':'purple'}>{t}</Tag>) },
    { title:'操作', width:140, fixed:'right', render:(_,r)=><Space><Button type="link" icon={<EditOutlined/>} onClick={()=>handleEdit(r)}>編集</Button><Button type="link" danger icon={<DeleteOutlined/>} onClick={()=>handleDelete(r)}>削除</Button></Space> },
  ];
  const handleSoExportPdf = async (r) => {
    try { await supplierOrderApi.downloadPdf(r.id); message.success('PDFをダウンロードしました'); }
    catch { message.error('PDF出力に失敗しました'); }
  };
  const soCols = [
    { title:'発注番号', dataIndex:'orderNumber', width:150 }, { title:'件名', dataIndex:'subject', width:180, ellipsis:true },
    { title:'受注方', dataIndex:'supplierName', width:160 }, { title:'発注日', dataIndex:'orderDate', width:100 },
    { title:'納品期限', dataIndex:'deliveryDate', width:100 }, { title:'税込金額', dataIndex:'totalWithTax', width:120, render:v=>v?.toLocaleString() },
    { title:'状態', dataIndex:'status', width:80, render:t=>(<Tag color={t==='下書き'?'gold':t==='発注済'?'blue':t==='納品済'?'green':'purple'}>{t}</Tag>) },
    { title:'操作', width:180, fixed:'right', render:(_,r)=><Space><Button type="link" icon={<PrinterOutlined/>} onClick={()=>handleSoExportPdf(r)}>PDF</Button><Button type="link" icon={<EditOutlined/>} onClick={()=>handleSoEdit(r)}>編集</Button><Button type="link" danger icon={<DeleteOutlined/>} onClick={()=>handleSoDelete(r)}>削除</Button></Space> },
  ];

  const tabItems = [
    { key:'purchase', label:<span><ShoppingOutlined/> 受注書</span> },
    { key:'supplier', label:<span><SendOutlined/> 発注書</span> },
  ];

  return (<div>
    <h2 style={{marginBottom:20}}><ShoppingOutlined /> 受注書・発注管理</h2>
    <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />

    {activeTab === 'purchase' && <>
      <Card extra={<Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>新規登録</Button>}>
        <Table columns={poCols} dataSource={data} rowKey="id" loading={loading} scroll={{x:'max-content'}}
          onRow={r=>({onClick:()=>handleSelect(r),style:{background:selectedOrder?.id===r.id?'#e6f7ff':undefined,cursor:'pointer'}})}
          pagination={{current:page,pageSize:PAGE_SIZE,total,showSizeChanger:false,showTotal:t=>`全 ${t} 件`,onChange:p=>setPage(p)}} />
      </Card>
      {selectedOrder && <Card title={`📎 受注書詳細: ${selectedOrder.orderNumber}`} style={{marginTop:16}}>
        <div style={{display:'flex',gap:16,marginBottom:16}}>
          <Card size="small" style={{flex:1}} title="📋 基本情報">
            <p><strong>発注元:</strong> {selectedOrder.customerName}</p>
            {selectedOrder.issuerDept && <p><strong>発注元部署:</strong> {selectedOrder.issuerDept}</p>}
            {selectedOrder.issuerName && <p><strong>発注元担当:</strong> {selectedOrder.issuerName}</p>}
            {selectedOrder.issuerTel && <p><strong>発注元TEL:</strong> {selectedOrder.issuerTel}</p>}
            <p style={{borderTop:'1px dashed #e8e8e8',margin:'6px 0',paddingTop:6}}></p>
            <p><strong>受注日:</strong> {selectedOrder.orderDate}</p>
            {selectedOrder.deliveryDate && <p><strong>納品期限:</strong> {selectedOrder.deliveryDate}</p>}
            {selectedOrder.recipientDept && <p><strong>受注方部署:</strong> {selectedOrder.recipientDept}</p>}
            {selectedOrder.recipientName && <p><strong>受注方担当:</strong> {selectedOrder.recipientName}</p>}
            {selectedOrder.recipientAddr && <p><strong>受注方住所:</strong> {selectedOrder.recipientAddr}</p>}
            {selectedOrder.recipientTel && <p><strong>受注方TEL:</strong> {selectedOrder.recipientTel}</p>}
          </Card>
          <Card size="small" style={{flex:1}} title="💰 金額">
            <p>税抜金額: <strong>¥{selectedOrder.amount?.toLocaleString()}</strong></p>
            <p>消費税({selectedOrder.taxRate||10}%): <strong>¥{selectedOrder.taxAmount?.toLocaleString()}</strong></p>
            <p style={{fontSize:16}}>税込合計: <strong style={{color:'#cf1322'}}>¥{selectedOrder.totalWithTax?.toLocaleString()}</strong></p>
          </Card>
        </div>
        <Table dataSource={orderDetails} rowKey="id" size="small" pagination={false} locale={{emptyText:'明細はありません'}}
          columns={[{title:'担当者',dataIndex:'employeeName',width:100},{title:'項目',dataIndex:'itemName',ellipsis:true},{title:'数量',dataIndex:'quantity',width:80},{title:'単価',dataIndex:'unitPrice',width:90,render:v=>v?.toLocaleString()},{title:'金額',dataIndex:'amount',width:100,render:v=>v?.toLocaleString()}]} />
        <div style={{borderTop:'1px solid #f0f0f0',paddingTop:12,marginTop:12}}>
          <div style={{display:'flex',alignItems:'center',justifyContent:'space-between',marginBottom:8}}>
            <strong style={{fontSize:14}}>📎 添付ファイル</strong>
            <Upload showUploadList={false} beforeUpload={async (f) => { const fd=new FormData(); fd.append('file',f); try { await axios.post(`/api/purchase-order/${selectedOrder.id}/upload`,fd); message.success('アップロードしました'); const res = await purchaseOrderApi.getById(selectedOrder.id); setSelectedOrder(res.data.data); setOrderDetails(res.data.data.details||[]); } catch { message.error('アップロードに失敗しました'); } return false; }}>
              <Button size="small" icon={<UploadOutlined />}>アップロード</Button>
            </Upload>
          </div>
          {selectedOrder.attachments && selectedOrder.attachments.length > 0 ? (
            <div style={{display:'flex',flexDirection:'column',gap:4}}>
              {selectedOrder.attachments.map((att) => (
                <div key={att.id} style={{display:'flex',alignItems:'center',justifyContent:'space-between',padding:'6px 12px',background:'#fafafa',borderRadius:4,border:'1px solid #e8e8e8'}}>
                  <span><FileTextOutlined style={{marginRight:8,color:'#1890ff'}}/>{att.fileName} <span style={{color:'rgba(0,0,0,0.35)',fontSize:11}}>({(att.fileSize/1024).toFixed(1)} KB)</span></span>
                  <Button type="link" danger size="small" onClick={()=>handleDeleteAttachment(att.id)}>削除</Button>
                </div>
              ))}
            </div>
          ) : (
            <div style={{padding:'8px 12px',color:'rgba(0,0,0,0.25)',background:'#fafafa',borderRadius:4,border:'1px dashed #e8e8e8',textAlign:'center'}}>添付ファイルはありません</div>
          )}
        </div>
      </Card>}
      <Modal title={editingRecord?'受注書編集':'受注書新規登録'} open={modalVisible} onOk={handleSubmit} onCancel={()=>setModalVisible(false)}
        confirmLoading={formLoading} okText="保存" cancelText="キャンセル" destroyOnClose centered width={900}>
        <Form form={form} layout="vertical" style={{marginTop:8}}>
          {editingRecord ? (<><Form.Item name="orderNumber" hidden><Input /></Form.Item><Form.Item label="受注番号"><Input value={editingRecord.orderNumber} disabled /></Form.Item></>) : <Form.Item label="受注番号"><Input value="自動採番（PO-YYYY-NNNN）" disabled /></Form.Item>}
          <Form.Item name="subject" label="件名"><Input placeholder="例: システム開発一式" maxLength={500}/></Form.Item>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:'0 16px'}}>
            <Form.Item name="customerId" label="発注元（顧客）" rules={[{required:true}]}><Select placeholder="選択" showSearch filterOption={(i,o)=>o.children.toLowerCase().includes(i.toLowerCase())}>{customers.map(c=><Option key={c.id} value={c.id}>{c.companyName} ({c.customerCode})</Option>)}</Select></Form.Item>
            <Form.Item name="status" label="状態">{editingRecord?<Select><Option value="下書き">下書き</Option><Option value="発注済">発注済</Option><Option value="納品済">納品済</Option><Option value="検収済">検収済</Option></Select>:<Input value="下書き" disabled/>}</Form.Item>
            <Form.Item name="orderDate" label="受注日" rules={[{required:true}]}><DatePicker style={{width:'100%'}}/></Form.Item>
            <Form.Item name="deliveryDate" label="納品期限" rules={[{required:true, message:'必須です'}]}><DatePicker style={{width:'100%'}}/></Form.Item></div>
          <Card size="small" title="発注元（顧客）" style={{marginBottom:16}}>
            <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:'0 16px'}}>
              <Form.Item name="issuerDept" label="部署"><Input placeholder="顧客部署" maxLength={100}/></Form.Item>
              <Form.Item name="issuerName" label="担当者"><Input placeholder="顧客担当者" maxLength={100}/></Form.Item>
              <Form.Item name="issuerTel" label="TEL"><Input placeholder="顧客TEL" maxLength={20}/></Form.Item>
            </div>
          </Card>
          <Card size="small" title="受注方（当社）" style={{marginBottom:16}}>
            <Form.Item label="担当者選択（社員DBより）" style={{marginBottom:8}}>
              <Select placeholder="社員を選択（自動補完）" allowClear showSearch
                filterOption={(i,o)=>o.children.toLowerCase().includes(i.toLowerCase())}
                onChange={(val) => { if (val) onEmployeeSelect(val, 'recipient'); else form.setFieldsValue({recipientName:'',recipientDept:'',recipientTel:'',recipientAddr:''}); }}>
                {employees.map(e=><Option key={e.id} value={e.id}>{e.name} ({e.department||'--'})</Option>)}
              </Select>
            </Form.Item>
            <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr 1fr',gap:'0 16px'}}>
              <Form.Item name="recipientDept" label="部署"><Input placeholder="自動補完" maxLength={100}/></Form.Item>
              <Form.Item name="recipientName" label="担当者"><Input placeholder="自動補完" maxLength={100}/></Form.Item>
              <Form.Item name="recipientTel" label="TEL"><Input placeholder="自動補完" maxLength={20}/></Form.Item>
              <Form.Item name="recipientAddr" label="住所"><Input placeholder="自動補完" maxLength={500}/></Form.Item>
            </div>
          </Card>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:'0 16px'}}>
            <Form.Item label="税抜金額"><Input value={details.reduce((s,d)=>s+((d.quantity||0)*(d.unitPrice||0)),0).toLocaleString()} disabled/></Form.Item>
            <Form.Item name="taxRate" label="消費税率(%)"><Input type="number" min={0} max={100} placeholder="10"/></Form.Item>
            <Form.Item label="税込合計"><Input value={(()=>{const sub=details.reduce((s,d)=>s+((d.quantity||0)*(d.unitPrice||0)),0);const r=Number(form.getFieldValue('taxRate'))||10;return (sub+Math.round(sub*r)/100).toLocaleString();})()} disabled/></Form.Item></div>
          <div style={{borderTop:'2px solid #722ed1',paddingTop:12,marginTop:8}}>
            <h4 style={{color:'#722ed1',marginBottom:8}}>● 受注明細</h4>
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
          onRow={r=>({onClick:()=>handleSoSelect(r),style:{background:soSelectedOrder?.id===r.id?'#e6f7ff':undefined,cursor:'pointer'}})}
          pagination={{current:soPage,pageSize:PAGE_SIZE,total:soTotal,showSizeChanger:false,showTotal:t=>`全 ${t} 件`,onChange:p=>setSoPage(p)}} />
      </Card>
      {soSelectedOrder && (
        <Card title={`📎 発注書詳細: ${soSelectedOrder.orderNumber}`} style={{marginTop:16}}>
          <div style={{display:'flex',gap:16,marginBottom:16}}>
            <Card size="small" style={{flex:1}} title="📋 基本情報">
              <p><strong>受注方:</strong> {soSelectedOrder.supplierName}</p>
              {soSelectedOrder.supplierDept && <p><strong>受注方部署:</strong> {soSelectedOrder.supplierDept}</p>}
              {soSelectedOrder.supplierContact && <p><strong>受注方担当:</strong> {soSelectedOrder.supplierContact}</p>}
              {soSelectedOrder.supplierTel && <p><strong>受注方TEL:</strong> {soSelectedOrder.supplierTel}</p>}
              {soSelectedOrder.supplierAddr && <p><strong>受注方住所:</strong> {soSelectedOrder.supplierAddr}</p>}
              <p style={{borderTop:'1px dashed #e8e8e8',margin:'6px 0',paddingTop:6}}></p>
              <p><strong>発注日:</strong> {soSelectedOrder.orderDate}</p>
              {soSelectedOrder.deliveryDate && <p><strong>納品期限:</strong> {soSelectedOrder.deliveryDate}</p>}
              {soSelectedOrder.subject && <p><strong>件名:</strong> {soSelectedOrder.subject}</p>}
              {soSelectedOrder.issuerDept && <p><strong>発注元部署:</strong> {soSelectedOrder.issuerDept}</p>}
              {soSelectedOrder.issuerName && <p><strong>発注元担当:</strong> {soSelectedOrder.issuerName}</p>}
            </Card>
            <Card size="small" style={{flex:1}} title="💰 金額">
              <p>税抜金額: <strong>¥{soSelectedOrder.amount?.toLocaleString()}</strong></p>
              <p>消費税({soSelectedOrder.taxRate||10}%): <strong>¥{soSelectedOrder.taxAmount?.toLocaleString()}</strong></p>
              <p style={{fontSize:16}}>税込合計: <strong style={{color:'#cf1322'}}>¥{soSelectedOrder.totalWithTax?.toLocaleString()}</strong></p>
              <p>状態: <Tag color={soSelectedOrder.status==='下書き'?'gold':soSelectedOrder.status==='発注済'?'blue':soSelectedOrder.status==='納品済'?'green':'purple'}>{soSelectedOrder.status}</Tag></p>
            </Card>
          </div>
          {soOrderDetails.length > 0 && (
            <Table dataSource={soOrderDetails} rowKey="id" size="small" pagination={false} locale={{emptyText:'明細はありません'}}
              columns={[{title:'担当者',dataIndex:'employeeName',width:100},{title:'項目',dataIndex:'itemName',ellipsis:true},{title:'数量',dataIndex:'quantity',width:80},{title:'単価',dataIndex:'unitPrice',width:90,render:v=>v?.toLocaleString()},{title:'金額',dataIndex:'amount',width:100,render:v=>v?.toLocaleString()}]} />
          )}
          {soSelectedOrder.remark && <p style={{marginTop:8}}><strong>備考:</strong> {soSelectedOrder.remark}</p>}
        </Card>
      )}
      <Modal title={soEditingRecord?'発注書編集':'発注書新規登録'} open={soModalVisible} onOk={handleSoSubmit} onCancel={()=>setSoModalVisible(false)}
        confirmLoading={soFormLoading} okText="保存" cancelText="キャンセル" destroyOnClose centered width={900}>
        <Form form={soForm} layout="vertical" style={{marginTop:8}}>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:'0 16px'}}>
            {soEditingRecord ? (<><Form.Item name="orderNumber" hidden><Input /></Form.Item><Form.Item label="発注番号"><Input value={soEditingRecord.orderNumber} disabled /></Form.Item></>) : <Form.Item label="発注番号"><Input value="自動採番（PO-SUP-YYYY-NNNN）" disabled /></Form.Item>}
            <Form.Item name="supplierName" label="受注方（会社名）" rules={[{required:true}]}><Input maxLength={200} placeholder="例: 株式会社〇〇"/></Form.Item>
          </div>
          <Form.Item name="subject" label="件名"><Input maxLength={500} placeholder="例: サーバー機器一式"/></Form.Item>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:'0 16px'}}>
            <Form.Item name="orderDate" label="発注日" rules={[{required:true}]}><DatePicker style={{width:'100%'}}/></Form.Item>
            <Form.Item name="deliveryDate" label="納品期限" rules={[{required:true, message:'必須です'}]}><DatePicker style={{width:'100%'}}/></Form.Item>
          </div>
          <Card size="small" title="発注元（当社）" style={{marginBottom:16}}>
            <Form.Item label="担当者選択（社員DBより）" style={{marginBottom:8}}>
              <Select placeholder="社員を選択（自動補完）" allowClear showSearch
                filterOption={(i,o)=>o.children.toLowerCase().includes(i.toLowerCase())}
                onChange={(val) => { if (val) onSoEmployeeSelect(val); else soForm.setFieldsValue({issuerName:'',issuerDept:'',issuerTel:''}); }}>
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
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:'0 16px'}}>
            <Form.Item label="税抜金額"><Input value={soDetails.reduce((s,d)=>s+((d.quantity||0)*(d.unitPrice||0)),0).toLocaleString()} disabled/></Form.Item>
            <Form.Item name="taxRate" label="消費税率(%)"><Input type="number" min={0} max={100} placeholder="10"/></Form.Item>
            <Form.Item label="税込合計"><Input value={(()=>{const sub=soDetails.reduce((s,d)=>s+((d.quantity||0)*(d.unitPrice||0)),0);const r=Number(soForm.getFieldValue('taxRate'))||10;return (sub+Math.round(sub*r)/100).toLocaleString();})()} disabled/></Form.Item>
          </div>
          {soEditingRecord && <Form.Item name="status" label="状態"><Select><Option value="下書き">下書き</Option><Option value="発注済">発注済</Option><Option value="納品済">納品済</Option><Option value="検収済">検収済</Option></Select></Form.Item>}
          <div style={{borderTop:'2px solid #722ed1',paddingTop:12,marginTop:8}}>
            <h4 style={{color:'#722ed1',marginBottom:8}}>● 発注明細</h4>
            <Button type="dashed" onClick={addSoDetail} style={{marginBottom:8}}>＋ 明細行追加</Button>
            {soDetails.length>0 && <table style={{width:'100%',borderCollapse:'collapse',marginBottom:8}}><thead><tr style={{background:'#fafafa'}}><th style={{padding:4}}>担当者</th><th style={{padding:4}}>項目</th><th style={{padding:4,width:70}}>数量</th><th style={{padding:4,width:90}}>単価</th><th style={{padding:4,width:90}}>金額</th><th style={{padding:4,width:50}}></th></tr></thead><tbody>
              {soDetails.map((d,i)=><tr key={i}><td style={{padding:2}}><Input value={d.employeeName} onChange={e=>updSoDetail(i,'employeeName',e.target.value)} size="small" placeholder="担当者"/></td><td style={{padding:2}}><Input value={d.itemName} onChange={e=>updSoDetail(i,'itemName',e.target.value)} size="small" placeholder="品名"/></td><td style={{padding:2}}><Input value={d.quantity} onChange={e=>updSoDetail(i,'quantity',Number(e.target.value))} size="small" type="number"/></td><td style={{padding:2}}><Input value={d.unitPrice} onChange={e=>updSoDetail(i,'unitPrice',Number(e.target.value))} size="small" type="number"/></td><td style={{padding:2,textAlign:'right'}}>{(d.amount||0).toLocaleString()}</td><td style={{padding:2}}><Button type="link" danger size="small" onClick={()=>remSoDetail(i)}>✕</Button></td></tr>)}</tbody></table>}
          </div>
          <Form.Item name="remark" label="備考"><Input.TextArea rows={2}/></Form.Item>
        </Form>
      </Modal>
    </>}
  </div>);
}
