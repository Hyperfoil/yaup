package io.hyperfoil.tools.yaup.linux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileSystems;

/**
 * Created by wreicher
 *
 */
public class Local {
    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    private static final int DEFAULT_PORT = 22;
    private static final String DEFAULT_SSH = "/usr/bin/ssh";
    private static final String DEFAULT_SSH_ADD = "/usr/bin/ssh-add";
    private static final String DEFAULT_RSYNC = "/usr/bin/rsync";
    private static final String DEFAULT_KNOWN_HOSTS = System.getProperty("user.home")+"/.ssh/known_hosts";
    private static final String DEAULLT_IDENTITY = System.getProperty("user.home")+"/.ssh/id_rsa";
    public static final String DEFAULT_PASSPHRASE = null;
    public static final int DEFAULT_SSH_TIMEOUT = 5;

    private String sshPath = DEFAULT_SSH;
    private String sshAddPath = DEFAULT_SSH_ADD;
    private String rsyncPath = DEFAULT_RSYNC;

    private boolean useRsync = true;

    private String identity = DEAULLT_IDENTITY;
    private String knownHosts = DEFAULT_KNOWN_HOSTS;
    private String passphrase = DEFAULT_PASSPHRASE;


    public Local(){
        this(null,null,null);
    }
    public Local(String identity, String knownHosts, String passphrase){
        this.identity = identity!=null && !identity.isEmpty() ? identity : DEAULLT_IDENTITY;
        this.knownHosts = knownHosts!=null && !knownHosts.isEmpty() ? knownHosts : DEFAULT_KNOWN_HOSTS;
        this.passphrase = passphrase!=null && !passphrase.isEmpty() ? passphrase : DEFAULT_PASSPHRASE;
    }
    public void upload(String localPath,String remotePath,String userName,String hostName) {
        upload(localPath,remotePath,userName,hostName,DEFAULT_PORT);
    }
    public void upload(String localPath,String remotePath,String userName,String hostName,int port){
        if(localPath==null || localPath.isEmpty() || remotePath==null || remotePath.isEmpty()){

            return;
        }

        if(useRsync){
            rsyncSend(userName,hostName,port,localPath,remotePath);
        }else{
            logger.error("upload currently only supports rsync");
        }
    }
    public void download(String localPath,String remotePath){


        if(remotePath==null || remotePath.isEmpty() || localPath==null || localPath.isEmpty()){
            //TODO log error
            return;
        }
        if(useRsync){
             String userName = "";
             String hostName = "";
             int idx=-1;
             int port = DEFAULT_PORT;
             if(remotePath.contains(":") && !remotePath.startsWith(":") && !remotePath.endsWith(":")){
                 idx = remotePath.indexOf(":");
                 hostName = remotePath.substring(0,idx);
                 remotePath = remotePath.substring(idx+1);

                 if(hostName.contains("@")){
                     idx = hostName.indexOf("@");
                     userName = hostName.substring(0,idx);
                     hostName = hostName.substring(idx+1);
                 }
                 if(remotePath.contains(":") && !remotePath.startsWith(":")){
                     idx = remotePath.indexOf(":");
                     String possiblePort = remotePath.substring(0,idx);
                     if(possiblePort.matches("\\d+")){
                         port = Integer.parseInt(possiblePort);
                         remotePath = remotePath.substring(idx+1);
                     }
                 }
                 download(localPath,remotePath,userName,hostName,port);
             }else{
                 logger.error("missing host:path for remote");
             }

        }else{
            logger.error("download currently only support rsync");
        }
    }
    public void download(String localPath,String remotePath,String userName,String hostName){
        download(remotePath,localPath,userName,hostName,DEFAULT_PORT);
    }
    public void download(String localPath,String remotePath,String userName,String hostName,int port){

        if(remotePath==null || remotePath.isEmpty() || localPath==null || localPath.isEmpty()){

            return;
        }
        if(useRsync){
            rsyncFetch(userName,hostName,port,localPath,remotePath);
        }else{
            logger.error("download currently only support rsync");
        }
    }


