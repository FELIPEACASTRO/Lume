import api from './api';
import { ProjectDto } from '../types';

export const projectService = {
  async findAll(): Promise<ProjectDto[]> {
    const response = await api.get<ProjectDto[]>('/projects');
    return response.data;
  },
};
