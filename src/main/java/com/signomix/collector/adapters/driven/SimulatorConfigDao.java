package com.signomix.collector.adapters.driven;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.signomix.collector.app.logic.simulator.dto.SimulatorConfig;
import com.signomix.common.db.IotDatabaseException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SimulatorConfigDao {

    @Inject
    DataSource dataSource;

    public void createTable() throws IotDatabaseException {
        String sql = """
            CREATE TABLE IF NOT EXISTS simulator_config (
                id SERIAL PRIMARY KEY,
                source VARCHAR(255),
                target VARCHAR(255),
                interval_ms BIGINT,
                from_date VARCHAR(50),
                to_date VARCHAR(50),
                start_date VARCHAR(50),
                data_names TEXT,
                not_modified_names TEXT,
                data_variability DOUBLE PRECISION,
                continuous BOOLEAN DEFAULT false,
                counter BIGINT DEFAULT 0,
                data TEXT,
                created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION, e.getMessage(), e);
        }
    }

    public void save(SimulatorConfig config) throws IotDatabaseException {
        String sql = """
            INSERT INTO simulator_config 
            (source, target, interval_ms, from_date, to_date, start_date, 
             data_names, not_modified_names, data_variability, continuous, counter, data)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, config.source);
            stmt.setString(2, config.target);
            stmt.setObject(3, config.interval);
            stmt.setString(4, config.fromDate);
            stmt.setString(5, config.toDate);
            stmt.setString(6, config.startDate);
            stmt.setString(7, config.dataNames);
            stmt.setString(8, config.notModifiedNames);
            stmt.setObject(9, config.dataVariability);
            stmt.setBoolean(10, config.continuous);
            stmt.setObject(11, config.counter);
            stmt.setString(12, config.data);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION, e.getMessage(), e);
        }
    }

    public void update(long id, SimulatorConfig config) throws IotDatabaseException {
        String sql = """
            UPDATE simulator_config SET 
            source = ?, target = ?, interval_ms = ?, from_date = ?, to_date = ?, 
            start_date = ?, data_names = ?, not_modified_names = ?, data_variability = ?, 
            continuous = ?, counter = ?, data = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, config.source);
            stmt.setString(2, config.target);
            stmt.setObject(3, config.interval);
            stmt.setString(4, config.fromDate);
            stmt.setString(5, config.toDate);
            stmt.setString(6, config.startDate);
            stmt.setString(7, config.dataNames);
            stmt.setString(8, config.notModifiedNames);
            stmt.setObject(9, config.dataVariability);
            stmt.setBoolean(10, config.continuous);
            stmt.setObject(11, config.counter);
            stmt.setString(12, config.data);
            stmt.setLong(13, id);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION, e.getMessage(), e);
        }
    }

    public SimulatorConfig findById(long id) throws IotDatabaseException {
        String sql = "SELECT * FROM simulator_config WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToConfig(rs);
                }
            }
        } catch (SQLException e) {
            throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION, e.getMessage(), e);
        }
        return null;
    }

    public List<SimulatorConfig> findByTarget(String target) throws IotDatabaseException {
        String sql = "SELECT * FROM simulator_config WHERE target = ?";
        List<SimulatorConfig> configs = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, target);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    configs.add(mapResultSetToConfig(rs));
                }
            }
        } catch (SQLException e) {
            throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION, e.getMessage(), e);
        }
        return configs;
    }

    public List<SimulatorConfig> findAll() throws IotDatabaseException {
        String sql = "SELECT * FROM simulator_config ORDER BY created_at DESC";
        List<SimulatorConfig> configs = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    configs.add(mapResultSetToConfig(rs));
                }
            }
        } catch (SQLException e) {
            throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION, e.getMessage(), e);
        }
        return configs;
    }

    public void deleteById(long id) throws IotDatabaseException {
        String sql = "DELETE FROM simulator_config WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION, e.getMessage(), e);
        }
    }

    private SimulatorConfig mapResultSetToConfig(ResultSet rs) throws SQLException {
        SimulatorConfig config = new SimulatorConfig();
        config.source = rs.getString("source");
        config.target = rs.getString("target");
        config.interval = (Long) rs.getObject("interval_ms");
        config.fromDate = rs.getString("from_date");
        config.toDate = rs.getString("to_date");
        config.startDate = rs.getString("start_date");
        config.dataNames = rs.getString("data_names");
        config.notModifiedNames = rs.getString("not_modified_names");
        config.dataVariability = (Double) rs.getObject("data_variability");
        config.continuous = rs.getBoolean("continuous");
        config.counter = (Long) rs.getObject("counter");
        config.data = rs.getString("data");
        return config;
    }
}