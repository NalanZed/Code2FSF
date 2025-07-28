package org.zed;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.zed.llm.ModelConfig;
import org.zed.llm.ModelMessage;
import org.zed.log.LogManager;
import org.zed.tcg.ExecutionEnabler;
import org.zed.tcg.TestCaseAutoGenerator;
import org.zed.trans.TransWorker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.zed.FSFGenerator.*;
import static org.zed.trans.TransWorker.pickSSMPCodes;


/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */

        public void testApp(){
            System.out.println("hello world");
        }

//    public void testApp2(){
//        String logDir = "resources/log/deepseek-chat";
//        String[] logFilePaths = LogManager.fetchSuffixFilePathInDir(logDir, ".txt");
//        for(String path : logFilePaths){
//            if(path.contains("summary")) continue;
//            String logName = path.substring(path.lastIndexOf("/"), path.indexOf(".txt"));
//            System.out.println("processing==>" + logName);
//            List<String[]> FSF = LogManager.getLastestFSFFromLog(path);
//            String code = LogManager.getProgramFromLog(path);
//            List<String> vars = fetchUnknownVarInFSF(FSF, code);
//            if(!vars.isEmpty()){
//                vars.stream().forEach(System.out::println);
//                System.err.println(path);
//            }
//        }
//    }

//    public void testApp3(){
//        String path = "resources/log/deepseek-chat/log-MySqrt_Mutant2.txt";
//        List<String[]> FSF = LogManager.getLastestFSFFromLog(path);
//        String code = LogManager.getProgramFromLog(path);
//        List<String> vars = fetchUnknownVarInFSF(FSF, code);
//        if(!vars.isEmpty()){
//            System.err.println(path);
//        }
//    }

    public void testApp4() throws Exception {
//        String resourceDir ="resources/dataset/程序归类_0722/Single-path Loop";
        String resourceDir = "resources/dataset/someBench";
        ModelConfig modelConfig = new ModelConfig();
//        ModelConfig modelConfig = new ModelConfig("resources/config/gpt-4o.txt");
        String SSMPDir = pickSSMPCodes(resourceDir);
        runConversationForDir(10, modelConfig, SSMPDir);
    }
//
//    public void testClassifyProgramOnHasLoopStmt() throws IOException {
//            String dir = "resources/dataset/AllCodes0721";
//            countHasLoopNumAndElseNum(dir);
//    }

//    public void testShot() throws Exception {
//        String resourceDir = "resources/dataset/someBench/";
//        ModelConfig modelConfig = new ModelConfig();
//        String SSMPDir = pickSSMPCodes(resourceDir);
//        runConversationForDir(1, modelConfig, SSMPDir);
//    }

//    public void testMod(){
//            int a = -10 % -11;
//        System.out.println(a);
//    }

    public void testGenerateTc(){
        int currentHeight = -1342177278;
        int targetHeight = 805306370;
        int r = targetHeight - currentHeight ;
//        int A = -Integer.MIN_VALUE;
        System.out.println(r);
    }
    public void testSubstituteConstantInFSF() throws Exception {
            String logPath = "resources/log/deepseek-chat/log-AltitudeController_Mutant4.txt";
            List<String[]> FSF = LogManager.getLastestFSFFromLog(logPath);
            substituteConstantValueInFSF(FSF);
            String pureProgram = LogManager.file2String("resources/dataset/someBench/AltitudeController_Mutant4.java");
            String ssmp = TransWorker.trans2SSMP(pureProgram);
            List<String> historyTestcases = new ArrayList<>();
            List<Result> results = new ArrayList<>();
            int count = 0;
        for(String[] td : FSF) {
            String T = td[0];
            String D = td[1];
            String currentTD = "T: " + T + "\n" + "D: " + D;
            System.out.println(YELLOW + "the current T&D is：" + currentTD + RESET);
            String validationTDResult = validateATAndD(ssmp, T, D, 10,
                    historyTestcases, results);
            if(validationTDResult.equals("SUCCESS") || validationTDResult.equals("PARTIALLY SUCCESS")){
                continue;
            }
            if(validationTDResult.equals("ERROR")){
                String errorInfo = "Some error unhandled happened in validation stage，please check the log!";
                System.err.println(currentTD + "\t" + errorInfo);
                LogManager.appendCode2FSFRemark(logPath,"Validation FAIL--" + errorInfo +
                        "\n" + "Current conversation round is: [" + count + "]");
            }
        }
        }

}
