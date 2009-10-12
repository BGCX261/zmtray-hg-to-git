package com.zimbra.app.systray;

import java.security.GeneralSecurityException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Prefs {

    private final static String SYMMETRIC_ALGORITHM = "AES";
    private final static String SECRET_KEY = "secret";
    private final static String PORT_KEY = "localPort";
    private final static Prefs INSTANCE;
    private final SecretKey key;
    private final Cipher cipher;

    static {
        try {
            INSTANCE = new Prefs();
        }
        catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
    private final Preferences prefs;

    private Prefs() throws GeneralSecurityException {
        prefs = Preferences.userNodeForPackage(Prefs.class);

        byte[] keyBytes = prefs.getByteArray(SECRET_KEY, null);
        if (keyBytes == null) { // generate a secret key to encrypting passwords
            KeyGenerator kg = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM);
            SecretKey newKey = kg.generateKey();
            keyBytes = newKey.getEncoded();
            prefs.putByteArray(SECRET_KEY, keyBytes);
        }
        key = new SecretKeySpec(keyBytes, SYMMETRIC_ALGORITHM);
        cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
    }

    public static Prefs getPrefs() { return INSTANCE; }

    public String[] listAccounts() {
        try {
            return prefs.childrenNames();
        }
        catch (BackingStoreException e) {
            throw new IllegalStateException(e);
        }
    }

    public Account getAccount(String name) {
        try {
            if (prefs.nodeExists(name)) {
                return new Account(prefs.node(name), cipher, key);
            }
        }
        catch (BackingStoreException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }
    
    public int getPort() {
        return prefs.getInt(PORT_KEY, -1);
    }
    
    public void setPort(int port) {
        prefs.putInt(PORT_KEY, port);
    }
}
