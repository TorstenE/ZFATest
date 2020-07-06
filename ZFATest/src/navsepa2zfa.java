/**
 * NavSep2ZFA
 * 
 * Liest aus einer CSV-Datei die SEPA-Mandate ein
 * und schreibt diese in die das ZFA
 * Danach wird die Datei in das Archiv-Verzeichnis verschoben
 */

import tk.INI;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import tk.CSV;
import tk.FileDir;
import zfa.ZFASQL;

public class navsepa2zfa {
	
	private ZFASQL _zfa;
	
	private String _readCSVPfad;
	private String _moveCSVPfad;
	
	private File[] _csvDateien;
	
	private String _iniDatei;
	private INI _iniHandler;
	
	
	public navsepa2zfa() {
		
		this._iniDatei = "src\\navsepa2zfa.ini";
	
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
		
			this._readCSVPfad = this._iniHandler.get("NAV-SEPA", "readcsvpfad"); // P:\Kaufmännischer Service\Abrechnung\SEPA
			this._moveCSVPfad = this._iniHandler.get("NAV-SEPA", "movecsvpfad"); // P:\Kaufmännischer Service\Abrechnung\SEPA\Archiv
			
			this._zfa.setHost(this._iniHandler.get("ZFA", "dbhost"));

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
	private void _readCSVFiles() {
		
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
			
			// 808809 ist Debitor "Egeler" für Testzwecke
			// debitor = "808809";
			
			sql = "UPDATE sapdebitoren set mandatsreferenz = '" + mandatsnummer + "' WHERE sapdebitornummer = " + debitor + ";";
				
			System.out.println(sql);
			
			try {
				this._zfa.updateRecord(sql);
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
	}
	
	
	private void _moveCSVFiles() {
		
		FileDir fileDir = new FileDir();
		
		String csvName;
		String csvExt;
		String csvNeuDatei;
		
		Path originFile;
		Path targetFile;

		// Datum-String generieren
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Date now = new Date();
		String strDate = sdf.format(now);
		
		if (this._csvDateien.length > 0) {
			
			try {
				for (File csvDatei : this._csvDateien) {

					originFile  = csvDatei.toPath();
					
					// Dateiname ohne Pfad und Extension
					csvName     = fileDir.stripExtension(csvDatei).getName();
					// Extension ermitteln
					csvExt      = fileDir.getExtension(csvDatei);
					// Neuen Dateinamen zusammen setzen
					// moveCSVPfad + Name + Datum + Extension
					csvNeuDatei = this._moveCSVPfad + File.separator + csvName + "_" + strDate + "." + csvExt; 
					
					System.out.print("CSV verschieben nach : "+csvNeuDatei);
					
					// Pfad aus neuem Dateiname generieren
					targetFile  = Paths.get(csvNeuDatei);
					
					if (Files.move(originFile, targetFile, REPLACE_EXISTING) != null) {
						System.out.println("Datei verschoben");
					}
					else {
						System.out.println("Datei " + csvDatei.getName() + " konnte nicht verschoben werden");
					}
				}
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
	}
	
	
	/**
	 * Prüft ob das Archiv-Verzeichnis vorhanden ist
	 * Wenn nicht, wird es angelegt.
	 * 
	 * @return
	 */
	private boolean _checkMovePfad() {
		
		File dir  = new File(this._moveCSVPfad); 
		
		FileDir filerDir = new FileDir();
		
		boolean result = filerDir.checkDir(dir, true);

		return result;
	}
	
	
	public void run() {

		this._readINIDatei();
		
		this._connectDB();
		
		this._checkMovePfad();
		
		this._getCSVFiles();
		
		this._readCSVFiles();
		
		this._moveCSVFiles();
		
		this._closeDB();
		
	}
	public static void main (String[] ags) {
		
		// Programm starten
		navsepa2zfa action = new navsepa2zfa();
		
		action.run();
	}
	

	class SepaCSV extends CSV {
		
		// Content ist ein Array mit einer HashMap
		private ArrayList<Map<String, String>> _dictContent;
		
		public SepaCSV (String datei) {
			
			super(datei);
			
			this._dictContent = new ArrayList<Map<String, String>>();
			
		}
		
		public void Row(String[] cols) {
			
			Map<String, String> dictCols = this.makeCols2Dict(cols, null);
			
			String debitor        = dictCols.get("Debitor");
			String mandatsnummer  = dictCols.get("Mandatsnummer");
			
			// Die Debitor-Nummer darf nicht leer sein
			if (debitor != null && !debitor.isEmpty() ) {
				
				// Prüft ob es ein korrekte Mandatsreferenz ist 
				// da in manchen Spalten hier z.B. auch "manuell" steht
				// Der Debotor-Nummer darf nicht 0 sein
				if (Integer.parseInt(debitor) > 0 && mandatsnummer != "" && mandatsnummer.length() > 5 && mandatsnummer.substring(0, 4).contentEquals("TKL-")) {
	
					// Ist die Zeile korrekt, wird diese in den CSV-Content übernommen
					// System.out.println(debitor);
					this._dictContent.add(dictCols);
				}
			}
		}
		
		
		public ArrayList<Map<String, String>> getDictContent() {
			
			return this._dictContent;
		}
	}
}