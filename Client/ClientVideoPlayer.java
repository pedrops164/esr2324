package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.Timer;

import Common.UDPDatagram;

import Client.FrameQueue;

class ClientVideoPlayer {
    private JFrame jframe;
    JLabel iconLabel;
    ImageIcon icon;
    JButton playButton;
    Timer updateFrame;
    private FrameQueue frameQueue;

    public ClientVideoPlayer() {
        this.frameQueue = new FrameQueue();
        //GUI
        //----
        this.jframe = new JFrame("Cliente de Testes");
        JButton setupButton = new JButton("Setup");
        playButton = new JButton("Play");
        JButton pauseButton = new JButton("Pause");
        JButton tearButton = new JButton("Teardown");
        JPanel mainPanel = new JPanel();
        JPanel buttonPanel = new JPanel();
        iconLabel = new JLabel();

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
    
        // handlers
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

    public void addFrame(UDPDatagram datagram) {
        this.frameQueue.addPacket(datagram);
    }

    public void setVideoPeriod(int framePeriod) {
        updateFrame = new Timer(framePeriod, new updateFrameListener(this));
        updateFrame.setInitialDelay(0);
        updateFrame.setCoalesce(true);
        updateFrame.start();
        System.out.println("Set video period and started the timer");
    }
    
    class updateFrameListener implements ActionListener {
        private ClientVideoPlayer cvp;
        private Toolkit toolkit;

        public updateFrameListener(ClientVideoPlayer cvp) {
            this.cvp = cvp;
            this.toolkit = Toolkit.getDefaultToolkit();
        }

        public void actionPerformed(ActionEvent e) {
            System.out.println("Updating Frame!");
            try {
                UDPDatagram nextFrame = frameQueue.getNextFrame();
                byte[] payload = nextFrame.getPayload();
                int payload_length = payload.length;
                //get an Image object from the payload bitstream
                Image image = toolkit.createImage(payload, 0, payload_length);
                //display the image as an ImageIcon object
                icon = new ImageIcon(image);
                iconLabel.setIcon(icon);
            } catch (NoNextFrameException exception) {
                // There are no frames in the queue, so we don't update the frame
                System.out.println("No new frames have arrived!");
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            
        }
    }

    class playButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){
            System.out.println("Play Button pressed !"); 
            //start the timers ... 
            updateFrame.start();
        }
      }
}
