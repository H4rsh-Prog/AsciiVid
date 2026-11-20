package com.aar.videostream.service;

import java.util.ArrayList;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_highgui;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.springframework.stereotype.Service;

@Service
public class AARService {
    final public char GRADIENT_ARRAY[] = " .,:;+*?%S#@".toCharArray();
    final int fontFace = opencv_imgproc.FONT_HERSHEY_SIMPLEX;
	final double fontScale = 0.1;
	final Scalar fontColor = new Scalar(255, 255, 255, 0);
	final int thickness = 1;
	final int lineType = opencv_imgproc.LINE_8;
    private FPSCounter fpsCounter;
	
	public char[][] generateAsciiArt(Mat image) {
		int rows = image.rows();
		int cols = image.cols();
		char[][] ascii = new char[rows][cols];
		for (int y = 0; y < rows; y++) {
			org.bytedeco.javacpp.BytePointer row = image.ptr(y);
			for (int x = 0; x < cols; x++) {
				int pixelValue = row.get(x) & 0xFF;
				int gradientIndex = (pixelValue * (GRADIENT_ARRAY.length - 1)) / 255;
				ascii[y][x] = GRADIENT_ARRAY[gradientIndex];
			}
		}
		return ascii;
	}

	private byte[][] createGlyphCache(int diffScale) {
		byte[][] glyphCache = new byte[128][];
		for (char character : GRADIENT_ARRAY) {
			Mat glyph = new Mat(diffScale, diffScale, opencv_core.CV_8UC1, new Scalar(0, 0, 0, 0));
			opencv_imgproc.putText(glyph, String.valueOf(character), new Point(0, diffScale), fontFace, fontScale, fontColor, thickness, lineType, false);
			byte[] glyphPixels = new byte[diffScale * diffScale];
			glyph.data().get(glyphPixels);
			glyphCache[character] = glyphPixels;
			glyph.close();
		}
		return glyphCache;
	}

	private void renderAsciiFrame(char[][] frame, int diffScale, int displayWidth, byte[][] glyphCache, byte[] framePixels, Mat display) {
		for (int i = 0; i < frame.length; i++) {
			for (int j = 0; j < frame[i].length; j++) {
				byte[] glyphPixels = glyphCache[frame[i][j]];
				for (int glyphRow = 0; glyphRow < diffScale; glyphRow++) {
					int sourceOffset = glyphRow * diffScale;
					int targetOffset = (i * diffScale + glyphRow) * displayWidth + j * diffScale;
					System.arraycopy(glyphPixels, sourceOffset, framePixels, targetOffset, diffScale);
				}
			}
		}
		display.data().put(framePixels);
	}

    public void processVideoStream(VideoCapture camera, int scale_y, int scale_x, int diffScale, boolean fpsToggle) {
    	if(!camera.isOpened()) throw new RuntimeException("Camera is not Opened");
    	int x = -1;
    	Mat frame = new Mat();
    	Mat grayFrame = new Mat();
    	Mat resizedFrame = new Mat();
		int displayWidth = scale_x * diffScale;
		int displayHeight = scale_y * diffScale;
		byte[][] glyphCache = createGlyphCache(diffScale);
		byte[] framePixels = new byte[displayWidth * displayHeight];
		Mat display = new Mat(displayHeight, displayWidth, opencv_core.CV_8UC1, new Scalar(0, 0, 0, 0));
		if(!fpsToggle) {fpsCounter = new FPSCounter();}
    	
		while(!camera.read(frame)) continue;
	    
    	while(camera.read(frame) && !frame.empty()) {
    		opencv_imgproc.cvtColor(frame, grayFrame, opencv_imgproc.COLOR_BGR2GRAY);
    		opencv_imgproc.resize(grayFrame, resizedFrame, new Size(scale_x, scale_y));
    		char[][] result = generateAsciiArt(resizedFrame);
	    		renderAsciiFrame(result, diffScale, displayWidth, glyphCache, framePixels, display);
			if(!fpsToggle) {
				double fps = fpsCounter.getFPS(System.nanoTime());
				opencv_imgproc.putText(display, String.format("FPS : %f", fps), new Point(10, 30), fontFace, fontScale*8, fontColor);
			}
    		opencv_highgui.imshow("ascii_stream", display);
    		x = opencv_highgui.waitKey(1);
    		if(x!=-1) break;
    	}
    	opencv_highgui.destroyWindow("ascii_stream");
		display.close();
    	camera.release();
    }
    
    public ArrayList<char[][]> processVideoObject(VideoCapture camera, int scale_y, int scale_x) {
    	if(!camera.isOpened()) throw new RuntimeException("Camera is not Opened");
    	int x = -1;
    	ArrayList<char[][]> response = new ArrayList<>();
    	Mat frame = new Mat();
    	Mat grayFrame = new Mat();
    	Mat resizedFrame = new Mat();
    	
    	while(!camera.read(frame)) continue;
	    int count = 0;
    	while(camera.read(frame) && !frame.empty()) {
    		opencv_imgproc.cvtColor(frame, grayFrame, opencv_imgproc.COLOR_BGR2GRAY);
    		opencv_imgproc.resize(grayFrame, resizedFrame, new Size(scale_x, scale_y));
        	Mat display = new Mat(100, 200, opencv_core.CV_8UC1, new Scalar(0,0,0,0));
        	opencv_imgproc.putText(display, "Press any key to stop video processing", new Point(0, 24), 1, 1, new Scalar(255,255,255,0));
        	opencv_imgproc.putText(display, "frames processed {"+count+"}", new Point(0, 12), 1, 1, new Scalar(255,255,255,0));
    		char[][] result = generateAsciiArt(resizedFrame);
    		response.add(result);
    		opencv_highgui.imshow("ascii_stream", display);
    		x = opencv_highgui.waitKey(1);
    		if(x!=-1) break;
    		count++;
    	}
    	opencv_highgui.destroyWindow("ascii_stream");
    	camera.release();
    	return response;
    }
    
    public void playProcessedObject(ArrayList<char[][]> processedObject) {
    	int scale_x = processedObject.get(0)[0].length;
    	int scale_y = processedObject.get(0).length;
    	int x = -1;
    	int diffScale = 5;
		int displayWidth = scale_x * diffScale;
		int displayHeight = scale_y * diffScale;
		byte[][] glyphCache = createGlyphCache(diffScale);
		Mat display = new Mat(displayHeight, displayWidth, opencv_core.CV_8UC1, new Scalar(0, 0, 0, 0));
		byte[] framePixels = new byte[displayWidth * displayHeight];
    	
		for(char[][] frame : processedObject) {
			renderAsciiFrame(frame, diffScale, displayWidth, glyphCache, framePixels, display);
			opencv_highgui.imshow("ascii_stream", display);
    		x = opencv_highgui.waitKey(1);
    		if(x!=-1) break;
    	}
    	opencv_highgui.destroyWindow("ascii_stream");
		display.close();
    }
}
