package com.smartomni.common.persistence;

import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

/** Bind Java enum names to the existing lower-case PostgreSQL named enums. */
public class LowercasePostgreSQLEnumJdbcType extends PostgreSQLEnumJdbcType {

    @Override
    public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
        return new BasicBinder<>(javaType, this) {
            @Override
            protected void doBind(PreparedStatement statement, X value, int index,
                                  WrapperOptions options) throws SQLException {
                statement.setObject(index, javaType.unwrap(value, String.class, options)
                        .toLowerCase(Locale.ROOT), Types.OTHER);
            }

            @Override
            protected void doBind(CallableStatement statement, X value, String name,
                                  WrapperOptions options) throws SQLException {
                statement.setObject(name, javaType.unwrap(value, String.class, options)
                        .toLowerCase(Locale.ROOT), Types.OTHER);
            }
        };
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
        return new BasicExtractor<>(javaType, this) {
            private X convert(String value, WrapperOptions options) {
                return value == null ? null : javaType.wrap(value.toUpperCase(Locale.ROOT), options);
            }

            @Override
            protected X doExtract(ResultSet result, int index, WrapperOptions options) throws SQLException {
                return convert(result.getString(index), options);
            }

            @Override
            protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
                return convert(statement.getString(index), options);
            }

            @Override
            protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
                return convert(statement.getString(name), options);
            }
        };
    }
}
