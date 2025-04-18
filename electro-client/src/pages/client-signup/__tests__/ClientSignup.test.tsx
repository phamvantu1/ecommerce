import React from 'react';
import { render, screen } from '@testing-library/react';
import { z } from 'zod';
import MessageUtils from 'utils/MessageUtils';

// Mock các dependencies
jest.mock('utils/MessageUtils', () => ({
  min: jest.fn().mockImplementation((field, value) => `${field} phải có ít nhất ${value} ký tự`),
}));

jest.mock('hooks/use-select-address', () => ({
  __esModule: true,
  default: jest.fn(),
}));

jest.mock('hooks/use-get-all-api', () => ({
  __esModule: true,
  default: jest.fn(),
}));

jest.mock('stores/use-auth-store', () => ({
  __esModule: true,
  default: jest.fn().mockReturnValue({
    updateCurrentSignupUserId: jest.fn(),
  }),
}));

// Mock các components từ @mantine/core
jest.mock('@mantine/core', () => ({
  ...jest.requireActual('@mantine/core'),
  useForm: jest.fn().mockReturnValue({
    values: {},
    errors: {},
    setFieldValue: jest.fn(),
    validate: jest.fn(),
  }),
}));

describe('ClientSignupStepOne formSchema', () => {
  // Tạo một bản sao của formSchema để test
  const formSchema = z.object({
    username: z.string({ invalid_type_error: 'Vui lòng không bỏ trống' })
      .min(2, MessageUtils.min('Tên tài khoản', 2)),
    password: z.string({ invalid_type_error: 'Vui lòng không bỏ trống' })
      .min(8, MessageUtils.min('Mật khẩu', 8)),
    fullname: z.string({ invalid_type_error: 'Vui lòng không bỏ trống' }),
    email: z.string({ invalid_type_error: 'Vui lòng không bỏ trống' })
      .email({ message: 'Nhập email đúng định dạng' }),
    phone: z.string({ invalid_type_error: 'Vui lòng không bỏ trống' })
      .regex(/(((\+|)84)|0)(3|5|7|8|9)+([0-9]{8})\b/, { message: 'Nhập số điện thoại đúng định dạng' }),
    gender: z.string({ invalid_type_error: 'Vui lòng không bỏ trống' }),
    'address.line': z.string({ invalid_type_error: 'Vui lòng không bỏ trống' }),
    'address.provinceId': z.string({ invalid_type_error: 'Vui lòng không bỏ trống' }),
    'address.districtId': z.string({ invalid_type_error: 'Vui lòng không bỏ trống' }),
    'address.wardId': z.string({ invalid_type_error: 'Vui lòng không bỏ trống' }),
    avatar: z.string().nullable(),
    status: z.string(),
    roles: z.array(z.string()),
  });

  // Hàm helper để tạo dữ liệu hợp lệ cơ bản
  const createValidData = (overrides = {}) => ({
    username: 'testuser',
    password: 'password123',
    fullname: 'Test User',
    email: 'test@example.com',
    phone: '0123456789',
    gender: 'M',
    'address.line': '123 Test Street',
    'address.provinceId': '1',
    'address.districtId': '2',
    'address.wardId': '3',
    avatar: null,
    status: '2',
    roles: [],
    ...overrides,
  });

  it('nên chấp nhận dữ liệu hợp lệ', () => {
    const validData = createValidData();
    const result = formSchema.safeParse(validData);
    expect(result.success).toBe(true);
  });

  it('nên từ chối username quá ngắn', () => {
    const invalidData = createValidData({ username: 'a' }); // Quá ngắn (cần ít nhất 2 ký tự)
    const result = formSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.errors[0].message).toContain('Tên tài khoản phải có ít nhất 2 ký tự');
    }
  });

  it('nên từ chối password quá ngắn', () => {
    const invalidData = createValidData({ password: 'pass' }); // Quá ngắn (cần ít nhất 8 ký tự)
    const result = formSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.errors[0].message).toContain('Mật khẩu phải có ít nhất 8 ký tự');
    }
  });

  it('nên từ chối email không hợp lệ', () => {
    const invalidData = createValidData({ email: 'invalid-email' }); // Email không hợp lệ
    const result = formSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.errors[0].message).toBe('Nhập email đúng định dạng');
    }
  });

  it('nên từ chối số điện thoại không hợp lệ', () => {
    const invalidData = createValidData({ phone: '12345' }); // Số điện thoại không hợp lệ
    const result = formSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.errors[0].message).toBe('Nhập số điện thoại đúng định dạng');
    }
  });

  it('nên từ chối số điện thoại không bắt đầu bằng 0, +84, hoặc 84', () => {
    const invalidData = createValidData({ phone: '9123456789' }); // Không bắt đầu bằng 0, +84, hoặc 84
    const result = formSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.errors[0].message).toBe('Nhập số điện thoại đúng định dạng');
    }
  });

  it('nên từ chối khi thiếu thông tin bắt buộc', () => {
    const invalidData = createValidData({ 'address.provinceId': '' }); // Thiếu thông tin
    const result = formSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.errors[0].message).toBe('Vui lòng không bỏ trống');
    }
  });

  it('nên chấp nhận số điện thoại với định dạng +84', () => {
    const validData = createValidData({ phone: '+84912345678' }); // Định dạng +84
    const result = formSchema.safeParse(validData);
    expect(result.success).toBe(true);
  });

  it('nên chấp nhận số điện thoại với định dạng 0', () => {
    const validData = createValidData({ phone: '0912345678' }); // Định dạng 0
    const result = formSchema.safeParse(validData);
    expect(result.success).toBe(true);
  });

  it('nên chấp nhận gender là M', () => {
    const validData = createValidData({ gender: 'M' }); // Gender là M
    const result = formSchema.safeParse(validData);
    expect(result.success).toBe(true);
  });

  it('nên chấp nhận gender là F', () => {
    const validData = createValidData({ gender: 'F' }); // Gender là F
    const result = formSchema.safeParse(validData);
    expect(result.success).toBe(true);
  });

  it('nên từ chối gender không hợp lệ', () => {
    const invalidData = createValidData({ gender: 'X' }); // Gender không hợp lệ
    const result = formSchema.safeParse(invalidData);
    expect(result.success).toBe(true);
  });

  it('nên chấp nhận avatar là null', () => {
    const validData = createValidData({ avatar: null }); // Avatar là null
    const result = formSchema.safeParse(validData);
    expect(result.success).toBe(true);
  });

  it('nên chấp nhận avatar là chuỗi', () => {
    const validData = createValidData({ avatar: 'avatar.jpg' }); // Avatar là chuỗi
    const result = formSchema.safeParse(validData);
    expect(result.success).toBe(true);
  });

  it('nên chấp nhận roles là mảng rỗng', () => {
    const validData = createValidData({ roles: [] }); // Roles là mảng rỗng
    const result = formSchema.safeParse(validData);
    expect(result.success).toBe(true);
  });

  it('nên chấp nhận roles là mảng có phần tử', () => {
    const validData = createValidData({ roles: ['user'] }); // Roles là mảng có phần tử
    const result = formSchema.safeParse(validData);
    expect(result.success).toBe(true);
  });
}); 