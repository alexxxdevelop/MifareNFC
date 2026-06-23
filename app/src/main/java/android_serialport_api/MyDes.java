package android_serialport_api;

import android.annotation.SuppressLint;
import android.util.Log;

import java.security.SecureRandom;
import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.SecretKeySpec;


@SuppressLint("DefaultLocale")
public class MyDes {

    static public byte[] ecbEncrypt(byte[] key, byte[] data)
    {
        try{

            byte[] tempkey = new byte[24];
            if(key.length==24){
                System.arraycopy( key,0 , tempkey, 0, 24);
            }
            else if(key.length==16){
                System.arraycopy( key,0 , tempkey, 0, 16);
                System.arraycopy( key,0 , tempkey, 16, 8);
            }
            else if(key.length==8){
                System.arraycopy( key,0 , tempkey, 0, 8);
                System.arraycopy( key,0 , tempkey, 8, 8);
                System.arraycopy( key,0 , tempkey, 16, 8);
            }
            else {
                System.arraycopy( key,0 , tempkey, 0, 8);
                System.arraycopy( key,0 , tempkey, 8, 8);
                System.arraycopy( key,0 , tempkey, 16, 8);
            }

            SecureRandom sr = new SecureRandom();
            DESedeKeySpec dks = new DESedeKeySpec( tempkey );
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey securekey = keyFactory.generateSecret(dks);
            Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, securekey, sr);

            return cipher.doFinal(data);
        }
        catch(Exception e)
        {
            Log.d("des","setControls:"+e.getMessage());
        }

        return null;
    }

    static public byte[] cbcEncrypt(byte[] key, byte[] iv, byte[] data)
    {
        try{
            byte[] tempkey = new byte[24];
            if(key.length==24){
                System.arraycopy( key,0 , tempkey, 0, 24);
            }
            else if(key.length==16){
                System.arraycopy( key,0 , tempkey, 0, 16);
                System.arraycopy( key,0 , tempkey, 16, 8);
            }
            else if(key.length==8){
                System.arraycopy( key,0 , tempkey, 0, 8);
                System.arraycopy( key,0 , tempkey, 8, 8);
                System.arraycopy( key,0 , tempkey, 16, 8);
            }
            else {
                System.arraycopy( key,0 , tempkey, 0, 8);
                System.arraycopy( key,0 , tempkey, 8, 8);
                System.arraycopy( key,0 , tempkey, 16, 8);
            }
            SecureRandom sr = new SecureRandom();
            DESedeKeySpec dks = new DESedeKeySpec( tempkey );
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey securekey = keyFactory.generateSecret(dks);
            IvParameterSpec ivc = new IvParameterSpec( iv );
            Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, securekey, ivc, sr);


            return cipher.doFinal(data);
        }
        catch(Exception e)
        {
            Log.d("des","setControls:"+e.getMessage());
        }

        return null;
    }

    static public byte[] ecbDecrypt(byte[] key, byte[] data)
    {
        try{

            byte[] tempkey = new byte[24];
            if(key.length==24){
                System.arraycopy( key,0 , tempkey, 0, 24);
            }
            else if(key.length==16){
                System.arraycopy( key,0 , tempkey, 0, 16);
                System.arraycopy( key,0 , tempkey, 16, 8);
            }
            else if(key.length==8){
                System.arraycopy( key,0 , tempkey, 0, 8);
                System.arraycopy( key,0 , tempkey, 8, 8);
                System.arraycopy( key,0 , tempkey, 16, 8);
            }
            else {
                System.arraycopy( key,0 , tempkey, 0, 8);
                System.arraycopy( key,0 , tempkey, 8, 8);
                System.arraycopy( key,0 , tempkey, 16, 8);
            }

            SecureRandom sr = new SecureRandom();
            DESedeKeySpec dks = new DESedeKeySpec( tempkey );
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey securekey = keyFactory.generateSecret(dks);
            Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, securekey, sr);

            return cipher.doFinal(data);
        }
        catch(Exception e)
        {
            Log.d("des","setControls:"+e.getMessage());
        }

        return null;
    }

    static public byte[] cbcDecrypt(byte[] key, byte[] iv, byte[] data)
    {
        try{
            byte[] tempkey = new byte[24];
            if(key.length==24){
                System.arraycopy( key,0 , tempkey, 0, 24);
            }
            else if(key.length==16){
                System.arraycopy( key,0 , tempkey, 0, 16);
                System.arraycopy( key,0 , tempkey, 16, 8);
            }
            else if(key.length==8){
                System.arraycopy( key,0 , tempkey, 0, 8);
                System.arraycopy( key,0 , tempkey, 8, 8);
                System.arraycopy( key,0 , tempkey, 16, 8);
            }
            else {
                System.arraycopy( key,0 , tempkey, 0, 8);
                System.arraycopy( key,0 , tempkey, 8, 8);
                System.arraycopy( key,0 , tempkey, 16, 8);
            }
            SecureRandom sr = new SecureRandom();
            DESedeKeySpec dks = new DESedeKeySpec( tempkey );
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey securekey = keyFactory.generateSecret(dks);
            IvParameterSpec ivc = new IvParameterSpec( iv );
            Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, securekey, ivc, sr);


            return cipher.doFinal(data);
        }
        catch(Exception e)
        {
            Log.d("des","setControls:"+e.getMessage());
        }

        return null;
    }
}
