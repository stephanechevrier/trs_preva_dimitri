package com.trs.preva.metier;

import com.trs.action.ProprieteCommune;
import com.trs.constant.TrsConstant;
import com.trs.exception.TrsException;
import com.trs.utils.calendar.CalendarHelper;
import com.trs.utils.fileSystem.FileUtil;
import com.trs.utils.format.StringMgt;
import com.trs.utils.java.JavaUtil;
import com.trs.utils.properties.Properties;
import com.trs.utils.string.StringUtil;
//import com.trs.wintrans.metier.HistoSav;
//import com.trs.wintrans.dbAccess.CalendrierTrsAccess;

import com.trs.wintrans.metier.Document;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.trs.utils.email.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.IOUtils;
import org.apache.logging.log4j.status.StatusLogger;


public class SpotToWebDocument 
{
	// 10/01/2023 - Lot 5 PREVA : transfert des documents
	
	private String	_sTargetDir = "";
	private String	_sTempDir = "";

	
	static final Logger myLog = LogManager.getLogger(SpotToWebDocument.class.getName());
	
	private List<Document>		_myDocList = null;
	private ProprieteCommune	_PC = null;
	private MailNotification	_myMailNotification = null;
	
	private String _dirConf ="";
	
	String asPropFileName;
	String asDbName;
	
	//private List<Document> 	myCPListBlNotFound = null; 
	
	
	/**
	 * <p>Initialisation puis génération du fichier</p>
	 * 
	 * @throws TrsException
	 */
	public SpotToWebDocument(List<Document> aDocList, ProprieteCommune aPC) throws TrsException
	{
		String	sPropKey = "";
		
		this._myDocList = aDocList;
		this._PC = aPC;
		 
		try { this._dirConf = this._PC.getDirConf(); }
        catch ( TrsException e )
        {
        	throw new TrsException(JavaUtil.getMethodeFullName() + " - PROBLEME lors de l'initalisation du répertoire de configuration : " + e.toString());
        }
		
		
		// Répertoire de stockage des fichiers à transférer
		// ********************************************************************************************************************
		this._sTargetDir = aPC.getProperty("preva.spotToWeb.document.directory.target", "", true);
		
		if ( this._sTargetDir.equals("") )
        	throw new TrsException(JavaUtil.getMethodeFullName() + " - " + aPC.getMessageErreur(1, "preva.spotToWeb.document.directory.target"));
		
		
		// Répertoire de stockage temporaire pour le fichier stocké dans la base de données
		this._sTempDir = aPC.getProperty("preva.spotToWeb.document.directory.temp", "", true);
		
		if ( this._sTempDir.equals("") )
        	throw new TrsException(JavaUtil.getMethodeFullName() + " - " + aPC.getMessageErreur(1, "preva.spotToWeb.document.directory.temp"));
		
		
		// Documents à prendre en compte
		// ****************************************
		if ( aDocList.size() > 0 )
		{
			myLog.info("");
			myLog.info("Il y a des Documents a transmettre au site web PREVA");
			
			this.copyFile(this._sTargetDir, this._sTempDir, aDocList);
		}
	}

	
	
