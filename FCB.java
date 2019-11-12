package FileSystem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by wuweisheng .
 */
public class FCB {
    private char diskName;
    private File myFile;
    private String fileName;

    public String getType() {
        return type;
    }

    private String type;
    double space;
    private String modifiedTime;

    public String getModifiedTime() {
        return modifiedTime;
    }

    public FCB(char diskName, File myFile, String fileName, String type, double space, String modifiedTime) {
        this.diskName = diskName;
        this.myFile = myFile;
        this.fileName = fileName;
        this.type = type;
        this.space = space;
        this.modifiedTime = modifiedTime;
    }


    public String getFileName(){
        return myFile.getName();
    }

    public String getFilePath(){
        return myFile.toString();
    }

    public boolean renameFile(String name){
        String c = myFile.getParent();
        File mm = new File(c + File.separator + name);
        if (myFile.renameTo(mm)){
            myFile = mm;
            fileName = name;
            return true;
        }else{
            return false;
        }
    }

    public File getMyFile(){
        return myFile;
    }

    public char getDiskName() {
        return diskName;
    }

    public double getSpace() {
        return space;
    }

    @Override
    public String toString(){
        return fileName;
    }
}

