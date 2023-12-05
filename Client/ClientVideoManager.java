package Client;

import Client.ClientVideoManager;

import Common.UDPDatagram;
import Common.VideoMetadata;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ClientVideoManager {
    private Client client;
    private Map<String, ClientVideoPlayer> videoPlayers;
    private Timer checkInactivePlayersTimer;

    public ClientVideoManager(Client client) {
        this.client = client;
        this.videoPlayers = new HashMap<>();
        startInactivePlayersCheck();
    }

    private void startInactivePlayersCheck() {
        // Starts the timer which will periodically look for video players which haven't been updated in 5 seconds
        // This timer does the check every second
        checkInactivePlayersTimer = new Timer();
        checkInactivePlayersTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkAndCloseInactivePlayers();
            }
        }, 0, 1000); // Check every second
    }

    /*
     * Periodically checks all the Video Players in order to close the ones whose stream has finished
     * If the last time the frame was updated was 5 seconds ago, closes the video player
     */
    private void checkAndCloseInactivePlayers() {
        // Iterates over the set of <key,value> pairs

        List<String> endingStreams = new ArrayList<>();
        for (Map.Entry<String, ClientVideoPlayer> entry : videoPlayers.entrySet()) {
            String streamName = entry.getKey();
            ClientVideoPlayer cvp = entry.getValue();
            if ((cvp.streamEnded() && cvp.allFramesRead())) {
                // Close player if the stream has ended and all frames have been read
                endingStreams.add(streamName);
            }
        }

        for(String stream: endingStreams)
            closePlayer(stream);
    }

    /*
     * Adds a frame to the respective video player
     * If the video player hasn't been created, it is created before adding
     */
    public synchronized void addFrame(UDPDatagram udpDatagram) {
        String streamName = udpDatagram.getStreamName();
        int framePeriod = udpDatagram.getFramePeriod();
        ClientVideoPlayer cvp = null;
        if (!videoPlayers.containsKey(streamName)) {

            cvp = createVideoPlayer(streamName);


            cvp.setVideoPeriod(framePeriod);


            cvp.setStreamName(streamName);
            
        } else {
            cvp = videoPlayers.get(streamName);
        }

        cvp.addFrame(udpDatagram);
    }

    // synchronized so that the same stream isn't added twice
    public synchronized void updateVideoInfo(VideoMetadata vm) {
        //gets video metadata of the stream

        String streamName = vm.getStreamName();
        if (!videoPlayers.containsKey(streamName)) {
            createVideoPlayer(streamName);
        }
        // Get the video player respective to the metadata of the stream received,
        // and update its frame period
        ClientVideoPlayer cvp = videoPlayers.get(streamName);
        cvp.setVideoPeriod(vm.getFramePeriod());
        cvp.setStreamName(streamName);
    }

    public ClientVideoPlayer createVideoPlayer(String streamName) {
        // Creates a new video player
        ClientVideoPlayer cvp = new ClientVideoPlayer(client, this);
        videoPlayers.put(streamName, cvp);
        return cvp;
    }

    /*
     * Closes the Video Player with the corresponding stream name
     */
    public void closePlayer(String streamName) {
        if (videoPlayers.containsKey(streamName)) {
            videoPlayers.get(streamName).close();
            videoPlayers.remove(streamName);
        }
    }

    /*
     * Closes all video players
     */
    public void closeAllPlayers() {
        videoPlayers.clear();
    }

    public void streamEnded(String streamName) {
        if (videoPlayers.containsKey(streamName)) {
            videoPlayers.get(streamName).setStreamEnded();
        }
    }
}