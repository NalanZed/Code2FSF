package org.zed;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.zed.llm.ModelConfig;
import org.zed.log.LogManager;
import org.zed.tcg.TestCaseAutoGenerator;
import org.zed.trans.TransWorker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

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

//    public void testApp2() throws Exception {
//            //进行完整的对5个类别的实验
//        String resourceDatasetDir = "resources/dataset/AllCodes0721";
//        String experimentName = "test0804-conversational";
//        Set<String> categories = LogManager.getCategoriesInDatasetDir(resourceDatasetDir);
//        //如果没有categories,那当前目录名作为一个类别,处理单独一个类别实验时起作用
//        if(categories.isEmpty()){
//            categories.add(resourceDatasetDir.substring(resourceDatasetDir.lastIndexOf("/") + 1));
//            resourceDatasetDir = resourceDatasetDir.substring(0, resourceDatasetDir.lastIndexOf("/"));
//        }
//        ModelConfig modelConfig = new ModelConfig();
//        for (String category : categories) {
//            System.out.println("Start experiment for category: " + category);
//            String experimentDir = LogManager.getExperimentLogPath(experimentName,category);
//            if(Files.exists(Path.of(experimentDir))) {
//                System.out.println("Experiment directory already exists, maybe this task has been done, skip it: " + experimentName + "-" + category);
//                continue;
//            }
//            String SSMPDir = pickSSMPCodes(resourceDatasetDir + "/" + category);
//            runConversationForDir(10, modelConfig, SSMPDir);
//            LogManager.collectExperimentRecords(category,experimentName,modelConfig.getModelName());
//            LogManager.clearCurrentExperimentTmpFiles(experimentName,category, modelConfig.getModelName());
//        }
//    }
//
//    public void testSomeBench() throws Exception {
//            String resourceDir = "resources/dataset/someBench";
//        ModelConfig modelConfig = new ModelConfig();
//        //        ModelConfig modelConfig = new ModelConfig("resources/config/gpt-4o.txt");
//        String SSMPDir = pickSSMPCodes(resourceDir);
//        runConversationForDir(10, modelConfig, SSMPDir);
//    }
//    public void testApp4() throws Exception {
//            String experimentName = "080103-conversational";
//            String category = "Single-path-Loop";
//        String resourceDir = "resources/dataset/程序归类_0722/" + category;
//        ModelConfig modelConfig = new ModelConfig();
//        //        ModelConfig modelConfig = new ModelConfig("resources/config/gpt-4o.txt");
//        String SSMPDir = pickSSMPCodes(resourceDir);
//        runConversationForDir(10, modelConfig, SSMPDir);
//        LogManager.collectExperimentRecords(category,experimentName,"deepseek-chat");
//        LogManager.deleteAllJavaFilesInDir("resources/failedDataset");
//        LogManager.deleteAllJavaFilesInDir("resources/succDataset");
//        Files.delete(Path.of("resources/log/" + modelConfig.getModelName()));
//    }

//    public void testSubstituteConstantInFSF() throws Exception {
//            String logPath = "resources/log/deepseek-chat/log-AltitudeController_Mutant4.txt";
//            List<String[]> FSF = LogManager.getLastestFSFFromLog(logPath);
//            TestCaseAutoGenerator.substituteConstantValueInFSF(FSF);
//            String pureProgram = LogManager.file2String("resources/dataset/someBench/AltitudeController_Mutant4.java");
//            String ssmp = TransWorker.trans2SSMP(pureProgram);
//            List<String> historyTestcases = new ArrayList<>();
//            List<Result> results = new ArrayList<>();
//            int count = 0;
//        for(String[] td : FSF) {
//            String T = td[0];
//            String D = td[1];
//            String currentTD = "T: " + T + "\n" + "D: " + D;
//            System.out.println(YELLOW + "the current T&D is：" + currentTD + RESET);
//            String validationTDResult = validateATAndD(ssmp, T, D, 10,
//                    historyTestcases, results);
//            if(validationTDResult.equals("SUCCESS") || validationTDResult.equals("PARTIALLY SUCCESS")){
//                continue;
//            }
//            if(validationTDResult.equals("ERROR")){
//                String errorInfo = "Some error unhandled happened in validation stage，please check the log!";
//                System.err.println(currentTD + "\t" + errorInfo);
//                LogManager.appendCode2FSFRemark(logPath,"Validation FAIL--" + errorInfo +
//                        "\n" + "Current conversation round is: [" + count + "]");
//            }
//        }
//    }
}
