package org.zed;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.zed.llm.ModelConfig;
import org.zed.llm.ModelMessage;
import org.zed.log.LogManager;
import org.zed.trans.TransWorker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static org.zed.FSFGenerator.*;
import static org.zed.llm.ModelConfig.CreateChatGptModel;
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
//    public void testApp3() throws Exception {
//        String resourceDir = "resources/dataset/SpecGenBench/";
//        String testFileName = "Conjunction/Conjunction";
//        String testFileNameJava = testFileName+".java";
//        String filePath = resourceDir + testFileNameJava;
//        ModelConfig modelConfig = new ModelConfig();
//        try {
//            runConversations(1, modelConfig, filePath);
//        }
//        catch (Exception e) {
//            System.err.println("Error during runConversations: " + e.getMessage());
//            //把验证失败的代码保存到文件中
//            Files.copy(Path.of(filePath),Path.of("resources/failedDataset/"+testFileNameJava), StandardCopyOption.REPLACE_EXISTING);
//            e.printStackTrace();
//        }
//    }
    public void testApp4() throws Exception {
//        String resourceDir = "resources/dataset/SpecGenBench/";
        String resourceDir ="resources/dataset/AllCode2PartA";
        ModelConfig modelConfig = new ModelConfig();
//        ModelConfig modelConfig = CreateChatGptModel();
        String SSMPDir = pickSSMPCodes(resourceDir);
        runConversationForDir(5, modelConfig, SSMPDir);
    }
//    public void testApp5() throws Exception {
//        String program = LogManager.file2String("resources/testCases/Test2.java");
//        String ssmp = TransWorker.trans2SSMP(program);
//        List<String[]> FSF = new ArrayList<>();
//        FSF.add(new String[]{"x > 0", "y > 0"});
//        FSF.add(new String[]{"x <= 0", "y > 1"});
//        //对FSF中T的互斥性进行验证
//        FSFValidationUnit fsfValidationUnit = new FSFValidationUnit(ssmp, FSF);
//        Result exclusivityResult = callTBFV4J(fsfValidationUnit);
//        if(exclusivityResult.getStatus() == 2){
//            String exclusivityWrongMsg = "检查到FSF中T不满足互斥性,具体是 Ti && Tj :[" + exclusivityResult.getCounterExample()+ "] 有解" +
//                    exclusivityResult.getPathConstrain() + "，请重新生成FSF，确保T之间的互斥性";
//            System.out.println(exclusivityWrongMsg);
//            ModelMessage msg = new ModelMessage("user", exclusivityWrongMsg);
//            fsfPrompt.addMessage(msg);
//            LogManager.appendMessage(fsfPrompt.getCodePath(), msg, fsfPrompt.getModel());
//            continue;
//        }
//    }

    // FSF审查阶段实验，统计平均多少回LLM生成的FSF可以通过FSF审查，即具备完备性和互斥性
//    public void testExperimentFSFReview() throws Exception {
//        String resourceDir = "resources/succDataset";
//        ModelConfig modelConfig = new ModelConfig();
//        ModelConfig modelConfig = CreateChatGptModel();
//        FSFReviewOnDir(modelConfig, resourceDir);
//    }




}
