package Common;

import java.awt.Image;
import java.awt.Toolkit;

import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

public class VideoMP4 extends Video {

    public VideoMP4(String videoPath) {
        // TODO: get frame_period from MP4
        super(videoPath, 25);
    }

    // Returns the bytes of the next frame, or returns null if all frames have been read
    public byte[] getNextVideoFrame(){
        return null;
    }

    public static Image decode(UDPDatagram udpDatagram) {
        byte[] payload = udpDatagram.getPayload(); // This payload would be the raw MP4 data

        try {
            // Start FFmpeg process to decode MP4 payload into an image format
            ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg", "-i", "-", "-f", "image2pipe", "-vcodec", "mjpeg", "-");
            builder.redirectErrorStream(true); // To handle both stdout and stderr in one stream
            Process ffmpegProcess = builder.start();

            // Write the MP4 payload to FFmpeg's input stream
            OutputStream ffmpegInput = ffmpegProcess.getOutputStream();
            ffmpegInput.write(payload);
            ffmpegInput.flush();
            ffmpegInput.close();

            // Read the output from FFmpeg into an Image object
            InputStream ffmpegOutput = ffmpegProcess.getInputStream();
            Image image = ImageIO.read(ffmpegOutput);
            
            //ffmpegProcess.waitFor(); // Wait for FFmpeg to complete processing
            return image;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
}