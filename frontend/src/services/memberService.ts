import api from './api';
import { User, UserCreateRequest, UserUpdateRequest } from '../types';

const BASE_PATH = '/api/v1/members';

export const memberService = {
  async findAll(): Promise<User[]> {
    const response = await api.get<User[]>(BASE_PATH);
    return response.data.map((member) => ({
      ...member,
      id: member.userId ?? member.id,
      membershipId: member.id,
      updatedAt: member.updatedAt ?? member.createdAt,
    }));
  },

  async create(data: UserCreateRequest): Promise<User> {
    const response = await api.post<User>(BASE_PATH, data);
    return {
      ...response.data,
      id: response.data.userId ?? response.data.id,
      membershipId: response.data.id,
      updatedAt: response.data.updatedAt ?? response.data.createdAt,
    };
  },

  async update(membershipId: number, data: UserUpdateRequest): Promise<User> {
    const response = await api.patch<User>(`${BASE_PATH}/${membershipId}`, data);
    return {
      ...response.data,
      id: response.data.userId ?? response.data.id,
      membershipId: response.data.id,
      updatedAt: response.data.updatedAt ?? response.data.createdAt,
    };
  },
};
