package io.hyperfoil.tools.yaup.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Created by wreicher
 */
public class ImmutableFile extends File{
    private static final long serialVersionUID = -9021111985835731796L;

    public ImmutableFile(String pathname){
        super(pathname);
    }
    public ImmutableFile(String parent, String child) {
        super(parent,child);
    }
    public ImmutableFile(File parent, String child) {
        super(parent,child);
    }
    public ImmutableFile(URI uri) {
        super(uri);
    }


    //methods which have a clear answer for an ImmableFile
    @Override
    public boolean canWrite(){return false;}



    //Mutable methods we don't support
    @Override
    public boolean createNewFile() throws IOException {return false;}
    @Override
    public boolean delete(){return false;}
    @Override
    public void deleteOnExit(){return;}

    @Override
    public boolean mkdir() {return false;}
    @Override
    public boolean mkdirs() {return false;}
    @Override
    public boolean renameTo(File dest) {return false;}
    @Override
    public boolean setLastModified(long time) {return false;}
    @Override
    public boolean setReadOnly() {return false;}
    @Override
    public boolean setWritable(boolean writable, boolean ownerOnly) {return false;}
    @Override
    public boolean setWritable(boolean writable) {return false;}
    @Override
    public boolean setReadable(boolean readable, boolean ownerOnly) {return false;}
    @Override
    public boolean setReadable(boolean readable) {return false;}
    @Override
    public boolean setExecutable(boolean executable, boolean ownerOnly) {return false;}
    @Override
    public boolean setExecutable(boolean executable) {return false;}
}
