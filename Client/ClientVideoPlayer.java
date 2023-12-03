package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.Timer;

import Common.UDPDatagram;
import Common.LogEntry;

class ClientVideoPlayer {
    private JFrame jframe;
    JLabel iconLabel;
    ImageIcon icon;
    JButton playButton;
    Timer updateFrame;
    private FrameQueue frameQueue;
    private int framesReceived;
    private Client client;
    private ClientVideoManager cvm;
    private String streamName;
    private long lastFrameUpdate;

    public ClientVideoPlayer(Client client, ClientVideoManager cvm) {
        this.frameQueue = new FrameQueue();
        this.client = client;
        this.cvm = cvm;
        this.framesReceived = 0;
        this.lastFrameUpdate = System.currentTimeMillis();
        initializeGUI();        
    }

    public void initializeGUI() {
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
        this.framesReceived++;
    }

    public void setVideoPeriod(int framePeriod) {
        updateFrame = new Timer(framePeriod, new updateFrameListener(this));
        updateFrame.setInitialDelay(0);
        updateFrame.setCoalesce(true);
        updateFrame.start();
        client.log(new LogEntry("Set video period and started the timer"));
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }
    
    class updateFrameListener implements ActionListener {
        private ClientVideoPlayer cvp;
        private Toolkit toolkit;

        // Sets the threshold for missed frame updates for which the video player ends
        // After 'closeThreshHold' amount of straight attempts to update the frame unsuccessfully, this videoPlayer ends.
        //private static int closeThreshHold = 25;
        // counter for the close threshold. When closeCounter is equal to closeThreshHold, the videoplayer ends itself.
        //private int closeCounter;

        public updateFrameListener(ClientVideoPlayer cvp) {
            this.cvp = cvp;
            this.toolkit = Toolkit.getDefaultToolkit();
            //this.closeCounter = 0;
        }

        public void actionPerformed(ActionEvent e) {
            try {
                try {
                    UDPDatagram nextFrame = frameQueue.getNextFrame();
                    byte[] payload = nextFrame.getPayload();
                    int payload_length = payload.length;
                    //get an Image object from the payload bitstream
                    Image image = toolkit.createImage(payload, 0, payload_length);
                    //display the image as an ImageIcon object
                    client.log(new LogEntry("Updating Frame!"));
                    icon = new ImageIcon(image);
                    iconLabel.setIcon(icon);
                    // Updates the time of the last frame update
                    cvp.lastFrameUpdate = System.currentTimeMillis();
                //this.closeCounter = 0;
                } catch (NoNextFrameException exception) {
                    // There are no frames in the queue, so we don't update the frame
                    client.log(new LogEntry("No new frames have arrived!"));
                    //this.closeCounter++;
                    //if (this.closeCounter == closeThreshHold) {
                    //    // update frames attempt threshold has been reached - terminate video player
                    //    cvp.close();
                    //}
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            } catch (Exception eLogger) {
                eLogger.printStackTrace();
            }
            
        }
    }

    class playButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){
            client.log(new LogEntry("Play Button pressed !"));
            //start the timer
            updateFrame.start();
        }
    }

    public void close() {
        // Stops the timer that updates the frames
        client.log(new LogEntry("Received " + this.framesReceived + " frame packets!"));
        this.updateFrame.stop();
        jframe.dispose();
    }

    public long getLastFrameUpdateTime() {
        // returns the instant when the last frame was updated
        return lastFrameUpdate;
    }
}
