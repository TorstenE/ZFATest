/**
 * zfa_kunden_mit_diskrepanz_evn
 * 
 * Liest aus einer CSV-Datei die SEPA-Mandate ein
 * und schreibt diese in die das ZFA
 * Danach wird die Datei in das Archiv-Verzeichnis verschoben
 */

import java.sql.ResultSet;
import java.sql.Date;

import tk.INI;

import zfa.ZFASQL;

public class zfa_kunden_mit_diskrepanz_evn {
	
	private int _errorlevel = 0;
	
	private ZFASQL _zfa;
	
	private String _iniDatei;
	private INI _iniHandler;
	
	
	public zfa_kunden_mit_diskrepanz_evn() {
		
		this._iniDatei = "src\\zfa.ini";
	
		this._zfa = new ZFASQL();
	}

	
	private void _connectDB() {
		
		
		if (!this._zfa.connect()) {
			System.err.println("Es konnte keine Datenbankverbindung hergestellt werden");
			System.exit(-1);
		}
	}

	
	private void _closeDB() {
		
		try {
			
			this._zfa.close();
			
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	/**
	 * Liest die INI-Datei ein und holt sich den Pfad für die CSV-Datei(en)
	 */
	private void _readINIDatei() {
		
		try { 
			this._iniHandler = new INI(this._iniDatei);
		
			this._zfa.setHost(this._iniHandler.get("ZFA", "dbhost"));

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	
	
	private boolean _checkVertraege() {
		
		String    sql;
		ResultSet rs;
		boolean   result          = true;

		sql = "SELECT * FROM evn_eintrag_ungueltig";

		// System.out.println(sql);
		
		try {
			rs = this._zfa.getRows(sql);
			
		} catch (Exception e) {
			System.err.println(e.getMessage());
			
			this._errorlevel = 1;
			
			return false;
		}

		try {
			if (rs.isBeforeFirst()) {
				
				result = false;
				
				System.out.println("");
				System.out.println(" kd.nr. | name");
				System.out.println("--------|--------------------------------");
				
				while(rs.next()) {
					
					int     kundennummer = rs.getInt("kundennummer");
					String  kundenname   = rs.getString("kundenname");
				
					System.out.print(String.format("%1$7d", kundennummer));
					System.out.print(" | " + String.format("%1$-30s", kundenname).substring(0, 30));

				}
			}
			else {
				// nur zu Testzwecken
				System.out.println("OK - Es wurden keine Datensätze gefunden");
			}
			
		} catch (java.sql.SQLException e) {
			System.err.println(e.getMessage());
			
			this._errorlevel = 1;
			
			result = false;
		}
		
		return result;
	}
	
	public int getErrorlevel() {
		
		return this._errorlevel;
	}
	
	
	public void run() {

		this._readINIDatei();
		
		this._connectDB();
		
		this._checkVertraege();
		
		this._closeDB();
		
	}
	
	
	public static void main (String[] args) {
		
		// Programm starten
		zfa_kunden_mit_diskrepanz_evn action = new zfa_kunden_mit_diskrepanz_evn();
		
		action.run();
		
		System.exit(action.getErrorlevel());
	}
}