import swiftbot.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Scanner;

//direction class
class Direction {
    String name;
    double brightness;

    Direction(String name, double brightness) {
        this.name = name;
        this.brightness = brightness;
    }
}

public class Main {
    static Scanner scanner = new Scanner(System.in);
	static SwiftBotAPI swiftBot;
	// ANSI color codes
	static final String RESET = "\u001B[0m";
	static final String CYAN = "\u001B[36m";
	static final String YELLOW = "\u001B[33m";
	static final String GREEN = "\u001B[32m";
	static final String WHITE = "\u001B[37m";
	static final String BOLD = "\u001B[1m";

    public static double[] splitBrightness(BufferedImage image) {

        int width = image.getWidth();
        int height = image.getHeight();
        int thirdWidth = width / 3;

        BufferedImage leftImage = image.getSubimage(0, 0, thirdWidth, height);
        BufferedImage centreImage = image.getSubimage(thirdWidth, 0, thirdWidth, height);
        BufferedImage rightImage = image.getSubimage(thirdWidth * 2, 0, width - (thirdWidth * 2), height);

        double avg_left = calculateAverageBrightness(leftImage);
        double avg_centre = calculateAverageBrightness(centreImage);
        double avg_right = calculateAverageBrightness(rightImage);

        return new double[]{avg_left, avg_centre, avg_right};
    }

    //image capturing
    public static BufferedImage captureImage() {
        try {
            BufferedImage image = swiftBot.takeStill(ImageSize.SQUARE_240x240);

            if (image == null) {
                System.out.println("ERROR: Image is null");
                return null;
            }
            return image;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: Failed to capture image.");
            return null;
        }
    }

    //helper method for image saving
    public static void captureAndSaveImage(String path) {
        try {
            BufferedImage image = captureImage();

            if (image != null) {
                ImageIO.write(image, "png", new File(path));
                System.out.println("Image saved: " + path);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //underlights
    public static void blinkRed() throws InterruptedException {
        int[] red = new int[] { 255, 0, 0 };
        try {
            for (int i = 0; i < 3; i++) {
                swiftBot.fillUnderlights(red);
                Thread.sleep(200);
                swiftBot.disableUnderlights();
                Thread.sleep(200);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: failed to blink obstacle warning.");
        }

    }



    //object detection
    public static boolean objectDetection() {
        double distanceToObject = 0.0;

        System.out.println("Checking for objects...");
        try {
            distanceToObject = SwiftBotAPI.INSTANCE.useUltrasound();
            if (distanceToObject > 50.0) {
                System.out.println("No objects detected. Moving...");
                return false;
            }
            else {
                System.out.println("OBSTACLE DETECTED");
                blinkRed();
                System.out.println("Detected object distance:" + distanceToObject);

                captureAndSaveImage("/data/home/pi/ObjectDetected.png");

                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //wheel activation
    public static void wheelActive(Integer leftwheel, Integer rightwheel, Integer msec) {

        int leftWheelVelocity = leftwheel;
        int rightWheelVelocity = rightwheel;

        try {
            swiftBot.move(leftWheelVelocity, rightWheelVelocity, msec);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: Error while testing wheel");
            System.exit(5);
        }
    }
    public static void moveLeft (){
        System.out.println("Brightest: LEFT. moving left . . .");
        wheelActive(0, 50, 400);
        wheelActive(100, 100, 1000);
    }

    public static void moveRight (){
        System.out.println("Brightest: RIGHT. moving right . . .");
        wheelActive(50, 0, 400);
        wheelActive(100, 100, 1000);
    }

    public static void moveCentre (){
        System.out.println("Brightest: CENTRE. moving centre . . .");
        wheelActive(100, 100, 1000);
    }

    //avg brightness calculation
    public static double calculateAverageBrightness(BufferedImage image) {

        long total = 0;
        long pixelCount = 0;

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {

                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int brightness = (r + g + b) / 3;

                total += brightness;
                pixelCount++;
            }
        }

        return (double) total / pixelCount;
    }

    public static void showStarterScreen() {
        System.out.println("""
\033[38;5;208m***************************************************************
*                  SWIFTBOT: SEARCH FOR LIGHT                 *
***************************************************************\033[0m


\033[38;5;129mSwiftBot Program made by Ashley Danylenko\033[0m

=======================================================================

Status   : IDLE
Controls : [A] Start   [X] Stop + Save Log
Notes    : Underlights show state (GREEN=OK, RED=OBSTACLE)

Output   : logs/search_for_light_<timestamp>.txt
Images   : logs/objects/

=======================================================================

\033[38;5;208mPlease place the SwiftBot on the floor press A to start . . .\033[0m
""");
    }

	public static void main(String[] args) throws InterruptedException {
		try {
			swiftBot = SwiftBotAPI.INSTANCE;
		} catch (Exception e) {

			System.out.println("\nI2C disabled!");
			System.exit(5);
		}

        //starting screen printed once
        showStarterScreen();


        Scanner reader = new Scanner(System.in); // reading from System.in

		// loops main menu after each user action
        while (true) {
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("a")) {
                System.out.println("Loading... please wait");
                break;
            }

            System.out.println("Invalid key. Please press A to start.");
        }

// main repeating loop

        boolean running = true;

        while (running) {
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("x")) {
                System.out.println("Stopping...");
                running = false;
                break;
            }

            try {
                //capturing image
                BufferedImage image = captureImage();

                //splitting it into 3 parts and getting the brightness index
                //this array is fixed: 0 = left, 1 = centre, 2 = right
                double[] brightness = splitBrightness(image);
                double avg_left = brightness[0];
                double avg_centre = brightness[1];
                double avg_right = brightness[2];

                //this array used to sort out the values ranking from 0 = the highest, 2 = the lowest
                //which is used for choosing direction from the brightest to the second brightest
                Direction[] directions = {
                        new Direction("left", avg_left),
                        new Direction("centre", avg_centre),
                        new Direction("right", avg_right)
                };

                Arrays.sort(directions, (a, b) -> Double.compare(b.brightness, a.brightness));

                //checking for obstacle and setting the boolean
                boolean obstacle = objectDetection();


                                     //if obstacle --> direction[1] else --> direction[0]
                Direction chosen = obstacle ? directions[1] : directions[0];

                if (chosen.name.equals("left")) {
                    moveLeft();
                } else if (chosen.name.equals("centre")) {
                    moveCentre();
                } else {
                    moveRight();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
	}
}