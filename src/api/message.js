import instance from '@/utils/request.js'; // 确保导入正确的实例

export const getMessagePageService = (getMessDTO) => {
  return instance.post("/message/page", getMessDTO);
};

export const markMessagesAsReadBatchService = (messageIds) => {
  return instance.put("/message/read/batch", messageIds);
};

export const deleteMessageService = (messageId) => {
  return instance.delete(`/message/${messageId}`);
};

export const getUnreadCountService = () => {
  return instance.get("/message/unread/count");
};

export const getCountByTypeService = () => {
  return instance.get("/message/count/by-type");
};
    

    