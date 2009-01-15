package org.limewire.promotion;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hsqldb.jdbcDriver;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.GGEP;
import org.limewire.promotion.containers.PromotionMessageContainer;
import org.limewire.promotion.exceptions.PromotionException;
import org.limewire.security.certificate.CertificateVerifier;
import org.limewire.security.certificate.CipherProvider;
import org.limewire.security.certificate.KeyStoreProvider;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SearcherDatabaseImpl implements SearcherDatabase {
    
    private static final Log LOG = LogFactory.getLog(SearcherDatabaseImpl.class);

    private Connection connection;

    private final KeywordUtil keywordUtil;

    private final CipherProvider cipherProvider;

    private final KeyStoreProvider keyStoreProvider;

    private final CertificateVerifier certificateVerifier;

    /** Package for testing. */
    static String getDBLocation() {
        return new File(CommonUtils.getUserSettingsDir(), "promotion/promodb").getAbsolutePath();
    }

    @Inject
    public SearcherDatabaseImpl(final KeywordUtil keywordUtil, final CipherProvider cipherProvider,
            final KeyStoreProvider keyStoreProvider, final CertificateVerifier certificateVerifier) {
        this.keywordUtil = keywordUtil;
        this.cipherProvider = cipherProvider;
        this.keyStoreProvider = keyStoreProvider;
        this.certificateVerifier = certificateVerifier;
    }

    public void init() throws InitializeException {
        try {
            new jdbcDriver();
            connection = DriverManager.getConnection("jdbc:hsqldb:file:" + getDBLocation(), "sa", "");
            createDBIfNeeded();
        } catch (SQLException sqlException) {
            throw new InitializeException(sqlException);
        } catch (DatabaseExecutionException e) {
            throw new InitializeException(e);
        }
    }

    public void shutDown() {
        if (connection != null) {
            try {
                executeUpdate("SHUTDOWN");
                org.hsqldb.DatabaseManager.closeDatabases(0);
            } catch (DatabaseExecutionException ignore) {
                // ignore
            }
        }
    }

    /**
     * Creates a statement and runs the given SQL, then returns the resulting
     * affected row count.
     * 
     * @see {@link java.sql.Statement#executeUpdate(String)}
     */
    private int executeUpdate(final String sql, final Object... values)
            throws DatabaseExecutionException {
        try {
            final PreparedStatement statement = connection.prepareStatement(sql);
            try {
                for (int i = 0; i < values.length; i++) {
                    if (values[i] instanceof Date)
                        statement.setDate(i + 1, new java.sql.Date(((Date) values[i]).getTime()));
                    else
                        statement.setObject(i + 1, values[i]);
                }
                return statement.executeUpdate();
            } finally {
                statement.close();
            }
        } catch (SQLException ex) {
            throw new DatabaseExecutionException(ex);
        }
    }

    /**
     * This represents a single SQL statement and values to fill in the holes.
     */
    private final static class Stmt {
        private final static Object[] EMPTY_VALUES = new Object[0];

        final String sql;

        final Object[] values;

        Stmt(String sql, Object[] values) {
            this.sql = sql;
            this.values = values;
        }

        Stmt(String sql) {
            this(sql, EMPTY_VALUES);
        }
    }

    /**
     * Shortcut for creating a {@link Stmt}.
     * 
     * @param sql sql statement
     */
    private static Stmt stmt(String sql) {
        return new Stmt(sql);
    }

    /**
     * Returns the total number of affected rows and executing multiple updates synchronously.
     * 
     * @param stmts statements to execute
     * @return the total number of affected rows and executing multiple updates
     *         synchronously.
     * @throws DatabaseExecutionException
     */
    private synchronized int executeUpdates(Stmt... stmts) throws DatabaseExecutionException {
        int numAffectedRows = 0;
        for (Stmt stmt : stmts) {
            numAffectedRows += executeUpdate(stmt.sql, stmt.values);
        }
        return numAffectedRows;
    }

    /**
     * Creates a statement and runs the given SQL, then returns the results of a
     * call to "CALL IDENTITY()"
     * 
     * @throws DatabaseExecutionException
     */
    private long executeInsert(final String sql, final Object... values) throws DatabaseExecutionException {
        try {
            executeUpdate(sql, values);
            PreparedStatement statement = connection.prepareStatement("CALL IDENTITY()");
            try {
                final ResultSet rs = statement.executeQuery();
                try {
                    rs.next();
                    return rs.getLong(1);
                } finally {
                    rs.close();
                }
            } finally {
                statement.close();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("SQLException during update", ex);
        }
    }

    private void createDBIfNeeded() throws DatabaseExecutionException {
        try {
            query("foo");
        } catch (RuntimeException ignored) {
            // Looks like the db needs some work, try clearing it.
            clear();
        }
    }

    public void clear() throws DatabaseExecutionException {
        executeUpdates(
                stmt("DROP TABLE keywords IF EXISTS"),
                stmt("DROP TABLE entries IF EXISTS"),
                stmt("DROP TABLE binders IF EXISTS"),
                stmt("CREATE CACHED TABLE entries (" + "entry_id IDENTITY, " + "unique_id BIGINT, "
                        + "probability_num FLOAT, " + "type_byte TINYINT, "
                        + "valid_start_dt DATETIME, "
                        + "valid_end_dt DATETIME, entry_ggep BINARY )"),
                stmt("CREATE CACHED TABLE keywords ("
                        + "keyword_id IDENTITY, "
                        + "binder_unique_name VARCHAR(1000),"
                        + "phrase VARCHAR,"
                        + "entry_id INTEGER, FOREIGN KEY (entry_id) REFERENCES entries (entry_id) ON DELETE CASCADE )"),
                stmt("CREATE CACHED TABLE binders (" + "binder_id IDENTITY, "
                        + "binder_unique_name VARCHAR(1000), " + "binder_bucket_id INTEGER, "
                        + "valid_end_dt DATETIME, " + "binder_blob BINARY)")
             );
    }

    public void expungeExpired() throws DatabaseExecutionException {
        executeUpdates(
                stmt("DELETE FROM entries WHERE valid_end_dt < CURRENT_TIMESTAMP"),
                stmt("DELETE FROM binders WHERE valid_end_dt < CURRENT_TIMESTAMP")
        );
    }

    private synchronized void saveBinder(final PromotionBinder binder) throws DatabaseExecutionException {
        executeUpdate("DELETE FROM binders where binder_unique_name = ?", binder.getUniqueName());
        executeInsert(
                "INSERT INTO binders (binder_unique_name, binder_bucket_id, valid_end_dt, binder_blob) VALUES (?,?,?,?)",
                new Object[] { binder.getUniqueName(), binder.getBucketNumber(),
                        binder.getValidEnd(), binder.getEncoded() });
    }

    public PromotionBinder getBinder(final String binderUniqueName) {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("SELECT binder_blob FROM "
                    + "binders WHERE binder_unique_name = ? ORDER BY binder_id DESC");
            statement.setString(1, binderUniqueName);
            final ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                final PromotionBinder binder = new PromotionBinder(cipherProvider,
                        keyStoreProvider, certificateVerifier);
                binder.initialize(rs.getBytes("binder_blob"));
                return binder;
            }
            return null;
        } catch (SQLException ex) {
            throw new RuntimeException("SQLException during query.", ex);
        } catch (PromotionException ex) {
            // ignore -- internal problem with the promotion,
            // and we'll notice the error on the server side.
            // most typical reason is the cert is invalid
            // (perhaps because DNS cached the cert hash,
            //  but the bucket is signed using a newer cert)
            LOG.error("promotion error", ex);
            return null;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public PromotionBinder getBinder(final int bucketNumber) {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("SELECT binder_blob FROM "
                    + "binders WHERE binder_bucket_id = ? ORDER BY binder_id DESC");
            statement.setInt(1, bucketNumber);
            final ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                final PromotionBinder binder = new PromotionBinder(cipherProvider,
                        keyStoreProvider, certificateVerifier);
                binder.initialize(rs.getBytes("binder_blob"));
                return binder;
            }
            return null;
        } catch (SQLException ex) {
            throw new RuntimeException("SQLException during query.", ex);
        } catch (PromotionException ex) {
            //
            // LWC-1452: This is only thrown when the binder has an invalid date
            // or is corrupt. In any case, do not show an exception
            // here, simply return null. By returning null we signal
            // that this binder needs to be re-ingested from the network.
            //
            return null;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * Does the actual ingestion of the given promo entry, inserting it into the
     * db. Package-visible for testing.
     * 
     * @throws DatabaseExecutionException
     */
    synchronized void ingest(final PromotionMessageContainer promo, final String binderUniqueName)
            throws DatabaseExecutionException {
        executeUpdate("DELETE FROM entries WHERE unique_id = ?", promo.getUniqueID());
        final long entryID = executeInsert(
                "INSERT INTO entries "
                        + "(unique_id, probability_num, type_byte, valid_start_dt, valid_end_dt, entry_ggep) "
                        + "values (?,?,?,?,?,?)", promo.getUniqueID(), promo.getProbability(),
                promo.getMediaType().getValue(), promo.getValidStart(), promo.getValidEnd(), promo
                        .encode());
        for (String keyword : keywordUtil.splitKeywords(promo.getKeywords()))
            executeInsert(
                    "INSERT INTO keywords (phrase, binder_unique_name, entry_id) values (?,?,?)",
                    keywordUtil.normalizeQuery(keyword), binderUniqueName, entryID);
    }

    public void ingest(final PromotionBinder binder) throws DatabaseExecutionException {
        for (PromotionMessageContainer promo : binder.getPromoMessageList())
            ingest(promo, binder.getUniqueName());
        saveBinder(binder);
    }

    public List<QueryResult> query(final String query) {
        final List<QueryResult> results = new ArrayList<QueryResult>();

        PreparedStatement statement = null;
        try {
            statement = connection
                    .prepareStatement("SELECT DISTINCT e.entry_id, k.binder_unique_name, e.probability_num FROM "
                            + "keywords k JOIN entries e ON e.entry_id = k.entry_id WHERE "
                            + "e.valid_start_dt <= CURRENT_TIMESTAMP AND e.valid_end_dt >= CURRENT_TIMESTAMP AND "
                            + "k.phrase = ? ORDER BY e.probability_num DESC, RAND()");
            statement.setString(1, keywordUtil.normalizeQuery(query));
            final ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String binderUniqueName = rs.getString("binder_unique_name");
                PromotionMessageContainer promo = getPromotionMessageContainer(rs
                        .getLong("entry_id"));
                if (promo != null && binderUniqueName != null)
                    results.add(new QueryResultImpl(binderUniqueName, promo, query));
            }
            rs.close();
        } catch (SQLException ex) {
            throw new RuntimeException("SQLException during query.", ex);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {

                }
            }
        }

        return results;
    }

    /**
     * Loads and return the given entry id from the database, ignoring all the
     * fields except the entry_ggep, reparsing the ggep into a promotion entry.
     * 
     * @param entryID entry ID to load
     * @return the given entry id from the database, ignoring all the fields
     *         except the entry_ggep, reparsing the ggep into a promotion entry.
     */
    PromotionMessageContainer getPromotionMessageContainer(final long entryID) {
        PreparedStatement statement = null;
        try {
            statement = connection
                    .prepareStatement("select entry_ggep from entries where entry_id = ?");
            statement.setLong(1, entryID);
            final ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                final byte[] ggep = rs.getBytes(1);
                final PromotionMessageContainer promo = new PromotionMessageContainer();
                promo.decode(new GGEP(ggep, 0));
                return promo;
            }
            rs.close();
            // Couldn't find the given entry id.
            return null;
        } catch (SQLException ex) {
            throw new RuntimeException("SQLException during query.", ex);
        } catch (BadGGEPBlockException ex) {
            throw new RuntimeException("GGEPException during query.", ex);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private class QueryResultImpl implements QueryResult {
        private final String binderUniqueName;

        private final Date creationDate = new Date();

        private final PromotionMessageContainer promotionMessageContainer;

        private final String query;

        QueryResultImpl(final String binderUniqueName, final PromotionMessageContainer promo,
                final String query) {
            this.binderUniqueName = binderUniqueName;
            this.promotionMessageContainer = promo;
            this.query = query;
        }

        public String getBinderUniqueName() {
            return binderUniqueName;
        }

        public Date getDate() {
            return creationDate;
        }

        public PromotionMessageContainer getPromotionMessageContainer() {
            return promotionMessageContainer;
        }

        public String getQuery() {
            return query;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof QueryResultImpl) {
                final QueryResultImpl other = (QueryResultImpl) obj;
                return this.promotionMessageContainer.equals(other.promotionMessageContainer);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return promotionMessageContainer.hashCode();
        }
    }
}
