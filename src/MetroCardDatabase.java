import java.sql.*;

public class MetroCardDatabase {
	
	public static void updateMetroCardBalance(String id, double updatedBalance) {
		Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        
        try {
        	Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:MetroCards.db");
            String selectDataSQL = "UPDATE MetroCards SET balance = ? WHERE id = ?";
            preparedStatement = connection.prepareStatement(selectDataSQL);
            preparedStatement.setDouble(1, updatedBalance);
            preparedStatement.setString(2, id);
            preparedStatement.executeUpdate();
                        
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
 
                if (preparedStatement != null) preparedStatement.close();
                if (connection != null) connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
		
	}
	
	public static double getMetroCardData(String id) {
		Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        
        try {
        	Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:MetroCards.db");
            String selectDataSQL = "SELECT balance FROM MetroCards WHERE id = ?";
            preparedStatement = connection.prepareStatement(selectDataSQL);
            preparedStatement.setString(1, id);
            
            resultSet = preparedStatement.executeQuery();
            
            return resultSet.getDouble("balance");
                        
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
 
                if (preparedStatement != null) preparedStatement.close();
                if (connection != null) connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
        
	
	}
	
	static void insertMetroCardData(PreparedStatement preparedStatement, String id, double balance)throws Exception {
		
		preparedStatement.setString(1, id);
		
		preparedStatement.setDouble(2, balance);
		preparedStatement.executeUpdate();
	}
	
	static void populateDataForSimulation() {
		
		Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:MetroCards.db");
            
            String insertDataSQL = "INSERT INTO MetroCards (id, balance) VALUES (?, ?)";
            preparedStatement = connection.prepareStatement(insertDataSQL);
            
            insertMetroCardData(preparedStatement,"11223344", 10);
            insertMetroCardData(preparedStatement,"44332211", 10);
            insertMetroCardData(preparedStatement,"76543210", 10);    
            System.out.println("metroCard data for simulation demo inserted successfully");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (preparedStatement != null) preparedStatement.close();
                if (connection != null) connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
        

	
    public static void main(String[] args) {
        Connection connection = null;
        Statement statement = null;

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:MetroCards.db");

            statement = connection.createStatement();

            String createTableSQL = "CREATE TABLE IF NOT EXISTS metroCards " +
                    "(id VARCHAR(50) PRIMARY KEY, " +
                    "balance DOUBLE);";

            statement.executeUpdate(createTableSQL);
            
           
            System.out.println("Database and table created successfully!");
            
            

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        populateDataForSimulation();
        
        
        
        
        
    }
}
