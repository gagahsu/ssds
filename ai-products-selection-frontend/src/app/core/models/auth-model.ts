export type UserRole =
  | 'BUYER'        // 採購專員
  | 'BUYER_LEAD'   // 採購主管
  | 'DATA_ADMIN'   // 資料管理員
  | 'SYS_ADMIN'    // 系統管理員
  | 'VIEWER';      // 唯讀觀察者

export const USER_ROLE_LABELS: Record<UserRole, string> = {
  BUYER: '採購專員',
  BUYER_LEAD: '採購主管',
  DATA_ADMIN: '資料管理員',
  SYS_ADMIN: '系統管理員',
  VIEWER: '唯讀觀察者',
};

export interface LoginRequest {
  email: string;
  password: string;
}

/** 對應後端 com.example.ssds.core.dto.TokenPair */
export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

/** 對應後端 com.example.ssds.core.dto.AuthenticatedUser（/auth/me 回傳） */
export interface AuthenticatedUser {
  id: number;
  email: string;
  displayName: string;
  roles: UserRole[];
}

/** 對應後端 com.example.ssds.core.dto.LoginResult（/auth/login 回傳） */
export interface LoginResult {
  tokens: TokenPair;
  user: AuthenticatedUser;
}

/** 對應後端 com.example.ssds.api.common.response.ApiResponse<T> 信封 */
export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  error: { code: string; message: string } | null;
  timestamp: string;
}
