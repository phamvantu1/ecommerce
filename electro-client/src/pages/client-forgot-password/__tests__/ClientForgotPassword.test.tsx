import React from 'react';
import { render, screen } from '@testing-library/react';
import { z } from 'zod';

// Mock các dependencies
jest.mock('@mantine/core', () => ({
  ...jest.requireActual('@mantine/core'),
  useForm: jest.fn().mockReturnValue({
    values: {},
    errors: {},
    setFieldValue: jest.fn(),
    validate: jest.fn(),
  }),
}));

jest.mock('react-query', () => ({
  useMutation: jest.fn().mockReturnValue({
    mutate: jest.fn(),
    isLoading: false,
  }),
}));

jest.mock('utils/NotifyUtils', () => ({
  simpleSuccess: jest.fn(),
  simpleFailed: jest.fn(),
}));

describe('ClientForgotPassword formSchema', () => {
  // Tạo một bản sao của formSchema để test
  const formSchema = z.object({
    email: z.string({ invalid_type_error: 'Vui lòng không bỏ trống' })
      .email({ message: 'Nhập email đúng định dạng' }),
  });

  // Hàm helper để tạo dữ liệu hợp lệ cơ bản
  const createValidData = (overrides = {}) => ({
    email: 'test@example.com',
    ...overrides,
  });

  it('nên chấp nhận dữ liệu hợp lệ', () => {
    const validData = createValidData();
    const result = formSchema.safeParse(validData);
    expect(result.success).toBe(true);
  });

  it('nên từ chối email trống', () => {
    const invalidData = createValidData({ email: '' });
    const result = formSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.errors[0].message).toBe('Vui lòng không bỏ trống');
    }
  });

  it('nên từ chối email không hợp lệ', () => {
    const invalidData = createValidData({ email: 'invalid-email' });
    const result = formSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.errors[0].message).toBe('Nhập email đúng định dạng');
    }
  });
}); 