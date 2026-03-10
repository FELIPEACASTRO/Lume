import api from './api';
import { CreateTaskRequest, TaskDetailDto, TaskSummaryDto } from '../types';

export const taskService = {
  async findAll(projectId?: string): Promise<TaskSummaryDto[]> {
    const response = await api.get<TaskSummaryDto[]>('/tasks', {
      params: projectId ? { project: projectId } : undefined,
    });
    return response.data;
  },

  async findById(taskId: string): Promise<TaskDetailDto> {
    const response = await api.get<TaskDetailDto>(`/tasks/${taskId}`);
    return response.data;
  },

  async create(request: CreateTaskRequest): Promise<TaskDetailDto> {
    const response = await api.post<TaskDetailDto>('/tasks', request);
    return response.data;
  },
};
