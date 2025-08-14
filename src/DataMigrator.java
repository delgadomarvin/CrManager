import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DataMigrator {

    // --- DATABASE CONNECTION DETAILS ---
    // Please replace these with your actual database credentials
    private static final String DB_URL = "jdbc:mysql://localhost:3306/dpadmwin";
    private static final String DB_USER = "your_username";
    private static final String DB_PASSWORD = "your_password";

    public static void main(String[] args) {
        Connection conn = null;
        Statement selectStmt = null;
        PreparedStatement insertStmt = null;
        ResultSet rs = null;

        try {
            // 1. Register the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. Open a connection
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            conn.setAutoCommit(false); // Start transaction

            // 3. Create the SELECT statement
            String selectSQL = "SELECT doc_codigo, doc_tipdoc, doc_neto, doc_basnet, doc_mtoiva, doc_cencos " +
                               "FROM dpdocpro " +
                               "WHERE doc_act = 1 " +
                               "AND doc_tiptra = 'act' " +
                               "AND doc_tipdoc IN ('orc', 'sct') " +
                               "AND doc_fecemi >= '2024-10-01' AND doc_fecemi < '2024-11-01'";

            System.out.println("Executing query: " + selectSQL);
            selectStmt = conn.createStatement();
            rs = selectStmt.executeQuery(selectSQL);
            System.out.println("Query executed successfully. Starting data migration...");

            // 4. Create the INSERT statement
            String insertSQL = "INSERT INTO dpdoccli (doc_codigo, doc_tipdoc, doc_neto, doc_basnet, doc_mtoiva, doc_cencos) " +
                               "VALUES (?, ?, ?, ?, ?, ?)";
            insertStmt = conn.prepareStatement(insertSQL);

            // 5. Iterate through the results and insert into dpdoccli
            int count = 0;
            while (rs.next()) {
                insertStmt.setString(1, rs.getString("doc_codigo"));
                insertStmt.setString(2, rs.getString("doc_tipdoc"));
                insertStmt.setBigDecimal(3, rs.getBigDecimal("doc_neto"));
                insertStmt.setBigDecimal(4, rs.getBigDecimal("doc_basnet"));
                insertStmt.setBigDecimal(5, rs.getBigDecimal("doc_mtoiva"));
                insertStmt.setInt(6, rs.getInt("doc_cencos"));

                insertStmt.addBatch();
                count++;
            }

            // 6. Execute the batch insert
            if (count > 0) {
                System.out.println("Inserting " + count + " records into dpdoccli...");
                insertStmt.executeBatch();
                conn.commit(); // Commit transaction
                System.out.println("Data migration completed successfully!");
            } else {
                System.out.println("No records found to migrate.");
            }

        } catch (SQLException se) {
            se.printStackTrace();
            if (conn != null) {
                try {
                    System.err.println("Transaction is being rolled back.");
                    conn.rollback();
                } catch (SQLException excep) {
                    excep.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 7. Close resources
            try {
                if (rs != null) rs.close();
                if (selectStmt != null) selectStmt.close();
                if (insertStmt != null) insertStmt.close();
                if (conn != null) conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}
