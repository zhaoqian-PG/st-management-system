import React, { useState, useEffect, useCallback } from 'react';
import { Table, Button, Modal, Form, Input, Select, Space, message, Card, Tag, DatePicker, TimePicker, Statistic, Row, Col } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, CalendarOutlined, DownloadOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { attendanceApi } from '../services/attendanceApi';
import axios from 'axios';
import dayjs from 'dayjs';

const { Option } = Select;
const PAGE_SIZE = 31;

export default function Attendance() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);
  const [formLoading, setFormLoading] = useState(false);
  const [year, setYear] = useState(dayjs().year());
  const [month, setMonth] = useState(dayjs().month() + 1);
  const [employeeId, setEmployeeId] = useState(null);
  const [employees, setEmployees] = useState([]);
  const [generating, setGenerating] = useState(false);
  const [form] = Form.useForm();
  const role = localStorage.getItem('userRole') || 'USER';

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await attendanceApi.list({ page: page - 1, size: PAGE_SIZE, year, month, employeeId: employeeId || undefined });
      setData(res.data.data.content || []); setTotal(res.data.data.totalElements || 0);
    } catch { message.error('データの取得に失敗しました'); }
    finally { setLoading(false); }
  }, [page, year, month, employeeId]);

  useEffect(() => { fetchData(); }, [fetchData]);
  useEffect(() => {
    axios.get('/api/employee?size=200').then(r => {
      const empList = r.data.data.content || [];
      // If USER role, filter to only show their own data
      if (role === 'USER') {
        const empId = parseInt(localStorage.getItem('employeeId') || '0');
        if (empId) { setEmployeeId(empId); setEmployees(empList.filter(e => e.id === empId)); }
        else setEmployees(empList);
      } else setEmployees(empList);
    }).catch(() => {});
  }, [role]);

  const handleGenerate = async () => {
    if (!employeeId) { message.warning('社員を選択してください'); return; }
    setGenerating(true);
    try {
      const res = await axios.post(`/api/attendance/generate?year=${year}&month=${month}&employeeId=${employeeId}`);
      message.success(res.data.message); fetchData();
    } catch { message.error('生成に失敗しました'); }
    finally { setGenerating(false); }
  };

  const handleCreate = () => {
    setEditingRecord(null); form.resetFields();
    form.setFieldsValue({ employeeId: employeeId || undefined, workDate: dayjs(), workHours: 8.0, overtimeHours: 0.0, workType: 'NORMAL', status: '出勤' });
    setModalVisible(true);
  };

  const handleEdit = (r) => {
    setEditingRecord(r);
    form.setFieldsValue({ ...r, workDate: dayjs(r.workDate), clockIn: r.clockIn ? dayjs(r.clockIn, 'HH:mm') : null, clockOut: r.clockOut ? dayjs(r.clockOut, 'HH:mm') : null });
    setModalVisible(true);
  };

  const handleSubmit = async () => {
    try {
      const v = await form.validateFields(); setFormLoading(true);
      const payload = { ...v, workDate: v.workDate.format('YYYY-MM-DD'), clockIn: v.clockIn ? v.clockIn.format('HH:mm') : null, clockOut: v.clockOut ? v.clockOut.format('HH:mm') : null };
      if (editingRecord) { await attendanceApi.update(editingRecord.id, payload); message.success('更新しました'); }
      else { await attendanceApi.create(payload); message.success('登録しました'); }
      setModalVisible(false); fetchData();
    } catch (e) { if (e.response?.data?.error) message.error(e.response.data.error); }
    finally { setFormLoading(false); }
  };

  const handleDelete = (r) => Modal.confirm({ title: '削除', content: '削除しますか？', okText: '削除', cancelText: 'キャンセル', okType: 'danger', centered: true,
    onOk: async () => { try { await attendanceApi.delete(r.id); fetchData(); } catch {} } });

  const totalWorkHours = data.reduce((s, r) => s + (r.workHours || 0), 0);
  const totalOvertime = data.reduce((s, r) => s + (r.overtimeHours || 0), 0);

  const columns = [
    { title: '日付', dataIndex: 'workDate', width: 120 }, { title: '社員名', dataIndex: 'employeeName', width: 120 },
    { title: '勤務(h)', dataIndex: 'workHours', width: 80 }, { title: '残業(h)', dataIndex: 'overtimeHours', width: 80 },
    { title: '出勤', dataIndex: 'clockIn', width: 70, render: t => t || '-' }, { title: '退勤', dataIndex: 'clockOut', width: 70, render: t => t || '-' },
    { title: '総労働(h)', dataIndex: 'totalHours', width: 90, render: t => t != null ? <strong>{t}</strong> : '-' },
    { title: '区分', dataIndex: 'workType', width: 90, render: t => { const m = { NORMAL: ['blue', '通常'], REMOTE: ['green', '在宅'], HOLIDAY_WORK: ['orange', '休出'], LEAVE: ['default', '休暇'] }; return <Tag color={m[t]?.[0]}>{m[t]?.[1] || t}</Tag>; }},
    { title: '状態', dataIndex: 'status', width: 70, render: t => { const m = { '出勤': ['green', '出勤'], '欠勤': ['red', '欠勤'], '休暇': ['blue', '休暇'] }; return <Tag color={m[t]?.[0]}>{m[t]?.[1] || t}</Tag>; }},
    { title: '備考', dataIndex: 'remark', width: 180, ellipsis: true },
    { title: '操作', width: 140, fixed: 'right', render: (_, r) => <Space><Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(r)}>編集</Button><Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(r)}>削除</Button></Space> },
  ];

  return (<div>
    <h2 style={{ marginBottom: 20 }}><CalendarOutlined /> 勤務管理</h2>
    <Card>
      <div style={{ display: 'flex', gap: 12, marginBottom: 16, alignItems: 'center', flexWrap: 'wrap' }}>
        <Select value={year} onChange={setYear} style={{ width: 100 }}>{[2024,2025,2026,2027].map(y => <Option key={y} value={y}>{y}</Option>)}</Select><span>年</span>
        <Select value={month} onChange={setMonth} style={{ width: 70 }}>{Array.from({length:12},(_,i)=>i+1).map(m => <Option key={m} value={m}>{m}</Option>)}</Select><span>月</span>
        {role === 'ADMIN' && <Select placeholder="全社員" allowClear value={employeeId} onChange={setEmployeeId} style={{ width: 180 }} showSearch filterOption={(i,o)=>o.children.toLowerCase().includes(i.toLowerCase())}>{employees.map(e => <Option key={e.id} value={e.id}>{e.name} ({e.employeeCode})</Option>)}</Select>}
        <Button icon={<ThunderboltOutlined />} onClick={handleGenerate} loading={generating} disabled={!employeeId}>勤務自動生成</Button>
        <span style={{ flex: 1 }} />
        <Button icon={<DownloadOutlined />} onClick={() => window.open(`/api/attendance/export?year=${year}&month=${month}${employeeId ? '&employeeId='+employeeId : ''}`)}>CSV出力</Button>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>新規登録</Button>
      </div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}><Card size="small"><Statistic title="総勤務時間(h)" value={totalWorkHours.toFixed(1)} suffix="h" /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="総残業時間(h)" value={totalOvertime.toFixed(1)} suffix="h" /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="勤務日数" value={data.filter(r=>r.status==='出勤').length} suffix="日" /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="総件数" value={total} suffix="件" /></Card></Col>
      </Row>
      <Table columns={columns} dataSource={data} rowKey="id" loading={loading}
        pagination={{ current: page, pageSize: PAGE_SIZE, total, showSizeChanger: false, showTotal: t => `全 ${t} 件`, onChange: p => setPage(p) }} />
    </Card>
    <Modal title={editingRecord ? '勤務情報編集' : '勤務新規登録'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)}
      confirmLoading={formLoading} okText="保存" cancelText="キャンセル" destroyOnClose centered width={550}>
      <Form form={form} layout="vertical" style={{ marginTop: 8 }}>
        <Form.Item name="employeeId" label="社員" rules={[{ required: true }]}>
          <Select placeholder="選択" showSearch filterOption={(i,o) => o.children.toLowerCase().includes(i.toLowerCase())} disabled={!!editingRecord}>{employees.map(e => <Option key={e.id} value={e.id}>{e.name} ({e.employeeCode})</Option>)}</Select></Form.Item>
        <Form.Item name="workDate" label="日付" rules={[{ required: true }]}><DatePicker style={{ width: '100%' }} /></Form.Item>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0 16px' }}>
          <Form.Item name="clockIn" label="出勤打刻"><TimePicker format="HH:mm" style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="clockOut" label="退勤打刻"><TimePicker format="HH:mm" style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="totalHours" label="総労働(h)"><Input type="number" step={0.5} min={0} max={24} disabled placeholder="打刻から自動計算" /></Form.Item></div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
          <Form.Item name="workHours" label="勤務時間(h)" rules={[{ required: true }]}><Input type="number" step={0.5} min={0} max={24} /></Form.Item>
          <Form.Item name="overtimeHours" label="残業時間(h)"><Input type="number" step={0.5} min={0} max={12} /></Form.Item></div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
          <Form.Item name="workType" label="勤務区分" rules={[{ required: true }]}><Select><Option value="NORMAL">通常</Option><Option value="REMOTE">在宅</Option><Option value="HOLIDAY_WORK">休日出勤</Option><Option value="LEAVE">休暇</Option></Select></Form.Item>
          <Form.Item name="status" label="状態" rules={[{ required: true }]}><Select><Option value="出勤">出勤</Option><Option value="欠勤">欠勤</Option><Option value="休暇">休暇</Option></Select></Form.Item></div>
        <Form.Item name="remark" label="備考"><Input.TextArea rows={2} maxLength={500} /></Form.Item>
      </Form>
    </Modal>
  </div>);
}
