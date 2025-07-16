package org.zed.evolution;
import org.zed.log.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ModifyFSF {
    public static List<String[]> exchangeTRandomly(List<String[]> FSF){
        List<String[]> modifiedFSF = new ArrayList<>(FSF);
        int size = modifiedFSF.size();
        int i = 0, j = 0;
        while(i == j){
            i = ThreadLocalRandom.current().nextInt(0,size);
            j = ThreadLocalRandom.current().nextInt(0,size);
        }
//        System.out.println("i = " + i + "," + "j = " + j);
        String tmpT = modifiedFSF.get(i)[0];
        modifiedFSF.get(i)[0] = modifiedFSF.get(j)[0];
        modifiedFSF.get(j)[0] = tmpT;
        return modifiedFSF;
    }

    public static void main(String[] args) {
        List<String[]> FSF = LogManager.getLastestFSFFromLog("resources/log/deepseek-chat/log-Abs.txt");
        for (String[] f : FSF) {
            System.out.println(f[0]);
            System.out.println(f[1]);
        }
        List<String[]> modifiedFSF = exchangeTRandomly(FSF);
        for (String[] f : modifiedFSF) {
            System.out.println(f[0]);
            System.out.println(f[1]);
        }
    }

}
