/**
 * NavSep2ZFA
 * 
 * Liest aus einer CSV-Datei die SEPA-Mandate ein
 * und schreibt diese in die das ZFA
 * Danach wird die Datei in das Archiv-Verzeichnis verschoben
 */

import java.sql.ResultSet;
import java.sql.Date;

import tk.INI;

import zfa.ZFASQL;

public class zfa_vertragsposten_mit_altem_abruf {
	
	private int _errorlevel = 0;
	
	private ZFASQL _zfa;
	
	private String _iniDatei;
	private INI _iniHandler;
	
	
	public zfa_vertragsposten_mit_altem_abruf() {
		
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

		sql = "SELECT * FROM billing.vertragsposten_alter_abruf_view_billing";

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
				
				while(rs.next()) {
					
					System.out.println("Zeile");
					
					int    vertragnummer  = rs.getInt("vertragnummer");
				
					System.out.println(vertragnummer); // + " " + buchungstext); //  + naechstabruf + vertragspostenid);
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
	
	
	public static void main (String[] ags) {
		
		// Programm starten
		zfa_vertragsposten_mit_altem_abruf action = new zfa_vertragsposten_mit_altem_abruf();
		
		action.run();
		
		System.exit(action.getErrorlevel());
	}
}