/*
 *  Copyright 2015 LG CNS.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package scouter.client.stack.base;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import scouter.client.stack.utils.ResourceUtils;

@SuppressWarnings("serial")
public class FilterInputDialog extends JDialog {
    private static FilterInputDialog m_dialog;

    private MainFrame m_mainWindow = null;
    private boolean m_isAscending = true;
    private JTextField m_field = null;
    private int m_jobType = 0;

    public static void init( MainFrame mainWindow, boolean isAscending, int jobType ) {
        try {
            m_dialog = new FilterInputDialog(mainWindow, isAscending, jobType);
            m_dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            m_dialog.setModal(true);
            m_dialog.setIconImage(ResourceUtils.getImageResource("filter.png"));
            m_dialog.setVisible(true);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public static FilterInputDialog get() {
        return m_dialog;
    }

    public FilterInputDialog(MainFrame mainWindow, boolean isAscending, int jobType) {
        super(MainFrame.getFrame());

        m_mainWindow = mainWindow;
        m_isAscending = isAscending;
        m_jobType = jobType;

        setTitle("Input Filter String");
        int [] screen = ResourceUtils.getScreenSize();
        setBounds((screen[0]/2)-300, (screen[1]/2)-50, 600, 100);
        Container container = getContentPane();
        container.setLayout(new BorderLayout());

        m_field = new JTextField();
        m_field.setHorizontalAlignment(SwingConstants.LEFT);
        m_field.setColumns(100);

        container.add(m_field, BorderLayout.CENTER);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        container.add(buttonPane, BorderLayout.SOUTH);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                String filter = m_field.getText();
                if ( filter == null || filter.length() == 0 ) {
                    dispose();
                    return;
                }
                if ( m_jobType == 1 )
                    m_mainWindow.createAnalyzedPerformance(filter, m_isAscending);
                else if ( m_jobType == 2 )
                    m_mainWindow.viewServiceCall(filter);
                else if ( m_jobType == 3 )
                    m_mainWindow.viewThreadStack(filter);
                else if ( m_jobType == 4 )
                    m_mainWindow.analyzeFilterStack(filter, m_isAscending);
                dispose();
            }
        });

        okButton.setActionCommand("OK");
        buttonPane.add(okButton);
        getRootPane().setDefaultButton(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                m_dialog.setEnabled(true);
                dispose();
            }
        });

        cancelButton.setActionCommand("Cancel");
        buttonPane.add(cancelButton);
    }
}
