package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import Common.UDPDatagram;

class ClientVideoPlayer {
    private JFrame jframe;
    private JLabel iconLabel;
    private ImageIcon icon;

    public ClientVideoPlayer() {
        System.out.println(java.awt.GraphicsEnvironment.isHeadless());
        //GUI
        //----
        this.jframe = new JFrame("Cliente de Testes");
        JButton setupButton = new JButton("Setup");
        JButton playButton = new JButton("Play");
        JButton pauseButton = new JButton("Pause");
        JButton tearButton = new JButton("Teardown");
        JPanel mainPanel = new JPanel();
        JPanel buttonPanel = new JPanel();
        this.iconLabel = new JLabel();

        //Frame
        this.jframe.addWindowListener(new WindowAdapter() {
           public void windowClosing(WindowEvent e) {
         System.exit(0);
           }
        });
    
        //Buttons
        buttonPanel.setLayout(new GridLayout(1,0));
        buttonPanel.add(setupButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tearButton);
    
        // handlers... (so dois)
        //playButton.addActionListener(new playButtonListener());
        //tearButton.addActionListener(new tearButtonListener());
    
        //Image display label
        iconLabel.setIcon(null);
        
        //frame layout
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        iconLabel.setBounds(0,0,380,280);
        buttonPanel.setBounds(0,280,380,50);
    
        this.jframe.getContentPane().add(mainPanel, BorderLayout.CENTER);
        this.jframe.setSize(new Dimension(390,370));
        this.jframe.setVisible(true);
    }

    public void updateFrame(UDPDatagram datagram) {
        byte[] payload = datagram.getPayload();
        int payload_length = payload.length;

        //get an Image object from the payload bitstream
	    Toolkit toolkit = Toolkit.getDefaultToolkit();
	    Image image = toolkit.createImage(payload, 0, payload_length);
	    //display the image as an ImageIcon object
	    this.icon = new ImageIcon(image);
	    this.iconLabel.setIcon(this.icon);
    }
}