package tv.zerator.ffs.api.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.jolbox.bonecp.BoneCPDataSource;
import com.jolbox.bonecp.PreparedStatementHandle;

import alexmog.apilib.dao.DAO;
import alexmog.apilib.managers.DaoManager.Dao;
import tv.zerator.ffs.api.dao.beans.AccountBean;

@Dao(database = "general")
public class AccountsDao extends DAO<AccountBean> {

	public AccountsDao(BoneCPDataSource dataSource) {
		super(dataSource);
	}

	@Override
	public int insert(AccountBean data) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("INSERT INTO accounts "
						+ "(twitch_id, username, email, views, followers, broadcaster_type, url, grade, email_activation_key, logo) VALUES "
						+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
			prep.setInt(1, data.getTwitchId());
			prep.setString(2, data.getUsername());
			prep.setString(3, data.getEmail());
			prep.setInt(4, data.getViews());
			prep.setInt(5, data.getFollowers());
			prep.setString(6, data.getBroadcasterType().name().toLowerCase());
			prep.setString(7, data.getUrl());
			prep.setInt(8, data.getGrade());
			prep.setString(9, data.getEmailActivationKey());
			prep.setString(10, data.getLogo());
			prep.executeUpdate();
			return data.getTwitchId();
		}
	}

	@Override
	public AccountBean update(AccountBean data) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("UPDATE accounts SET "
						+ "username = ?, email = ?, views = ?, followers = ?, broadcaster_type = ?, url = ?, grade = ?, email_activation_key = ?, logo = ? "
						+ "WHERE twitch_id = ?")) {
			prep.setString(1, data.getUsername());
			prep.setString(2, data.getEmail());
			prep.setInt(3, data.getViews());
			prep.setInt(4, data.getFollowers());
			prep.setString(5, data.getBroadcasterType().name().toLowerCase());
			prep.setString(6, data.getUrl());
			prep.setInt(7, data.getGrade());
			prep.setString(8, data.getEmailActivationKey());
			prep.setString(9, data.getLogo());
			prep.setInt(10, data.getTwitchId());
			prep.executeUpdate();
		}
		return data;
	}
	
	public AccountBean getAccountFromToken(String token) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("SELECT "
						+ "a.broadcaster_type, a.email, a.email_activation_key, a.followers, a.grade, a.twitch_id, a.url, a.username, a.views, a.logo "
						+ "FROM accounts a LEFT JOIN auth_tokens t ON t.account_id = a.twitch_id WHERE t.token = ?")) {
			prep.setString(1, token);
			try (ResultSet rs = prep.executeQuery()) {
				if (!rs.next()) return null;
				AccountBean bean = new AccountBean();
				bean.setBroadcasterType(AccountBean.BroadcasterType.valueOf(rs.getString("a.broadcaster_type")));
				bean.setEmail(rs.getString("a.email"));
				bean.setEmailActivationKey(rs.getString("a.email_activation_key"));
				bean.setFollowers(rs.getInt("a.followers"));
				bean.setGrade(rs.getInt("a.grade"));
				bean.setTwitchId(rs.getInt("a.twitch_id"));
				bean.setUrl(rs.getString("a.url"));
				bean.setUsername(rs.getString("a.username"));
				bean.setViews(rs.getInt("a.views"));
				bean.setLogo(rs.getString("a.logo"));
				return bean;
			}
		}
	}
	
	private AccountBean constructAccountBean(ResultSet rs) throws SQLException {
		AccountBean bean = new AccountBean();
		bean.setBroadcasterType(AccountBean.BroadcasterType.valueOf(rs.getString("broadcaster_type")));
		bean.setEmail(rs.getString("email"));
		bean.setEmailActivationKey(rs.getString("email_activation_key"));
		bean.setFollowers(rs.getInt("followers"));
		bean.setGrade(rs.getInt("grade"));
		bean.setTwitchId(rs.getInt("twitch_id"));
		bean.setUrl(rs.getString("url"));
		bean.setUsername(rs.getString("username"));
		bean.setViews(rs.getInt("views"));
		bean.setLogo(rs.getString("logo"));
		return bean;
	}

	public AccountBean get(int twitchId) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("SELECT * FROM accounts WHERE twitch_id = ?")) {
			prep.setInt(1, twitchId);
			try (ResultSet rs = prep.executeQuery()) {
				if (!rs.next()) return null;
				return constructAccountBean(rs);
			}
		}
	}

	public AccountBean getFromValidationCode(String code) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("SELECT * FROM accounts WHERE email_activation_key = ?")) {
			prep.setString(1, code);
			try (ResultSet rs = prep.executeQuery()) {
				if (!rs.next()) return null;
				return constructAccountBean(rs);
			}
		}
	}

}
