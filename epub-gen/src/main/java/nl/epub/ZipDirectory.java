package nl.epub;


import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipDirectory {


    public static void getAllFiles(File dir, List<File> fileList) {

            System.out.println(dir);
            File[] files = dir.listFiles();
            for (File file : files) {
                fileList.add(file);
                if (file.isDirectory()) {
                    getAllFiles(file, fileList);
                } else {
                }
            }

    }

    public static void writeZipFile(File directoryToZip, List<File> fileList, String filename) {

        try {
            new File("books").mkdirs();
            FileOutputStream fos = new FileOutputStream("books/" + filename + ".epub");
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (File file : fileList) {
                if (!file.isDirectory()) { // we only zip files, not directories
                    addToZip(directoryToZip, file, zos);
                }
            }

            zos.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addToZip(File directoryToZip, File file, ZipOutputStream zos) throws FileNotFoundException,
            IOException {

        FileInputStream fis = new FileInputStream(file);

        // we want the zipEntry's path to be a relative path that is relative
        // to the directory being zipped, so chop off the rest of the path
        String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1,
                file.getCanonicalPath().length());
        ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }

        zos.closeEntry();
        fis.close();
    }

}