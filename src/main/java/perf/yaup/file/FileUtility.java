package perf.yaup.file;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import perf.yaup.Sets;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by wreicher
 */
public class FileUtility {



    public static final String ARCHIVE_KEY = "#";
    public static final String SEARCH_KEY = ">";
    public static final String REMOTE_KEY = ":";

    public static int OPERATION_LENGTH = 2;
    public static final String ADD_OPERATION = "++";
    public static final String DELETE_OPERATION = "--";
    public static final String SET_OPERATION = "==";

    public static final Set<String> OPERATIONS = Collections.unmodifiableSet(Sets.of(ADD_OPERATION,DELETE_OPERATION,SET_OPERATION));

    /**
     * Get a <code>List</code> of all the files in baseDir that contain nameSubstring
     * @param baseDir the full path to the directory to start the search
     * @param nameSubstring the substring to find in the file name
     * @param recursive - search subdirectories if <code>true</code>
     * @return an <code>Immutable</code> list of the files which match the search
     */
    public static List<String> getFiles(String baseDir, String nameSubstring,
                                        boolean recursive) {
        return search(baseDir, nameSubstring, recursive, true, false, false);
    }
    /**
     * Get a <code>List</code> of all the directories in baseDir that contain nameSubstring
     * @param baseDir the full path to the directory to start the search
     * @param nameSubstring the substring to find in the file name
     * @param recursive - search subdirectories if <code>true</code>
     * @return an <code>Immutable</code> list of the files which match the search
     */
    public static List<String> getDirectories(String baseDir, String nameSubstring,
                                              boolean recursive) {
        return search(baseDir, nameSubstring, recursive, false, true, false);
    }

    /**
     * return <code>true</code> if fileName refers to an entry within an archive file and is not an existing file
     * @param fileName
     * @return
     */
    public static boolean isArchiveEntryPath(String fileName) {
        if(fileName == null || !fileName.contains(ARCHIVE_KEY)){
            return false;
        }
        File parentFile = new File(fileName.substring(0,fileName.indexOf(ARCHIVE_KEY)));
        File tmpFile = new File(fileName);

        return (!tmpFile.exists() && parentFile.exists());

    }

