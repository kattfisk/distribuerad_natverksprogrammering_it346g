package GBall.server;

import java.io.Serializable;

import org.json.simple.JSONObject;

public abstract class GameEntity implements Serializable
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	private final Vector2D		m_position;
	private final Vector2D		m_initialPosition;
	private final Vector2D		m_initialDirection;
	private final Vector2D		m_speed;
	protected final Vector2D	m_direction;				// Should always be unit vector; determines the object's facing

	private double				m_acceleration;			// Accelerates by multiplying this with m_direction
	private long				m_lastUpdateTime;
	private double				m_maxAcceleration;
	private double				m_maxSpeed;
	private double				m_friction;

	private int m_stateUpdates;
	
	public abstract void render(java.awt.Graphics g);

	public abstract double getRadius();

	public abstract boolean givesPoints();

	public abstract void setState(JSONObject state);
	
	public GameEntity(final Vector2D position, final Vector2D speed, final Vector2D direction, double maxAcceleration,
			double maxSpeed, double friction)
	{
		m_position = position;
		m_speed = speed; // Actually represents velocity, not speed
		m_direction = direction;
		m_maxAcceleration = maxAcceleration;
		m_friction = friction;
		m_maxSpeed = maxSpeed;
		m_acceleration = 0;
		m_lastUpdateTime = System.currentTimeMillis();
		m_initialPosition = new Vector2D(position.getX(), position.getY());
		m_initialDirection = new Vector2D(direction.getX(), direction.getY());
		m_stateUpdates = Const.STATE_UPDATES;
	}
	
	public void stateChanged()
	{
		m_stateUpdates = Const.STATE_UPDATES;
	}

	public boolean getStateChanged()
	{
		return m_stateUpdates > 0;
	}
	
	public void setAcceleration(double a)
	{
		if (a > m_maxAcceleration)
		{
			m_acceleration = m_maxAcceleration;
		}
		else if (a < (-m_maxAcceleration))
		{
			m_acceleration = -m_maxAcceleration;
		}
		else
			m_acceleration = a;
	}

	public void move()
	{
		// Change to per-frame movement by setting delta to a constant
		// Such as 0.017 for ~60FPS

		long currentTime = System.currentTimeMillis();
		double delta = (double) (currentTime - m_lastUpdateTime) / (double) 1000;

		if (m_acceleration > 0)
		{
			changeSpeed(m_direction.multiplyOperator(m_acceleration * delta));
		}
		else
			scaleSpeed(m_friction);

		m_position.add(m_speed.multiplyOperator(delta));
		m_lastUpdateTime = currentTime;
	}

	public void scaleSpeed(double scale)
	{
		m_speed.scale(scale);
		if (m_speed.length() > m_maxSpeed)
		{
			m_speed.setLength(m_maxSpeed);
		}
	}

	public void changeSpeed(final Vector2D delta)
	{
		m_speed.add(delta);
		if (m_speed.length() > m_maxSpeed)
		{
			m_speed.setLength(m_maxSpeed);
		}
	}

	public void resetPosition()
	{
		m_position.set(m_initialPosition.getX(), m_initialPosition.getY());
		m_direction.set(m_initialDirection.getX(), m_initialDirection.getY());
		m_speed.set(0.0, 0.0);
		m_acceleration = 0;
		m_stateUpdates = Const.STATE_UPDATES;
	}

	public void deflectX()
	{
		m_speed.setX(-m_speed.getX());
	}

	public void deflectY()
	{
		m_speed.setY(-m_speed.getY());
	}

	public void rotate(double radians)
	{
		m_direction.rotate(radians);
	}

	public Vector2D getPosition()
	{
		return m_position;
	}

	public Vector2D getSpeed()
	{
		return m_speed;
	}

	public Vector2D getDirection()
	{
		return m_direction;
	}

	public void setPosition(double x, double y)
	{
		m_position.set(x, y);
	}

	public void displace(final Vector2D displacement)
	{
		m_position.add(displacement);
	}

	@SuppressWarnings("unchecked")
	public JSONObject getJSON()
	{
		--m_stateUpdates;
		JSONObject state = new JSONObject();
		state.put("pos_x", m_position.getX());
		state.put("pos_y", m_position.getY());
		state.put("vel_x", m_speed.getX());
		state.put("vel_y", m_speed.getY());
		return state;
	}
}