package com.gnilc.test.cleanup;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseCleanerTest {
    @Test
    void truncatesBusinessTablesAndPreservesInfrastructureMetadata() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(List.of(
                "az_role",
                "flyway_schema_history",
                "databasechangelog",
                "databasechangeloglock",
                "QRTZ_JOB_DETAILS",
                "undo_log"));
        when(connection.createStatement()).thenReturn(statement);
        when(jdbc.execute(org.mockito.ArgumentMatchers.<ConnectionCallback<Void>>any())).thenAnswer(invocation ->
                invocation.<ConnectionCallback<Void>>getArgument(0).doInConnection(connection));

        new DatabaseCleaner(jdbc).truncateBusinessTables();

        verify(statement).addBatch("TRUNCATE TABLE `az_role`");
        verify(statement, never()).addBatch("TRUNCATE TABLE `flyway_schema_history`");
        verify(statement, never()).addBatch("TRUNCATE TABLE `databasechangelog`");
        verify(statement, never()).addBatch("TRUNCATE TABLE `databasechangeloglock`");
        verify(statement, never()).addBatch("TRUNCATE TABLE `QRTZ_JOB_DETAILS`");
        verify(statement, never()).addBatch("TRUNCATE TABLE `undo_log`");
        verify(statement).executeBatch();
    }
}
