import api from './api';
import { NotificationDto } from '../types';

export const notificationService = {
  async findAll(): Promise<NotificationDto[]> {
    const response = await api.get<NotificationDto[]>('/notifications');
    return response.data;
  },
};
