/*
 *  Nojeca
 *  Code pour TRS
 *  jncattin		15/04/2014		création
 */

package com.trs.preva;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.trs.action.ProprieteCommune;
import com.trs.exception.TrsException;
import com.trs.preva.action.DoFileAttenteInLot2;
import com.trs.preva.action.DoFileAttenteOutBL;
import com.trs.preva.action.DoFileAttenteOutConf;
import com.trs.preva.action.DoFileAttenteOutDocument;
import com.trs.preva.action.DoFileAttenteOutLot2;
import com.trs.preva.action.DoFileAttenteOutLot4;
import com.trs.preva.action.DoFileAttenteOutPch;
import com.trs.preva.action.DoTransfertFtpPreva;
import com.trs.utils.java.JavaUtil;


/**
 * <p>Traitement principal PREVA pour tester le transfert des documents et des fichiers d'événements : tout ce qui se trouve dans ... </p>
 *  
 * @author Jean-Noël CATTIN
 */
public class TrsPrevaLot5_jnc 
{
	// Log
	private static final Logger myLog = LogManager.getLogger("trsPreva");
	
	public static void main(String[] args) 
	{
		int iNbFichiers = 0; 
		
		String	sDirArchive = "";
		String	sDirFileAttente = "";
		String	sParam = "";
		String	sPropFileName = "conf/trsPreva.properties";

		
		myLog.info("");
		myLog.info("*** DEBUT du traitement *******************************************************************");
		
		ProprieteCommune myProps = null;
		
    	try { myProps = new ProprieteCommune(sPropFileName); }
    	catch ( TrsException e )
    	{
    		myLog.fatal(e.toString());
    		return;
    	}


    	// Pour envoi de mail log4J via Office365
    	// Il faut déclarer { smtpProtocol="smtp" } dans l'Appender SMTP Log4J
		System.setProperty("mail.smtp.starttls.enable", "true");
    	
    	myLog.info("Environnement = " + myProps._sEnvironment);
    	
    	if ( args.length == 0 )
    		sParam = "123456789";
    	else
    		sParam = args[0];
	
    	
		myLog.info("");
		myLog.info("*** 9. RECUPERATION des Documents a transmettre a PREVA *************************");
		
		DoFileAttenteOutDocument myDFA_doc = null;
		
		iNbFichiers = 0;
		
		if ( sParam.indexOf("9") > -1 )
		{
			try
			{
				myDFA_doc = new DoFileAttenteOutDocument(myProps);
				iNbFichiers = myDFA_doc.generateFile();
				
				myLog.info("       - " + iNbFichiers + "  Documents generes pour PREVA");
				myLog.info("         - " + myDFA_doc.getNbNc() + " NC");
				myLog.info("         - " + myDFA_doc.getNbPhotos() + " Photos");
				myLog.info("         - " + myDFA_doc.getNbRecep() + " Recepisses emarges");
				myLog.info("         - " + myDFA_doc.getNbDocErreur() + " Documents en ERREUR (recepisses emarges)");
			}
			catch ( TrsException e )
			{
				myLog.fatal("PROBLEME lors de la prise en compte des Documents pour PREVA : " + e.toString());
			}
		}
		else
		{
			myLog.info("  - NON demande");
		}
		
	
    	
		myLog.info("");
		myLog.info("*** A. TRANSFERT des Fichiers et Documents en attente de transfert FTP a PREVA ***************************");
		
		DoTransfertFtpPreva myDTFP = null;
		iNbFichiers = 0;
		
		if ( sParam.indexOf("4") > -1 )
		{
			if ( myDTFP == null )
				myDTFP = new DoTransfertFtpPreva(myProps);

			
			// Les fichiers d'événements
			// ---------------------------------------------------------------------------------------------------
			myLog.info("");
			myLog.info("Transfert FTP des fichiers d'evenements (PCH, PREVA, CONF, ...)");
			
			sDirFileAttente = myProps.getProperty(ProprieteCommune.dir_out_target_propKey, "", true);
			 
			if ( sDirFileAttente.equals("") )
			{
				myLog.fatal(myProps.getMessageErreur(1, ProprieteCommune.dir_out_target_propKey, true));
				return;
			}
			else 
				myLog.info("  - Lecture dans " + sDirFileAttente);
			
			sDirArchive = myProps.getProperty(ProprieteCommune.dir_out_archive_propKey, "", true);
			
			if ( sDirArchive.equals("") )
			{
	        	myLog.fatal(myProps.getMessageErreur(1, ProprieteCommune.dir_out_archive_propKey, true));
	        	return;
			}
			else 
				myLog.info("  - Archivage dans " + sDirArchive);
			
			try
			{
				iNbFichiers = myDTFP.run(sDirFileAttente, sDirArchive);
				myLog.info("  - " + iNbFichiers + " fichier(s) transmi(s)");
			}
			catch ( TrsException e )
			{
				myLog.fatal(JavaUtil.getMethodeFullName() + " - PROBLEME lors du transfert FTP des fichiers d'evenements (PCH, PREVA, CONF, ...) : " + e.toString());
			}
			
			
			// Les BL
			// ---------------------------------------------------------------------------------------------------
			myLog.info("");
			myLog.info("Transfert FTP des BL");
			
			sDirFileAttente = myProps.getProperty(ProprieteCommune.preva_spotToWeb_bl_directory_propKey + ".target", "", true);
			 
			if ( sDirFileAttente.equals("") )
			{
				myLog.fatal(myProps.getMessageErreur(1, ProprieteCommune.preva_spotToWeb_bl_directory_propKey + ".target", true));
				return;
			}
			
			try
			{
				iNbFichiers = myDTFP.run(sDirFileAttente, sDirArchive);
				myLog.info("  - " + iNbFichiers + " fichier(s) transmi(s)");
			}
			catch ( TrsException e )
			{
				myLog.fatal(JavaUtil.getMethodeFullName() + " - PROBLEME lors du transfert FTP des BL : " + e.toString());
			}
			
			
			// Les Documents
			// ---------------------------------------------------------------------------------------------------
			myLog.info("");
			myLog.info("Transfert FTP des Documents");
			
			sDirFileAttente = myProps.getProperty(ProprieteCommune.preva_spotToWeb_doc_directory_propKey + ".target", "", true);
			 
			if ( sDirFileAttente.equals("") )
			{
				myLog.fatal(myProps.getMessageErreur(1, ProprieteCommune.preva_spotToWeb_doc_directory_propKey + ".target", true));
				return;
			}
			
			sDirArchive = myProps.getProperty(ProprieteCommune.preva_spotToWeb_doc_directory_propKey + ".archive", "", true);
			 
			if ( sDirArchive.equals("") )
			{
				myLog.fatal(myProps.getMessageErreur(1, ProprieteCommune.preva_spotToWeb_doc_directory_propKey + ".archive", true));
				return;
			}
			
			try
			{
				iNbFichiers = myDTFP.run(sDirFileAttente, sDirArchive);
				myLog.info("  - " + iNbFichiers + " fichier(s) transmi(s)");
			}
			catch ( TrsException e )
			{
				myLog.fatal(JavaUtil.getMethodeFullName() + " - PROBLEME lors du transfert FTP des Documents : " + e.toString());
			}
		}
		else
		{
			myLog.info("  - NON demande");
		}
	
		
		myLog.info("");
		myLog.info("*** FIN du traitement *********************************************************************");
		myLog.info("");
	}
}