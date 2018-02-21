
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/*
 * The program connects to the FTP server and tries to perform basic operations 
 * such as listing files, creating directory, uploading files, download files
 *
 *
 * @author Ajay
 */
public class Assn3 {

    public static void main(String[] args) throws IOException {
        
        //first argument is the IP address of the ftp server
        String server = args[0];
        //second argument comes in the form of username:password
        String user = args[1].substring(0, args[1].indexOf(":"));
        String password = args[1].substring(args[1].indexOf(":") + 1, args[1].length());
        
        //third argument is the command that is asked to be performed
        String command = args[2];
        
        String argument = "";

        try {
            //fourth argument, if passed in, is the parameter for the command
            argument = args[3];

            //parameters need to concatenated if passed seperately
            for (int i = 4; i < args.length; i++) {
                argument = argument + " " + args[i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Arguments are only provided in some commands");
        }
        int port = 21;

        FTPClient ftpClient = new FTPClient();

        //connect to the ftp server
        ftpClient.connect(server, 21);
        showServerReply(ftpClient);
        int replyCode = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(replyCode)) {
            System.out.println("The operation was not successful");
            return;
        }

        //check if the login is successful
        boolean success = ftpClient.login(user, password);
        showServerReply(ftpClient);
        if (!success) {
            System.out.println("Cannot connect to the server");

        } else {
            System.out.println("Login successful.");
        }

        switch (command) {
            
            case "ls":

                FTPFile[] files = ftpClient.listFiles();
                showServerReply(ftpClient);
                for (FTPFile file : files) {
                    System.out.println(file.getName());
                }
                break;

            case "cd":
                if (argument.equals("..")) {
                    success = ftpClient.changeToParentDirectory();
                } else {
                    success = ftpClient.changeWorkingDirectory(argument);
                }
                showServerReply(ftpClient);

                if (success) {
                    System.out.println("Remote directory change successful");
                } else {
                    System.out.println("Remote directory change failed");
                }
                break;

            case "delete":

                success = ftpClient.deleteFile(argument);
                showServerReply(ftpClient);

                if (success) {
                    System.out.println("File deleted succesfully");
                } else {
                    System.out.println("File could not be deleted");
                }
                break;

            case "get":

                FTPFile[] fileList = ftpClient.listFiles(argument);
                if (fileList.length == 1 && fileList[0].isFile()) {
                    String downloadFile = "";
                    if (argument.contains("/")) {
                        downloadFile = argument.substring(argument.lastIndexOf('/') + 1, argument.length());
                    } else {
                        downloadFile = argument;
                    }
                    File file = new File(downloadFile);
                    OutputStream output = new BufferedOutputStream(new FileOutputStream(file));

                    success = ftpClient.retrieveFile(downloadFile, output);
                    output.close();
                    showServerReply(ftpClient);

                    if (success) {
                        System.out.println("File downloaded succesfully");
                    } else {
                        System.out.println("File could not be downloaded");
                    }
                } else {
                    if (!argument.contains("/")) {
                        argument = "/" + argument;
                    }
                    getDirectory(ftpClient, argument, "", System.getProperty("user.dir"));
                }
                break;

            case "put":

                putDirectory(ftpClient, ftpClient.printWorkingDirectory(), argument, "");

                break;

            case "mkdir":

                success = ftpClient.makeDirectory(argument);
                showServerReply(ftpClient);

                if (success) {
                    System.out.println("Directory created successfully");
                } else {
                    System.out.println("Directory not created");
                }

                break;

            case "rmdir":
                removeDirectory(ftpClient, argument, "");
                break;
        }
        
        ftpClient.logout();
        ftpClient.disconnect();
       
    }

    private static void showServerReply(FTPClient client) {
        String[] replies = client.getReplyStrings();
        if (replies != null && replies.length > 0) {
            for (String reply : replies) {
                System.out.println("SERVER: " + reply);
            }
        }
    }

    private static void removeDirectory(FTPClient client, String parentDir, String currentDir) throws IOException {
        String dirToList = parentDir;
        if (!currentDir.equals("")) {
            dirToList += "/" + currentDir;
        }

        FTPFile[] subFiles = client.listFiles(dirToList);

        if (subFiles != null && subFiles.length > 0) {
            for (FTPFile aFile : subFiles) {
                String currentFileName = aFile.getName();
                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    // skip parent directory and the directory itself
                    continue;
                }
                String filePath = parentDir + "/" + currentDir + "/"
                        + currentFileName;
                if (currentDir.equals("")) {
                    filePath = parentDir + "/" + currentFileName;
                }

                if (aFile.isDirectory()) {
                    // remove the sub directory
                    removeDirectory(client, dirToList, currentFileName);
                } else {
                    // delete the file
                    boolean deleted = client.deleteFile(filePath);
                    if (deleted) {
                        System.out.println("DELETED the file: " + filePath);
                    } else {
                        System.out.println("CANNOT delete the file: "
                                + filePath);
                    }
                }
            }

            // finally, remove the directory itself
            boolean removed = client.removeDirectory(dirToList);
            if (removed) {
                System.out.println("REMOVED the directory: " + dirToList);
            } else {
                System.out.println("CANNOT remove the directory: " + dirToList);
            }
        } else {
            boolean removed = client.removeDirectory(parentDir);
            if (removed) {
                System.out.println("REMOVED the directory: " + parentDir);
            } else {
                System.out.println("CANNOT remove the directory: " + parentDir);
            }
        }
    }

