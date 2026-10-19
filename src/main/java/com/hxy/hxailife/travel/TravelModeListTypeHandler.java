package com.hxy.hxailife.travel;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/** 将多选出行方式以逗号分隔保存到原有 travel_mode 字段。 */
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class TravelModeListTypeHandler extends BaseTypeHandler<List<TravelMode>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<TravelMode> parameter,
                                    JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter == null ? "" : parameter.stream()
                .filter(Objects::nonNull)
                .map(Enum::name)
                .distinct()
                .collect(joining(",")));
    }

    @Override
    public List<TravelMode> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<TravelMode> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<TravelMode> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<TravelMode> parse(String value) {
        if (value == null || value.isBlank()) {
            return List.of(TravelMode.AUTO);
        }
        List<TravelMode> modes = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(this::parseMode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(toList());
        return modes.isEmpty() ? List.of(TravelMode.AUTO) : Collections.unmodifiableList(modes);
    }

    private TravelMode parseMode(String value) {
        try {
            return TravelMode.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
