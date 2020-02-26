import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

/*
 * Explanation
 * 
 * I am using the main thread to listen for incoming HTTP requests on a TCP socket.
 * For each connection received on the main thread, I am spawning a separate thread
 * for that connection and maintaining it.
 * 
 * 
 */

public class Main implements Runnable
{

	/**
	 * Impose a hard limit of 8KB for the maximum HTTP request size. This is for
	 * reading in the string.
	 */
	public static final int HTTP_REQUEST_MAX_SIZE = 8 * 1024;
	public static final int DEFAULT_PORT = 8080;

	private ServerSocket Server;
	private Thread ServerThread;
	private boolean Running;

	public Main()
	{
	}

	public void run()
	{
		System.out.println("Server waiting for connections");

		while (Running)
		{
			try
			{
				Socket Connection = Server.accept();
				System.out.println("Received an HTTP request");

				// Read and store the message
				InputStream ConnectionInput = Connection.getInputStream();
				byte[] Buffer = new byte[HTTP_REQUEST_MAX_SIZE];
				int ActualSize = ConnectionInput.read(Buffer);
				
				if(ActualSize > 0)
				{
					String AsString = new String(Buffer);
					String[] Lines = AsString.split("\r\n");
					

					// Parse first line of request
					String[] Line0 = Lines[0].split(" ");

					System.out.println("Line0: " + Lines[0]);
					
					String Method = Line0[0];
					String URI = Line0[1];
					String Version = Line0[2];
					
					// Canonicalize URI if asking for main page
					if(URI.equals("/"))
						URI = "/index.html";
					
					// Check if we can find the resource
					String SystemPath = "." + File.separator + "page" + File.separator + URI;
					File SystemFile = new File(SystemPath);
					System.out.println("Resource system: " + SystemFile.getAbsolutePath());
					
					if(SystemFile.exists())
					{
						System.out.println("Found resource");
						
						String ContentString = LoadFileAsString(SystemPath);
						byte[] ContentBytes = ContentString.getBytes("UTF-8");
						
						ByteArrayOutputStream BAOut = new ByteArrayOutputStream();
						OutputStream Output = Connection.getOutputStream();
						Output.write((Version + " 200 OK\r\n").getBytes("utf-8"));
						Output.write(("Date: Sun, 18 Oct 2012 10:36:20 GMT\r\n").getBytes("utf-8"));
						Output.write(("Server: CSE4344-jgl0715\r\n").getBytes("utf-8"));
						Output.write(("Content-Length: " + ContentBytes.length + "\r\n").getBytes("utf-8"));
						Output.write(("Connection: closed\r\n").getBytes("utf-8"));
						Output.write(("Content-Type: text/html; charset=utf-8\r\n\r\n").getBytes("utf-8"));
						Output.write(ContentBytes);
						
//						System.out.println(new String(BAOut.toByteArray(), Charset.forName("utf-8")));
//						System.out.println(BAOut.toString());
						
						System.out.println(Arrays.toString(ContentBytes));
						System.out.println(ContentString);
						System.out.println(ContentBytes.length + " vs " + ContentString.length());
						Connection.getOutputStream().write(BAOut.toByteArray());
					}
					else
					{
						System.out.println("Did not find resource");
						OutputStream Output = Connection.getOutputStream();
						Output.write((Version + " 404 Not Found\r\n").getBytes("utf-8"));
						Output.write(("Date: Sun, 18 Oct 2012 10:36:20 GMT\r\n").getBytes("utf-8"));
						Output.write(("Server: CSE4344-jgl0715\r\n").getBytes("utf-8"));
						Output.write(("Content-Length: 0\r\n").getBytes("utf-8"));
						Output.write(("Connection: closed\r\n").getBytes("utf-8"));
						Output.write(("Content-Type: text/html; charset=utf-8\r\n").getBytes("utf-8"));
					}
					
					System.out.println("Method: " + Method);
					System.out.println("URI: " + URI);
					System.out.println("Version: " + Version);
				}

				Connection.close();
			} catch (IOException e)
			{
				System.err.println("A problem occurred while accepting a socket connection");
			}
		}
	}

	public boolean Start(int Port)
	{
		int PortToUse = DEFAULT_PORT;
		if (Port >= 0)
		{
			PortToUse = Port;
			System.out.println("Attempting to start server on port " + Port + "...");
		} else
		{
			System.out.println("Attempting to start server on default port " + DEFAULT_PORT + "...");
		}

		// Initialize server socket
		try
		{
			Server = new ServerSocket(PortToUse);
		} catch (IOException e)
		{
			System.err.println("Failed to start server!");
			return false;
		}

		// Indicate that the server is now running.
		Running = true;

		// Spawn a new thread for server to listen for connections on
		ServerThread = new Thread(this, "ServerThread");
		ServerThread.run();

		return true;
	}

	public String LoadFileAsString(String Path)
	{
		try
		{
			BufferedReader Reader = new BufferedReader(new FileReader(Path));
			StringBuilder Result = new StringBuilder();
			String Line = "";

			while ((Line = Reader.readLine()) != null)
			{
				Result.append(Line + "\n");
			}

			Reader.close();

			return Result.toString();
		} catch (IOException e)
		{
			System.err.println("Failed reading the file " + Path);
			return null;
		}
	}

	public static void main(String[] args)
	{
		// Initialize the driver and start
		Main Driver = new Main();

		int PortToUse = 0;

		if (args.length == 0)
		{
			PortToUse = -1;
		} else if (args.length == 2 && args[0].equalsIgnoreCase("-p"))
		{
			try
			{
				PortToUse = Integer.parseInt(args[1]);
			} catch (NumberFormatException e)
			{
				System.err.println("Incorrect port formatting");
				return;
			}
		} else
		{
			System.err.println("Incorrect options, valid options are as follows:");
			System.err.println("\t-p <port>: Specifies the port to that the server will locally bind to.");
			return;
		}

		if (Driver.Start(PortToUse))
		{
			String CommandLine = "";
			String Command = "";
			Scanner CommandReader = new Scanner(System.in);
			do
			{
				System.out.println(">> ");
				CommandLine = CommandReader.nextLine();
				String[] Tokens = CommandLine.split("\\W+");

				if (Tokens.length > 0)
				{
					Command = Tokens[0];
				}

			} while (!(Command.equalsIgnoreCase("stop") || Command.equalsIgnoreCase("exit") || Command.equalsIgnoreCase("quit")));

			CommandReader.close();

			// Close out the server
			System.out.println("Shutting down the server...");
			Driver.Running = false;

			// Forcefully close the connection
			try
			{
				Driver.Server.close();
			} catch (IOException e1)
			{
				System.err.println("A problem occurred while closing the server connection");
			}

			// Join threads with the server to synchronize execution
			try
			{
				Driver.ServerThread.join();
			} catch (InterruptedException e)
			{
				System.err.println("A problem occurred while rejoining with the server thread");
			}
		} else
		{
			System.err.println("Server failed, exiting application.");
		}
	}

}
