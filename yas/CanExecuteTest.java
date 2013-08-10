package yas;

import java.io.File;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author muhammed
 */
public class CanExecuteTest {
    public static void main (String[] args) {
        File currentDir = new File(System.getProperty("user.dir"));
        traverse(currentDir);
    }
    
    public static void traverse(File dir) {
        String[] filesAndDirs = dir.list();
        for (String fileOrDir : filesAndDirs) {
            File f = new File(dir, fileOrDir);
            if (f.isDirectory()) {
                traverse(f);
            } else {
                System.out.print(f);
                if (f.canExecute()) {
                    System.out.println(" can execute");
                } else {
                    System.out.println(" cannot execute");
                }
            }
        }
    }
}
