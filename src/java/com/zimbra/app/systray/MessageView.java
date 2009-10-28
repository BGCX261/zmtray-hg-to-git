package com.zimbra.app.systray;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.hanhuy.common.ui.ResourceBundleForm;

public class MessageView extends ResourceBundleForm {
    private JComponent component = new JPanel();
    
    private int preferredWidth;
    
    private final static int ELIDE_LENGTH = 72;
    
    private JLabel from     = new JLabel();
    private JLabel subject  = new JLabel();
    private JLabel fragment = new JLabel();
    
    public MessageView() {
        resetPreferredWidth();
        component.setLayout(createLayoutManager());
        layout();
    }
    public MessageView(Message m) {
        this();
        setMessage(m);
    }

    private void layout() {
        fragment.setVerticalAlignment(JLabel.NORTH);
        component.add(from,     "fromText");
        component.add(subject,  "subjectText");
        component.add(fragment, "fragmentText");
    }

    public void resetPreferredWidth() {
        preferredWidth = getInt("preferredWidth");
    }
    
    public Component getComponent() {
        return component;
    }
    
    public void setMessage(Message m) {
        String name = m.getSenderName();
        String f = m.getSenderAddress();
        if (name == null || "".equals(name.trim())) {
            if (f.length() > ELIDE_LENGTH)
                f = name.substring(0, ELIDE_LENGTH) + getString("elideString");
            name = format("fromFormat2", f);
        } else {
            if (name.length() + f.length() > ELIDE_LENGTH) {
                int i = ELIDE_LENGTH - f.length();
                name = name.substring(0, i);
            }
            name = format("fromFormat1", name, m.getSenderAddress());
        }
        from.setText(name);

        if (m.getSubject() == null || "".equals(m.getSubject().trim())) {
            setComponentVisible("subjectText", false);
            setComponentVisible("$label.subject", false);
        } else {
            setComponentVisible("subjectText", true);
            setComponentVisible("$label.subject", true);
            String s = m.getSubject();
            if (s.length() > ELIDE_LENGTH) {
                s = s.substring(0, ELIDE_LENGTH) + getString("elideString");
            }
            subject.setText(s);
        }

        if (m.getFragment() == null || "".equals(m.getFragment().trim())) {
            setComponentVisible("fragmentText", false);
        } else {
            setComponentVisible("fragmentText", true);
            fragment.setText("<html>" + m.getFragment());
            wrapLabel(fragment, getWidth(subject));
        }
    }
    
    private void setComponentVisible(String name, boolean b) {
        Component[] comps = component.getComponents();
        for (Component c : comps) {
            if (name.equals(c.getName())) {
                c.setVisible(b);
                break;
            }
        }
        component.invalidate();
    }
    
    private int getWidth(JLabel l) {
        if (!l.isVisible())
            return 0;
        Font f = l.getFont();
        return l.getFontMetrics(f).stringWidth(l.getText());
    }

    private void wrapLabel(JLabel l, int width) {
        preferredWidth = Math.max(preferredWidth, width);
        Font f = l.getFont();
        FontMetrics fm = l.getFontMetrics(f);
        int fontHeight = fm.getHeight();
        int stringWidth = fm.stringWidth(l.getText());
        int linesCount = (int) Math.floor(stringWidth / preferredWidth);
        linesCount = Math.max(1, linesCount + 1);
        l.setPreferredSize(new Dimension(preferredWidth,
                (fontHeight+2)*linesCount));
    }
}
