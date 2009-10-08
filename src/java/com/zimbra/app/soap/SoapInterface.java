package com.zimbra.app.soap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;

public class SoapInterface {
    private final static boolean DEBUG = true;
    private final static MessageFactory factory;
    static {
        try {
            factory = MessageFactory.newInstance(
                    SOAPConstants.SOAP_1_2_PROTOCOL);
        }
        catch (SOAPException e) {
            throw new IllegalStateException(e);
        }
    }
    public static SOAPMessage newMessage() throws SOAPException {
        return factory.createMessage();
    }

    public static SOAPMessage call(SOAPMessage r, URL u)
    throws IOException, SOAPException {
        URLConnection c =  u.openConnection();
        c.setDoOutput(true);
        c.setDoInput(true);
        c.connect();
        OutputStream out = c.getOutputStream();
        InputStream in = null;
        try {
            if (DEBUG) {
                System.out.println("\nREQUEST:");
                r.writeTo(System.out);
            }
            r.writeTo(out);
            in = c.getInputStream();
            SOAPMessage response = factory.createMessage(null, in);
            if (DEBUG) {
                System.out.println("\nRESPONSE:");
                response.writeTo(System.out);
            }
            return response;
        }
        finally {
            out.close();
            if (in != null)
                in.close();
        }
    }
}