    /**
     * Get an input stream for the file path which can contain an optional achive entry subPath
     * @param fullPath the path for the file with an optional archive entry subPaths for archive files (e.g. jars)
     * @return an InputStream if fullPath exists or null
     */
    public static long getInputSize(String fullPath){
        String archivePath = fullPath;
        String entryPath = "";
        //TODO get an input stream for remote file ?
        if(isArchiveEntryPath(fullPath)){
            archivePath = getArchiveFilePath(fullPath);
            entryPath = getArchiveEntrySubPath(fullPath);
        }
        File archiveFile = new File(archivePath);
        if(!archiveFile.exists()){
            return 0;
        }
        try {
            InputStream rtrn = null;
            if (archivePath.endsWith(".tar.gz") || archivePath.endsWith(".tgz")) {
                rtrn = new TarArchiveInputStream(new GzipCompressorInputStream(rtrn));
            } else if (archivePath.endsWith((".gz"))) {
                rtrn = new GzipCompressorInputStream(rtrn);
            } else if (archivePath.endsWith(".tar")) {
                rtrn = new TarArchiveInputStream(rtrn);
            } else if (archivePath.endsWith(".zip")) {
                rtrn = new ZipArchiveInputStream(rtrn);
            } else if (archivePath.endsWith(".Z")) {
                rtrn = new ZCompressorInputStream(rtrn);
            } else if (archivePath.endsWith(".tar.bz2") || archivePath.endsWith("tbz2")) {
                rtrn = new TarArchiveInputStream(new BZip2CompressorInputStream(rtrn));
            } else if (archivePath.endsWith(".bz2")) {
                rtrn = new BZip2CompressorInputStream(rtrn);
            } else if (archivePath.endsWith(".jar")) {
                rtrn = new JarArchiveInputStream(rtrn);
            } else { //just a file
                return archiveFile.length();
            }
            if(!entryPath.isEmpty()){
                if(rtrn instanceof ArchiveInputStream){
                    ArchiveInputStream ais = (ArchiveInputStream) rtrn;
                    ArchiveEntry ae = null;
                    InputStream entryStream = null;
                    while( (ae = ais.getNextEntry()) != null ){

                        String aeName = ae.getName();
                        if(aeName.startsWith("./")){
                            aeName = aeName.substring(2);
                        }else if (aeName.startsWith("/")){
                            aeName = aeName.substring(1);
                        }
                        if(entryPath.startsWith("./")){
                            entryPath = entryPath.substring(2);
                        }else if (entryPath.startsWith("/")){
                            entryPath = entryPath.substring(1);
                        }

                        if(aeName.equals(entryPath)){
                            ae.getSize();
                            entryStream = (rtrn);
                            break;
                        }
                    }
                    if(entryStream == null){
                        throw new RuntimeException("Could not find "+entryPath+" in "+archivePath+" on local file system");
                    }else{
                        rtrn = entryStream;
                    }
                }else {
                    throw new RuntimeException("Could not find "+entryPath+" in "+archivePath+" because it is not an archive collection");
                }
            }
            if(rtrn!=null){
                return rtrn.available();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public static InputStream getInputStream(String fullPath){
        InputStream rtrn = null;
        String archivePath = fullPath;
        String entryPath = "";
        //TODO get an input stream for remote file ?
        if(isArchiveEntryPath(fullPath)){
            archivePath = getArchiveFilePath(fullPath);
            entryPath = getArchiveEntrySubPath(fullPath);
        }
        //for each of the possible fileSuffix types
        try {
            if(!(new File(archivePath)).exists()){
                rtrn = null;
                throw new RuntimeException("Cannot find "+archivePath+" on local file system");
            }
            rtrn = new FileInputStream(archivePath);
            if(archivePath.endsWith(".tar.gz") || archivePath.endsWith(".tgz")) {
                rtrn = new TarArchiveInputStream(new GzipCompressorInputStream(rtrn));
            }else if (archivePath.endsWith((".gz"))){
                rtrn = new GzipCompressorInputStream(rtrn);
            }else if (archivePath.endsWith(".tar")){
                rtrn = new TarArchiveInputStream(rtrn);
            }else if (archivePath.endsWith(".zip")){
                rtrn = new ZipArchiveInputStream(rtrn);
            }else if (archivePath.endsWith(".Z")){
                rtrn = new ZCompressorInputStream(rtrn);
            }else if (archivePath.endsWith(".tar.bz2") || archivePath.endsWith("tbz2")){
                rtrn = new TarArchiveInputStream(new BZip2CompressorInputStream(rtrn));
            }else if (archivePath.endsWith(".bz2")){
                rtrn = new BZip2CompressorInputStream(rtrn);
            }else if (archivePath.endsWith(".jar")){
                rtrn = new JarArchiveInputStream(rtrn);
            }
            if(!entryPath.isEmpty()){
                if(rtrn instanceof ArchiveInputStream){
                    ArchiveInputStream ais = (ArchiveInputStream) rtrn;
                    ArchiveEntry ae = null;
                    InputStream entryStream = null;
                    while( (ae = ais.getNextEntry()) != null ){

                        String aeName = ae.getName();
                        if(aeName.startsWith("./")){
                            aeName = aeName.substring(2);
                        }else if (aeName.startsWith("/")){
                            aeName = aeName.substring(1);
                        }
                        if(entryPath.startsWith("./")){
                            entryPath = entryPath.substring(2);
                        }else if (entryPath.startsWith("/")){
                            entryPath = entryPath.substring(1);
                        }

                        if(aeName.equals(entryPath)){
                            ae.getSize();
                            entryStream = (rtrn);
                            break;
                        }
                    }
                    if(entryStream == null){
                        throw new RuntimeException("Could not find "+entryPath+" in "+archivePath+" on local file system");
                    }else{
                        rtrn = entryStream;
                    }
                }else {
                    throw new RuntimeException("Could not find "+entryPath+" in "+archivePath+" because it is not an archive collection");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return rtrn;
    }

    /**
     * Get an Unmodifiable list of the entries in the archive
     * @param archivePath
     * @return a List containing all the named entires in the archive or an empty list
     */
    public static List<String> getArchiveEntries(String archivePath){
        InputStream is = getInputStream(archivePath);
        List<String> rtrn = new LinkedList<String>();
        if(is !=null && is instanceof ArchiveInputStream){
            ArchiveInputStream ais = (ArchiveInputStream)is;
            ArchiveEntry ae = null;
            InputStream entryStream = null;
            try {
                while( (ae = ais.getNextEntry()) != null ){
                    rtrn.add(ae.getName());
                }
            } catch (IOException e) {
                e.printStackTrace();
                //TODO log the error
            }

        }
        return Collections.unmodifiableList(rtrn);
    }

    /**
     * return the archive entry subPath portion or an empty <code>String</code>
     * @param archiveEntryPath
     * @return the archive entry subPath or an empty String
     */
    public static String getArchiveEntrySubPath(String archiveEntryPath) {
        if (archiveEntryPath == null || archiveEntryPath.isEmpty())
            return "";
        if (isArchiveEntryPath(archiveEntryPath))
            return archiveEntryPath.substring(archiveEntryPath
                    .indexOf(ARCHIVE_KEY) + ARCHIVE_KEY.length());

        return "";
    }

    /**
     * return the path to the archive file, removing any archive entry subPath
     * @param archiveEntryPath
     * @return the archive's filePath or an empty String
     */
    public static String getArchiveFilePath(String archiveEntryPath) {
        if (archiveEntryPath == null || archiveEntryPath.isEmpty())
            return "";
        if (isArchiveEntryPath(archiveEntryPath))
            return archiveEntryPath.substring(0,
                    archiveEntryPath.indexOf(ARCHIVE_KEY));

        return "";
    }

    public static String getDirectory(String fileName) {
        if (isArchiveEntryPath(fileName))
            fileName = fileName.substring(0, fileName.indexOf(ARCHIVE_KEY));
        if (fileName.contains("\\")) {
            fileName = fileName.replaceAll("\\\\", "/");
        }
        if (fileName.endsWith("/"))
            return fileName;
        else
            return fileName.substring(0, fileName.lastIndexOf("/") + 1);
    }
    public static String getParentDirectory(String fileName){
        if(fileName==null)
            return "";
        if (fileName.contains("\\")) {
            fileName = fileName.replaceAll("\\\\", "/");
        }
        if (fileName.endsWith("/"))
            fileName=fileName.substring(0, fileName.length()-1);

        return fileName.substring(0, fileName.lastIndexOf("/")+1);
    }
    private static final List<String> search(String baseDir, String nameSubstring, boolean recursive, boolean wantFiles, boolean depthFirst, boolean inArchive) {
        List<String> rtrn = new ArrayList<String>();
        List<String> toParse = new ArrayList<String>();
        toParse.add(baseDir);

        while (!toParse.isEmpty()) {
            String next = toParse.remove(0);
            File f = new File(next);
            if (f.exists() && f.isDirectory()) {
                for (File sub : f.listFiles()) {
                    if (recursive && sub.isDirectory()) {
                        if (depthFirst)
                            toParse.add(0, sub.getAbsolutePath());
                        else
                            toParse.add(sub.getAbsolutePath());
                    }
                    // probably don't need both boolean comparisons but I'm
                    // curious if isFile!=isDirectory is a contract
                    if (sub.isFile() == wantFiles && sub.isDirectory() != wantFiles) {
                        // if there is name filtering
                        if (nameSubstring != null && !nameSubstring.isEmpty()) {
                            if (sub.getName().contains(nameSubstring) /*&& !isArchive(sub)*/ ) {
                                rtrn.add(sub.getAbsolutePath());
                            }
                        }
                    }
                    if (inArchive && sub.isFile() && isArchive(sub)) {

                        List<String> entries = getArchiveEntries(sub.getPath());
                        for(String entry : entries){
                            if(entry.contains(nameSubstring)){
                                rtrn.add(entry);
                            }
                        }
                    }
                }
            }else if (f.exists()){
                if (f.isFile() == wantFiles && f.isDirectory() != wantFiles) {
                    // if there is name filtering
                    if (nameSubstring != null && !nameSubstring.isEmpty()) {
                        if (f.getName().contains(nameSubstring) /*&& !isArchive(sub)*/ ) {
                            rtrn.add(f.getAbsolutePath());
                        }
                    }
                }
                if (inArchive && f.isFile() && isArchive(f)) {

                    List<String> entries = getArchiveEntries(f.getPath());
                    for(String entry : entries){
                        if(entry.contains(nameSubstring)){
                            rtrn.add(entry);
                        }
                    }
                }

            }
        }
        return rtrn;
    }
    public static JSONObject readJsonObjectFile(String fileName){
        JSONObject rtrn = new JSONObject();
        if(Files.exists(Paths.get(fileName))){
            try {
                rtrn = new JSONObject(new String(Files.readAllBytes(Paths.get(fileName))));
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        return rtrn;
    }
    public static JSONArray readJsonArrayFile(String fileName){
        JSONArray rtrn = new JSONArray();
        if(Files.exists(Paths.get(fileName))){
            try {
                rtrn = new JSONArray(new String(Files.readAllBytes(Paths.get(fileName))));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return rtrn;
    }
    public static Object readObjectFile(String fileName){
        return readObjectFile(new File(fileName));
    }
    public static Object readObjectFile(File file){
        ObjectInputStream ois = null;
        Object rtrn = null;
        try{
            ois = new ObjectInputStream(new FileInputStream(file));
            rtrn = ois.readObject();
            ois.close();
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally{
            if(ois!=null){
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return rtrn;
    }
    public static int lineCount(String substring,String fileName){
        int index = 0-substring.length();
        int count = 0;
        String line = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            while( (line=reader.readLine())!=null){
                if(line.indexOf(substring)>-1){
                    count++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return count;
    }
    public static void writeObjectFile(String fileName,Object object){
        writeObjectFile(new File(fileName),object);
    }

    public static void writeObjectFile(File file,Object object){
        ObjectOutputStream oos = null;
        try{
            oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(object);
            oos.flush();
            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(oos!=null){
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public static boolean isArchive(String filePath){
        return isArchive(new File(filePath));
    }
    public static boolean isArchive(File file) {
        String n = file.getName();
        if (   n.endsWith(".zip")
            || n.endsWith(".tar")
            || n.endsWith("tar.gz")
            || n.endsWith(".tgz")
            || n.endsWith(".Z")
            || n.endsWith(".jar")
            || n.endsWith(".bzip2") ) {
            return true;
        }
        return false;
    }
}
