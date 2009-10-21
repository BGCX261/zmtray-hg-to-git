package com.zimbra.app.systray;

import java.awt.Dialog;
import java.awt.Window;
import java.io.IOException;

import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.UUID;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.swing.JOptionPane;

import com.hanhuy.common.ui.ResourceBundleForm;
import com.zimbra.app.soap.SoapInterface;

public class TrustManager extends ResourceBundleForm
implements X509TrustManager {
    private final X509TrustManager defaultTrustManager;
    private X509TrustManager ourTrustManager;
    private final ZimbraTray zmtray;
    private ArrayList<X509Certificate> sessionOnlyCerts =
            new ArrayList<X509Certificate>();
    
    private final MessageDigest md5;
    private final MessageDigest sha1;
    
    private static char[] HEX_DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    public TrustManager(ZimbraTray zt) {
        zmtray = zt;
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            javax.net.ssl.TrustManager[] tm = tmf.getTrustManagers();
            if (tm.length == 0)
                throw new IllegalStateException("No trust managers found");
            defaultTrustManager = (X509TrustManager) tm[0];
            initOurTrustManager();
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }
        
        try {
            md5  = MessageDigest.getInstance("MD5");
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void initOurTrustManager()
    throws NoSuchAlgorithmException, KeyStoreException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try {
            ks.load(null, null);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        catch (CertificateException e) {
            throw new IllegalStateException(e);
        }
        Prefs prefs = Prefs.getPrefs();
        for (String name : prefs.getAccountNames()) {
            Account acct = prefs.getAccount(name);
            X509Certificate cert = acct.getCertificate();
            if (cert != null) {
                ks.setCertificateEntry(acct.getAccountName(), cert);
            }
        }
        for (X509Certificate cert : sessionOnlyCerts) {
            ks.setCertificateEntry(UUID.randomUUID().toString(), cert);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        javax.net.ssl.TrustManager[] tm = tmf.getTrustManagers();
        if (tm.length == 0)
            throw new IllegalStateException("No custom trust managers found");
        ourTrustManager = (X509TrustManager) tm[0];
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        throw new UnsupportedOperationException("checkClientTrusted");
    }

    public X509Certificate[] getAcceptedIssuers() {
        return defaultTrustManager.getAcceptedIssuers();
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType)
    throws CertificateException {
        try {
            defaultTrustManager.checkServerTrusted(chain, authType);
        }
        catch (CertificateException e) {
            if (chain == null || chain.length == 0)
                throw e;
            try {
                ourTrustManager.checkServerTrusted(chain, authType);
            }
            catch (CertificateException e2) {
                promptForCertificate(chain, e);
            }
            catch (RuntimeException e2) {
                promptForCertificate(chain, e);
            }
        }
    }

    private void promptForCertificate(
            X509Certificate[] chain, CertificateException e)
    throws CertificateException {

        String[] options = {
            getString("yesAlways"),
            getString("sessionOnly"),
            getString("never")
        };
        URL target = SoapInterface.getCurrentServiceTarget();
        String dn = chain[0].getSubjectDN().getName();
        String caDN = chain[0].getIssuerX500Principal().getName();
        int r = JOptionPane.showOptionDialog(findAlwaysOnTopDialog(),
                format("certificateWarning",
                        target.getHost(), parseDN(dn, "cn"), parseDN(dn, "o"),
                        parseDN(caDN, "cn"), parseDN(caDN, "o"),
                        chain[0].getNotBefore(), chain[0].getNotAfter(),
                        hash(sha1, chain[0]), hash(md5, chain[0])),
                getString("unknownCertificateTitle"),
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[2]);
        switch (r) {
        case 0:
        case 1:
            sessionOnlyCerts.add(chain[0]);
            try {
                initOurTrustManager();
            }
            catch (KeyStoreException ex) {
                throw new IllegalStateException(ex);
            }
            catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException(ex);
            }
            break;
        default:
            throw e;
        }
    }
    
    private static String parseDN(String dn, String field) {
        String[] fields = dn.split("\\s*,\\s*");
        for (String f : fields) {
            if (f.toUpperCase().startsWith(field.toUpperCase() + "=")) {
                return f.substring(f.indexOf('=') + 1);
            }
        }
        return null;
    }

    private static String toHexString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(HEX_DIGITS[(b & 0xf0) >> 4]);
            sb.append(HEX_DIGITS[b & 0xf]);
            sb.append(':');
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private static String hash(MessageDigest digest, X509Certificate cert) {
        try {
            return toHexString(digest.digest(cert.getEncoded()));
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private Window findAlwaysOnTopDialog() {
        Window d = null;
        for (Window w : zmtray.HIDDEN_PARENT.getOwnedWindows()) {
            if (w instanceof Dialog)
                d = findChildDialog((Dialog) w);
        }
        return d == null ? zmtray.HIDDEN_PARENT : d;
    }
    
    private Window findChildDialog(Window d) {
        for (Window w : d.getOwnedWindows()) {
            if (w instanceof Dialog)
                d = findChildDialog((Dialog) w);
        }
        return d;
    }
}