    private static void putDirectory(FTPClient client,
            String remoteDirPath, String localParentDir, String remoteParentDir)
            throws IOException {

        System.out.println("LISTING directory: " + localParentDir);

        File localDir = new File(localParentDir);
        File[] subFiles = localDir.listFiles();
        if (subFiles != null && subFiles.length > 0) {
            for (File item : subFiles) {
                String remoteFilePath = remoteDirPath + "/" + remoteParentDir
                        + "/" + item.getName();
                if (remoteParentDir.equals("")) {
                    remoteFilePath = remoteDirPath + "/" + item.getName();
                }

                if (item.isFile()) {
                    // upload the file
                    String localFilePath = item.getAbsolutePath();
                    System.out.println("About to upload the file: " + localFilePath);
                    boolean uploaded = putSingleFile(client,
                            localFilePath, remoteFilePath);
                    if (uploaded) {
                        System.out.println("UPLOADED a file to: "
                                + remoteFilePath);
                    } else {
                        System.out.println("COULD NOT upload the file: "
                                + localFilePath);
                    }
                } else {
                    // create directory on the server
                    boolean created = client.makeDirectory(remoteFilePath);
                    if (created) {
                        System.out.println("CREATED the directory: "
                                + remoteFilePath);
                    } else {
                        System.out.println("COULD NOT create the directory: "
                                + remoteFilePath);
                    }

                    // upload the sub directory
                    String parent = remoteParentDir + "/" + item.getName();
                    if (remoteParentDir.equals("")) {
                        parent = item.getName();
                    }

                    localParentDir = item.getAbsolutePath();
                    putDirectory(client, remoteDirPath, localParentDir,
                            parent);
                }
            }
        } else {
            String uploadFile = localParentDir;
            InputStream inputStream = new FileInputStream(uploadFile);
            boolean success = client.storeFile(uploadFile, inputStream);
            System.out.println(localParentDir);
            showServerReply(client);

            if (success) {
                System.out.println("File uploaded succesfully");
            } else {
                System.out.println("File could not be uploaded");
            }
        }
    }

    private static boolean putSingleFile(FTPClient client, String localFilePath, String remoteFilePath) throws IOException {
        File localFile = new File(localFilePath);
        InputStream inputStream = new FileInputStream(localFile);
        try {
            return client.storeFile(remoteFilePath, inputStream);
        } finally {
            inputStream.close();
        }
    }

    private static void getDirectory(FTPClient client, String parentDir,
            String currentDir, String saveDir) throws IOException {

        String dirToList = parentDir;
        if (!currentDir.equals("")) {
            dirToList += "/" + currentDir;
        }
        FTPFile[] subFiles = client.listFiles(dirToList);
        System.out.println(subFiles.length);
        if (subFiles != null && subFiles.length > 0) {
            for (FTPFile aFile : subFiles) {
                String currentFileName = aFile.getName();
                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    // skip parent directory and the directory itself
                    continue;
                }
                String filePath = parentDir + "/" + currentDir + "/"
                        + currentFileName;
                if (currentDir.equals("")) {
                    filePath = parentDir + "/" + currentFileName;
                }

                String newDirPath = saveDir + parentDir + File.separator
                        + currentDir + File.separator + currentFileName;
                if (currentDir.equals("")) {
                    newDirPath = saveDir + parentDir + File.separator
                            + currentFileName;
                }

                if (aFile.isDirectory()) {
                    // create the directory in saveDir
                    File newDir = new File(newDirPath);
                    boolean created = newDir.mkdirs();
                    if (created) {
                        System.out.println("CREATED the directory: " + newDirPath);
                    } else {
                        System.out.println("COULD NOT create the directory: " + newDirPath);
                    }

                    // download the sub directory
                    getDirectory(client, dirToList, currentFileName,
                            saveDir);
                } else {
                    // download the file
                    boolean success = getSingleFile(client, filePath,
                            newDirPath);
                    System.out.println(filePath);
                    if (success) {
                        System.out.println("DOWNLOADED the file: " + filePath);
                    } else {
                        System.out.println("COULD NOT download the file: "
                                + filePath);
                    }
                }
            }
        } else {

            String downloadFile = currentDir;
            File file = new File(downloadFile);
            OutputStream output = new BufferedOutputStream(new FileOutputStream(file));

            boolean success = client.retrieveFile(downloadFile, output);
            output.close();
            showServerReply(client);

            if (success) {
                System.out.println("File downloaded succesfully");
            } else {
                System.out.println("File could not be downloaded");
            }
        }
    }

    private static boolean getSingleFile(FTPClient client,
            String remoteFilePath, String savePath) throws IOException {
        File downloadFile = new File(savePath);

        File parentDir = downloadFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdir();
        }

        OutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(downloadFile));
        try {
            return client.retrieveFile(remoteFilePath, outputStream);
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }
}
