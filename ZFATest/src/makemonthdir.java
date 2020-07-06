import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import tk.FileDir;
import tk.INI;
import zfa.ZFASQL;

public class makemonthdir {
	
	private String _iniDatei;
	private INI _iniHandler;
	private INI _iniAbrHandler;
	
	private String _readKundenDir;
	
	static String ABRDIR = "autoAbrechnung";


	public makemonthdir() {
		
		this._iniDatei = "src\\makemonthdir.ini";
	
	}


	/**
	 * Liest die INI-Datei ein und holt sich den Pfad für die CSV-Datei(en)
	 */
	private void _readINIDatei() {
		
		try { 
			this._iniHandler = new INI(this._iniDatei);
		
			this._readKundenDir = this._iniHandler.get("KUNDEN", "dir"); // P:\Vertrieb RZ und P2P\Kunden RZ und P2P
				
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	
	private void _run() {
		
		this._readINIDatei();
		
		this._createDirs();
	}
	
	
	private void _createDirs() {
		
		INI     iniAbrHandler;
		String  cDatei;
		String  KundeName;
		Date    now = new Date();
		
		// Monat und Jahr String erstellen
		SimpleDateFormat sdf_jahr  = new SimpleDateFormat("yyyy");
		SimpleDateFormat sdf_monat = new SimpleDateFormat("MM");
		
		String strJahr = sdf_jahr.format(now);
		String strMonat =sdf_monat.format(now);

		// Kundenverzeichnis
		FileDir filedir = new FileDir();
		
		File f = new File(this._readKundenDir);

		ArrayList<File> fileArray = filedir.listDir(f, ".");
		
		if (!fileArray.isEmpty()) {
			
			for (File ordner : fileArray) {
				
				System.out.println("abrechnung.ini suchen in  = " + ordner);
			
				// Prüfen ob abrechnung.ini existiert
				// Steht in der abrechnung.ini 
				// [KUNDE]
				// name="Firma XY"
				// dann nimmt der Kunde an der autom. Abrechnung teil
				// das ist noch nicht ganz richtig, da es nicht vom Name abghängig ist, sondern
				// von den Leistungen, welche der Kunde in Anspruch nicht.
				// Im Augenblick nehmen wir dies jedoch als Anhaltspunkt
				
				cDatei = ordner.getAbsolutePath() + File.separator + "abrechnung.ini";
				
				KundeName = null; 
				
				try {
					
					System.out.println("Kunden Eintrag prüfen");
					
					iniAbrHandler = new INI(cDatei);
				
					KundeName = iniAbrHandler.get("KUNDE", "name");
					
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
				
				if (KundeName != null) {
	
					System.out.println("Kunden : "+KundeName);

					
					// z.B. P:\Vertrieb RZ und P2P\Kunden RZ und P2P\autoAbrechnung\[jahr]\[monat]
					
					String neuOrdner = ordner.getAbsolutePath() + File.separator + ABRDIR + File.separator + strJahr + File.separator + strMonat; 
				
					System.out.println("Abrechnungsverzeichnkis prüfen : " + neuOrdner);
					
					File neuDir = new File(neuOrdner); 
				
					System.out.println("Verzeichnis prüfen = " + neuDir);
					filedir.checkDir(neuDir, true);
				}
			}
		}
		else {
			System.out.print("Es wurden keine Verzeichnisse gefunden");
		}

		
	}
	

	public static void main(String[] args) {
		
		makemonthdir makemonthdir = new makemonthdir();
		
		makemonthdir._run();
	}
}
