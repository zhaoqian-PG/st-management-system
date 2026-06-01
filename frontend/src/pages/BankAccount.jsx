import React, { useState, useEffect, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, Select, Space, message,
  Card, Tag,
} from 'antd';
import {
  PlusOutlined, EyeOutlined, EyeInvisibleOutlined,
  EditOutlined, DeleteOutlined, SafetyOutlined,
} from '@ant-design/icons';
import { bankAccountApi } from '../services/bankAccountApi';

const { Option } = Select;

const PAGE_SIZE = 10;

export default function BankAccount() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);
  const [formLoading, setFormLoading] = useState(false);
  const [revealedAccounts, setRevealedAccounts] = useState({});
  const [revealTimers, setRevealTimers] = useState({});
  const [form] = Form.useForm();

  // Fetch bank account list
  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await bankAccountApi.list({ page: page - 1, size: PAGE_SIZE });
      const result = res.data.data;
      setData(result.content || []);
      setTotal(result.totalElements || 0);
    } catch (err) {
      message.error('データの取得に失敗しました');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Open modal for create
  const handleCreate = () => {
    setEditingRecord(null);
    form.resetFields();
    setModalVisible(true);
  };

  // Open modal for edit
  const handleEdit = (record) => {
    setEditingRecord(record);
    form.setFieldsValue({
      branchCode: record.branchCode,
      bankName: record.bankName,
      accountType: record.accountType,
      accountNumber: record.accountNumber,
      accountHolder: record.accountHolder,
    });
    setModalVisible(true);
  };

  // Submit form
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setFormLoading(true);
      if (editingRecord) {
        await bankAccountApi.update(editingRecord.id, values);
        message.success('銀行口座を更新しました');
      } else {
        await bankAccountApi.create(values);
        message.success('銀行口座を登録しました');
      }
      setModalVisible(false);
      fetchData();
    } catch (err) {
      if (err.response?.data?.error) {
        message.error(err.response.data.error);
      } else if (err.response?.data?.details) {
        message.error(err.response.data.details.join(', '));
      }
    } finally {
      setFormLoading(false);
    }
  };

  // Delete (centered confirmation)
  const handleDeleteConfirm = (record) => {
    Modal.confirm({
      title: '削除確認',
      content: `取引番号 ${record.torihikiNo} の銀行口座を削除してもよろしいですか？\nこの操作は取り消せません。`,
      okText: '削除',
      cancelText: 'キャンセル',
      okType: 'danger',
      centered: true,
      onOk: async () => {
        try {
          await bankAccountApi.delete(record.id);
          message.success('銀行口座を削除しました');
          fetchData();
        } catch (err) {
          message.error(err.response?.data?.error || '削除に失敗しました');
        }
      },
    });
  };

  // Toggle masked account number
  const toggleReveal = (id, fullNumber) => {
    if (revealedAccounts[id]) {
      const newRevealed = { ...revealedAccounts };
      delete newRevealed[id];
      setRevealedAccounts(newRevealed);
      if (revealTimers[id]) {
        clearTimeout(revealTimers[id]);
        const newTimers = { ...revealTimers };
        delete newTimers[id];
        setRevealTimers(newTimers);
      }
    } else {
      Modal.confirm({
        title: '口座番号を表示',
        content: '口座番号を表示します。この操作はログに記録されます。続行しますか？',
        okText: '表示',
        cancelText: 'キャンセル',
        centered: true,
        onOk: () => {
          setRevealedAccounts({ ...revealedAccounts, [id]: true });
          const timer = setTimeout(() => {
            setRevealedAccounts((prev) => {
              const next = { ...prev };
              delete next[id];
              return next;
            });
          }, 30000);
          setRevealTimers((prev) => ({ ...prev, [id]: timer }));
        },
      });
    }
  };

  // Render masked account number
  const renderAccountNumber = (text, record) => {
    const isRevealed = revealedAccounts[record.id];
    const masked = '****' + (text ? text.slice(-4) : '****');
    return (
      <Space size={4}>
        <span style={{ fontFamily: 'monospace', letterSpacing: 1 }}>
          {isRevealed ? text : masked}
        </span>
        <Button
          type="link"
          size="small"
          icon={isRevealed ? <EyeInvisibleOutlined /> : <EyeOutlined />}
          onClick={() => toggleReveal(record.id, text)}
          style={{ fontSize: 11 }}
        >
          {isRevealed ? '隠す' : '表示'}
        </Button>
      </Space>
    );
  };

  const columns = [
    {
      title: '取引番号',
      dataIndex: 'torihikiNo',
      key: 'torihikiNo',
      width: 120,
      render: (text) => <strong>{text}</strong>,
    },
    {
      title: '支店番号',
      dataIndex: 'branchCode',
      key: 'branchCode',
      width: 90,
    },
    {
      title: '銀行名称',
      dataIndex: 'bankName',
      key: 'bankName',
      width: 150,
    },
    {
      title: '口座種類',
      dataIndex: 'accountType',
      key: 'accountType',
      width: 90,
      render: (text) => (
        <Tag color={text === '普通' ? 'blue' : 'orange'}>{text}</Tag>
      ),
    },
    {
      title: '口座番号',
      dataIndex: 'accountNumber',
      key: 'accountNumber',
      width: 200,
      render: renderAccountNumber,
    },
    {
      title: '口座名義',
      dataIndex: 'accountHolder',
      key: 'accountHolder',
      width: 180,
    },
    {
      title: '操作',
      key: 'actions',
      width: 140,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            編集
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDeleteConfirm(record)}
          >
            削除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 20 }}>
        <SafetyOutlined /> 銀行口座管理
      </h2>

      <Card
        title="銀行口座一覧"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新規登録
          </Button>
        }
      >
        <Table
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1100 }}
          pagination={{
            current: page,
            pageSize: PAGE_SIZE,
            total,
            showSizeChanger: false,
            showTotal: (t) => `全 ${t} 件`,
            onChange: (p) => setPage(p),
          }}
        />
      </Card>

      {/* Add / Edit Modal */}
      <Modal
        title={editingRecord ? '銀行口座を編集' : '銀行口座を追加'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        confirmLoading={formLoading}
        okText="保存"
        cancelText="キャンセル"
        destroyOnClose
        centered
        width={640}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="bankName"
            label="銀行名称"
            rules={[
              { required: true, message: '銀行名称は必須です' },
              { max: 200, message: '銀行名称は200文字以内で入力してください' },
            ]}
          >
            <Input placeholder="例: 三菱UFJ銀行" maxLength={200} />
          </Form.Item>

          <Form.Item
            name="branchCode"
            label="支店番号"
            rules={[
              { required: true, message: '支店番号は必須です' },
              { max: 10, message: '支店番号は10文字以内で入力してください' },
              {
                pattern: /^[0-9]*$/,
                message: '支店番号は半角数字で入力してください',
              },
            ]}
          >
            <Input placeholder="例: 038" maxLength={10} />
          </Form.Item>

          <Form.Item
            name="accountType"
            label="口座種類"
            rules={[{ required: true, message: '口座種類を選択してください' }]}
          >
            <Select placeholder="選択してください">
              <Option value="普通">普通</Option>
              <Option value="当座">当座</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="accountNumber"
            label="口座番号"
            rules={[
              { required: true, message: '口座番号は必須です' },
              { max: 50, message: '口座番号は50文字以内で入力してください' },
              {
                pattern: /^[0-9]+$/,
                message: '口座番号は半角数字で入力してください',
              },
            ]}
          >
            <Input.Password
              placeholder="例: 1234567"
              maxLength={50}
              autoComplete="off"
            />
          </Form.Item>

          <Form.Item
            name="accountHolder"
            label="口座名義"
            rules={[
              { required: true, message: '口座名義は必須です' },
              { max: 100, message: '口座名義は100文字以内で入力してください' },
            ]}
          >
            <Input placeholder="例: カ）テクノトウキョウ" maxLength={100} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
