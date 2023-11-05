import React, {useState, useEffect} from 'react';
import {UploadOutlined} from '@ant-design/icons';
import {Button, message, Upload} from 'antd';
import ProForm, {
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
} from '@ant-design/pro-form';
import {useRequest} from 'umi';
import {currentUser, updateLoginUser, uploadAvatar} from '@/services/ant-design-pro/api'; // 引入uploadAvatar函数
import styles from './BaseView.less';


const beforeUpload = (file: { type: string; size: number; }) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
  if (!isJpgOrPng) {
    message.error('只能上传 JPG/PNG 格式的图片！');
    return false;
  }
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) {
    message.error('图片必须小于 2MB！');
    return false;
  }
  message.success('头像修改成功');
  return true;
};
// 头像组件 方便以后独立，增加裁剪之类的功能
const AvatarView = ({avatar}) => (
  <>
    <div className={styles.avatar_title}>头像</div>
    <div className={styles.avatar}>
      <img src={avatar} alt="avatar"/>
    </div>
    <Upload
      showUploadList={false}
      beforeUpload={beforeUpload}
      customRequest={({file}) => uploadAvatar(file)} // 将上传的文件对象作为参数传递给uploadAvatar函数
    >
      <div className={styles.button_view}>
        <Button>
          <UploadOutlined/>
          更换头像
        </Button>
      </div>
    </Upload>
  </>
);

const BaseView = () => {
  const {data: loginUser, loading} = useRequest(async () => {
    return await currentUser();
  }, {formatResult: (res) => res});

  //没有头像时，默认显示的头像
  const [avatarUrl, setAvatarUrl] = useState('https://web-frame-job01.oss-cn-beijing.aliyuncs.com/70e235a9-d644-4513-82be-a12b8245369a.png');

  useEffect(() => {
    if (loginUser && loginUser.avatarUrl) {
      setAvatarUrl(loginUser.avatarUrl);
    }
  }, [loginUser]);

  const handleFinish = async (fields) => {
    const hide = message.loading('正在修改');
    try {
      await updateLoginUser({
        id: fields.id ?? 0,
        ...fields,
      });
      hide();
      message.success('修改成功');
      return true;
    } catch (error) {
      hide();
      message.error('修改失败请重试！');
      return false;
    }
  };

  return (
    <div className={styles.baseView}>
      {loading ? null : (
        <>
          <div className={styles.left}>
            <ProForm
              layout="vertical"
              onFinish={handleFinish}
              submitter={{
                resetButtonProps: {
                  style: {
                    display: 'none',
                  },
                },
                submitButtonProps: {
                  children: '更新基本信息',
                },
              }}
              initialValues={{
                ...loginUser,
              }}
            >
              <ProFormText
                width="md"
                name="username"
                label="昵称"
                rules={[
                  {
                    required: true,
                    message: '请输入您的昵称!',
                  },
                ]}
              />
              <ProFormSelect
                width="sm"
                name="gender"
                label="性别"
                rules={[
                  {
                    required: true,
                    message: '请选择您的性别!',
                  },
                ]}
                options={[
                  {
                    label: '男',
                    value: 0,
                  }, {
                    label: '女',
                    value: 1,
                  },
                ]}
                valueEnum={{
                  0: '男',
                  1: '女',
                }}
              />
              <ProFormSelect
                width="sm"
                name="age"
                label="年龄"
                rules={[
                  {
                    required: true,
                    message: '请选择您的年龄!',
                  },
                ]}
                options={Array.from({length: 101}, (_, index) => ({
                  label: String(index),
                  value: String(index),
                }))}
              />
              <ProFormText
                width="md"
                name="phone"
                label="联系电话"
                placeholder="请输入您的联系电话"
                rules={[
                  {
                    required: false,
                    message: '请输入您的联系电话!',
                  },
                ]}
              />
              <ProFormText
                width="md"
                name="email"
                label="联系邮箱"
                placeholder="请输入您的联系邮箱"
                rules={[
                  {
                    required: false,
                    message: '请输入您的联系邮箱!',
                  },
                ]}
              />
              <ProFormTextArea
                name="introduction"
                label="个人简介"
                rules={[
                  {
                    required: false,
                    message: '请用几句语言简单介绍自己!',
                  },
                ]}
                placeholder="请用几句语言简单介绍自己!"
              />
            </ProForm>
          </div>
          <div className={styles.right}>
            <AvatarView avatar={avatarUrl}/>
          </div>
        </>
      )}
    </div>
  );
};

export default BaseView;
