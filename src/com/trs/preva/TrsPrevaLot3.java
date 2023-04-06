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
import com.trs.preva.action.DoFileAttenteOutLot2;
import com.trs.preva.action.DoFileAttenteOutPch;

/**
 * <p>Traitement principal PREVA</p>
 *  
 * @author Jean-Noël CATTIN
 */
public class TrsPrevaLot3 
{
	// Log
	private static final Logger myLog = LogManager.getLogger("trsPreva");
	
	public static void main(String[] args) 
	{
		int iNbFichiers = 0; 
		
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
    	
    	// 16/02/2021 - Avec le lot 3 de PREVA, on introduit le transfert des prises en charge : param = 5
    	if ( args.length == 0 )
    		//sParam = "1234";
    		sParam = "12345";
    	// 16/02/2021 - FIN modif
    	else
    		sParam = args[0];
	
    	
		// 	Traitement des fichiers en provenance du site web
		myLog.info("");
		myLog.info("*** 1. RECUPERATION des fichier sur serveur FTP PREVA Site web ****************************");
		
		DoFileAttenteInLot2 myDFA_in = null;
		
		iNbFichiers = 0;
		
		if ( sParam.indexOf("1") > -1 )
		{
			try 
			{ 
				myDFA_in = new DoFileAttenteInLot2(myProps); 
				iNbFichiers = myDFA_in.getFile();
			}
			catch ( TrsException e )
			{
				myLog.fatal("PROBLEME lors de la lecture des fichiers en provenance du site web : " + e.toString());
			}
		}
		else
		{
			myLog.info("  - NON demande");
		}
		
		
		// Lecture des fichiers à traiter
		myLog.info("");
		myLog.info("*** 2. LECTURE des fichiers WebToSpot a traiter *****************************************************");
		
		iNbFichiers = 0;
		
		if ( sParam.indexOf("2") > -1 )
		{
			try 
			{ 
				if ( myDFA_in == null )
					myDFA_in = new DoFileAttenteInLot2(myProps);
			}
			catch ( TrsException e )
			{
				myLog.fatal("PROBLEME lors de l'initialisation de DoFileAttenteInLot2 : " + e.toString());
			}
			
			try 
			{ 
				iNbFichiers = myDFA_in.readFileAndUpdate();
				myLog.info("       - " + iNbFichiers + " traite(s)");
			}
			catch ( TrsException e )
			{
				myLog.fatal("PROBLEME lors de la lecture des fichiers en provenance du site web : " + e.toString());
			}
			
			try
			{
				myLog.info("APPEL de la fonction d'envoi du rapport aux exploitants");
				myDFA_in.getWTSRManager().sendNotifExploitation();
			}
			catch ( TrsException e )
			{
				myLog.fatal("PROBLEME lors de l'envoi du rapport aux exploitants : " + e.toString());
			}
		}
		else
		{
			myLog.info("  - NON demande");
		}
		
		
		// 	Traitement des données PREVA Spot
		myLog.info("");
		myLog.info("*** 3. RECUPERATION des donnees PREVA Spot a transmettre au site web *************************");
		
		DoFileAttenteOutLot2 myDFA_out = null;
		
		iNbFichiers = 0;
		
		if ( sParam.indexOf("3") > -1 )
		{
			try
			{
				myDFA_out = new DoFileAttenteOutLot2(myProps);
				iNbFichiers = myDFA_out.generateFile();
				
				myLog.info("       - " + iNbFichiers + " fichier PREVA genere pour le Web");
				myLog.info("       - " + myDFA_out.getiNbFichiersGeneresSpotBatch() + " fichier SpotBatch genere");
				myLog.info("       - " + myDFA_out.getiNbFichiersExecutesSpotBatch() + " fichier SpotBatch execute");
			}
			catch ( TrsException e )
			{
				myLog.fatal("PROBLEME lors de la prise en compte des donnees SAV Spot pour le site web : " + e.toString());
			}
		}
		else
		{
			myLog.info("  - NON demande");
		}
		
		
		// 	16/02/2021 - Traitement des Prises en charge Wintrans
		myLog.info("");
		myLog.info("*** 4. RECUPERATION des Prises en charge Wintrans a transmettre au site web *************************");
		
		DoFileAttenteOutPch myDFA_out3 = null;
		
		iNbFichiers = 0;
		
		if ( sParam.indexOf("5") > -1 )
		{
			try
			{
				myDFA_out3 = new DoFileAttenteOutPch(myProps);
				iNbFichiers = myDFA_out3.generateFile();
				
				myLog.info("       - " + iNbFichiers + " fichier PCH genere pour le Web");
				myLog.info("       - " + myDFA_out3.getiNbFichiersGeneresSpotBatch() + " fichier SpotBatch genere");
				myLog.info("       - " + myDFA_out3.getiNbFichiersExecutesSpotBatch() + " fichier SpotBatch execute");
			}
			catch ( TrsException e )
			{
				myLog.fatal("PROBLEME lors de la prise en compte des donnees SAV PCH pour le site web : " + e.toString());
			}
		}
		else
		{
			myLog.info("  - NON demande");
		}
		// 16/02/2021 - FIN modif
	
    	
		// Transfert des données Spot au site web
		// Avec le lot 3, on transfert donc le fichier PREVA + le fichier prise en charge 
		myLog.info("");
		myLog.info("*** 5. TRANSFERT des fichiers Spot au serveur FTP du site web PREVA ***************************");
		
		iNbFichiers = 0;
		
		if ( sParam.indexOf("4") > -1 )
		{
			if ( myDFA_out == null )
				// Transfert des fichiers PREVA et Prise en charge
				myDFA_out = new DoFileAttenteOutLot2(myProps);
			
			try
			{
				iNbFichiers = myDFA_out.putFile();
				myLog.info("       - " + iNbFichiers + " envoye(s)");
			}
			catch ( TrsException e )
			{
				myLog.fatal("PROBLEME lors du transfert des fichiers Spot au site web PREVA : " + e.toString());
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