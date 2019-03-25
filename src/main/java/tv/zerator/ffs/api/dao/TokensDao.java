package tv.zerator.ffs.api.dao;

import java.sql.SQLException;

import com.jolbox.bonecp.PreparedStatementHandle;

import alexmog.apilib.dao.DAO;
import alexmog.apilib.managers.DaoManager.Dao;
import tv.zerator.ffs.api.dao.beans.TokenBean;

@Dao(database = "general")
public class TokensDao extends DAO {

	public int insert(TokenBean data) throws SQLException {
		try (PreparedStatementHandle prep = (PreparedStatementHandle) getConnection().prepareStatement("INSERT INTO auth_tokens "
						+ "(account_id, token, last_used_timestamp) VALUES (?, ?, ?)")) {
			prep.setInt(1, data.getAccountId());
			prep.setString(2, data.getToken());
			prep.setLong(3, data.getLastUsedTimestamp());
			prep.executeUpdate();
			return 0;
		}
	}
	
	public void delete(String tokenId) throws SQLException {
		try (PreparedStatementHandle prep = (PreparedStatementHandle) getConnection().prepareStatement("DELETE FROM auth_tokens WHERE token = ?")) {
			prep.setString(1, tokenId);
			prep.executeUpdate();
		}
	}

	public TokenBean update(TokenBean data) throws SQLException {
		try (PreparedStatementHandle prep = (PreparedStatementHandle) getConnection().prepareStatement("UPDATE auth_tokens SET "
						+ "account_id = ?, last_used_timestamp = ? WHERE token = ?")) {
			prep.setInt(1, data.getAccountId());
			prep.setLong(2, data.getLastUsedTimestamp());
			prep.setString(3, data.getToken());
			prep.executeUpdate();
		}
		return data;
	}

}
