package yas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.zip.DeflaterOutputStream;

public class test {

    public static void main(String[] args) {
        String input = "yasar\0yasar"; // this normally comes from function argument

        File test_file = new File(System.getProperty("user.dir"), "test_file");
        try {
            try (OutputStream out = new DeflaterOutputStream(new FileOutputStream(test_file))) {
                out.write("hello!".getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
