/**
 * NavSep2ZFA
 * 
 * Liest aus einer CSV-Datei die SEPA-Mandate ein
 * und schreibt diese in die das ZFA
 */

import tk.INI;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import tk.CSV;
import tk.FileDir;
import zfa.Adresse;
import zfa.ZFATestSQL;

public class navsepa2zfa {
	
	private ZFATestSQL _zfa;
	
	private String _dateiPfad;
	
	private String _readCSVPfad;
	private String _moveCSVPfad;
	
	private File[] _csvDateien;
	
	private String _csvDatei;
	private String _iniDatei;
	private INI _iniHandler;
	private CSV _csvHandler;
	
	public navsepa2zfa() {
		
		this._iniDatei = "src\\navsepa2zfa.ini";
	}

	
	private void _connectDB() {
		
		
		this._zfa = new ZFATestSQL(null, null, null, null, null);
		
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
		
			this._readCSVPfad = this._iniHandler.get("NAV-SEPA", "readcsvpfad"); // P:\Kaufmännischer Service\Abrechnung\SEPA
			this._moveCSVPfad = this._iniHandler.get("NAV-SEPA", "movecsvpfad"); // P:\Kaufmännischer Service\Abrechnung\SEPA\Archiv
			
			// System.out.print(this._readCSVPfad);
			// System.out.print(this._moveCSVPfad);
				
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	
	
	/**
	 * Liest alle CSV-Dateinamen im angegebenen Pfad ein
	 */
	private void _getCSVFiles() {
		
		File f = new File(this._readCSVPfad);
		
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File f, String name) {
				// We want to find only .c files
				return name.endsWith(".csv");
			}
		};

		this._csvDateien = f.listFiles(filter);
	}
	
	
	/**
	 * 
	 */
	private void _csvDateien2DB() {
		
		if (this._csvDateien.length > 0) {
			
			 for (File csvDatei : this._csvDateien) {

				SepaCSV csvHandler = new SepaCSV(csvDatei.toString());
			
				csvHandler.readFile();
				
				this._csvContent2DB(csvHandler.getDictContent());
			}
		}
		else {
			System.out.print("Es wurden keine CSV-Dateien gefunden");
		}
	}
	
	
	private void _csvContent2DB(ArrayList<Map<String, String>> content) {
		
		String debitor;
		String mandatsnummer;
		String sql;
		
		for (Map<String, String> zeile: content) {
			
			debitor       = zeile.get("Debitor");
			mandatsnummer = zeile.get("Mandatsnummer");
			
			sql = "'UPDATE sapdebitoren set mandatsreferenz = '" + mandatsnummer + "' WHERE sapdebitornummer = " + debitor + ";";
				
			System.out.println(sql);
			// this._zfa.updateRecord(sql);
		}
	}
	
	
	public void run() {
		
		this._connectDB();
		
		this._readINIDatei();
		
		this._getCSVFiles();
		
		this._csvDateien2DB();
		
		this._closeDB();
		
	}
	public static void main (String[] ags) {
		
		// Programm starten
		navsepa2zfa action = new navsepa2zfa();
		
		action.run();
	}
	
}

class SepaCSV extends CSV {
	
	// Content ist ein Array mit einer HashMap
	private ArrayList<Map<String, String>> _dictContent;
	
	public SepaCSV (String datei) {
		
		super(datei);
	}
	
	public void Row(String[] cols) {
		
		String[] headline = {"Debitor", "Mandatsnummer"}; 
		
		Map<String, String> dictCols = this.makeCols2Dict(cols, headline);
		
		String debitor        = dictCols.get("Debitor");
		String mandatsnummer  = dictCols.get("Mandatsnummer");
		
		// Prüft ob es ein korrekte Mandatsreferenz ist 
		// da in manchen Spalten hier z.B. auch "manuell" steht
		// Die Debotor-Nummer darf nicht 0 sein
		if (Integer.parseInt(debitor) > 0 && mandatsnummer != "" && mandatsnummer.substring(0, 4).contentEquals("TKL-")) {

			// Ist die Zeile korrekt, wird diese in den CSV-Content übernommen
			System.out.println(debitor);
			this._dictContent.add(dictCols);
		}
	}
	
	
	public ArrayList<Map<String, String>> getDictContent() {
		
		return this._dictContent;
	}
}
