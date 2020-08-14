/**
 * NavSep2ZFA
 * 
 * Prüfen ob neu angelegte Produkte/Artikel prinzipiell in die Abrechnung mit aufgenommen werden.
 * Hier darf kein Ergebnis zurückkommen.
 * Wenn doch: Überarbeitung des Produkts (z.B. Tabelle rechnungklammer ergänzen)
 * und erneute Kontrolle
 */

import java.sql.ResultSet;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.sql.Date;

import tk.INI;

import zfa.ZFASQL;

public class zfa_check_posten_abruf {
	
	private int _errorlevel = 0;
	
	private ZFASQL _zfa;
	
	private String _iniDatei;
	private INI _iniHandler;
	
	private int _monat = 0;
	private int _jahr  = 0;
	
	
	public zfa_check_posten_abruf(String[] args) {
		
		this._iniDatei = "src\\zfa.ini";
	
		this._zfa = new ZFASQL();
		
		if (args.length > 0) {
			this._monat = Integer.parseInt(args[0]);
		}
		
		if (args.length > 1) {
			this._jahr = Integer.parseInt(args[1]);
		}
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

		GregorianCalendar kalender = (GregorianCalendar) GregorianCalendar.getInstance();
		
		if (this._jahr == 0) {
			this._jahr = kalender.get(Calendar.YEAR);
		}
		
		// Datum auf den 1. das angegeben Monats setzen
		// Monat (0 - 11), deshalb - 1
		kalender.set(this._jahr, this._monat - 1, 1);
		
		int letzterTag = kalender.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
		
		String vonDatum = kalender.get(Calendar.YEAR) + "-" + String.format("%02d", kalender.get(Calendar.MONTH)+1) + "-" + "01";
		String bisDatum = kalender.get(Calendar.YEAR) + "-" + String.format("%02d", kalender.get(Calendar.MONTH)+1) + "-" + String.format("%02d", letzterTag);

		sql = "SELECT * FROM billing.check_posten_abruf('STANDARD','" + vonDatum + "','" + bisDatum + "')";

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
				System.out.println("vertrag | posten                         | faktura_beginn | naechster_abruf");
				System.out.println("--------|--------------------------------|----------------|----------------");
				while(rs.next()) {
					
					int    vertragnummer   = rs.getInt("vertragnummer");
					String posten          = rs.getString("posten");
					Date   faktura_beginn  = rs.getDate("faktura_beginn");
					Date   naechster_abruf = rs.getDate("naechster_abruf");
				
					System.out.print(String.format("%1$7d", vertragnummer));
					System.out.print(" | " + String.format("%1$-30s",posten).substring(0, 30));
					System.out.print(" | " + faktura_beginn + "    ");
					System.out.println(" | " + naechster_abruf);
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
		
		if (args.length < 1) {
			System.out.println("Fehler: Es wurde kein Monat 1 - 12 als Parameter angegeben");
			System.out.println("Aufruf: zfa_check_posten_abruf [monat] ([jahr])");
			System.exit(1);
		}
		
		int monat = Integer.parseInt(args[0]);
		
		if (monat < 1 || monat > 12) {
			System.out.println("Fehler: Es darf nur ein Monat von 1 - 12 angegeben werden");
			System.out.println("Aufruf: zfa_check_posten_abruf [monat] ([jahr])");
			System.exit(1);
		}
		
		if (args.length > 1) {
			
			int jahr = Integer.parseInt(args[1]);
			
			Calendar kalender = GregorianCalendar.getInstance();

			int aktJahr = kalender.get(GregorianCalendar.YEAR);
			
			if (jahr < (aktJahr - 1) || jahr > (aktJahr + 1)) {
				System.out.println("Fehler: Das Jahr darf max. +/- 1 Jahr Abweichung vom aktuellen Jahr " + aktJahr + " haben.");
				System.out.println("Aufruf: zfa_check_posten_abruf [monat] ([jahr])");
				System.exit(1);
			}
		}
		
		
		// Programm starten
		zfa_check_posten_abruf action = new zfa_check_posten_abruf(args);
		
		action.run();
		
		System.exit(action.getErrorlevel());
	}
}