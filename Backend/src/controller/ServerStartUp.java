package controller;

import java.io.File;
import java.util.concurrent.Semaphore;

public class ServerStartUp
{

	public static void main(String[] args)
	{
		String path = new File(System.getProperty("user.dir")).getParentFile().getAbsolutePath() + "/Frontend";
		Semaphore min = new Semaphore(0);
		Semaphore lock = new Semaphore(1);
		ResponseBuilder responseBuilder = new ResponseBuilder(path, min, lock);
		PortListener portListener = new PortListener(responseBuilder, 80, min, lock);
		Thread responseBuilderThread = new Thread(responseBuilder);
		Thread portListenerThread = new Thread(portListener);
		portListenerThread.start();
		responseBuilderThread.start();
	}

}
