import java.io.PrintStream;

import javax.microedition.sensor.HiTechnicColorSensorInfo;

import lejos.nxt.Button;
import lejos.nxt.ColorSensor;
import lejos.nxt.ColorSensor.Color;
import lejos.nxt.ButtonListener;
import lejos.nxt.LCD;
import lejos.nxt.LCDOutputStream;
import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.MotorPort;
import lejos.nxt.NXTMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.TouchSensor;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.addon.ColorHTSensor;
import lejos.robotics.DCMotor;
import lejos.robotics.PressureDetector;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.util.PilotProps;

public class Robot {

	private static final int SONAR_WALL_DISTANCE = 7;
	private static final int SONAR_OBJECT_DISTANCE = 5;
	private static final int SONAR_GATE_DISTANCE = 7;
	private static boolean _turnedLeft = true;
	
	private static int _rotate_iter_num = 12;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] colorNames = { "Red", "Green", "Blue", "Yellow", "Magenta",
				"Orange", "White", "Black", "Pink", "Gray", "Light gray",
				"Dark Gray", "Cyan" };
		String[] forwardSensorColorNames = { "None", "Red", "Green", "Blue",
				"Yellow", "Megenta", "Orange", "White", "Black", "Pink",
				"Grey", "Light Grey", "Dark Grey", "Cyan" };

		SensorPort FORWARD_SENSOR_PORT = SensorPort.S1;
		SensorPort SURFACE_SENSOR_PORT = SensorPort.S2;
		SensorPort LIGHT_SENSOR_PORT = SensorPort.S3;
		SensorPort SONAR_SENSOR_PORT = SensorPort.S4;
		// SensorPort TOUCH_SENSOR_PORT = SensorPort.S4;

		MotorPort ARM_PORT = MotorPort.B;

		final float WHEEL_DIAMETER = 4.96f; // 5.6
		final float TRACK_WIDTH = 10.0f; // 11.4f

		boolean IS_FLOODLIGHT = true;

		// Initialize Sensors
		LightSensor lightSensor = new LightSensor(LIGHT_SENSOR_PORT,
				IS_FLOODLIGHT);
		ColorHTSensor surfaceSensor = new ColorHTSensor(SURFACE_SENSOR_PORT);
		ColorSensor forwardSensor = new ColorSensor(FORWARD_SENSOR_PORT);
		UltrasonicSensor sonar = new UltrasonicSensor(SONAR_SENSOR_PORT);
		sonar.continuous();
		
		// TouchSensor touchSensor = new TouchSensor(TOUCH_SENSOR_PORT);

		// Initialize Pilot
		DifferentialPilot pilot = new DifferentialPilot(WHEEL_DIAMETER,
				TRACK_WIDTH, Motor.A, Motor.C); // parameters in cm
		pilot.setRotateSpeed(180);

		// Initilize ARM (move it to open state)
		DCMotor arm = new NXTMotor(ARM_PORT);
		SetArm(arm, false);

		Button.ENTER.waitForPressAndRelease();
		
		Button.LEFT.addButtonListener(new ButtonListener() {
			
			@Override
			public void buttonReleased(Button b) {
				// TODO Auto-generated method stub
				if(Robot.this._rotate_iter_num < 50)
					Robot.this._rotate_iter_num++;
				
				LCD.drawInt(_rotate_iter_num, 11, 2);
				LCD.drawInt(_rotate_iter_num, 0, 9);
				LCD.refresh();
			}
			
			@Override
			public void buttonPressed(Button b) {

				
			}
		});
		
		Button.RIGHT.addButtonListener(new ButtonListener() {
			
			@Override
			public void buttonReleased(Button b) {
				// TODO Auto-generated method stub
				if(Robot.this._rotate_iter_num > 5)
					Robot.this._rotate_iter_num--;
				
				LCD.drawInt(_rotate_iter_num, 11, 2);
				LCD.drawInt(_rotate_iter_num, 0, 9);
				LCD.refresh();
			}
			
			@Override
			public void buttonPressed(Button b) {

			}
		});
		
		
		try {
			do {
				// while we havn't found either a blue or red square, try to
				// move forward or turn left or right
				// now we will turn every five moves
				String sufaceColorName = colorNames[surfaceSensor.getColorID()];
				int i = 0;
				// find the colored square
				while (!color_in_wanted_box(sufaceColorName)) {
					LCD.drawString("RoateIter:", 0, 2);
					LCD.drawInt(Robot._rotate_iter_num, 11, 2);
					LCD.drawInt(i, 0, 9);
					LCD.drawString("Color:          ", 0, 6);
					LCD.drawString("B", 10, 5);
					LCD.drawInt(surfaceSensor.getRGBComponent(Color.BLUE), 11,
							5);

					LCD.drawString(sufaceColorName, 7, 6);
					LCD.refresh();
					
					checkWall(pilot, sonar);

					pilot.setTravelSpeed(15);
					pilot.travel(15);
					// TODO: deal with
					i++;

					sufaceColorName = colorNames[surfaceSensor.getColorID()];


				}

				// draw the current color
				LCD.drawString(sufaceColorName, 7, 6);
				LCD.refresh();

				try {
					// we reached a lock on a blue / red spot
					Sound.playTone(196, 200); // G
					Thread.sleep(200);
					Sound.playTone(165, 200); // E
					Thread.sleep(200);
					Sound.playTone(165, 200); // E
					Thread.sleep(200);
					Sound.playTone(175, 200); // F
					Thread.sleep(200);
					Sound.playTone(123, 200); // B
					Thread.sleep(200);
					Sound.playTone(123, 200); // B
				} catch (Exception ex) {
					LCD.drawString("Error!" + ex.getMessage(), 7, 6);
					LCD.refresh();
				}

				// pilot.setTravelSpeed(5);
				// pilot.rotate(360);
				// pilot.rotate(360);
				String boxColor = sufaceColorName;

				String forwardColor = forwardSensorColorNames[forwardSensor
						.getColor().getColor() + 1];

				// make sure we're not out of the colored-box
				sufaceColorName = colorNames[surfaceSensor.getColorID()];
				int setps_to_ball_counter = 0;

				while (!color_in_wanted_object(forwardColor, sufaceColorName, sonar)) {
					// Out of colored box. exit.
					if (!color_in_wanted_box(sufaceColorName))
						break;

					pilot.setTravelSpeed(5);
					pilot.travel(2);

					setps_to_ball_counter++;

					forwardColor = forwardSensorColorNames[forwardSensor
							.getColor().getColor() + 1];
					sufaceColorName = colorNames[surfaceSensor.getColorID()];
				}
				if (!color_in_wanted_box(sufaceColorName))
					// if we got out of the colored square continue
					continue;

//				 //we found a ball
//				 for(int j = 0; j < 4; j++)
//				 {
//				 pilot.rotate(10);
//				 pilot.rotate(-10);
//				 }

				LCD.drawString("FOUND BALL!", 5, 5);
				LCD.refresh();

				// Let's Calculate the color of the destination gate.
				String gate_color = calc_gate_color(forwardColor, boxColor);

				// Go back and get ready to spin
				for (int j = 0; j <= setps_to_ball_counter; j++) {
					pilot.setTravelSpeed(5);
					pilot.travel(-2);
				}

				// Since the front is shorter by 7cm
				// from the butt move back another 7 cm
				// so we could spin properly.
				pilot.travel(-10);
				
				LCD.drawString("RoateIter:", 0, 2);
				LCD.drawInt(Robot._rotate_iter_num, 11, 2);
				LCD.refresh();
				
				// spin
				for (int j = 0; j < _rotate_iter_num ; j++) {
					pilot.rotate(25);
				}

				// Open Cage.
				SetArm(arm, true);
				pilot.setTravelSpeed(10);
				pilot.travel(-5);
				
				// Go back to the ball
				for (int j = 0; j <= setps_to_ball_counter; j++) {
					pilot.setTravelSpeed(5);
					pilot.travel(-2);
				}
				// catch the ball
				SetArm(arm, false);


				
				pilot.setTravelSpeed(10);
				pilot.travel(15);

				// Now move towards the light
				int currentLight = lightSensor.readValue();
				LCD.clear();
				LCD.drawString("Light:          ", 0, 5);
				LCD.drawInt(currentLight, 7, 5);
				LCD.drawString("Gate:", 0, 7);
				LCD.drawString(gate_color,7, 7);
				LCD.refresh();
				boolean hitWall = false;
				int rotateCount = 0;

				forwardColor = "";

				while (!forwardColor.equals(gate_color)) {
					while ((currentLight < 75) && (sonar.getDistance() > SONAR_GATE_DISTANCE)) 
					{
						// checkWall(pilot,touchSensor);

						int newLight = lightSensor.readValue();
						if ((newLight > currentLight) && !hitWall) {
							rotateCount = 0;
							if (currentLight > 70)
								pilot.travel(5);
							else
								pilot.travel(10);
						} else {

							if (currentLight > 70) {
								rotateCount += 10;
								pilot.rotate(10);
							} else {
								rotateCount += 25;
								pilot.rotate(25);
							}

							if (rotateCount > 360)
								pilot.travel(5);

							if (hitWall)
								hitWall = false;
							
							if (sonar.getDistance()< SONAR_GATE_DISTANCE) {
								hitWall = true;
								pilot.travel(-15);
							}
						}
						currentLight = Math.max(newLight, currentLight);
						Thread.sleep(100);
						LCD.drawString("Light:          ", 0, 5);
						LCD.drawInt(currentLight, 7, 5);
						LCD.refresh();
					}
					if(!forwardColor.equals(gate_color))
					{
						LCD.clear();
						LCD.drawString("ohhh. WORNG GATE!", 0, 0);
						LCD.refresh();
						
						pilot.travel(-20);
						pilot.rotate(100);
						forwardColor = forwardSensorColorNames[forwardSensor
						               						.getColor().getColor() + 1];
						currentLight = 0;
					}
				}
				
				LCD.drawString("YES! DONE.", 0, 0);
				LCD.refresh();
				
				SetArm(arm, true);
				
				// Rotate Twice
				 pilot.rotate(360);
				 pilot.rotate(360);

				 break;
			} while (!Button.ESCAPE.isDown());
		} catch (Exception ex) {
		}

		/*
		 * pilot.setTravelSpeed(15); // cm per second
		 * System.out.println(" Press enter to start!."); while
		 * (!Button.ENTER.isDown()) {} pilot.travel(120); // cm
		 * pilot.rotate(-180); // degree clockwise pilot.travel(120); // move
		 * backward for 50 cm
		 * 
		 * while(pilot.isMoving()) Thread.yield(); pilot.rotate(-90);
		 * pilot.rotateLeft(); //pilot.rotateTo(270); pilot.steer(-50,180,true);
		 * // turn 180 degrees to the right pilot.steer(100); // turns with left
		 * wheel stationary pilot.stop(); System.out.println("Done.");
		 */
	}

	private static void SetArm(DCMotor arm, boolean isOpen) {
		arm.setPower(30);
		if (isOpen)
			arm.forward();
		else
			arm.backward();
		try {
			Thread.sleep(1000);
		} catch (Exception ex) {}

		arm.stop();

	}

	private static String calc_gate_color(String ballColor, String squareColor) {
		LCD.clear();
		LCD.drawString("ball"+ballColor+"\n sqr"+squareColor,0,0);
		LCD.refresh();
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (is_blue(squareColor))
		{
			if (ballColor.equals("Blue") || ballColor.equals("Green"))
				return "Blue";
			return "Red";
		} 
		else 
		{
			// Square is red.
			if (ballColor.equals("Blue") || ballColor.equals("Green"))
				return "Red";

			return "Blue";
		}
	}

	private static void checkWall(DifferentialPilot pilot,
			TouchSensor touchSensor) {
		if (touchSensor.isPressed()) {
			pilot.travel(-5);

			if (!_turnedLeft) {
				pilot.rotate(-90);
				pilot.setTravelSpeed(15);
				pilot.travel(10);

				pilot.rotate(-90);
				_turnedLeft = true;
			}

			pilot.rotate(90);
			pilot.setTravelSpeed(15);
			pilot.travel(10);

			pilot.rotate(90);
			_turnedLeft = false;
		}

	}

	private static void checkWall(DifferentialPilot pilot,
			UltrasonicSensor sensor) {
		if (sensor.getDistance() < SONAR_WALL_DISTANCE) {
			pilot.travel(-15);

			if (!_turnedLeft) {
				pilot.rotate(-90);
				pilot.setTravelSpeed(15);
				pilot.travel(10);

				pilot.rotate(-90);
				_turnedLeft = true;
			}

			pilot.rotate(90);
			pilot.setTravelSpeed(15);
			pilot.travel(10);

			pilot.rotate(90);
			_turnedLeft = false;
		}

	}

	private static boolean color_in_wanted_object(String forward_color,
			String surface_color, UltrasonicSensor sonar) {
		
		if(forward_color.equals(surface_color) && sonar != null)
			return (sonar.getDistance()<SONAR_OBJECT_DISTANCE);

		return (!forward_color.equals(surface_color) && (forward_color
				.equals("Green")
				|| forward_color.equals("Blue")
				|| forward_color.equals("Red") || forward_color.equals("White")));
	}

	private static boolean color_in_wanted_box(String sufaceColorName) {
		// TODO Auto-generated method stub
		return (is_blue(sufaceColorName) || is_red(sufaceColorName));
	}

	/**
	 * Work around to return what color names we are ready to accept as blue
	 * 
	 * @param sufaceColorName
	 *            the color name from the colors array
	 * @return true if blue
	 */
	public static boolean is_blue(String sufaceColorName) {
		return (sufaceColorName.equals("Blue") || sufaceColorName
				.equals("Magenta"));
	}

	/**
	 * Work around to return what color names we are ready to accept as red
	 * 
	 * @param sufaceColorName
	 *            the color name from the colors array
	 * @return true if red
	 */
	public static boolean is_red(String sufaceColorName) {
		return (sufaceColorName.equals("Red") || sufaceColorName
				.equals("Orange"));
	}

}
