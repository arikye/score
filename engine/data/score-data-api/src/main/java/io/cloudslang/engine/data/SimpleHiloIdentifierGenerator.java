/*******************************************************************************
* (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Apache License v2.0 which accompany this distribution.
*
* The Apache License is available at
* http://www.apache.org/licenses/LICENSE-2.0
*
*******************************************************************************/

package io.cloudslang.engine.data;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by IntelliJ IDEA.
 * User: froelica
 * Date: 4/25/13
 * Time: 11:03 AM
 */
public class SimpleHiloIdentifierGenerator implements IdentifierGenerator, IdentityGenerator {

	private final Logger logger = Logger.getLogger(getClass());

	static final String TABLE_NAME = "OO_HILO";
	static final String SQL_SELECT = "SELECT NEXT_HI FROM " + TABLE_NAME;
	static final String SQL_UPDATE = "UPDATE " + TABLE_NAME + " SET NEXT_HI = NEXT_HI+1";
    static final String SQL_LOCK = "UPDATE " + TABLE_NAME + " SET NEXT_HI = NEXT_HI";
	static final long CHUNK_SIZE = 100000L;

	private static DataSource dataSource;
    private int currentChunk;
    private long currentId;
    private Lock lock = new ReentrantLock();

    // been initialized by Hibernate
    public SimpleHiloIdentifierGenerator() {
        updateCurrentChunk();
    }

    public static void setDataSource(DataSource injectedDataSource) {
        dataSource = injectedDataSource;
    }

    @Override
    public Long next() {
        return (Long) generate(null, null);
    }

    @Override
    public List<Long> bulk(int bulkSize) {
        List <Long> idsList = new ArrayList<>();
        for (int i = 0; i < bulkSize; i++) {
            idsList.add(next());
        }
        return idsList;
    }

    @Override
    public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
        lock.lock();
        try {
            long id = ++currentId;
            if (id > CHUNK_SIZE) {
                if (logger.isDebugEnabled()) logger.debug("ID has reached chunk size");
                updateCurrentChunk();
                id = ++currentId;
            }
            return currentChunk * CHUNK_SIZE + id;
        } finally {
            lock.unlock();
        }
    }

    private void updateCurrentChunk() {
        if (logger.isDebugEnabled()) {
            logger.debug("Updating HILO chunk...");
        }

        long t = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(conn, true));

            jdbcTemplate.update(SQL_LOCK);
            currentChunk = queryForInt(jdbcTemplate, SQL_SELECT);
            if (logger.isDebugEnabled())
                logger.debug("Current chunk: " + currentChunk);
            jdbcTemplate.execute(SQL_UPDATE);
            jdbcTemplate.execute("commit");

            if (logger.isDebugEnabled()) {
                logger.debug("Updating HILO chunk done in " + (System.currentTimeMillis() - t) + " ms");
            }
            currentId = 0;
        } catch (SQLException e) {
            logger.error("Unable to update current chunk", e);
            throw new IllegalStateException("Unable to update current chunk");
        }
    }

    private int queryForInt(JdbcTemplate jdbcTemplate, String sql) throws DataAccessException {
        Number number = jdbcTemplate.queryForObject(sql, Integer.class);
        return (number != null ? number.intValue() : 0);
    }
}
