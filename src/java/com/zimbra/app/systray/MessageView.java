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
    
    // TODO show interesting flags: urgent, has attachment, etc.
    // TODO possibly show folder name?
    public void setMessage(Message m) {
        String name = m.getSenderName();
        String f = m.getSenderAddress();
        if (name == null || "".equals(name.trim())) {
            name = format("fromFormat2", f);
        } else {
            name = format("fromFormat1", name, m.getSenderAddress());
        }
        from.setText(name);
        from.setPreferredSize(new Dimension(10,
                from.getPreferredSize().height));

        if (m.getSubject() == null || "".equals(m.getSubject().trim())) {
            setComponentVisible("subjectText", false);
            setComponentVisible("$label.subject", false);
        } else {
            setComponentVisible("subjectText", true);
            setComponentVisible("$label.subject", true);
            String s = m.getSubject();
            subject.setText(s);
            subject.setPreferredSize(new Dimension(10,
                    subject.getPreferredSize().height));
        }

        if (m.getFragment() == null || "".equals(m.getFragment().trim())) {
            setComponentVisible("fragmentText", false);
        } else {
            setComponentVisible("fragmentText", true);
            fragment.setText("<html>" + m.getFragment());
            wrapLabel(fragment);
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
    
    private void wrapLabel(JLabel l) {
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
