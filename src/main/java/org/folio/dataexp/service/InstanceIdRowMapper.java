package org.folio.dataexp.service;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class InstanceIdRowMapper implements RowMapper<UUID> {

  @Override
  public UUID mapRow(ResultSet rs, int i) throws SQLException {
    return UUID.fromString(rs.getString("id"));
  }
}
