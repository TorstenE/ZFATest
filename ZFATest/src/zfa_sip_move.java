/**
 * zfa_sip_move
 * 
 * Kopiert die Rufnummern und SIP-Accounts von einem Vertrag in den anderen
 */

import java.awt.List;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Iterator;
import java.util.Vector;

import tk.INI;

import zfa.ZFASQL;

public class zfa_sip_move {
	
	private int _errorlevel = 0;
	
	private ZFASQL _zfa;
	
	private String _iniDatei;
	private INI _iniHandler;
	
	
	public zfa_sip_move() {
		
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
	
	
	private int _getVertragID(int vertragnummer) {
		
		String    sql;
		ResultSet rs;
		int       result = -1;

		sql = "SELECT vertragid FROM vertrag WHERE vertragnummer = " + vertragnummer;

		System.out.println(sql);
		
		try {
			rs = this._zfa.getRows(sql);
			
			if (rs.isBeforeFirst()) {
				
				rs.next();
				
				result = rs.getInt("vertragid");
				
				System.out.println(String.format("Vertrag ID %d", result));
			}
			else {
				// nur zu Testzwecken
				System.out.println("OK - Es wurde kein Vertrag gefunden");
			}
			
		} catch (java.sql.SQLException e) {
			System.err.println(e.getMessage());
			
			this._errorlevel = 1;
			
			result = -1;
		}
		
		return result;
	}
	
	
	private int _getRfonVertragID(int vertragid) {
		
		String    sql;
		ResultSet rs;
		int       result = 0;

		sql = "SELECT rfonvertragid FROM rfonvertrag WHERE vertragid = " + vertragid;

		System.out.println(sql);
		
		try {
			
			rs = this._zfa.getRows(sql);
			
			if (rs.isBeforeFirst()) {
				
				rs.next();
				
				result = rs.getInt("rfonvertragid");
				
				System.out.println(String.format("RfonVertrag ID %d", result));
			}
			else {
				// nur zu Testzwecken
				System.out.println("OK - Es wurde kein RfonVertrag gefunden");
			}
			
		} catch (java.sql.SQLException e) {
			System.err.println(e.getMessage());
			
			this._errorlevel = 1;
		}
		
		return result;
	}


	private int _getCudaID(int rfonvertragid) {
		
		String    sql;
		ResultSet rs;
		int       result = 0;

		sql = "SELECT cudaid FROM cuda WHERE rfonvertragid = " + rfonvertragid;

		System.out.println(sql);
		
		try {
			rs = this._zfa.getRows(sql);
			
			if (rs.isBeforeFirst()) {
				
				rs.next();
				
				result = rs.getInt("cudaid");
				
				System.out.println(String.format("Cuda ID %d", result));
			}
			else {
				// nur zu Testzwecken
				System.out.println("OK - Es wurde kein Datensatz gefunden");
			}
			
		} catch (java.sql.SQLException e) {
			System.err.println(e.getMessage());
			
			this._errorlevel = 1;
		}
		
		return result;
	}

	
	private int _getAuftragID(int vertragid) {
		
		String    sql;
		ResultSet rs;
		int       result = -1;

		sql =  "SELECT a.auftragid";
		sql += " FROM vertrag v";
		sql += " JOIN vertragsposten vp ON vp.vertragid = v.vertragid";
		sql += " JOIN auftrag a ON a.vertragspostenid = vp.vertragspostenid";
		sql += " WHERE v.vertragid = " + vertragid;

		System.out.println(sql);
		
		try {
			
			rs = this._zfa.getRows(sql);
			
			if (rs.isBeforeFirst()) {
				
				rs.next();
				
				result = rs.getInt("auftragid");
				
				System.out.println(String.format("Auftrag ID %d", result));
			}
			else {
				// nur zu Testzwecken
				System.out.println("OK - Es wurde kein Auftrag gefunden");
			}
			
		} catch (java.sql.SQLException e) {
			System.err.println(e.getMessage());
			
			this._errorlevel = 1;
		}
		
		return result;
	}


	private Date _getTermin(int nvernr) {
		
		String    sql;
		ResultSet rs;
		Date       result = null;

		sql  = "SELECT vp.inbestaetigttermin";
		sql += " FROM vertrag v";
		sql += " JOIN vertragsposten vp ON vp.vertragid = v.vertragid AND vp.is_produkt = 1";
		sql += " WHERE v.vertragnummer = " + nvernr; 

		System.out.println(sql);
		
		try {
			
			rs = this._zfa.getRows(sql);
			
			if (rs.isBeforeFirst()) {
				
				rs.next();
				
				result = rs.getDate("inbestaetigttermin");
				
			}
			
			if (result != null) {

				String dateStr = String.format("%td.%tm.%tY",  result, result, result);
				
				System.out.println(String.format("Inbetriebnahmetermin %s ermittelt", dateStr));
			}
			else {
				// nur zu Testzwecken
				System.out.println("OK - Es wurde kein Inbetriebnahme beim neuen Vertrag gefunden");
			}
			
		} catch (java.sql.SQLException e) {
			System.err.println(e.getMessage());
			
			this._errorlevel = 1;
		}
		
		return result;
	}
	
	
	private Vector<Integer> _getSipAccNumbers(int rfid) {
		
		String    sql;
		ResultSet rs;
		Vector<Integer> result = new Vector<Integer>();

		sql =  "SELECT DISTINCT sipaccountnr FROM aakz WHERE rfonvertragid=" + rfid + " AND gueltig_bis IS NULL";
		
		System.out.println(sql);
		
		try {
			
			rs = this._zfa.getRows(sql);
			
			if (rs.isBeforeFirst()) {
				
				int anzahl = this._zfa.getRowCount(rs);
				
				System.out.println("Anzahl SIP Nummern " + anzahl);
				
				while (rs.next()) {
					
					int counter = rs.getInt("sipaccountnr");
					
					result.add(counter);

					System.out.println(String.format("SIPAccount Nr %d", counter));
					
					counter += counter;
				}
			}
			else {
				// nur zu Testzwecken
				System.out.println("OK - Es wurde keine Rufnummer gefunden");
			}
			
		} catch (java.sql.SQLException e) {
			System.err.println(e.getMessage());
			
			this._errorlevel = 1;
		}
		
		return result;
	}
	
	
	private boolean _checkVertraege(int overnr, int nvernr) {
		
		String    sql;
		ResultSet rs;
		
		boolean   result  = false;

		// Wenn beide Verträge zum selben Kunde gehören, ist das
		// Ergebnis der Abfrage die gemeinsame kundeid in 1 Datensatz
		sql  = "SELECT kundeid";
		sql += " FROM vertrag";
		sql += " WHERE vertragnummer IN (" + overnr + "," + nvernr + ")";
		sql += " GROUP BY kundeid";
		
		try {
		
			System.out.println(sql);
			
			rs = this._zfa.getRows(sql);

			if (rs.isBeforeFirst()) {

				// Es MUSS genau 1 Datensatz gefunden werden
				result = (this._zfa.getRowCount(rs) == 1);
			}	
			
			if (result) {
					
				System.out.println(String.format("Vertragsvergleich erfolgreich"));
			}
			else {
				System.out.println(String.format("Vertragsvergleich NICHT erfolgreich"));
			}
			
		} catch (java.sql.SQLException e) {
			System.err.println(e.getMessage());
			
			this._errorlevel = 1;
		}
		
		return result;
	}

	
	private boolean _copyAakz(int nrfid, int orfid, int auftragid, Date termin) {
		
		String    sql1, sql2;
		String    terminStr; 
		int       anzahl1 = 0;
		int       anzahl2 = 0;
		
		boolean   result  = false;
		
		

		terminStr = String.format("%tY-%tm-%td",  termin, termin, termin);
		
		sql1  = "INSERT INTO aakz(anschluss,von,bis,portierung,rfonvertragid,zentrale,gueltig_von,vorwahl,sipaccountnr";
		// nachfolgende Zeile (Parameter) wurde durch tk selbst hinzugefügt
		sql1 += ",hauptmsn,colr,caw,dnd,cfb,cfu,cfrn,zielanschluss,timeout)";
		sql1 += " SELECT anschluss, von, bis, portierung," + nrfid + ",zentrale";
		sql1 += ",'" + terminStr + "'::date,vorwahl,sipaccountnr";
		sql1 += ",hauptmsn,colr,caw,dnd,cfb,cfu,cfrn,zielanschluss,timeout";
		sql1 += " FROM aakz";
		sql1 += " WHERE rfonvertragid=" + orfid;
		sql1 += " AND gueltig_bis ISNULL";
		
		// 1 Tag davor alten Eintrag auf Ende setzen
		sql2  = "UPDATE aakz";
		sql2 += " SET gueltig_bis='" + terminStr + "'::date - interval '1 day'";
		sql2 += " WHERE rfonvertragid=" + orfid + " AND gueltig_bis ISNULL";


		try {
		
			System.out.println(sql1);
			
			anzahl1 = this._zfa.insert(sql1);

			if (anzahl1 > 0) {
				
				System.out.println(sql2);
				
				anzahl2 = this._zfa.update(sql2);
			}	
			
			// Wenn beide SQL-Befehle die selbe Anzahl Datensätze haben
			// Gibt es überhaupt kein Datensätze beim 1. Befehl, dann braucht der 2 auch nicht ausgeführt werden
			// und beide haben den Wert 0 was somit auch OK ist
			if (anzahl1 == anzahl2) {
					
				result = true;

				System.out.println(String.format("Update erfolgreich %d", anzahl1));
			}
			else {
				System.out.println(String.format("Update NICHT erfolgreich %d <> %d", anzahl1, anzahl2));
			}
			
		} catch (java.sql.SQLException e) {
			System.err.println(e.getMessage());
			
			this._errorlevel = 1;
		}
		
		return result;
	}

	

	private boolean _copyCudaDetails(Vector<Integer> sipAccNumbers, int auftragid, int orfid, int nrfid, int ocudaid, int ncudaid, Date termin) {
		
		String    sql, sql2, sql3, sql4;
		String    terminStr;
		int       counter = 0;
		int       count2 = 0;
		int       count3 = 0;
		int       count4 = 0;
		ResultSet rs;
		int       sipAccNumber = 0;
		boolean   result = false;


		counter = 0;
		
		terminStr = String.format("%tY-%tm-%td",  termin, termin, termin);

		try {
			
			for (Iterator<Integer> it = sipAccNumbers.iterator(); it.hasNext();) {
			
				sipAccNumber = (int)it.next();
			
				sql  = "SELECT gid,sip_user,sip_pw,sip_realm,mgmtipadresse";
				sql += " FROM cuda_sip_details cd";
				sql += " WHERE cd.cudaid=" + ocudaid + " AND sipaccountnr=" + sipAccNumber;
				sql += " GROUP BY gid,sip_user,sip_pw,sip_realm,mgmtipadresse";
			
				System.out.println(sql);
			
				rs = this._zfa.getRows(sql);
			
				System.out.println("SQL Anzahl " + this._zfa.getRowCount(rs));
				
				if (rs.isBeforeFirst()) {
					
					rs.next();
					
					// alte SIP-Daten ermitteln
					int    agid                = rs.getInt("gid");
					String asip_user           = rs.getString("sip_user");
					String asip_pw             = rs.getString("sip_pw");
					String asip_realm          = rs.getString("sip_realm");
					String asip_amgmtipadresse = rs.getString("mgmtipadresse");
					
					sql2  = "INSERT INTO cuda_sip_details(cudaid,aakzid,gid,sip_user,sip_pw,sip_realm,mgmtipadresse,sipaccountnr)";
					sql2 += " SELECT " + ncudaid + ",aakzid," + agid + ",'" + asip_user + "'";
					sql2 += ",'" + asip_pw + "','" + asip_realm + "','" + asip_amgmtipadresse + "',sipaccountnr";
					sql2 += " FROM aakz";
					sql2 += " WHERE rfonvertragid=" + nrfid + " AND sipaccountnr=" + sipAccNumber;
					
					System.out.println(sql2);
					
					count2 = this._zfa.insert(sql2);
				}
				
				counter++;
			}
			
			sql3  = "UPDATE cuda_sip_details";
			sql3 += " SET gueltig_bis='" + terminStr + "'::date - interval '1 day'";
			sql3 += " WHERE cudaid=" + ocudaid + " AND gueltig_bis ISNULL"; 
				
			System.out.println(sql3);
				
			count3 = this._zfa.update(sql3);
			
			
			// Überträgt noch G-Fit Profil und paar Einstellungen in den rfonvertrag
			sql  = "SELECT *";
			sql += " FROM rfonvertrag";
			sql += " WHERE rfonvertragid = " + orfid;
		
			System.out.println(sql);
		
			rs = this._zfa.getRows(sql);
		
			System.out.println("SQL Anzahl " + this._zfa.getRowCount(rs));
			
			if (rs.isBeforeFirst()) {
				
				rs.next();
				
				// alte Daten ermitteln
				int aclir           = rs.getInt("clir");
				int aevntyp         = rs.getInt("evntyp");
				int ateilsperre     = rs.getInt("teilsperre");
				String abemerkung   = rs.getString("bemerkung");
				int agfit_profileid = rs.getInt("gfit_profileid");
				int amacadresseid   = rs.getInt("macadresseid");
				int aclipns         = rs.getInt("clipns");
				int aoverlap        = rs.getInt("overlap");
			
				sql  = "UPDATE rfonvertrag";
				sql += " SET clir = " + aclir;
				sql += ",evntyp = " + aevntyp;
				sql += ",teilsperre = " + ateilsperre;
				sql += ",bemerkung = '" + abemerkung + "'";
				sql += ",gfit_profileid = " + agfit_profileid;
				sql += ",macadresseid = " + amacadresseid;
				sql += ",clipns = " + aclipns;
				sql += ",overlap = " + aoverlap;
				sql += " WHERE rfonvertragid = " + nrfid; 
			
				System.out.println(sql);
			
				count4 = this._zfa.update(sql);
			}

		} catch (java.sql.SQLException e) {
			System.err.println(e.getMessage());
		
			this._errorlevel = 1;
			
			counter = -1;
		}
		
		
		if (counter > 0) {
			
			System.out.println(String.format("SIP-Übertrag erfolgreich %d", sipAccNumbers.size()));
		}

		
		if (counter > 0) {
			result = true;
		}
		
		return result;
	}


	private boolean _copyTelefonbuch(Vector<Integer> sipAccNumbers, int orfid, int nrfid) {
		
		String    sql;
		int       counter;
		int       sipAccNumber;		
		boolean   result = false;

		counter = 0;
		
		for (Iterator<Integer> it = sipAccNumbers.iterator(); it.hasNext();) {
			
			sipAccNumber = (int)it.next();
			
			try {
				
				sql  = "INSERT INTO tbeintrag";
				sql += "(rfonvertragid";
				sql += ",nutzung, stichwort, name2, print, online, auskunft, auskunftrufnummer";
				sql += ",telefon, telefax, zentrale, name, vorname, strasse, plzort, vorwahl, durchwahl";
				sql += ",eintragart, beruf, rufnummerzg, hausnummer, hausnummerzusatz, invers, inverswider, plz";
				sql += ",kennzeichen, sipaccountnr, titel)";
				sql += " SELECT";
				sql += " " + nrfid;
				sql += ",nutzung, stichwort, name2, print, online, auskunft, auskunftrufnummer";
				sql += ",telefon, telefax, zentrale, name, vorname, strasse, plzort, vorwahl, durchwahl";
				sql += ",eintragart, beruf, rufnummerzg, hausnummer, hausnummerzusatz, invers, inverswider, plz";
				sql += ",kennzeichen, sipaccountnr, titel";
				sql += " FROM tbeintrag";
				sql += " WHERE rfonvertragid=" + orfid + " AND sipaccountnr=" + sipAccNumber;
					
				System.out.println(sql);
					
				this._zfa.insert(sql);
					
			} catch (java.sql.SQLException e) {
				
				System.err.println(e.getMessage());
			
				this._errorlevel = 1;
				
				break;
			}
			
			counter++;
		}
		
		if (counter == sipAccNumbers.size()) {
			System.out.println("OK - Telefonbucheinträge erfolgreich übernommen");
			result = true;
		}
		else {
			System.out.println("Fehler - Telefonbucheinträge wurde nicht korrekt übernommen");
		}
		
		return result;
	}


	public int getErrorlevel() {
		
		return this._errorlevel;
	}
	
	
	public void run(int overnr, int nvernr) {

		boolean status = true;
		
		int ovid      = 0;
		int nvid      = 0;
		int orfid     = 0;
		int nrfid     = 0;
		int ocudaid   = 0;
		int ncudaid   = 0;
		int auftragid = 0;
		
		Date termin = null;
		
		Vector<Integer> sipAccNumbers = null;

		this._readINIDatei();
		
		this._connectDB();

		
		// Alte Vertragsdaten suchen
		ovid = this._getVertragID(overnr);
		
		status = ovid > 0;

		if (status) {
			orfid = this._getRfonVertragID(ovid);
			status = orfid > 0;
		}
		
		if (status) {
			ocudaid = this._getCudaID(orfid);
			status = ocudaid > 0;
		}
		
		if (status) {
			sipAccNumbers = this._getSipAccNumbers(orfid);
			status = sipAccNumbers.size() > 0;
		}
		
		// Neuen Vertragsdaten suchen
		if (status) {
			nvid = this._getVertragID(nvernr);
			status = nvid > 0;
		}
		
		if (status) {
			nrfid = this._getRfonVertragID(nvid);
			status = nrfid > 0;
		}
		
		if (status) {
			ncudaid = this._getCudaID(nrfid);
			status = ncudaid > 0;
		}
		
		if (status) {
			auftragid = this._getAuftragID(nvid);
			status = auftragid > 0;
		}
		
		// Bestätigter Inbetriebnahmetermin MUSS vorhanden sein
		if (status) {
			termin   = this._getTermin(nvernr);
			status = termin != null;
		}
		
		if (status) {
			status = this._checkVertraege(overnr, nvernr);
		}

		// Wenn alle ID's/Werte vorhanden sind und die Prüfungen erfolgreich waren
		if (status) {
			this._copyAakz(nrfid, orfid, auftragid, termin);
			this._copyCudaDetails(sipAccNumbers, auftragid, orfid, nrfid, ocudaid, ncudaid, termin);
			this._copyTelefonbuch(sipAccNumbers, orfid, nrfid);
		}
		
		// Datenbank schließen
		this._closeDB();
		
	}
	
	
	
	
	public static void main (String[] args) {
		
		
		if (args.length < 2 || args.length > 2) {
			System.out.println("Fehler: Es wurden keine 2 Verträge angegeben");
			System.out.println("Aufruf: zfa_sip_move [von Vertragsnummer] [zu Vertragsnummer])");
			System.exit(1);
		}
			
		int overnr = Integer.parseInt(args[0]);
			
		if (overnr < 1) {
			System.out.println("Fehler: Die [von Vertragsnummer] muß größer als 0 sein");
			System.out.println("Aufruf: zfa_sip_move [von Vertragsnummer] [zu Vertragsnummer])");
			System.exit(1);
		}
		
		int nvernr = Integer.parseInt(args[1]);
		
		if (nvernr < 1) {
			System.out.println("Fehler: Die [zu Vertragsnummer] muß größer als 0 sein");
			System.out.println("Aufruf: zfa_sip_move [von Vertragsnummer] [zu Vertragsnummer])");
			System.exit(1);
		}
		
		// Programm starten
		zfa_sip_move action = new zfa_sip_move();
		
		action.run(overnr, nvernr);
		
		System.exit(action.getErrorlevel());
	}
}