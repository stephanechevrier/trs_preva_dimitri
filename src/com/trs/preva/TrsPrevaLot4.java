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
import com.trs.preva.action.DoFileAttenteOutLot2;
import com.trs.preva.action.DoFileAttenteOutLot4;
import com.trs.preva.action.DoFileAttenteOutPch;


/**
 * <p>Traitement principal PREVA</p>
 *  
 * @author Jean-Noël CATTIN
 */
public class TrsPrevaLot4 
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
    		sParam = "12345678";
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
		
		
		// 16/02/2021 - Traitement des Confirmations de commandes LDD Wintrans
		myLog.info("");
		myLog.info("*** 6. RECUPERATION des Confirmations des RDV Wintrans a transmettre au site web *************************");
		
		DoFileAttenteOutConf myDFA_out31 = null;
		
		iNbFichiers = 0;
		
		if ( sParam.indexOf("6") > -1 )
		{
			try
			{
				myDFA_out31 = new DoFileAttenteOutConf("ConfirmationExplicite",myProps);
				iNbFichiers = myDFA_out31.generateFile();
				
				myLog.info("       - " + iNbFichiers + " fichier Conf genere pour le Web");
				myLog.info("       - " + myDFA_out31.getiNbFichiersGeneresSpotBatch() + " fichier SpotBatch genere");
				myLog.info("       - " + myDFA_out31.getiNbFichiersExecutesSpotBatch() + " fichier SpotBatch execute");
			}
			catch ( TrsException e )
			{
				myLog.fatal("PROBLEME lors de la prise en compte des donnees de confirmation de RDV pour le site web : " + e.toString());
			}
		}
		else
		{
			myLog.info("  - NON demande");
		}
		// 16/02/2021 - FIN modif
			
		// 16/02/2021 - Traitement des Confirmations de commandes Non LDD Wintrans
		myLog.info("");
		myLog.info("*** 7. RECUPERATION des Confirmations de RDV Wintrans a transmettre au site web *************************");
		
		DoFileAttenteOutConf myDFA_out311 = null;
		
		iNbFichiers = 0;
		
		if ( sParam.indexOf("7") > -1 )
		{
			try
			{
				myDFA_out311 = new DoFileAttenteOutConf("ConfirmationImplicite",myProps);
				iNbFichiers = myDFA_out311.generateFile();
				
				myLog.info("       - " + iNbFichiers + " fichier Conf genere pour le Web");
				myLog.info("       - " + myDFA_out311.getiNbFichiersGeneresSpotBatch() + " fichier SpotBatch genere");
				myLog.info("       - " + myDFA_out311.getiNbFichiersExecutesSpotBatch() + " fichier SpotBatch execute");
			}
			catch ( TrsException e )
			{
				myLog.fatal("PROBLEME lors de la prise en compte des donnees de confirmation de RDV pour le site web : " + e.toString());
			}
		}
		else
		{
			myLog.info("  - NON demande");
		}
		// 16/02/2021 - FIN modif
				
		// 16/02/2021 - Traitement des BL
		myLog.info("");
		myLog.info("*** 8. RECUPERATION des BL a transmettre au site web *************************");
		
		DoFileAttenteOutBL myDFA_out312 = null;
		
		iNbFichiers = 0;
		
		if ( sParam.indexOf("8") > -1 )
		{
			try
			{
				myDFA_out312 = new DoFileAttenteOutBL(myProps);
				iNbFichiers = myDFA_out312.generateFile();
				
				myLog.info("       - " + iNbFichiers + "  BL genere pour le Web");
				myLog.info("       - " + myDFA_out312.getiNbFichiersGeneresSpotBatch() + " fichier SpotBatch genere");
				myLog.info("       - " + myDFA_out312.getiNbFichiersExecutesSpotBatch() + " fichier SpotBatch execute");
			}
			catch ( TrsException e )
			{
				myLog.fatal("PROBLEME lors de la prise en compte des BL pour le site web : " + e.toString());
			}
		}
		else
		{
			myLog.info("  - NON demande");
		}
		// 16/02/2021 - FIN modif
		
	
    	
		// Transfert des données Spot au site web
		// Avec le lot 3, on transfert donc le fichier PREVA + le fichier prise en charge
		
		// TODO 16/01/2023 - Est-ce que cela concerne tous les fichiers à transférer par FTP ?
		
		myLog.info("");
		myLog.info("*** 5. TRANSFERT des fichiers Spot au serveur FTP du site web PREVA ***************************");
		DoFileAttenteOutLot4 myDFA_out4 = null;
		iNbFichiers = 0;
		
		if ( sParam.indexOf("4") > -1 )
		{
			if ( myDFA_out4 == null )
				// Transfert des fichiers PREVA et Prise en charge
				myDFA_out4 = new DoFileAttenteOutLot4(myProps);
			
			try
			{
				iNbFichiers = myDFA_out4.putFile();
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