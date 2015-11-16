import javax.microedition.sensor.HiTechnicColorSensorInfo;

import lejos.nxt.Button;
import lejos.nxt.ColorSensor;
import lejos.nxt.ColorSensor.Color;
import lejos.nxt.LCD;
import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.TouchSensor;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.addon.ColorHTSensor;
import lejos.robotics.PressureDetector;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.util.PilotProps;


public class Robot {

	private static boolean _turnedLeft = true;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] colorNames = { "Red", "Green", "Blue", "Yellow",
				"Magenta", "Orange", "White", "Black", "Pink", "Gray",
				"Light gray", "Dark Gray", "Cyan" };
		String[] forwardSensorColorNames = {"None", "Red", "Green", "Blue", "Yellow",
                "Megenta", "Orange", "White", "Black", "Pink",
                "Grey", "Light Grey", "Dark Grey", "Cyan" };
		
		SensorPort FORWARD_SENSOR_PORT = SensorPort.S1;
		SensorPort SURFACE_SENSOR_PORT = SensorPort.S2;
		SensorPort LIGHT_SENSOR_PORT = SensorPort.S3;
		//SensorPort SONAR_SENSOR_PORT = SensorPort.S4;
		SensorPort TOUCH_SENSOR_PORT = SensorPort.S4;
		
		final float WHEEL_DIAMETER = 4.96f; // 5.6
		final float TRACK_WIDTH = 10.0f; // 11.4f
		
		boolean IS_FLOODLIGHT = true;
		
		// Initialize Sensors
		LightSensor lightSensor = new LightSensor(LIGHT_SENSOR_PORT,IS_FLOODLIGHT);
		ColorHTSensor surfaceSensor = new ColorHTSensor(SURFACE_SENSOR_PORT);
		ColorSensor forwardSensor = new ColorSensor(FORWARD_SENSOR_PORT);
		//UltrasonicSensor sonar = new UltrasonicSensor(SONAR_SENSOR_PORT);
		TouchSensor touchSensor = new TouchSensor(TOUCH_SENSOR_PORT);
		
		// Initialize Pilot
		DifferentialPilot pilot = new DifferentialPilot(WHEEL_DIAMETER, TRACK_WIDTH, Motor.A, Motor.C);  // parameters in cm
		pilot.setRotateSpeed(180); 

		Button.ENTER.waitForPressAndRelease();
		try{
			do
			{
				// while we havn't found either a blue or red square, try to move forward or turn left or right
				// now we will turn every five moves
				String sufaceColorName = colorNames[surfaceSensor.getColorID()];
				int i = 0;
				// find the colored square
				while( !color_in_wanted_box(sufaceColorName) )
				{
					LCD.drawInt(i, 0, 9);
	                LCD.drawString("Color:          ", 0, 6);
	                LCD.drawString("B", 10, 5);
					LCD.drawInt(surfaceSensor.getRGBComponent(Color.BLUE), 11, 5);
					
	                LCD.drawString(sufaceColorName, 7, 6);
	                
	                checkWall(pilot,touchSensor);
	                
					pilot.setTravelSpeed(15);
					pilot.travel(15);
					// TODO: deal with 
					i++;
					
					sufaceColorName = colorNames[surfaceSensor.getColorID()];
					LCD.refresh();                                                                                                                              
	
				}
				try{
				// we reached a lock or a blue / red spot
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
				}
				catch(Exception ex) {}
				
				//pilot.setTravelSpeed(5);
				//pilot.rotate(360);
				//pilot.rotate(360);
				
				String forwardColor = forwardSensorColorNames[forwardSensor.getColor().getColor() + 1];
				
				// make sure we're not out of the colored-box
				sufaceColorName = colorNames[surfaceSensor.getColorID()];
				
				while (!color_in_wanted_object(forwardColor, sufaceColorName))
				{
					
					if(!color_in_wanted_box(sufaceColorName))
						break;
					
					pilot.setTravelSpeed(5);
					pilot.travel(2);
					
					forwardColor = forwardSensorColorNames[forwardSensor.getColor().getColor() + 1];
					sufaceColorName = colorNames[surfaceSensor.getColorID()];
				}
				if(!color_in_wanted_box (sufaceColorName))
					//if we got out of the colored square continue
					continue;
				
				// we found a ball
				for(int j = 0; j < 4; j++)
				{
					pilot.rotate(10);
					pilot.rotate(-10);
				}
				
				pilot.setTravelSpeed(10);
				pilot.travel(15);
				
				// Now move towards the light
				int currentLight = lightSensor.readValue();
	            LCD.drawString("Light:          ", 0, 5);
	            LCD.drawInt(currentLight, 7, 5);
	            LCD.refresh();
	            boolean hitWall  = false;
	            int rotateCount = 0;
				while(currentLight < 80)
				{
					//checkWall(pilot,touchSensor);
					if(touchSensor.isPressed())
					{
						hitWall = true;
						pilot.travel(-15);
					}

					int newLight = lightSensor.readValue();
					if((newLight > currentLight) && !hitWall)
					{
						rotateCount = 0;
						if(currentLight > 70)
							pilot.travel(5);
						else
							pilot.travel(10);
					}
					else{
						
						if(currentLight > 70)
						{
							rotateCount+= 10;
							pilot.rotate(10);
						}
						else
						{
							rotateCount+= 25;
							pilot.rotate(25);
						}
						
						if(rotateCount > 360)
							pilot.travel(5);
						
						if(hitWall)
							hitWall = false;
					}
					currentLight = Math.max(newLight,currentLight);
					Thread.sleep(100);
		            LCD.drawString("Light:          ", 0, 5);
		            LCD.drawInt(currentLight, 7, 5);
		            LCD.refresh();
				}
				
				break;
			}
			while(!Button.ESCAPE.isDown());
		}
		catch(Exception ex)
		{}
		
		/*
		 * 		  pilot.setTravelSpeed(15);  // cm per second
		  System.out.println(" Press enter to start!.");
		  while (!Button.ENTER.isDown())
		  {}
		  pilot.travel(120);         // cm
		  pilot.rotate(-180);        // degree clockwise
		  pilot.travel(120);  //  move backward for 50 cm
		  
		  while(pilot.isMoving())
			  Thread.yield();
		  pilot.rotate(-90);
		  pilot.rotateLeft();
		  //pilot.rotateTo(270);
		  pilot.steer(-50,180,true); // turn 180 degrees to the right
		  pilot.steer(100);          // turns with left wheel stationary
		  pilot.stop();
		  System.out.println("Done.");
		 * 
		 * 
		 * */
	}

	private static void checkWall(DifferentialPilot pilot ,TouchSensor touchSensor) {
		if(touchSensor.isPressed())
		{
			pilot.travel(-5);
			
			if(!_turnedLeft)
			{
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

	private static boolean color_in_wanted_object(String forward_color, String surface_color) {
		
		return (!forward_color.equals(surface_color) && 
				(forward_color.equals("Green") || forward_color.equals("Blue") || forward_color.equals("Red") || forward_color.equals("White")));
	}

	private static boolean color_in_wanted_box(String sufaceColorName) {
		// TODO Auto-generated method stub
		return ( sufaceColorName.equals("Red")  || sufaceColorName.equals("Blue") || sufaceColorName.equals("Magenta") || sufaceColorName.equals("Orange") );
	}

}
