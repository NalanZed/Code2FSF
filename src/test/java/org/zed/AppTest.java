package org.zed;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.zed.llm.ModelConfig;

import java.io.IOException;

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

//    public void testApp4() throws Exception {
//        String resourceDir ="resources/dataset/someBench/";
//        ModelConfig modelConfig = new ModelConfig();
//        String SSMPDir = pickSSMPCodes(resourceDir);
//        runConversationForDir(5, modelConfig, SSMPDir);
//    }
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
}
