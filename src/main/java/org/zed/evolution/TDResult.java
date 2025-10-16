package org.zed.evolution;

import lombok.Data;

//定一个枚举类型内部类
enum TDValidationStatus {
    SUCCESS,
    PARTIALLY_SUCCESS,
    COUNTER_EXAMPLE,
    UNKNOWN_VARIABLE,
    ABSENCE_EXCEPTION,
    UNEXPECTED_EXCEPTION,
    TESTCASE_GENERATION_FAILED,
    TIMEOUT_ERROR,
    UNEXPECTED_ERROR
}

@Data
public class TDResult {
    private boolean isExceptionTD;
    private TDValidationStatus validationStatus;
    private String validationInfo;
    private String testCase;

    public TDResult(TDValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public TDResult(String validationInfo) {
        this.validationInfo = validationInfo;
    }

    public TDResult(TDValidationStatus tdValidationStatus, String validationInfo) {
        this.validationStatus = tdValidationStatus;
        this.validationInfo = validationInfo;
    }
    public TDResult(TDValidationStatus tdValidationStatus, String validationInfo,String testCase) {
        this.validationStatus = tdValidationStatus;
        this.validationInfo = validationInfo;
        this.testCase = testCase;
    }

    public static TDResult createSuccessResult() {
        return new TDResult(TDValidationStatus.SUCCESS);
    }
    public static TDResult createUnexpectedErrorResult(String validationInfo) {
        return new TDResult(TDValidationStatus.UNEXPECTED_ERROR, validationInfo);
    }
    public static TDResult createTimeoutResult(String validationInfo,String T, String D) {
        return new TDResult(TDValidationStatus.TIMEOUT_ERROR, validationInfo);
    }

    public static TDResult createUnexpectedExceptionResult(String validationInfo) {
        return new TDResult(TDValidationStatus.UNEXPECTED_EXCEPTION, validationInfo);
    }
    public static TDResult createPartiallySuccessResult() {
        return new TDResult(TDValidationStatus.PARTIALLY_SUCCESS);
    }
    public static TDResult createCounterExampleResult(String validationInfo, String testCase) {
        return new TDResult(TDValidationStatus.COUNTER_EXAMPLE, validationInfo, testCase);
    }
    public static TDResult createUnknownVariableResult(String validationInfo) {
        return new TDResult(TDValidationStatus.TESTCASE_GENERATION_FAILED, validationInfo);
    }
}
