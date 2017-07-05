package GBall.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ServerConnection
{
	// How often connectivity to server will be checked. In milliseconds.
	private static final int CONNECTION_TEST_INTERVAL = 3000;
	private volatile boolean m_run;

	private Session m_session; // TCP session used for handshake, score and to detect disconnects
	private UDPReceiver m_receiver;
	private UDPSender m_sender;
	
	// Use connect() and then start() after creation
	public ServerConnection()
	{
	}
	
	// Returns the id of the entity the player has been assigned
	@SuppressWarnings("unchecked")
	public int connect(String address, int port)
	{
		int id = -1;
		try
		{
			InetAddress serverAddress = InetAddress.getByName(address);
		
			// Establish TCP session
			Socket socket = new Socket(serverAddress, port);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			
			JSONObject connectMessage = (JSONObject)JSONValue.parse(in.readLine());
			// Get authentication token
			String token = (String)connectMessage.get("tkn");
			// Get remote UDP port
			int UDPport = (int)(long)connectMessage.get("UDPport");
			// Get id of assigned entity
			id = (int)(long)connectMessage.get("id");
			
			// Set up socket
			DatagramSocket UDPSocket = new DatagramSocket();
			// In a real world implementation we'd need firewall/NAT hole punching here
			// and the server would need to use our outside UDP source port (in case of NAT).
			// Thankfully this is not the real world.
			
			// Send local UDP port number to server
			JSONObject message = new JSONObject();
			message.put("UDPport", UDPSocket.getLocalPort());
			out.write(message.toJSONString()+"\n");
			out.flush();
			
			m_session = new Session(socket, in, out);
			m_receiver = new UDPReceiver(UDPSocket);
			m_sender = new UDPSender(UDPSocket, serverAddress, UDPport, token);			
		}
		catch(UnknownHostException e)
		{
			System.err.println("Could not resolve host");
			System.exit(-1);
		}
		catch (IOException e)
		{
			System.err.println("Could not connect to host");
			System.exit(-1);
		}
		return id;
	}
	
	public void start()
	{
		m_run = true;
		new Thread(m_session).start();
		new Thread(m_receiver).start();
		new Thread(m_sender).start();
	}
	
	public boolean stateUnread()
	{
		return m_receiver.stateUnread();
	}
	
	public JSONObject getState()
	{
		return m_receiver.getState();
	}
	
	public boolean scoreUnread()
	{
		return m_session.scoreUnread(); 
	}
	
	public JSONObject getScore()
	{
		return m_session.getScore();
	}
	
	public void sendState(JSONObject state)
	{
		if(state != null)
		{
			m_sender.send(state);
		}
	}
	
	private class Session implements Runnable
	{
		private Socket m_socket;
		private BufferedReader m_in;
		private BufferedWriter m_out;
		private JSONParser m_parser;
		private volatile boolean m_scoreUnread;
		private volatile JSONObject m_latestScore;
		private Semaphore m_mutex; // Used when changing latestState and unreadState
		
		public Session(Socket socket, BufferedReader in, BufferedWriter out)
		{
			try
			{
				m_socket = socket;
				m_socket.setSoTimeout(CONNECTION_TEST_INTERVAL);
				m_in = in;
				m_out = out;
				m_parser = new JSONParser();
				m_scoreUnread = false;
				m_mutex = new Semaphore(1, true);
			}
			catch (SocketException e)
			{
				e.printStackTrace();
				System.exit(-1);
			}

		}
		
		public void run()
		{
			while(m_run)
			{
				boolean testConnection = false;
				try
				{
					if(testConnection)
					{
						// Test that the connection still works by sending an empty JSONObject
						m_out.write(new JSONObject().toJSONString()+"\n");
						m_out.flush();
						testConnection = false;
					}
					JSONObject message = (JSONObject)m_parser.parse(m_in.readLine());
					if(message.get("score_t1") != null)
					{ // Message contains score
						m_mutex.acquire();
						m_latestScore = message;
						m_scoreUnread = true;
						m_mutex.release();
					}
				}
				catch(SocketTimeoutException e)
				{
					// If no message has been received within the specified interval
					testConnection = true;
				}
				catch (IOException e)
				{
					System.err.println("Lost connection to server.");
					System.exit(-1);
				}
				catch (ParseException e)
				{
					e.printStackTrace();
				}
				catch (InterruptedException e)
				{
					// Thread interrupted while waiting for mutex
					// Time to die
					return;
				}
			}
			try
			{
				m_socket.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		public boolean scoreUnread()
		{
			return m_scoreUnread;
		}
		
		public JSONObject getScore()
		{
			JSONObject retval;
			try
			{
				m_mutex.acquire();
				retval = m_latestScore;
				m_scoreUnread = false;
				m_mutex.release();
			}
			catch (InterruptedException e)
			{ // Thread interrupted while waiting on semaphore
				// Restore interrupt flag
				Thread.currentThread().interrupt();
				// Return state
				retval = m_latestScore;
				m_scoreUnread = false;
				// But don't signal on semaphore (since we didn't acquire it)
			}
			return retval;
		}
	}
	
	private class UDPReceiver implements Runnable
	{
		private DatagramSocket m_socket;
		private JSONParser m_parser;
		private volatile boolean m_stateUnread; // Indicates whether a new state has been received since last read
		private volatile JSONObject m_latestState; // Stores latest received state
		private Semaphore m_mutex; // Used when changing latestState and unreadState
		private int m_packetNumber;
		
		public UDPReceiver(DatagramSocket socket)
		{
			m_socket = socket;
			m_parser = new JSONParser();
			m_stateUnread = false;
			m_mutex = new Semaphore(1, true);
			m_packetNumber = 0;
		}
		
		public void run()
		{
			byte[] data = new byte[2048]; // 2kB should be larger than anything we'll receive
			DatagramPacket packet = new DatagramPacket(data, data.length);
			JSONObject received;
			int pnum;
			while(m_run)
			{
				try
				{
					packet.setLength(data.length); // To avoid issue with some implementations
					m_socket.receive(packet);
					
					received = (JSONObject) m_parser.parse(new String(packet.getData(), packet.getOffset(), packet.getLength()));
					pnum = (int)(long)received.get("pnum");
					if(pnum - m_packetNumber > 0) // Works even on overflow
					{ // Unread packet
						m_packetNumber = pnum;
					
						m_mutex.acquire();
						m_latestState = received;
						m_stateUnread = true;
						m_mutex.release();
					}
					else
					{ // TODO: DELETEME
						System.out.println("Discarded packet with pnum="+pnum+" and local value "+m_packetNumber);
					}

				}
				catch (IOException e)
				{
					e.printStackTrace();
					System.exit(-1);
				}
				catch (ParseException e)
				{
					e.printStackTrace();
					System.exit(-1);
				}
				catch (InterruptedException e)
				{
					// Thread interrupted while waiting for mutex
					// Time to die
					return;
				}
			}
		}
		
		public boolean stateUnread()
		{
			return m_stateUnread;
		}
		
		public JSONObject getState()
		{
			JSONObject retval;
			try
			{
				m_mutex.acquire();
				retval = m_latestState;
				m_stateUnread = false;
				m_mutex.release();
			}
			catch (InterruptedException e)
			{ // Thread interrupted while waiting on semaphore
				// Restore interrupt flag
				Thread.currentThread().interrupt();
				// Return state
				retval = m_latestState;
				m_stateUnread = false;
				// But don't signal on semaphore (since we didn't acquire it)
			}
			return retval;
		}
	}
	
	private class UDPSender implements Runnable
	{
		private DatagramSocket m_socket;
		private InetAddress m_address;
		private int m_port;
		private BlockingQueue<JSONObject> m_outQueue;
		private int m_packetNumber; // Used to detect out of order and duplicate packets
		private String m_token;
		
		public UDPSender(DatagramSocket socket, InetAddress address, int port, String token)
		{
			m_socket = socket;
			m_address = address;
			m_port = port;
			m_outQueue = new LinkedBlockingQueue<JSONObject>();
			m_packetNumber = 0;
			m_token = token;
		}
		
		@SuppressWarnings("unchecked")
		public void run()
		{
			JSONObject outMsg;
			byte[] msgData;
			DatagramPacket packet;
			while(m_run)
			{
				try
				{
					// take() will block if queue is empty
					outMsg = m_outQueue.take();
					outMsg.put("tkn", m_token);
					outMsg.put("pnum", ++m_packetNumber);
					msgData = outMsg.toJSONString().getBytes();
					packet = new DatagramPacket(msgData, msgData.length, m_address, m_port);
					m_socket.send(packet);
				}
				catch (InterruptedException e)
				{
					// Thread interrupted while waiting, exit
					return;
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		public void send(JSONObject message)
		{
			m_outQueue.add(message);
		}
	}
}
