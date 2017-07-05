package GBall.server;

public class GBMain
{
	private static void invalidPort()
	{
		System.err.println("Invalid port number specified");
	}
	
	public static void main(String[] argc)
	{
		if(argc.length == 1)
		{
			try
			{
				int port = Integer.parseInt(argc[0]);
				if(!(0 < port && port <= 65535))
				{
					invalidPort();
				}
				
				ClientConnections connections = new ClientConnections(port);
				connections.Start();
				World.getInstance().setConnection(connections);
				World.getInstance().process();
			}
			catch(NumberFormatException e)
			{
				invalidPort();
			}
		}
		else
		{
			System.err.println("Usage: server portnumber");
		}
	}
}