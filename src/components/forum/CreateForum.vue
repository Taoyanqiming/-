<template>
  <div class="create-post-container">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
      <!-- 帖子标题 -->
      <el-form-item label="帖子标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入帖子标题" maxlength="50" show-word-limit />
      </el-form-item>

      <!-- 帖子内容 -->
      <el-form-item label="帖子内容" prop="content">
        <el-input v-model="form.content" type="textarea" placeholder="请输入帖子内容" :rows="6" maxlength="2000"
          show-word-limit />
      </el-form-item>

      <!-- 图片上传 -->
      <el-form-item label="上传图片">
        <div class="custom-upload-container">
          <!-- 原生文件上传控件 -->
          <input ref="fileInputRef" type="file" multiple accept="image/jpeg,image/png,image/gif,image/bmp"
            @change="handleFileChange" class="hidden-input" />

          <!-- 自定义上传按钮 -->
          <div class="upload-button" @click="openFileDialog">
            <i class="el-icon-plus"></i>
            <span>点击上传图片</span>
          </div>

          <!-- 图片预览区域 -->
          <div class="preview-container">
            <div v-for="(image, index) in fileList" :key="index" class="preview-item">
              <img :src="image.url" alt="预览图" class="preview-image" />
              <div class="preview-remove" @click="removeImage(index)">
                <i class="el-icon-delete"></i>
              </div>
            </div>
          </div>

          <!-- 图片数量提示 -->
          <div class="image-count" v-if="fileList.length > 0">
            已选择 {{ fileList.length }} 张图片，最多上传 9 张
          </div>
        </div>
      </el-form-item>

      <!-- 提交按钮 -->
      <el-form-item>
        <el-button type="primary" @click="submitForm" :loading="isSubmitting">发布帖子</el-button>
        <el-button @click="resetForm">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter, useRoute } from 'vue-router';
import { addPostService, uploadImagesService } from '@/api/forum.js';

// 表单数据
const form = reactive({
  title: '',
  content: '',
  images: []
});

// 表单验证规则
const rules = reactive({
  title: [
    { required: true, message: '请输入帖子标题', trigger: 'blur' },
    { min: 2, max: 50, message: '标题长度在2-50个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入帖子内容', trigger: 'blur' },
    { min: 5, message: '内容至少5个字符', trigger: 'blur' }
  ]
});

// 表单引用
const formRef = ref(null);

// 状态管理
const isSubmitting = ref(false);

// 文件上传相关
const fileInputRef = ref(null);
const fileList = ref([]);
const MAX_IMAGES = 9;
const router = useRouter();
const route = useRoute();
// 打开文件选择对话框
const openFileDialog = () => {
  fileInputRef.value.click();
};

// 处理文件变化
const handleFileChange = async (e) => {
  const files = e.target.files;
  if (!files || files.length === 0) return;

  // 检查文件数量限制
  if (fileList.value.length + files.length > MAX_IMAGES) {
    ElMessage.warning(`最多只能上传${MAX_IMAGES}张图片`);
    return;
  }

  try {
    // 处理每个文件
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      console.log("【图片上传】开始处理文件:", file.name);
      console.log("【图片上传】文件类型:", file.type);
      console.log("【图片上传】文件大小:", file.size / 1024 / 1024, "MB");

      // 验证文件类型和大小
      const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/bmp'];
      const isAllowedType = allowedTypes.includes(file.type);
      const isLt5M = file.size / 1024 / 1024 < 5;

      if (!isAllowedType) {
        ElMessage.error('仅支持JPG、PNG、GIF、BMP格式的图片');
        continue;
      }
      if (!isLt5M) {
        ElMessage.error('图片大小不能超过5MB');
        continue;
      }

      // 生成预览URL
      const url = URL.createObjectURL(file);

      // 添加到文件列表
      fileList.value.push({
        url,
        raw: file,
        name: file.name
      });

      console.log("【图片上传】成功添加到fileList，当前数量:", fileList.value.length);
    }

    // 清空文件输入，允许重复选择相同文件
    fileInputRef.value.value = '';

  } catch (error) {
    console.error("【图片上传】处理失败:", error);
    ElMessage.error("图片处理失败，请重试");
  }
};

// 移除图片
const removeImage = (index) => {
  const image = fileList.value[index];
  console.log("【移除图片】移除文件:", image.name);

  // 释放URL对象
  URL.revokeObjectURL(image.url);

  // 从列表中移除
  fileList.value.splice(index, 1);
  console.log("【移除图片】剩余文件数量:", fileList.value.length);
};

// 提交表单
const submitForm = async () => {
  // 表单验证
  await formRef.value.validate((valid) => {
    if (!valid) return false;
  });

  console.log("【表单提交】fileList内容:", fileList.value);
  const images = fileList.value.map(item => item.raw);
  console.log("【表单提交】提取的图片文件数量:", images.length);

  // 如果没有图片，直接提示错误
  if (!images || images.length === 0) {
    ElMessage.warning('请上传至少一张图片');
    return;
  }

  isSubmitting.value = true;
  try {
    // 1. 先发布帖子，获取postId
    const postDTO = {
      title: form.title,
      content: form.content
    };

    const postResponse = await addPostService(postDTO);
    if (postResponse.data.code !== 1) {
      ElMessage.error(postResponse.data.message || '发布帖子失败');
      return;
    }

    const postId = postResponse.data.data;
    console.log('发布帖子成功，帖子ID:', postId);

    // 2. 上传图片
    const uploadResponse = await uploadImagesService(postId, images);
    console.log('上传图片响应:', uploadResponse);
    if (uploadResponse.data.code !== 1) {
      ElMessage.error(uploadResponse.data.message || '上传图片失败');
      return;
    }

    // 3. 两个接口都成功，提示并跳转
    ElMessage.success('帖子发布成功');
    router.push({
      name: 'UserForumDetail',
      params: { postId }
    });

  } catch (error) {
    console.error('发布帖子失败:', error);
    ElMessage.error('网络错误，请稍后重试');
  } finally {
    isSubmitting.value = false;
  }
};

// 重置表单
const resetForm = () => {
  formRef.value.resetFields();

  // 释放所有URL对象
  fileList.value.forEach(image => {
    URL.revokeObjectURL(image.url);
  });

  // 清空文件列表
  fileList.value = [];
  form.images = [];
};

onMounted(() => {
  console.log("【组件初始化】帖子发布页面加载完成");
});

onUnmounted(() => {
  // 释放所有URL对象
  fileList.value.forEach(image => {
    URL.revokeObjectURL(image.url);
  });
  fileList.value = [];
});
</script>

<style scoped>
.create-post-container {
  max-width: 800px;
  margin: 30px auto;
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.hidden-input {
  display: none;
}

.upload-button {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  margin-right: 10px;
  margin-bottom: 10px;
}

.upload-button:hover {
  border-color: #409eff;
  color: #409eff;
}

.upload-button i {
  font-size: 28px;
  margin-bottom: 8px;
}

.preview-container {
  display: flex;
  flex-wrap: wrap;
  margin-top: 10px;
}

.preview-item {
  position: relative;
  width: 100px;
  height: 100px;
  margin-right: 10px;
  margin-bottom: 10px;
  border-radius: 4px;
  overflow: hidden;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-remove {
  position: absolute;
  top: 0;
  right: 0;
  width: 20px;
  height: 20px;
  background-color: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  opacity: 0;
}

.preview-item:hover .preview-remove {
  opacity: 1;
}

.image-count {
  margin-top: 10px;
  color: #606266;
  font-size: 12px;
}
</style>
