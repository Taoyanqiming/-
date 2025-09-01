<template>
  <div class="create-product-container">
    <div class="left-upload">
      <input ref="fileInputRef" type="file" accept="image/*" @change="handleFileChange" class="hidden-input" />
      <div class="upload-area" @click="openFileDialog">
        <img v-if="imagePreview" :src="imagePreview" class="preview-image" />
        <div v-else class="upload-placeholder">点击上传商品图片</div>
      </div>
    </div>
    <div class="right-form">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="商品名称" prop="productName">
          <el-input v-model="form.productName" />
        </el-form-item>
        <el-form-item label="价格" prop="productPrice">
          <el-input v-model.number="form.productPrice" type="number" />
        </el-form-item>
        <el-form-item label="库存" prop="productStock">
          <el-input v-model.number="form.productStock" type="number" />
        </el-form-item>
        <el-form-item label="限购数量" prop="productLimit">
          <el-input v-model.number="form.productLimit" type="number" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="isSubmitting">发布商品</el-button>
          <el-button @click="openSeckillDialog">设置秒杀</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 秒杀设置弹窗（字段名与模型统一） -->
    <el-dialog v-model="seckillDialogVisible" title="设置限时秒杀" width="400px">
      <el-form :model="seckillForm" :rules="seckillRules" ref="seckillFormRef" label-width="90px">
        <el-form-item label="秒杀开始" prop="seckillStartTime">
          <el-date-picker v-model="seckillForm.seckillStartTime" type="datetime" placeholder="选择开始时间" />
        </el-form-item>
        <el-form-item label="秒杀结束" prop="seckillEndTime">
          <el-date-picker v-model="seckillForm.seckillEndTime" type="datetime" placeholder="选择结束时间" />
        </el-form-item>
        <el-form-item label="秒杀库存" prop="seckillStock">
          <el-input v-model.number="seckillForm.seckillStock" type="number" />
        </el-form-item>
        <el-form-item label="限购数量" prop="seckillLimit">
          <el-input v-model.number="seckillForm.seckillLimit" type="number" />
        </el-form-item>
        <el-form-item label="秒杀价" prop="seckillPrice">
          <el-input v-model.number="seckillForm.seckillPrice" type="number" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="seckillForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="seckillDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSeckill" :loading="isSeckillSubmitting">设置秒杀</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { createProductService, uploadAvatarService, setSeckillProductService } from '@/api/mall.js';
import { useRouter } from 'vue-router';
const router = useRouter();

// 普通商品表单数据
const form = reactive({
  productName: '',
  productPrice: '',
  productStock: '',
  productLimit: '',
  description: ''
});

// 表单验证规则
const rules = {
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  productPrice: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  productStock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  productLimit: [{ required: true, message: '请输入限购数量', trigger: 'blur' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }]
};
const formRef = ref(null);

// 图片上传相关
const fileInputRef = ref(null);
const imageFile = ref(null);
const imagePreview = ref('');
const productImageUrl = ref('');

// 加载状态
const isSubmitting = ref(false);
const productId = ref(null);

// 商品信息（用于秒杀表单填充）
const productInfo = ref({
  productId: null,
  productName: '',
  productLimit: '',
  productImageUrl: ''
});

// 秒杀表单数据（字段名与表单绑定一致）
const seckillDialogVisible = ref(false);
const isSeckillSubmitting = ref(false);
const seckillForm = reactive({
  seckillStartTime: '',
  seckillEndTime: '',
  seckillLimit: '',
  seckillPrice: '',
  description: '',
  seckillProductImage: ''
});
const seckillRules = {
  seckillStartTime: [{ required: true, message: '请选择开始时间', trigger: 'blur' }],
  seckillEndTime: [{ required: true, message: '请选择结束时间', trigger: 'blur' }],
  seckillLimit: [{ required: true, message: '请输入限购数量', trigger: 'blur' }],
  seckillPrice: [{ required: true, message: '请输入秒杀价', trigger: 'blur' }]
};
const seckillFormRef = ref(null);

// 打开文件选择对话框
const openFileDialog = () => {
  fileInputRef.value.click();
};

// 处理文件选择
const handleFileChange = (e) => {
  const file = e.target.files[0];
  if (!file) return;
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件');
    return;
  }
  imageFile.value = file;
  imagePreview.value = URL.createObjectURL(file);
};

// 提交创建商品表单
const submitForm = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    if (!imageFile.value) {
      ElMessage.warning('请上传商品图片');
      return;
    }
    isSubmitting.value = true;
    try {
      // 创建商品
      const createRes = await createProductService({ ...form });
      if (createRes.data.code !== 1) {
        ElMessage.error(createRes.data.message || '发布商品失败');
        return;
      }

      // 保存商品ID和基础信息
      productId.value = createRes.data.data.productId;
      productInfo.value = {
        productId: productId.value,
        productName: form.productName,
        productLimit: form.productLimit,
        productImageUrl: ''
      };

      // 上传图片
      const uploadRes = await uploadAvatarService(imageFile.value, productId.value);
      if (uploadRes.data.code !== 1) {
        ElMessage.error(uploadRes.data.message || '图片上传失败');
        return;
      }

      // 保存图片URL
      productImageUrl.value = uploadRes.data.data.imageUrl;
      productInfo.value.productImageUrl = productImageUrl.value;

      ElMessage.success('商品发布成功');

      resetForm();

    } catch (err) {
      ElMessage.error('网络错误，请稍后重试');
    } finally {
      isSubmitting.value = false;
    }
  });
};

// 重置表单
const resetForm = () => {
  formRef.value.resetFields();
  imageFile.value = null;
  if (imagePreview.value) {
    URL.revokeObjectURL(imagePreview.value);
    imagePreview.value = '';
  }

};

// 打开秒杀设置对话框
const openSeckillDialog = () => {
  if (!productId.value) {
    ElMessage.warning('请先发布商品');
    return;
  }

  // 自动填充秒杀表单
  seckillForm.seckillPrice = form.productPrice;
  seckillForm.seckillLimit = form.productLimit;
  seckillForm.description = form.description;
  seckillForm.seckillProductImage = productImageUrl.value;

  seckillDialogVisible.value = true;
};

// 提交秒杀设置
const submitSeckill = async () => {
  await seckillFormRef.value.validate(async (valid) => {
    if (!valid) return;
    isSeckillSubmitting.value = true;
    try {
      // 构造秒杀设置DTO
      const seckSetDTO = {
        productId: productId.value,
        seckillStartTime: seckillForm.seckillStartTime,
        seckillEndTime: seckillForm.seckillEndTime,
        seckillLimit: seckillForm.seckillLimit,
        seckillPrice: seckillForm.seckillPrice,
        seckillStock: seckillForm.seckillStock,
        description: seckillForm.description,
        seckillImageUrl: seckillForm.seckillProductImage
      };

      const res = await setSeckillProductService(seckSetDTO);
      console.log('设置秒杀返回结果:', res);
      if (res.data.code !== 1) {
        ElMessage.error(res.data.message || '设置秒杀失败');
        return;
      }

      ElMessage.success('设置秒杀成功');
      seckillDialogVisible.value = false;

      // 跳转到秒杀详情页
      const seckillId = res.data.data;
      router.push({
        name: 'SeckListDetail',
        params: { seckillId }
      });

    } catch (err) {
      ElMessage.error('网络错误，请稍后重试');
    } finally {
      isSeckillSubmitting.value = false;
    }
  });
};
</script>

<style scoped>
/* 样式代码与原代码一致，此处省略 */
</style>