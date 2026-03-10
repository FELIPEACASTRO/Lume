import api from './api';
import { Page, User, UserCreateRequest, UserUpdateRequest } from '../types';

const BASE_PATH = '/users';

export const userService = {
  async findAll(page = 0, size = 20): Promise<Page<User>> {
    const response = await api.get<Page<User>>(BASE_PATH, {
      params: { page, size },
    });
    return response.data;
  },

  async findById(id: number): Promise<User> {
    const response = await api.get<User>(`${BASE_PATH}/${id}`);
    return response.data;
  },

  async create(data: UserCreateRequest): Promise<User> {
    const response = await api.post<User>(BASE_PATH, data);
    return response.data;
  },

  async update(id: number, data: UserUpdateRequest): Promise<User> {
    const response = await api.put<User>(`${BASE_PATH}/${id}`, data);
    return response.data;
  },

  async delete(id: number): Promise<void> {
    await api.delete(`${BASE_PATH}/${id}`);
  },
};
