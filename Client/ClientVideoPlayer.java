package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.Timer;

import Common.UDPDatagram;
import Common.LogEntry;
import Common.VideoMjpeg;

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
    private boolean streamEnded;
    private boolean paused;

    public ClientVideoPlayer(Client client, ClientVideoManager cvm) {
        this.frameQueue = new FrameQueue();
        this.client = client;
        this.cvm = cvm;
        this.framesReceived = 0;
        this.lastFrameUpdate = System.currentTimeMillis();
        this.streamEnded = false;
        this.paused = false;
        initializeGUI();        
    }

    public void initializeGUI() {
        //GUI
        //----
        this.jframe = new JFrame(this.streamName);
        JButton setupButton = new JButton("Setup");
        playButton = new JButton("Play");
        JButton pauseButton = new JButton("Pause");
        JButton tearButton = new JButton("Teardown");
        JPanel mainPanel = new JPanel();
        JPanel buttonPanel = new JPanel();
        iconLabel = new JLabel();

        //Frame
        // adds the listener that implements the behaviour that happens when window is closed
        this.jframe.addWindowListener(new windowCloseListener(this));
    
        //Buttons
        buttonPanel.setLayout(new GridLayout(1,0));
        buttonPanel.add(setupButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tearButton);
    
        // handlers
        //tearButton.addActionListener(new tearButtonListener());
        playButton.addActionListener(new playButtonListener(this, this.client));
        pauseButton.addActionListener(new pauseButtonListener(this, this.client));
    
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

    class windowCloseListener extends WindowAdapter {
        private ClientVideoPlayer cvp;

        public windowCloseListener(ClientVideoPlayer cvp) {
            this.cvp = cvp;
        }

        public void windowClosing(WindowEvent e) {
            client.log(new LogEntry("Window closed! Sending stop stream request!"));
            // stopStream is true because the client closed the Video Player
            client.requestStopStreaming(this.cvp.streamName, true);
            System.exit(0);
        }
    }
    
    class updateFrameListener implements ActionListener {
        private ClientVideoPlayer cvp;
        private Toolkit toolkit;

        public updateFrameListener(ClientVideoPlayer cvp) {
            this.cvp = cvp;
            this.toolkit = Toolkit.getDefaultToolkit();
            //this.closeCounter = 0;
        }

        public void actionPerformed(ActionEvent e) {
            try {
                try {
                    if(!paused){
                        UDPDatagram nextFrame = frameQueue.getNextFrame();
                        //byte[] payload = nextFrame.getPayload();
                        //int payload_length = payload.length;
                        ////get an Image object from the payload bitstream
                        //Image image = toolkit.createImage(payload, 0, payload_length);
                        Image image = VideoMjpeg.decode(nextFrame);
                        //display the image as an ImageIcon object
                        //client.log(new LogEntry("Updating Frame!"));
                        icon = new ImageIcon(image);
                        iconLabel.setIcon(icon);
                        // Updates the time of the last frame update
                        cvp.lastFrameUpdate = System.currentTimeMillis();
                        //this.closeCounter = 0;
                    }
                } catch (NoNextFrameException exception) {
                    // There are no frames in the queue, so we don't update the frame
                    //client.log(new LogEntry("No new frames have arrived!"));
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            } catch (Exception eLogger) {
                eLogger.printStackTrace();
            }
            
        }
    }

    class pauseButtonListener implements ActionListener{
        private Client client;
        private ClientVideoPlayer cvp;

        public pauseButtonListener(ClientVideoPlayer cvp, Client client){
            this.client = client;
            this.cvp = cvp;
        }

        public void actionPerformed(ActionEvent e) {
            this.client.log(new LogEntry("Pause Button pressed!"));
            this.cvp.setPaused(true);
        }        
    }

    class playButtonListener implements ActionListener {
        private Client client;
        private ClientVideoPlayer cvp;

        public playButtonListener(ClientVideoPlayer cvp, Client client){
            this.client = client;
            this.cvp = cvp;
        }

        public void actionPerformed(ActionEvent e){
            this.client.log(new LogEntry("Play Button pressed!"));
            this.cvp.setPaused(false);
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

    public void setPaused(boolean paused){
        this.paused = paused;
    }

    public void setStreamEnded() {
        // Sets the stream ended flag to true
        this.streamEnded = true;
    }

    public boolean streamEnded() {
        // Returns true if the stream has ended (doesnt mean that all the frames have been transmitted in the GUI)
        return this.streamEnded;
    }

    public boolean allFramesRead() {
        // Returns true if all the frames have been read (transmitted in the GUI).
        // This means that the queue of frames is empty
        return this.frameQueue.isEmpty();
    }
}
