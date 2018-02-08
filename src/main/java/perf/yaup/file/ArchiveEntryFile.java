package perf.yaup.file;

import java.util.LinkedList;
import java.util.List;

/**
 * Not ready
 * Created by wreicher
 */
public class ArchiveEntryFile extends ImmutableFile {

    private String pathName;
    private String entryPath;
    private boolean isDirectory = false;
    private boolean exists = false;
    private List<String> children;
    public ArchiveEntryFile(String pathName){
        super(FileUtility.getArchiveFilePath(pathName));

        this.pathName = pathName;
        this.entryPath = FileUtility.getArchiveEntrySubPath(pathName);
        this.isDirectory = entryPath.endsWith("/");

        this.children = new LinkedList<>();
        populateChildren();
    }

    private void populateChildren(){
        List<String> entries = FileUtility.getArchiveEntries(pathName);
        for(String entry : entries){
            if(entry.startsWith(this.entryPath) && isDirectory){
                children.add(entry);
                exists = true;
            }
            if(entry.equals(entryPath) && !isDirectory){
                exists = true;
            }
        }
    }


}
