package GBall.client;

import java.awt.Color;
import java.awt.event.*;

import org.json.simple.JSONObject;

@SuppressWarnings("serial")
public class Ship extends GameEntity implements KeyListener
{

	private Color			m_color;
	private final KeyConfig	m_keyConfig;
	private int				rotation	= 0;		// Set to 1 when rotating clockwise,
	                                                // -1 when rotating counterclockwise
	private boolean			braking		= false;
	private boolean         m_accelerating = false;
	private int m_id;

	public Ship(final Vector2D position, final Vector2D speed, final Vector2D direction, final Color col, final KeyConfig kc, int id)
	{
		super(position, speed, direction, Const.SHIP_MAX_ACCELERATION, Const.SHIP_MAX_SPEED, Const.SHIP_FRICTION);
		m_color = col;
		m_keyConfig = kc;
		World.getInstance().addKeyListener(this);
		m_id = id;
	}

	@Override
	public void setState(JSONObject state)
	{
		super.setState(state);
		
		// Don't set these values for the controlled entity to avoid creating a feedback loop.
		if(m_id != EntityManager.getControlledEntity())
		{
			super.m_direction.set((double)state.get("dir_x"), (double)state.get("dir_y"));
			braking = (boolean)state.get("brk");
			m_accelerating = (boolean)state.get("acc");
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getJSON()
	{
		JSONObject state = super.getJSON();
		state.put("dir_x", super.m_direction.getX());
		state.put("dir_y", super.m_direction.getY());
		state.put("acc", m_accelerating);
		state.put("brk", braking);
		return state;
	}
	
	@Override
	public void resetPosition()
	{
		rotation = 0;
		braking	= false;
		m_accelerating = false;
		super.resetPosition();
	}
	
	@Override
	public void keyPressed(KeyEvent e)
	{
		if(m_keyConfig == null)
		{ // Non-player ship
			return;
		}
		
		try
		{
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
			{
				System.exit(0);
			}
			else if (e.getKeyCode() == m_keyConfig.rightKey())
			{
				super.stateChanged();
				rotation = 1;
			}
			else if (e.getKeyCode() == m_keyConfig.leftKey())
			{
				super.stateChanged();
				rotation = -1;
			}
			else if (e.getKeyCode() == m_keyConfig.accelerateKey())
			{
				super.stateChanged();
				m_accelerating = true;
			}
			else if (e.getKeyCode() == m_keyConfig.brakeKey())
			{
				super.stateChanged();
				braking = true;
			}
		}
		catch (Exception x)
		{
			System.err.println(x);
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		if(m_keyConfig == null)
		{ // Non-player ship
			return;
		}
		
		try
		{
			if (e.getKeyCode() == m_keyConfig.rightKey() && rotation == 1)
			{
				super.stateChanged();
				rotation = 0;
			}
			else if (e.getKeyCode() == m_keyConfig.leftKey() && rotation == -1)
			{
				super.stateChanged();
				rotation = 0;
			}
			else if (e.getKeyCode() == m_keyConfig.accelerateKey())
			{
				super.stateChanged();
				m_accelerating = false;
			}
			else if (e.getKeyCode() == m_keyConfig.brakeKey())
			{
				super.stateChanged();
				braking = false;
			}
		}
		catch (Exception x)
		{
			System.out.println(x);
		}
	}

	@Override
	public void keyTyped(KeyEvent e)
	{}

	@Override
	public void move()
	{
		if(m_accelerating)
		{
			setAcceleration(Const.SHIP_MAX_ACCELERATION);
		}
		else
		{
			setAcceleration(0);
		}
		
		
		if (rotation != 0)
		{
			rotate(rotation * Const.SHIP_ROTATION);
			scaleSpeed(Const.SHIP_TURN_BRAKE_SCALE);
		}
		
		if (braking)
		{
			scaleSpeed(Const.SHIP_BRAKE_SCALE);
			setAcceleration(0);
		}
		super.move();
	}

	@Override
	public void render(java.awt.Graphics g)
	{
		g.setColor(m_color);
		g.drawOval((int) getPosition().getX() - Const.SHIP_RADIUS, (int) getPosition().getY() - Const.SHIP_RADIUS, Const.SHIP_RADIUS * 2, Const.SHIP_RADIUS * 2);

		if(m_id == EntityManager.getControlledEntity())
		{ // Only draw line for player ship
			g.drawLine((int) getPosition().getX(), (int) getPosition().getY(), (int) (getPosition().getX() + getDirection().getX() * Const.SHIP_RADIUS), (int) (getPosition().getY() + getDirection().getY() * Const.SHIP_RADIUS));
		}
	}

	@Override
	public boolean givesPoints()
	{
		return false;
	}

	@Override
	public double getRadius()
	{
		return Const.SHIP_RADIUS;
	}
}