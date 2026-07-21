/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package netstudiolibrary;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 *
 * @author clgarcias
 */
public class TextUitl {
    
    /**
     * This method read the events in log.
     *
     * @param logPath path of log to read
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String CountLogEvents(String logPath, String traceName) {
        String result = "1-No Events";
        int count = 0;
        try {

            File file = new File(logPath);
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.endsWith(traceName)) {
                        count++;                        
                    }
                }
            }
            result = String.valueOf(count);
        } catch (FileNotFoundException e) {
            System.err.println("Error:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        }
        return result;
    }
    
    /**
     * This method read and print the content of a file.
     *
     * @param filePath path of file to read
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String ReadFile(String filePath) {
        String result = "1";
        try {

            File file = new File(filePath);
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    System.out.println(line);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        }
        return result;
    }

    /**
     * This method split the content of a file in n files with n (nLines) lines
     * in each one file.
     *
     * @param filePath path of file to read
     * @param nLines number of lines for each file
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String SplitFile(String filePath, String pathNewFile, int nLines) {

        int countLine = 0;
        int countFile = 1;
        String result = "1";
        String nameFile = "updates#.csv";
        List<String> lines = new ArrayList<>();

        try {

            File file = new File(filePath);
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    lines.add(line);
                    countLine++;
                    if (countLine == nLines) {
                        String newFile = nameFile.replace("#", String.valueOf(countFile));
                        if (WriteListToFile(pathNewFile + newFile, (ArrayList<String>) lines).equals("1")) {
                            lines.clear();
                            countLine = 0;
                            countFile++;
                        } else {
                            System.err.println("Error writing new file " + newFile + ".");
                            result = "0-" + "Error writing new file " + newFile + ".";
                            return result;
                        }
                    }
                }
                if (!lines.isEmpty()) {
                    String newFile = nameFile.replace("#", String.valueOf(countFile));
                    if (!WriteListToFile(pathNewFile + newFile, (ArrayList<String>) lines).equals("1")) {
                        System.err.println("Error writing new file " + newFile + ".");
                        result = "0-" + "Error writing new file " + newFile + ".";
                        return result;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error reading file:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        }
        return result;
    }
    
    /**
     * This method split the content of a file in n files with n (nLines) lines
     * in each one file.
     *
     * @param filesPath path of file to read
     * @param pathNewFile path of concatenated file
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String ConcatFiles(String filesPath, String pathNewFile, String nameNewFile) {

        String result = "1";
        try {
            Path inputDir = Paths.get(filesPath);    
            if (Files.isDirectory(inputDir)) {
                List<Path> filePaths = Files.list(inputDir).collect(Collectors.toList());
                try (Writer writer = new FileWriter(pathNewFile + "\\" + nameNewFile)) {
                    System.out.println("Files in directory: " + filesPath);
                    for (Path filePath : filePaths) {
                        System.out.println(filePath.getFileName());
                        //File file = new File(filePath.getFileSystem().toString());
                        Scanner scanner = new Scanner(filePath);
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            writer.append(line);
                            writer.append("\n");
                        }                    
                    }
                    writer.flush();
                }
            }            
        } catch (IOException e) {
            System.err.println("Error reading file:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        }
        return result;
    }
    
    /**
     * This method save the content of the list in a file.
     *
     * @param filePath path of file to write.
     * @param list list to write in the file.
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String WriteListToFile(String filePath, ArrayList<String> list) {
        String result = "1";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String item : list) {
                writer.write(item);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error creating file:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        }
        return result;
    }
    
    /**
     * This method save the content of the file in a list.
     *
     * @param filePath path of file to read.
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    ArrayList WriteFileToList (String filePath) {
        ArrayList<String> result = new ArrayList<>();
        try {

            File file = new File(filePath);
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    result.add(line);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error:" + e.toString());
            result.add("Error:" + e.toString());
        }
        return result;
    }
}
