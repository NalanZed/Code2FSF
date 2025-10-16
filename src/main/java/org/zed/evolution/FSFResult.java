package org.zed.evolution;

import lombok.Data;

//定一个枚举类型内部类
enum FSFValidationStatus {
    SUCCESS,
    UNEXPECTED_ERROR,
    OVERTIME_ERROR,
    TESTCASE_GENERATION_FAILED,
    UNKNOWN_VARIABLE,
    ABSENCE_EXCEPTION,
    UNEXPECTED_EXCEPTION,
    COUNTER_EXAMPLE
}
@Data
public class FSFResult {
    private FSFValidationStatus validationStatus;
    private String validationInfo;
    private String troubleT;
    private String troubleD;
    private String testCase;

    public static FSFResult createUnexpectedErrorResult(TDResult tdResult,String T,String D) {
        FSFResult r = new FSFResult();
        r.setValidationStatus(FSFValidationStatus.UNEXPECTED_ERROR);
        r.setValidationInfo(tdResult.getValidationInfo());
        r.setTroubleT(T);
        r.setTroubleD(D);
        return r;
    }

    public static FSFResult createSuccessResult(){
        FSFResult r = new FSFResult();
        r.setValidationStatus(FSFValidationStatus.SUCCESS);
        return r;
    }

    public static FSFResult createOvertimeErrorResult(TDResult tdResult, String T, String D) {
        FSFResult r = new FSFResult();
        r.setValidationStatus(FSFValidationStatus.OVERTIME_ERROR);
        r.setValidationInfo(tdResult.getValidationInfo());
        r.setTroubleT(T);
        r.setTroubleD(D);
        return r;
    }

    public static FSFResult createTestCaseGenerationFailedResult(TDResult tdResult, String T, String D) {
        FSFResult r = new FSFResult();
        r.setValidationStatus(FSFValidationStatus.TESTCASE_GENERATION_FAILED);
        r.setValidationInfo(tdResult.getValidationInfo());
        r.setTroubleT(T);
        r.setTroubleD(D);
        return r;
    }

    public static FSFResult createUnknownVariableResult(TDResult tdResult, String T, String D) {
        FSFResult r = new FSFResult();
        r.setValidationStatus(FSFValidationStatus.UNKNOWN_VARIABLE);
        r.setValidationInfo(tdResult.getValidationInfo());
        r.setTroubleT(T);
        r.setTroubleD(D);
        return r;
    }

    public static FSFResult createAbsenceExceptionResult(TDResult tdResult, String T, String D) {
        FSFResult r = new FSFResult();
        r.setValidationStatus(FSFValidationStatus.ABSENCE_EXCEPTION);
        r.setValidationInfo(tdResult.getValidationInfo());
        r.setTroubleT(T);
        r.setTroubleD(D);
        return r;
    }

    public static FSFResult createUnexpectedExceptionResult(TDResult tdResult, String T, String D) {
        FSFResult r = new FSFResult();
        r.setValidationStatus(FSFValidationStatus.UNEXPECTED_EXCEPTION);
        r.setValidationInfo(tdResult.getValidationInfo());
        r.setTroubleT(T);
        r.setTroubleD(D);
        return r;
    }

    public static FSFResult createCounterExampleResult(TDResult tdResult, String T, String D) {
        FSFResult r = new FSFResult();
        r.setValidationStatus(FSFValidationStatus.COUNTER_EXAMPLE);
        r.setValidationInfo(tdResult.getValidationInfo());
        r.setTroubleT(T);
        r.setTroubleD(D);
        r.setTestCase(tdResult.getTestCase());
        return r;
    }

}
