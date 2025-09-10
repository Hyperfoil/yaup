package io.hyperfoil.tools.yaup.hash;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by wreicher
 */
public class HashFactory {

    private static final byte[] HEX_CHAR_TABLE = {(byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'};

    private String digestEncoding;

    public HashFactory(){
        this("MD5");
    }
    public HashFactory(String digestEncoding){
        this.digestEncoding = digestEncoding;
    }

    private String getHexString(byte[] raw) throws UnsupportedEncodingException {
        byte[] hex = new byte[2*raw.length];
        int index = 0;


        for(byte b : raw){
            int v = b & 0xFF;
            hex[index++]=HEX_CHAR_TABLE[v >>> 4];
            hex[index++]=HEX_CHAR_TABLE[v & 0xF];
        }
        return new String(hex,"ASCII");
    }
    public String getStringHash(String input){
        MessageDigest md = null;
        try{
            md = MessageDigest.getInstance(digestEncoding);
            md.reset();
            md.update(input.getBytes("UTF-8"));
            return getHexString(md.digest());
        } catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }
    public String getInputHash(InputStream input){
        MessageDigest md = null;
        InputStream is = null;
        try{
            md = MessageDigest.getInstance(digestEncoding);
            is = new DigestInputStream(input,md);
            byte[] toRead = new byte[1024];

            while(is.read(toRead)!= -1){}

            byte[] digest = md.digest();
            return getHexString(digest);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(is!=null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }
}
