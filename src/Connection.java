
public class Connection implements Runnable
{
	private static int ConnectionCounter;
	private Thread RunningThread;
	
	// Static initialization block for when class is first loaded
	static
	{
		ConnectionCounter = 0;
	}
	
	public Connection()
	{
		// Initialize and run a new thread
		this.RunningThread = new Thread(this, "Connection");
		this.RunningThread.run();
	}

	public void run()
	{

	}

}
