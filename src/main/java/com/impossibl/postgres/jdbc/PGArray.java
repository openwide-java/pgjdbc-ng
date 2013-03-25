package com.impossibl.postgres.jdbc;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.impossibl.postgres.protocol.ResultField;
import com.impossibl.postgres.protocol.ResultField.Format;
import com.impossibl.postgres.types.ArrayType;
import com.impossibl.postgres.types.Registry;

public class PGArray implements Array {

	PGConnection connection;
	ArrayType type;
	Object[] value;

	public PGArray(PGConnection connection, ArrayType type, Object[] value) {
		super();
		this.connection = connection;
		this.type = type;
		this.value = value;
	}

	public Object[] getValue() {
		return value;
	}

	@Override
	public String getBaseTypeName() throws SQLException {
		return type.getElementType().getName();
	}

	@Override
	public int getBaseType() throws SQLException {
		return SQLTypeMetaData.getSQLType(type.getElementType());
	}

	@Override
	public Object getArray() throws SQLException {
		return getArray(connection.getTypeMap());
	}

	@Override
	public Object getArray(Map<String, Class<?>> map) throws SQLException {
		
		return SQLTypeUtils.coerceToArrayType(value, type, map, connection);
	}

	@Override
	public Object getArray(long index, int count) throws SQLException {
		return getArray(index, count, connection.getTypeMap());
	}

	@Override
	public Object getArray(long index, int count, Map<String, Class<?>> map) throws SQLException {

		if(index < 1 || index > value.length || (index + count) > (value.length + 1)) {
			 throw new SQLException("Invalid array slice");
		}

		return SQLTypeUtils.coerceToArrayType(value, (int)index-1, count, type, map, connection);
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		return getResultSet(connection.getTypeMap());
	}

	@Override
	public ResultSet getResultSet(Map<String, Class<?>> map) throws SQLException {
		return getResultSet(1, value.length);
	}

	@Override
	public ResultSet getResultSet(long index, int count) throws SQLException {
		return getResultSet(index, count, connection.getTypeMap());
	}

	@Override
	public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map) throws SQLException {

		if(index < 1 || index > value.length || (index + count) > (value.length + 1)) {
			 throw new SQLException("Invalid array slice");
		}

		Registry reg = connection.getRegistry();
		
		ResultField[] fields = {
				new ResultField("INDEX", 0, (short)0, reg.loadType("int4"), (short)0, 0, Format.Binary),
				new ResultField("VALUE", 0, (short)0, type.getElementType(), (short)0, 0, Format.Binary)
		};
		
		List<Object[]> results = new ArrayList<Object[]>(value.length);
		for(long c=index,end=index+count; c < end; ++c) {
			results.add(new Object[]{c, value[(int) c-1]});
		}
		
		PGStatement stmt = connection.createStatement();
		stmt.closeOnCompletion();
		return new PGResultSet(stmt, Arrays.asList(fields), results, map);
	}

	@Override
	public void free() throws SQLException {
		connection = null;
		type = null;
		value = null;
	}

}
