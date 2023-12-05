package Client;

import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/*
* Class responsible to show the available streams in the client using GUI
*/

public class ClientGUI {
    private Client client;

    private JFrame jframe;
    private JPanel mainPanel;
    private JLabel text;
    private JPanel buttonsPanel;
    private List<JButton> buttons;
    private JPanel exitPanel;
    private JButton exit;

    public ClientGUI(Client c){
        this.client = c;
        initGUI();
    }

    public void initGUI(){
        this.jframe = new JFrame("Client GUI");
        this.mainPanel = new JPanel();
        this.text = new JLabel("Selecione uma das seguintes streams:");
        this.buttonsPanel = new JPanel();
        this.exitPanel = new JPanel();

        this.jframe.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
          System.exit(0);
            }
        });

        // Buttons and ButtonsPanel
        List<String> streams = this.client.getAvailableStreamsList();
        this.buttons = new ArrayList<>();

        this.buttonsPanel.setLayout(new GridLayout(streams.size(), 1));
        for (int i = 0; i < streams.size(); i++) {
            JButton button = new JButton(streams.get(i));
            this.buttons.add(button);
            this.buttonsPanel.add(this.buttons.get(i));
            // handler
            button.addActionListener(new ButtonListener(i+1, this.client));
        }

        // Exit panel
        this.exit = new JButton("Exit");
        this.exitPanel.add(this.exit);
        // handler
        this.exit.addActionListener(new ExitListener(this.jframe));

        //main layout
        this.mainPanel.setLayout(null);
        this.mainPanel.add(this.text);
        this.mainPanel.add(this.buttonsPanel);
        this.mainPanel.add(exitPanel);
        this.text.setBounds(0,0,380, 20);
        this.buttonsPanel.setBounds(0,20,380,280);
        this.exitPanel.setBounds(0,300,380,30);
    
        this.jframe.getContentPane().add(mainPanel, BorderLayout.CENTER);
        this.jframe.setSize(new Dimension(390,370));
        this.jframe.setVisible(true);
    }

    class ButtonListener implements ActionListener{
        private int streamID;
        private Client client;

        public ButtonListener(int streamID, Client client){
            this.streamID = streamID;
            this.client = client;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            this.client.requestStreaming(this.streamID);
        }
    }

    class ExitListener implements ActionListener{
        JFrame frame; 

        public ExitListener(JFrame frame){
            this.frame = frame;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            this.frame.dispose();
        }
    }
}