    public void setIdentify(String identify){
        this.identity = identify;
    }
    public String getIdentity(){return this.identity;}
    public boolean hasIdentity(){return !DEAULLT_IDENTITY.equals(getIdentity());}
    public String getKnownHosts(){return knownHosts;}
    public boolean hasKnownHosts(){ return !DEFAULT_KNOWN_HOSTS.endsWith(getKnownHosts());}
    public String getPassphrase(){return passphrase;}
    public boolean hasPassphrase(){return DEFAULT_PASSPHRASE!=getPassphrase();}
    private String prepSshCommand(int port){
        String rtrn = sshPath;
        if(knownHosts!=null && !knownHosts.equals(DEFAULT_KNOWN_HOSTS)){
            rtrn+=" -o UserKnownHostsFile="+knownHosts+" ";
        }
        if(identity!=null && !identity.equals(DEAULLT_IDENTITY)){//default is null so use !=
            rtrn+=" -i "+identity+" ";
        }
        if(passphrase!=null && !passphrase.equals(DEFAULT_PASSPHRASE)){
            storePassphrase(identity,passphrase);
        }
        if(port!=DEFAULT_PORT){
            rtrn+=" -p "+port+" ";
        }
        return rtrn;
    }
    private void storePassphrase(String identity, String passphrase){
        if(passphrase!= DEFAULT_PASSPHRASE){
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(sshAddPath, identity);
            try {
                Process p =  builder.start();
                final InputStream inputStream = p.getInputStream();
                final OutputStream outputStream = p.getOutputStream();
                final InputStream errorStream = p.getErrorStream();

                outputStream.write(passphrase.getBytes());
                outputStream.flush();
                int result = p.waitFor();
                logger.debug("ssh-add.result = {}",result);
                String line = null;
                BufferedReader reader = null;
                reader = new BufferedReader(new InputStreamReader(errorStream));
                while( (line=reader.readLine())!=null){
                    logger.error("  E: {}",line);
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
                while( (line=reader.readLine())!=null){
                    logger.trace("  I: {}",line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private void rsync(String from,String to,String args,String sshOpt){

        ProcessBuilder builder = new ProcessBuilder();
        if(sshPath.equals(sshOpt)){
            builder.command(rsyncPath,args,from,to);
        }else{
            builder.command(rsyncPath,args,"-e",sshOpt,from,to);
        }
        try {
            Process p = builder.start();
            final InputStream inputStream = p.getInputStream();
            final OutputStream outputStream = p.getOutputStream();
            final InputStream errorStream = p.getErrorStream();

            int result = p.waitFor();
            String line = null;
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))){
                while( (line=reader.readLine())!=null){
                    logger.error("  E: {}",line);
                }
            }
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))){
                while( (line=reader.readLine())!=null){
                    logger.trace("  I: {}",line);
                }
            }
        } catch (IOException | InterruptedException e){
            logger.error(e.getMessage(),e);
        }
    }
    private void rsyncSend(String userName,String hostName,int port,String localPath,String remotePath){
        File localFile = new File(localPath);
        if(!localFile.exists()){
            logger.error("{} does not exist",localPath);
            return;
        }
        String sshOpt = prepSshCommand(port);
        rsync(
            localPath,
            (userName==null || userName.isEmpty() ? "" : userName+"@")+hostName+":"+remotePath,
            "-avzI",//--archive --verbose --compress --ignore-times
            sshOpt
        );
    }

    private void rsyncFetch(String userName,String hostName,int port,String localPath,String remotePath){
        File localFile = new File(localPath);
        if(!localFile.exists()){
            if(localFile.isDirectory()){
                localFile.mkdirs();
            }else{
                localFile.getParentFile().mkdirs();
            }
        }
        ProcessBuilder builder = new ProcessBuilder();
        String args = "-avz";//--archive --verbose --compress
        if(remotePath.contains("./")){
            logger.trace("rsync enabling relative mode for {}",remotePath);
            args = args+"R";//turn on relative mode for rsync --relative
        }

        String sshOpts = prepSshCommand(port);

        //TODO handle case with userName but no hostName
        rsync(
                (userName==null || userName.isEmpty() ? "" : userName+"@") +
                    (hostName==null || hostName.isEmpty()? "" : hostName+":") +
                    remotePath,
            localPath,
            args,
            sshOpts
        );
    }
}
