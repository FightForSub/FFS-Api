package tv.zerator.ffs.api.dao;

import java.sql.Connection;
import java.sql.SQLException;

import com.jolbox.bonecp.BoneCPDataSource;
import com.jolbox.bonecp.PreparedStatementHandle;

import alexmog.apilib.dao.DAO;
import alexmog.apilib.managers.DaoManager.Dao;
import tv.zerator.ffs.api.dao.beans.TokenBean;

@Dao(database = "general")
public class TokensDao extends DAO<TokenBean> {

	public TokensDao(BoneCPDataSource dataSource) {
		super(dataSource);
	}

	@Override
	public int insert(TokenBean data) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("INSERT INTO auth_tokens "
						+ "(account_id, token, last_used_timestamp) VALUES (?, ?, ?)")) {
			prep.setInt(1, data.getAccountId());
			prep.setString(2, data.getToken());
			prep.setLong(3, data.getLastUsedTimestamp());
			prep.executeUpdate();
			return 0;
		}
	}
	
	public void delete(String tokenId) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("DELETE FROM auth_tokens WHERE token = ?")) {
			prep.setString(1, tokenId);
			prep.executeUpdate();
		}
	}

	@Override
	public TokenBean update(TokenBean data) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("UPDATE auth_tokens SET "
						+ "account_id = ?, last_used_timestamp = ? WHERE token = ?")) {
			prep.setInt(1, data.getAccountId());
			prep.setLong(2, data.getLastUsedTimestamp());
			prep.setString(3, data.getToken());
			prep.executeUpdate();
		}
		return data;
	}

}
