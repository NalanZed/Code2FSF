package org.zed;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.zed.evolution.CodeGenerator;
import org.zed.llm.ModelConfig;
import org.zed.log.LogManager;

import java.io.IOException;

import static org.zed.verification.FSFGenerator.*;
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
    public void testApp4() throws Exception {
            String experimentName = "tset";
        String resourceDir = "resources/dataset/someBench/";
        ModelConfig modelConfig = ModelConfig.CreateChatGptModel();
        //        ModelConfig modelConfig = new ModelConfig("resources/config/gpt-4o.txt");
        String SSMPDir = pickSSMPCodes(resourceDir);
        runConversationForDir(10, modelConfig, SSMPDir);
        LogManager.collectExperimentRecords("test",experimentName,"deepseek-chat");
    }

}
