package GBall.client;

public class GBMain
{	
	private static void invalidPort()
	{
		System.err.println("Invalid port number specified");
	}
	
	public static void main(String[] argc)
	{
		if(argc.length == 2)
		{
			try
			{
				String address = argc[0];
				int serverPort = Integer.parseInt(argc[1]);
				
				ServerConnection connection = new ServerConnection();
				int entityID = connection.connect(address, serverPort);
				connection.start();
				EntityManager.setControlledEntity(entityID);
				World.getInstance().setConnection(connection);
				World.getInstance().process();
			}
			catch(NumberFormatException e)
			{
				invalidPort();
			}
		}
		else
		{
			System.err.println("Usage: client hostname port");
		}
	}
}