import java.util.Iterator;
import java.util.Vector;

import zfa.Adresse;
import zfa.ZFATestSQL;


public class Postgresqltest {
	
	public static void main(String args[]) {
		
		ZFATestSQL zfa = new ZFATestSQL(null, null, null, null, null);
		
		try {
			
			if (!zfa.connect()) {
				System.err.println("Es konnte keine Datenbankverbindung hergestellt werden");
				System.exit(-1);
			}
			// open connection to database
//			Connection verbindung = DriverManager.getConnection(
//					// "jdbc:postgresql://dbhost:port/dbname", "user", "dbpass");
//					"jdbc:postgresql://192.168.165.88:5432/application", "postgres", "Kss9ix8hG9");

			// execute query
			Vector<Adresse> adressen = zfa.getAdressen("name LIKE '%Egel%'");

			Iterator<Adresse> adresse = adressen.iterator();
			
			if (adresse.hasNext()) {
				do {
					Adresse adr = adresse.next();
					System.out.println(adr.name); 
				} while(adresse.hasNext());
			}
			else {
				System.out.println("Es wurden keine Daten gefunden!");
			}
					
			zfa.close();
		} catch (java.sql.SQLException e) {
			System.err.println(e);
			System.exit(-1);
		}
	}
}
