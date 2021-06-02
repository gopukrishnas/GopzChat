package com.example.gopzchat.Models;

public class UserFiles {
    private String filesList, actualPath, folderName;
    int fileNo;

    public UserFiles(String filesList, String actualPath, int fileNo, String folderName) {
        this.filesList = filesList;
        this.actualPath = actualPath;
        this.fileNo = fileNo;
        this.folderName = folderName;
    }


    public UserFiles() {
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getFilesList() {
        return filesList;
    }

    public void setFilesList(String FilesList) {
        this.filesList = FilesList;
    }

    public String getActualPath() {
        return actualPath;
    }

    public void setActualPath(String actualPath) {
        this.actualPath = actualPath;
    }

    public int getFileNo() {
        return fileNo;
    }

    public void setFileNo(int fileNo) {
        this.fileNo = fileNo;
    }
}
