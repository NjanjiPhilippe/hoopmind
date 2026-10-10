export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: Date;
}

export interface ErrorResponse {
  errorCode: string;
  message: string;
  status: number;
  timestamp: Date;
  path: string;
  validationErrors?: ValidationError[];
}

export interface ValidationError {
  field: string;
  message: string;
}