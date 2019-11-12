package FileSystem;

import javax.swing.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wws.
 */
public class Disk {
    private char diskName;
    private File diskFile;
    private File diskBitMap;
    private File recover;
    private FileWriter bitWriter;
    private FileWriter recoverWriter;
    private int fileNum;
    private double space;
    public int [][] bitmap = new int[32][32];
    private Map<String, int[][] > filesBit = new HashMap<String, int[][]>();
    private ArrayList<File> files = new ArrayList<File>();

    public Disk(char name, File file, boolean rec) throws IOException {
        diskName = name;
        diskFile = file;
        diskBitMap = new File(diskFile.getPath() + File.separator + diskName + "_BitMap&&Fat.txt");
        recover = new File(diskFile.getPath() + File.separator + "recover.txt");
        if (!rec) {
            space = 0;
            fileNum = 0;
            diskFile.mkdir();
            diskBitMap.createNewFile();
            bitWriter = new FileWriter(diskBitMap);
            for (int i = 0; i < 32; i++) {
                for (int k = 0; k < 32; k++) {
                    bitmap[i][k] = 0;
                    bitWriter.write("0");
                }
                bitWriter.write("\r\n");
            }
            bitWriter.flush();

            recover.createNewFile();
            recoverWriter = new FileWriter(recover);
            recoverWriter.write(String.valueOf(space) + "\r\n");
            recoverWriter.write(String.valueOf(fileNum) + "\r\n");
            for (int i = 0; i < 32; i++) {
                for (int k = 0; k < 32; k++) {
                    if (bitmap[i][k] == 0) {
                        recoverWriter.write("0\r\n");
                    } else {
                        recoverWriter.write("1\r\n");
                    }
                }
            }
            recoverWriter.flush();
        }else{
            try {
                BufferedReader reader = new BufferedReader(new FileReader(recover));
                space = Double.parseDouble(reader.readLine());
                fileNum = Integer.parseInt(reader.readLine());
                for (int i = 0; i < 32; i++) {
                    for (int k = 0; k < 32; k++) {
                        if (Integer.parseInt(reader.readLine()) == 0) {
                            bitmap[i][k] = 0;
                        } else {
                            bitmap[i][k] = 1;
                        }
                    }
                }
                String temp;
                while ((temp = reader.readLine()) != null) {
                    File myFile = new File(diskFile.getPath() + File.separator + temp);
                    files.add(myFile);
                    int[][] tempBit = new int[32][32];
                    for (int i = 0; i < 32; i++) {
                        for (int k = 0; k < 32; k++) {
                            if (Integer.parseInt(reader.readLine()) == 0) {
                                tempBit[i][k] = 0;
                            } else {
                                tempBit[i][k] = 1;
                            }
                        }
                    }
                    filesBit.put(myFile.getName(), tempBit);
                }
                reader.close();
            }catch (Exception e){
                JOptionPane.showMessageDialog(null, "The files aren't compelete. You can choose another place or delete \"myFileSystem\" in this dir and run this again!",
                "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
    }

    public File getDiskFile(){
        return diskFile;
    }


    public void rewriteBitMap() throws IOException {
        bitWriter = new FileWriter(diskBitMap);
        bitWriter.write("");
        for (int i = 0; i < 32;i++){
            for (int k = 0; k < 32; k++){
                if (bitmap[i][k] == 0){
                    bitWriter.write("0");
                }else{
                    bitWriter.write("1");
                }
            }
            bitWriter.write("\r\n");
        }
        for (int i = 0; i < files.size(); i++){
            bitWriter.write(files.get(i).getName() + ":");
            for (int k = 0; k < 32; k++){
                for (int j = 0; j < 32; j++){
                    try {
                        if (filesBit.get(files.get(i).getName())[k][j] == 1) {
                         bitWriter.write(String.valueOf(k * 32 + j) + " ");
                        }
                    }catch (Exception e){
                        System.out.println("wrong");
                    }
                }
            }
            bitWriter.write("\r\n");
        }
        bitWriter.flush();
    }

    public void rewriteRecoverWriter() throws IOException{
        recoverWriter = new FileWriter(recover);
        recoverWriter.write("");

        recoverWriter.write(String.valueOf(space) + "\r\n");
        recoverWriter.write(String.valueOf(fileNum) + "\r\n");
        for (int i = 0; i < 32; i++){
            for (int k = 0; k < 32; k++){
                if (bitmap[i][k] == 0){
                    recoverWriter.write("0\r\n");
                }else{
                    recoverWriter.write("1\r\n");
                }
            }
        }
        for (int i = 0; i < files.size(); i++){
            recoverWriter.write(files.get(i).getName() + "\r\n");
            int [][] bitTemp = filesBit.get(files.get(i).getName());
            for (int k = 0; k < 32; k++){
                for (int j = 0; j < 32; j++){
                    if (bitTemp[k][j] == 0){
                        recoverWriter.write("0\r\n");
                    }else {
                        recoverWriter.write("1\r\n");
                    }
                }
            }
        }
        recoverWriter.flush();
    }

    public boolean createFile(File file, double capacity) throws IOException {
        files.add(file);
//        file.createNewFile();
        int cap[][] = new int[32][32];
        for (int i = 0; i < 32; i++){
            for (int k = 0; k < 32; k++)
                cap[i][k] = 0;
        }
        BufferedReader in = new BufferedReader(new FileReader(diskBitMap));
        int count;
        if(capacity % 1 == 0){// 是这个整数，小数点后面是0
            count = (int) capacity;
        }else{//不是整数，小数点后面不是0
            count = (int) capacity + 1;
        }

        for (int i = 0; i < 32; i++){
            String line  = in.readLine();
            for (int k = 0; k < 32; k++){
                if (count > 0) {
                    if (line.charAt(k) == '0') {
                        count--;
                        cap[i][k] = 1;
                        bitmap[i][k] = 1;
                    }
                }
            }
        }
        if (count > 0){
            JOptionPane.showMessageDialog(null, "Insufficient memory!!", "Fail", JOptionPane.ERROR_MESSAGE);
            file.delete();
            for (int i = 0; i < 32; i++){
                for (int k = 0; k < 32; k++){
                    if (cap[i][k] == 1){
                        bitmap[i][k] = 0;
                    }
                }
            }
            return false;
        }else{
            fileNum++;
            space += capacity;
            filesBit.put(file.getName(), cap);
            rewriteBitMap();
            rewriteRecoverWriter();
            return true;
        }
    }

    public boolean modifyFile(File file, double capacity) throws IOException {

        space -= capacity;
        int[][] fileStore = filesBit.get(file.getName());
        if (fileStore!=null)
            for (int i = 0; i < 32; i++){
                for (int k = 0; k < 32; k++){
                    if (bitmap[i][k] == 1 && fileStore[i][k] == 1){
                        bitmap[i][k] = 0;
                    }
                }
            }

        filesBit.remove(file.getName());
        for (int i = 0; i < files.size(); i++){
            if (files.get(i).getName().equals(file.getName())){
                files.remove(i);
                break;
            }
        }
        file = new File(file.getPath());
        createFile(file,file.length()/1024);
        rewriteBitMap();
        return true;
    }

    public boolean pasteFile(File sourceFile, File targetFile) {
        int cap[][] = new int[32][32];
        for (int i = 0; i < 32; i++) {
            for (int k = 0; k < 32; k++)
                cap[i][k] = 0;
        }
        try{
                if (sourceFile.isFile()) {

                    // 新建文件输入流并对它进行缓冲
                    FileInputStream input = null;
                    try {
                        input = new FileInputStream(sourceFile);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    BufferedInputStream inBuff = new BufferedInputStream(input);

                    // 新建文件输出流并对它进行缓冲
                    FileOutputStream output = null;
                    try {
                        output = new FileOutputStream(targetFile.getPath()+"/"+sourceFile.getName());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    BufferedOutputStream outBuff = new BufferedOutputStream(output);

                    // 缓冲数组
                    byte[] b = new byte[1024];
                    int len;

                    try {
                        while ((len = inBuff.read(b)) != -1) {
                            outBuff.write(b, 0, len);
                        }
                        // 刷新此缓冲的输出流
                        outBuff.flush();
                        //关闭流
                        inBuff.close();
                        outBuff.close();
                        output.close();
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    File tarFile = new File(targetFile.getPath()+"/"+sourceFile.getName());
                    try {
                        createFile(tarFile,tarFile.length()/1024);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    File[] file = sourceFile.listFiles();
                    for (int i = 0; i < file.length; i++) {

                        // 准备复制的源文件夹
                        File dir1 = new File(sourceFile.getPath() + "/" + file[i].getName());
                        // 准备复制的目标文件夹
                        File dir2 = new File(targetFile.getPath() + "/" + sourceFile.getName());
                        dir2.mkdir();
                        pasteFile(dir1, dir2);
                    }
                }
            }catch (Exception e) {
                JOptionPane.showMessageDialog(null, " Cannot paste to a file ", "Access fail", JOptionPane.ERROR_MESSAGE);
            }
        try {
            rewriteBitMap();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }


    public boolean deleteFile(File file, double capacity){
        if (file.getName().equals("C") || file.getName().equals("D") || file.getName().equals("E") || file.getName().equals("CBitMap&&Fat.txt")
                || file.getName().equals("DBitMap&&Fat.txt") || file.getName().equals("EBitMap&&Fat.txt")
                || file.getName().equals("recover.txt")){
            JOptionPane.showMessageDialog(null, "Cannot delete the file ！", "Delete fail", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try{
            if (file.isFile()){
                try {
                    file.delete();
                }catch (Exception e){
                    e.printStackTrace();
                }
                space -= capacity;
                fileNum--;
                int[][] fileStore = filesBit.get(file.getName());
                for (int i = 0; i < 32; i++){
                    for (int k = 0; k < 32; k++){
                        if (bitmap[i][k] == 1 && fileStore[i][k] == 1){
                            bitmap[i][k] = 0;
                        }
                    }
                }
                filesBit.remove(file.getName());
                for (int i = 0; i < files.size(); i++){
                    if (files.get(i).getName().equals(file.getName())){
                        files.remove(i);
                        break;
                    }
                }
            }else{
                File [] files = file.listFiles();
                for(File myFile : files){
                    deleteFile(myFile, myFile.length()/1024+1);
                }
                while(file.exists()) {
                    file.delete();
                }
            }
            return true;
        }catch (Exception e){
            System.out.println("fail");
            return false;
        }
    }

    public boolean renameFile(File file, String name, double capacity) throws IOException {
        String oldName = file.getName();
        int[][] tempBit = filesBit.get(oldName);
        String c = file.getParent();
        File mm;
        if(file.isFile()) {
            mm = new File(c + File.separator + name + ".txt");
            if (file.renameTo(mm)){
                file = mm;
                filesBit.remove(oldName);
                filesBit.put(file.getName(), tempBit);
                for (int i = 0; i < files.size(); i++){
                    if (files.get(i).getName().equals(oldName)){
                        files.remove(i);
                        files.add(file);
                        break;
                    }
                }
                rewriteBitMap();
                rewriteRecoverWriter();
                return true;
            }else{
                return false;
            }
        }
        else {
            mm = new File(c + File.separator + name);
            file.renameTo(mm);
            return true;
        }
    }

    public int getFileNum() {
        return fileNum;
    }

    public double getSpace() {
        return space;
    }
}
