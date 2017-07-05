package GBall.client;

import java.awt.event.*;

public class World
{
	private double m_lastTime = System.currentTimeMillis();
	private double m_actualFps = 0.0;
	private final GameWindow m_gameWindow = new GameWindow();
	private ServerConnection m_connection;

	private static class WorldSingletonHolder
	{
		public static final World instance = new World();
	}

	public static World getInstance()
	{
		return WorldSingletonHolder.instance;
	}

	private World()
	{
	}

	public void setConnection(ServerConnection connection)
	{ // Dependency injecting into a static singleton is totally rad.
		m_connection = connection;
	}
	
	public void process()
	{
		initPlayers();

		while (true)
		{
			// Main loop
			if (newFrame())
			{
				if(m_connection.stateUnread())
				{ // If there's a new state from the server
					EntityManager.updateState(m_connection.getState());
				}
				EntityManager.getInstance().updatePositions();
				EntityManager.getInstance().checkBorderCollisions(Const.DISPLAY_WIDTH, Const.DISPLAY_HEIGHT);
				EntityManager.getInstance().checkShipCollisions();		
				m_gameWindow.repaint();
				// Send updated state
				m_connection.sendState(EntityManager.getControlledEntityStateUpdate());
				if(m_connection.scoreUnread())
				{ // Get new score if there is one
					ScoreKeeper.setScore(m_connection.getScore());
				}
			}
		}
	}

	private boolean newFrame()
	{
		double currentTime = System.currentTimeMillis();
		double delta = currentTime - m_lastTime;
		boolean rv = (delta > Const.FRAME_INCREMENT);
		if (rv)
		{
			m_lastTime += Const.FRAME_INCREMENT;
			if (delta > 10 * Const.FRAME_INCREMENT)
			{
				m_lastTime = currentTime;
			}
			m_actualFps = 1000 / delta;
		}
		return rv;
	}

	private void initPlayers()
	{
	// CAVEAT: It's imperative that objects are created in the same order on the client as on the server.
		// Ball
		EntityManager.getInstance().addBall(new Vector2D(Const.BALL_X, Const.BALL_Y), new Vector2D(0.0, 0.0));
		
		// KeyConfig for assigned entity set to arrow keys and null for others
		KeyConfig[] keyConfigs = new KeyConfig[4];
		keyConfigs[EntityManager.getControlledEntity()-1] = new KeyConfig(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP);
		
		// Team 1
		EntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM1_SHIP1_X, Const.START_TEAM1_SHIP1_Y), new Vector2D(0.0, 0.0), new Vector2D(1.0, 0.0), Const.TEAM1_COLOR, keyConfigs[0]);

		EntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM1_SHIP2_X, Const.START_TEAM1_SHIP2_Y), new Vector2D(0.0, 0.0), new Vector2D(1.0, 0.0), Const.TEAM1_COLOR, keyConfigs[1]);

		// Team 2
		EntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM2_SHIP1_X, Const.START_TEAM2_SHIP1_Y), new Vector2D(0.0, 0.0), new Vector2D(-1.0, 0.0), Const.TEAM2_COLOR, keyConfigs[2]);

		EntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM2_SHIP2_X, Const.START_TEAM2_SHIP2_Y), new Vector2D(0.0, 0.0), new Vector2D(-1.0, 0.0), Const.TEAM2_COLOR, keyConfigs[3]);
	}

	public double getActualFps()
	{
		return m_actualFps;
	}

	public void addKeyListener(KeyListener k)
	{
		m_gameWindow.addKeyListener(k);
	}

}