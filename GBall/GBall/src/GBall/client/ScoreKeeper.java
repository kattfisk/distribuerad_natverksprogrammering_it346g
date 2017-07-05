package GBall.client;

import org.json.simple.JSONObject;

public class ScoreKeeper
{
	private static class ScoreKeeperSingletonHolder
	{
		public static final ScoreKeeper	instance	= new ScoreKeeper();
	}

	public static ScoreKeeper getInstance()
	{
		return ScoreKeeperSingletonHolder.instance;
	}

	private static int m_team1Score;
	private static int m_team2Score;

	public static void setScore(JSONObject score)
	{
		m_team1Score = (int)(long)score.get("score_t1");
		m_team2Score = (int)(long)score.get("score_t2");
	}

	private ScoreKeeper()
	{
		m_team1Score = 0;
		m_team2Score = 0;
	}

	public void render(java.awt.Graphics g)
	{
		g.setFont(Const.SCORE_FONT);
		g.setColor(Const.TEAM1_COLOR);
		g.drawString(new Integer(m_team1Score).toString(), (int) Const.TEAM1_SCORE_TEXT_POSITION.getX(),
				(int) Const.TEAM1_SCORE_TEXT_POSITION.getY());

		g.setColor(Const.TEAM2_COLOR);
		g.drawString(new Integer(m_team2Score).toString(), (int) Const.TEAM2_SCORE_TEXT_POSITION.getX(),
				(int) Const.TEAM2_SCORE_TEXT_POSITION.getY());
	}
}