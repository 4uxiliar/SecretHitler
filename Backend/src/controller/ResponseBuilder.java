package controller;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class ResponseBuilder implements Runnable
{
	private LinkedList<Socket> sockets;
	private final String BASEURL;
	private Semaphore min;
	private Semaphore lock;

	public ResponseBuilder(String BASEURL, Semaphore min, Semaphore lock)
	{
		sockets = new LinkedList<>();
		this.BASEURL = BASEURL;
		this.min = min;
		this.lock = lock;
	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				min.acquire();
				lock.acquire();
				Socket currentSocket = sockets.pop();
				lock.release();
				System.out.println("Ich wurde angesprochen");
				try (BufferedReader in = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));
						PrintWriter out = new PrintWriter(currentSocket.getOutputStream());)
				{
					LinkedList<String> requestHeaderFields = new LinkedList<String>();
					String line;
					while ((line = in.readLine()) != null && !"".equals(line))
						requestHeaderFields.add(line);
					String headerField = requestHeaderFields.pop();
					String[] splittedHeaderField = headerField.split(" ");
					if (splittedHeaderField.length >= 3)
					{
						switch (splittedHeaderField[0])
						{
							case "GET":
								String[] address = splittedHeaderField[1].split("\\?");
								if (address.length == 1)
									printResponse(out, address[0]);
								else if (address.length == 2)
									printStatusLine(out, 501, "Not Implemented");
								else
									printStatusLine(out, 400, "Bad Request");
								break;
							case "POST":
								// TODO: implement POST
								printStatusLine(out, 501, "Not Implemented");
								break;
							case "OPTIONS":
							case "PUT":
							case "DELETE":
							case "TRACE":
							case "CONNECT":
								printStatusLine(out, 501, "Not Implemented");
								break;
							default:
								printStatusLine(out, 400, "Bad Request");
								break;
						}
					}
					else
					{
						printStatusLine(out, 400, "Bad Request");
					}
					out.flush();
					out.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

				currentSocket.close();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

	}

	private void printResponse(PrintWriter out, String address)
	{
		String fileExtension;
		if (address.equals("/"))
		{
			address = "/index.html";
			fileExtension = "html";
		}
		else
		{
			String[] splittedAddress = address.split("\\.");
			if (splittedAddress.length == 0)
				fileExtension = "";
			else
				fileExtension = splittedAddress[splittedAddress.length - 1];
		}
		address = BASEURL + address;
		a: try (BufferedReader fileReader = new BufferedReader(new FileReader(address));)
		{
			String contentType;
			switch (fileExtension)
			{
				case "html":
					contentType = "text/html";
					break;
				case "css":
					contentType = "text/css";
					break;
				case "js":
					contentType = "application/javascript";
					break;
				case "php":

				case "":
					printStatusLine(out, 404, "Not Found");
					break a;
				default:
					printStatusLine(out, 501, "Not Implemented");
					break a;
			}
			printStatusLine(out, 200, "OK");
			out.print("Content-Type: " + contentType + "\r\n");
			// TODO Weitere Responsefelder anfügen
			out.print("\r\n");
			String line;
			while ((line = fileReader.readLine()) != null)
				out.print(line + "\r\n");

		}
		catch (FileNotFoundException e)
		{
			printStatusLine(out, 404, "Not Found");
		}
		catch (IOException e)
		{
			printStatusLine(out, 500, "Internal Server Error");
		}
	}

	private void printStatusLine(PrintWriter out, int statusCode, String statusMessage)
	{
		out.print("HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n");
		if (statusCode >= 400)
			out.print("\r\n");
	}

	public void addSocket(Socket socket)
	{
		sockets.add(socket);
	}

}
