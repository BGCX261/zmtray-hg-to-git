package com.zimbra.app.systray;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

import com.hanhuy.common.ui.ResourceBundleForm;

public class ZimbraTray extends ResourceBundleForm implements Runnable {
    private boolean hasSystemTray = false;
    
    private TrayIcon trayicon;
    
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        EventQueue.invokeLater(new ZimbraTray());
    }
    
    public ZimbraTray() {
        checkIfRunning();
    }

    public void run() {
        setupSystemTray();
        Prefs prefs = Prefs.getPrefs();

        List<String> names = prefs.getAccountNames();

        if (names.size() == 0) {
            // show configuration/add accounts dialog
            NewAccountForm form = new NewAccountForm();
            form.show();
        } else {
            // perform logins and start polling
            for (String name : names) {
                Account account = prefs.getAccount(name);
                AccountHandler handler = new AccountHandler(account, this);
                Thread t = new Thread(handler,
                        "Account Handler: " + account.getAccountName());
                t.start();
            }
        }
    }
    
    private void checkIfRunning() {
        TrayServer ts = new TrayServer();
        if (ts.checkIfRunning())
            System.exit(0);
        ts.start();
    }

    private void setupSystemTray() {
        SystemTray tray;
        
        try {
            tray = SystemTray.getSystemTray();
        }
        catch (NoClassDefFoundError e) {
            return;
        }
        if (!SystemTray.isSupported())
            return;
        
        hasSystemTray = true;
        
        ImageIcon icon = (ImageIcon) getIcon("emailIcon");
        PopupMenu menu = new PopupMenu();
        MenuItem item = new MenuItem(getString("webmailMenu"));
        menu.add(item);
        item = new MenuItem(getString("optionsMenu"));
        menu.add(item);
        menu.addSeparator();
        item = new MenuItem(getString("exitMenu"));
        menu.add(item);
        trayicon = new TrayIcon(icon.getImage(),
                getString("defaultToolTip"), menu);
        trayicon.setImageAutoSize(true);
        try {
            tray.add(trayicon);
        }
        catch (AWTException e) {
            e.printStackTrace();
        }
    }
    
    public void setTrayIcon(Image icon) {
        if (!hasSystemTray)
            return;
        trayicon.setImage(icon);
    }
    
    public void setTrayTip(String text) {
        if (!hasSystemTray)
            return;
        trayicon.setToolTip(text);
    }
    
    public void setTrayMessage(String title, String text) {
        if (!hasSystemTray)
            return;
        trayicon.displayMessage(title, text, TrayIcon.MessageType.NONE);
    }
    public void setTrayError(String title, String text) {
        if (!hasSystemTray)
            return;
        trayicon.displayMessage(title, text, TrayIcon.MessageType.ERROR);
    }
}