	public void copyFile(String asTargetDir, String asTempDir, List<Document> aDocList) throws TrsException
	{
		Date 				myDate = new Date(System.currentTimeMillis());

		SimpleDateFormat	mySDF = null;
		
		File 		myFile = null;
		FileUtil	myFU = new FileUtil();
		
		InputStream 	myIS = null;
		OutputStream 	myOS = null;
		PrintWriter		myPWFile = null;
		
		String	sDateCycle = "";
		String	sFileName = "";
		String	sFileNameTarget = "";
		String	sHorodatage = "";
		String	sLog = "";
		String	sNoLigneCommande = "";
		
		//boolean bFileExist = false;

		int		iIndiceDoc = 0;
		int		iPos = 0;
		int		iRetour = 0;
		
		myLog.info("");
		myLog.info("PREPARATION copie Document(s) dans [" + asTargetDir + "]");
		
		
		// Création du répertoire cible
		// ----------------------------------------------------------------------
		myFU.makeDir(asTargetDir);
		
		
		// Création du répertoire de stockage temporaire
		// ----------------------------------------------------------------------
		myFU.makeDir(asTempDir);
		
		
		// Date dy cycle de traitement
		// ----------------------------------------------------------------------
		mySDF = new SimpleDateFormat("yyyyMMdd-HHmmSS");
		sDateCycle = mySDF.format(myDate);
		
		
		myLog.info("");
		myLog.info("BOUCLE sur les " + aDocList.size() + " Document" + ( aDocList.size() > 1 ? "s" : "" ));
		
        // Boucle sur les Documents
        for ( Document myDoc : aDocList )
        {
        	iIndiceDoc ++;
        	
    		myLog.info("");
    		myLog.info("Traitement Document " + iIndiceDoc + "/" + aDocList.size());
    		
        	sFileName = myDoc.get_nomFichier();
        	sFileNameTarget = "";
        	sNoLigneCommande = myDoc.get_noLigneCommande() + "";
        	
        	sHorodatage = this.get_horodatage(sFileName);
    		sFileNameTarget = sFileName.replaceFirst(sHorodatage, "@@@");
        	
        	// Cas des documents stockés dans la base de données
        	if ( myDoc.get_fichier() != null )
        	{
	        	// Définition du nom cible du fichier
	        	// ----------------------------------------------------------
	        	if ( sFileName.indexOf("NC_") > - 1 )
	        	{
	        		// Il faut pour PREVA "NC-{no_ligne_commande}-yyyymmdd_HHMM.pdf"
	        		// Le nom d'origine possède ce format, en dehors du séparateur.
	        		// Nom d'origine = NC_{numéro commande}_{date génération NC au format yyyymmdd_HHMM}.pdf
	        		
	        		sFileNameTarget = sFileNameTarget.replaceAll("_",  "-");
	        		sFileNameTarget = sFileNameTarget.replaceFirst("@@@", sHorodatage);
	        		sLog = "Traitement de la NC [" + sFileName + "]";
	        	}
	        	
	        	// Enlèvement et Livraison
	        	if ( sFileName.indexOf("RECEP_") > - 1 || sFileName.indexOf("RECEPLIV_") > - 1 )
	        	{
	        		// Il faut pour PREVA "RECEP-{no_ligne_commande}-..."
	        		// Le nom d'origine possède ce format, en dehors du séparateur.
	        		// Nom d'origine = {RECEP|RECEPLIV}_{numéro commande}_{numero OT}_{date génération récépissé émargé au format yyyymmdd_HHMM}.pdf
	        		
	        		sFileNameTarget = sFileNameTarget.replaceAll("_",  "-");
	        		sFileNameTarget = sFileNameTarget.replaceFirst("@@@", sHorodatage);
	        		sLog = "Traitement du Recepisse Emarge [" + sFileName + "]";
	        	}
	        	
	        	if ( sFileName.indexOf("_Photo") > - 1 )
	        	{
	        		// Il faut pour PREVA "PHOTO-{no_ligne_commande}-..."
	        		// Le nom d'origine NE possède PAS ce format
	        		// Nom d'origine = {numéro commande}_{date génération photo au format yyyymmdd_HHMM}_{Photo|Photo1|Photo2}.pdf
	        		
	        		sFileNameTarget = sFileNameTarget.replaceAll("_",  "-");
	        		sFileNameTarget = "PHOTO-" + myDoc.get_noLigneCommande() + "-" + sFileNameTarget.replaceFirst("@@@", sHorodatage);
	        		sFileNameTarget = sFileNameTarget.replaceFirst("bmp", "jpg");
	        		sLog = "Traitement de la Photo [" + sFileName + "]";
	        	}
	        	
        		myDoc.set_nomFichierPreva(sFileNameTarget);
	        	myLog.info("  - " + sLog);
	        	myLog.info("  - qui devient [" + sFileNameTarget + "] pour PREVA");
	        	
	        	// NON reconnaissance du type de fichier
	        	if ( sFileNameTarget.equals("") )
	        		throw new TrsException(JavaUtil.getMethodeFullName() + " - Type de fichier NON reconnu avec le Document [" + sFileName + "]");
	
				try 
				{
					myFile = new File(asTempDir + "/" + sFileName);
					myIS = myDoc.get_fichier();
					myOS = new FileOutputStream(myFile);
					
					org.apache.commons.io.IOUtils.copy(myIS, myOS);
				}
				catch( Exception e) 
				{
					throw new TrsException(JavaUtil.getMethodeFullName() + " - ERREUR lors de la copie du Document [" + asTempDir + "/" + sFileName + "] dans ["
						+ asTargetDir + "/" + sFileNameTarget + "] : " + e.toString());
				}
				finally
				{
					try 
					{
						if ( myOS != null )
							myOS.close();
					} 
					catch ( Exception e2 )
					{
						throw new TrsException(JavaUtil.getMethodeFullName() + " - ERREUR lors de la copie du Document [" + asTempDir + "/" + sFileName + "] dans ["
							+ asTargetDir + "/" + sFileNameTarget + "] : ERREUR lors de la fermeture de [" + myFile.getPath() + "] : " + e2.toString());
					}
				}

				// Copie avec suppression
				iRetour = myFU.copyBytes(asTempDir + "/" + sFileName, asTargetDir + "/" + sFileNameTarget, true);
        	}
        	// Cas des documents stockés sur le réseau
			else 
			{
	        	sHorodatage = this.get_horodatage(sFileName);
	    		sFileNameTarget = sFileName.replaceFirst(sHorodatage, "@@@");
        		sFileNameTarget = StringUtil.getLastString(sFileNameTarget, "\\\\");
	        	
	        	// Enlèvement
	        	if ( sFileName.indexOf("RECEP_") > - 1 )
	        	{
	        		// Il faut pour PREVA "RECEP-{no_ligne_commande}-..."
	        		// Le nom d'origine possède ce format
	        		//sFileNameTarget = sFileName.replaceAll(".pdf",  "") + "-" + sDateCycle + ".pdf";
	        		//sFileNameTarget = StringUtil.getLastString(sFileName, "\\\\").replaceAll(".pdf",  "") + "-" + sDateCycle + ".pdf";
	        		sLog = "Traitement du Recepisse Emarge d'Enlevement [" + sFileName + "]";
	        	}
	        	
	        	// Livraison
	        	if ( sFileName.indexOf("RECEPLIV_") > - 1 )
	        	{
	        		// Il faut pour PREVA "RECEP-{no_ligne_commande}-..."
	        		// Le nom d'origine possède ce format
	        		//sFileNameTarget = sFileName.replaceAll(".pdf", "") + "-" + sDateCycle + ".pdf";
	        		//sFileNameTarget = StringUtil.getLastString(sFileName, "\\\\").replaceAll(".pdf",  "") + "-" + sDateCycle + ".pdf";
	        		sLog = "Traitement du Recepisse Emarge de Livraison [" + sFileName + "]";
	        	}
	        	
        		sFileNameTarget = sFileNameTarget.replaceAll("_",  "-");
        		sFileNameTarget = sFileNameTarget.replaceFirst("@@@", sHorodatage);
	        	
	        	myLog.info("  - " + sLog);
	        	myLog.info("  - qui devient [" + sFileNameTarget + "] pour PREVA");
	        	
	        	// NON reconnaissance du type de fichier
	        	if ( sFileNameTarget.equals("") )
	        		throw new TrsException(JavaUtil.getMethodeFullName() + " - Type de fichier NON reconnu avec le Recepisse Emarge [" + sFileName + "]");
	        	
        		myDoc.set_nomFichierPreva(sFileNameTarget);
	
				try 
				{
					// Copie (PAS de suppression !)
					iRetour = myFU.copyBytes(sFileName, asTargetDir + "/" + sFileNameTarget, false);
					
					// Erreur !
					// Le fichier n'est pas copié
					if ( iRetour < 0 )
					{
						myDoc.set_notExistingOnDisk(true);
						myDoc.set_sValeur1(myFU.getLastError());
					}
				}
				catch( Exception e) 
				{
					throw new TrsException(JavaUtil.getMethodeFullName() + " - ERREUR lors de la copie du Document [" + sFileName + "] dans ["
						+ asTargetDir + "/" + sFileNameTarget + "]");
				}
			}
        }
        
		myLog.info("FIN BOUCLE");
	}
	
	
	public String get_targetDir()
	{
		return this._sTargetDir;
	}
	
	private String get_horodatage(String asValeur)
	{
		String 	sRetour = "";
		// yyyymmdd_HHMM
		String 	sPattern = "[2-4]\\d\\d\\d[0-1]\\d[0-3]\\d_[0-2]\\d[0-5]\\d";
		
		// Si paramètre vide, on retourne VIDE
		if ( asValeur == null )
			asValeur = "";
		
		asValeur = asValeur.trim();
		
		if ( asValeur.equals("") ) 
			return "";
		
		Pattern myPattern = Pattern.compile(sPattern, Pattern.CASE_INSENSITIVE);
		Matcher myMatcher = myPattern.matcher(asValeur);
		
		// On ne traite que le premier trouvé
		if ( myMatcher.find() )
		{
			sRetour = asValeur.substring(myMatcher.start(), myMatcher.end());
		}
		
		return sRetour;
	}
}