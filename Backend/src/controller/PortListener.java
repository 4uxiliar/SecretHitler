package controller;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class PortListener implements Runnable
{
	private ServerSocket serverSocket;
	private ResponseBuilder responseBuilder;
	private Semaphore lock;
	private Semaphore min;

	public PortListener(ResponseBuilder responseBuilder, int serverport, Semaphore min, Semaphore lock)
	{
		this.responseBuilder = responseBuilder;
		this.lock = lock;
		this.min = min;
		try
		{
			serverSocket = new ServerSocket(serverport);

		}
		catch (IOException e)
		{
			System.err.println("Port probably already in use.");
		}

	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				Socket socket = serverSocket.accept();
				System.out.println(socket);
				lock.acquire();
				responseBuilder.addSocket(socket);
				min.release();
				lock.release();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}

	}
}
