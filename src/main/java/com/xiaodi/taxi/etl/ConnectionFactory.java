package com.xiaodi.taxi.etl;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionFactory {
    Connection getConnection(String url) throws SQLException;
}
