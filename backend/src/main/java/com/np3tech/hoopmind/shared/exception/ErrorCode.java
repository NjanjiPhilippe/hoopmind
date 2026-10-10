package com.np3tech.hoopmind.shared.exception;

public class ErrorCode {
    // Authentication & Authorization
    public static final String INVALID_CREDENTIALS = "AUTH_001";
    public static final String TOKEN_EXPIRED = "AUTH_002";
    public static final String TOKEN_INVALID = "AUTH_003";
    public static final String ACCESS_DENIED = "AUTH_004";
    public static final String USER_DISABLED = "AUTH_005";

    // Resource Not Found
    public static final String USER_NOT_FOUND = "RES_001";
    public static final String TEAM_NOT_FOUND = "RES_002";
    public static final String PLAYER_NOT_FOUND = "RES_003";
    public static final String MATCH_NOT_FOUND = "RES_004";
    public static final String PREDICTION_NOT_FOUND = "RES_005";

    // Validation
    public static final String INVALID_INPUT = "VAL_001";
    public static final String DUPLICATE_ENTRY = "VAL_002";
    public static final String CONSTRAINT_VIOLATION = "VAL_003";
    public static final String MISSING_FIELD = "VAL_004";

    // Business Logic
    public static final String MATCH_ALREADY_PLAYED = "BUS_001";
    public static final String PREDICTION_ALREADY_EXISTS = "BUS_002";
    public static final String INSUFFICIENT_DATA = "BUS_003";
    public static final String INVALID_SEASON = "BUS_004";
    public static final String TEAM_NOT_IN_LEAGUE = "BUS_005";

    // Data Processing
    public static final String DATA_PROCESSING_ERROR = "DATA_001";
    public static final String SPARK_JOB_FAILED = "DATA_002";
    public static final String MODEL_TRAINING_FAILED = "DATA_003";
    public static final String PREDICTION_GENERATION_FAILED = "DATA_004";

    // External Services
    public static final String EXTERNAL_API_ERROR = "EXT_001";
    public static final String EXTERNAL_API_TIMEOUT = "EXT_002";
    public static final String EXTERNAL_API_UNAVAILABLE = "EXT_003";

    // System
    public static final String INTERNAL_SERVER_ERROR = "SYS_001";
    public static final String DATABASE_ERROR = "SYS_002";
    public static final String FILE_PROCESSING_ERROR = "SYS_003";
}
