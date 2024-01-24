package com.dehigher.biutils.base;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileUtil {

    public static void writeToFile(String content, String filePath) {
        try {
            // 将字符串转换为字节数组
            byte[] contentBytes = content.getBytes();
            // 使用 Files 类写入文件
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.write(path, contentBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            System.out.println("Content has been written to the file: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 要写入文件的字符串内容
        String content = "Hello, this is a sample content.";

        // 文件路径
        String filePath = "test/output.txt";

        // 调用工具类方法写入文件
        writeToFile(content, filePath);
    }
}
