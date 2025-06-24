package org.zed;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.zed.llm.ModelConfig;
import org.zed.log.LogManager;
import org.zed.trans.TransFileOperator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static org.zed.FSFGenerator.*;
import static org.zed.log.LogManager.*;
import static org.zed.tcg.ExecutionEnabler.*;
import static org.zed.trans.ExecutionPathPrinter.addPrintStmt;
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
    public void testApp() throws Exception {
        String testFileName = "Test1";
        String testFileNameJava = testFileName+".java";
        String testLogFileName = "log-" + testFileName+".txt";
        pickSSMPCodes("resources/dataset/"+ testFileNameJava);
        LogManager.cleanLogOfModel("deepseek-chat");
        runConversations(1, new ModelConfig(), "resources/trans/oneStaticMdCodes/"+testFileNameJava);
        List<String[]> TDList = getLastestTDsFromLog("resources/log/deepseek-chat/"+testLogFileName);
        int n = 1;
        for (String[] td : TDList){
            String T = td[0];
            String D = td[1];
            String program = getProgramFromLog("resources/log/deepseek-chat/" + testLogFileName);
            String mainMd = generateMainMdUnderExpr(T,program);
            System.out.println(mainMd);
            //插入main函数后形成新program
            String runnableProgram = insertMainMdInSSMP(program, mainMd);
            System.out.println("runnableProgram:"+runnableProgram);
            // 保存文件
            TransFileOperator.saveRunnablePrograms(testFileNameJava,runnableProgram,n);
            n++;
            //执行插桩，即验证前的准备工作
            String addedPrintProgram = addPrintStmt(runnableProgram);
            System.out.println("addedPrintProgram:"+addedPrintProgram);
            //开始验证
            List<String> preConstrains = new ArrayList<String>();
            SpecUnit su =new SpecUnit(addedPrintProgram,T,D,preConstrains);
            System.out.println("开始验证");
            Result result = callTBFV4J(su);
            if(result != null){
                System.out.println(result);
            }
        }
    }
    public void testApp2() throws Exception {
        String testFileName = "Test1";
        String testFileNameJava = testFileName+".java";
        String testLogFileName = "log-" + testFileName+".txt";
        List<String[]> TDList = getLastestTDsFromLog("resources/log/deepseek-chat/"+testLogFileName);
        int n = 1;
        for (String[] td : TDList){
            String T = td[0];
            String D = td[1];
//            System.out.println("T:" + T);
//            System.out.println("D:" + D);
            String program = getProgramFromLog("resources/log/deepseek-chat/" + testLogFileName);
            String mainMd = generateMainMdUnderExpr(T,program);
            System.out.println(mainMd);
            String addedPrintProgram = addPrintStmt(program);
            //插入main函数后形成新program
            String runnableProgram = insertMainMdInSSMP(addedPrintProgram, mainMd);
            TransFileOperator.saveRunnablePrograms(testFileNameJava,runnableProgram,n);
            n++;
            //开始验证
            List<String> preConstarins = new ArrayList<>();
            System.out.println("T:"+T);
            System.out.println("D:"+D);
            System.out.println("runnableProgram:\n"+runnableProgram);
            SpecUnit su =new SpecUnit(runnableProgram,T,D,preConstarins);
            System.out.println("开始验证");
            Result result = callTBFV4J(su);
            if(result != null){
                System.out.println(result);
            }
        }
    }

    public void aPathValidationTask(String T, List<String> pathConstrains,String program){
        StringBuilder constrains  = new StringBuilder(T);
        for(String pc : pathConstrains){
            constrains.append("&&");
            constrains.append(pc);
        }
        String cons = constrains.toString();
        System.out.println("本次路径验证的约束条件为" + cons);

    }


    public void testApp3() throws Exception {
        String resourceDir = "resources/dataset/";
        String testFileName = "Example";
        String testFileNameJava = testFileName+".java";
        String filePath = resourceDir + testFileNameJava;
        ModelConfig modelConfig = new ModelConfig();
        try {
            runConversations(1, modelConfig, filePath);

        }
        catch (Exception e) {
            System.err.println("Error during runConversations: " + e.getMessage());
            //把验证失败的代码保存到文件中
            Files.move(Path.of(filePath),Path.of("resources/failedDataset/"+testFileNameJava), StandardCopyOption.REPLACE_EXISTING);
            e.printStackTrace();
        }
    }
    public void testApp4() throws Exception {
        String resourceDir = "resources/dataset/";
        ModelConfig modelConfig = new ModelConfig();
        runConversationForDir(1, modelConfig, resourceDir);
    }
}
