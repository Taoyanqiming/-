import instance from '@/utils/request.js'; // 确保导入正确的实例

// 1. 用户提交预约申请
export const submitAppointmentService = (appointmentDTO) => {
  return instance.post("/appoint/submit", appointmentDTO);
};

// 2. 修改预约信息
export const updateAppointmentService = (appointmentDTO) => {
  return instance.put("/appoint/update", appointmentDTO);
};

// 3. 管理员审核预约
export const approveAppointmentService = (appointmentId, status) => {
  return instance.put(`/appoint/approve/${appointmentId}/${status}`);
};

// 4. 根据条件查询预约记录
export const getAppointmentListService = (appointmentDTO) => {
  return instance.post("/appoint/list",appointmentDTO);
};

// 5. 根据ID查询预约
export const getAppointmentByIdService = (appointmentId) => {
  return instance.get(`/appoint/${appointmentId}`);
};
    