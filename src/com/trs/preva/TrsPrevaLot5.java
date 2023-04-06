
/*
 *  Nojeca
 *  Code pour TRS
 *  jncattin		15/04/2014		cr�ation
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
import com.trs.preva.action.DoFileAttenteOutLiv;
import com.trs.preva.action.DoFileAttenteOutLot2;
import com.trs.preva.action.DoFileAttenteOutLot4;
import com.trs.preva.action.DoFileAttenteOutMlv;
import com.trs.preva.action.DoFileAttenteOutOrder;
import com.trs.preva.action.DoFileAttenteOutPch;
import com.trs.preva.action.DoFileAttenteOutSav;
import com.trs.preva.action.DoFileAttenteOutTraction;
import com.trs.preva.action.DoTransfertFtpPreva;
import com.trs.utils.java.JavaUtil;


/**
 * <p>Traitement principal PREVA</p>
 *  
 * @author Jean-No�l CATTIN
 */
public class TrsPrevaLot5 
{
	// Log
	private static final Logger myLog = LogManager.getLogger("trsPreva");
	
	public static void main(String[] args) 
	{
		int iNbFichiers = 0; 
		
		String	sParam = "";
		String	sDirArchive = "";
		String	sDirFileAttente = "";
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
    	// Il faut d�clarer { smtpProtocol="smtp" } dans l'Appender SMTP Log4J
		System.setProperty("mail.smtp.starttls.enable", "true");
    	
    	myLog.info("Environnement = " + myProps._sEnvironment);
    	
    	// 16/02/2021 - Avec le lot 3 de PREVA, on introduit le transfert des prises en charge : param = 5
    	if ( args.length == 0 )
    		//sParam = "1234";
    		sParam = "12345678910";
    	// 16/02/2021 - FIN modif
    	else
    		sParam = args[0];
    	
    	//LOT 5 Parametre pour transmission des fichiers en-t�te pour chaque SAV
    	String[] sParamEntete = new String[] {"PCH","PREVA","CONF","MLV","LIV"};
    	//LOT 5 Parametre pour transmission des fichiers Traction pour chaque SAV
    	String[] sParamTraction = new String[] {"PCH","PREVA","CONFIMPLICITE","CONFEXPLICITE"};
    	//LOT 5 Parametre pour transmission des fichiers Mise en Livraison pour chaque SAV
    	String[] sParamMlv = new String[] {"CREATION","ANNULATION"};
	
    	
		// 	Traitement des fichiers en provenance du site web
		myLog.info("");
		myLog.info("*** 1. RECUPERATION des fichier sur serveur FTP PREVA Site web ****************************");
		
		DoFileAttenteInLot2 myDFA_in = null;
		
		iNbFichiers = 0;
		
		if ( sParam.indexOf("2") > -1 )
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
		
		
		// Lecture des fichiers � traiter
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
		
		
		// 	Traitement des donn�es PREVA Spot
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
		
		
//	 	16/02/2021 - Traitement des Confirmations de commandes LDD Wintrans
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
			
//		 	16/02/2021 - Traitement des Confirmations de commandes Non LDD Wintrans
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
				
//			 	16/02/2021 - Traitement des BL
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
					
					//16/02/2021 - Traitement des en-t�tes  
					myLog.info("");
					myLog.info("*** 9. RECUPERATION des en-t�tes de commandes a transmettre au site web *************************");
					
					for (int i=0; i<=sParamEntete.length-1; i++) {
					
					DoFileAttenteOutOrder myDFA_out32 = null;
					
					iNbFichiers = 0;
	
					if ( sParam.indexOf("9") > -1 )
					{
						try
						{
							myDFA_out32 = new DoFileAttenteOutOrder(sParamEntete[i],myProps);
							iNbFichiers = myDFA_out32.generateFile();
							
							myLog.info("       - " + iNbFichiers + " fichier en-t�te " + sParamEntete[i] +  " genere pour le Web");
							myLog.info("       - " + myDFA_out32.getiNbFichiersGeneresSpotBatch() + " fichier SpotBatch genere");
							myLog.info("       - " + myDFA_out32.getiNbFichiersExecutesSpotBatch() + " fichier SpotBatch execute");
						}
						catch ( TrsException e )
						{
							myLog.fatal("PROBLEME lors de la prise en compte des donnees des en-t�tes de commandes " + sParamEntete[i] + " pour le site web : " + e.toString());
						}
					}
					else
					{
						myLog.info("  - NON demande");
					}
					// 16/02/2021 - FIN modif
				}
					//16/02/2021 - Traitement des Tractions PRISE EN CHARGE Non LDD Wintrans
					myLog.info("");
					myLog.info("*** 10. RECUPERATION des Tractions a transmettre au site web *************************");
					
					for (int i=0; i<=sParamTraction.length-1; i++) {
					
					DoFileAttenteOutTraction myDFA_out33 = null;
					
					iNbFichiers = 0;
	
					if ( sParam.indexOf("9") > -1 )
					{
						try
						{
							myDFA_out33 = new DoFileAttenteOutTraction(sParamTraction[i],myProps);
							iNbFichiers = myDFA_out33.generateFile();
							
							myLog.info("       - " + iNbFichiers + " fichier traction " + sParamTraction[i] +  " genere pour le Web");
							myLog.info("       - " + myDFA_out33.getiNbFichiersGeneresSpotBatch() + " fichier SpotBatch genere");
							myLog.info("       - " + myDFA_out33.getiNbFichiersExecutesSpotBatch() + " fichier SpotBatch execute");
						}
						catch ( TrsException e )
						{
							myLog.fatal("PROBLEME lors de la prise en compte des donnees de traction " + sParamTraction[i] + " pour le site web : " + e.toString());
						}
					}
					else
					{
						myLog.info("  - NON demande");
					}
					// 16/02/2021 - FIN modif
				}
	
					myLog.info("");
					myLog.info("*** 11. RECUPERATION des MLV a transmettre au site web *************************");
					
					for (int i=0; i<=sParamMlv.length-1; i++) {
					
					DoFileAttenteOutMlv myDFA_out34 = null;
					
					iNbFichiers = 0;
	
					if ( sParam.indexOf("9") > -1 )
					{
						try
						{
							myDFA_out34 = new DoFileAttenteOutMlv(sParamMlv[i],myProps);
							iNbFichiers = myDFA_out34.generateFile();
							
							myLog.info("       - " + iNbFichiers + " fichier MLV " + sParamMlv[i] +  " genere pour le Web");
							myLog.info("       - " + myDFA_out34.getiNbFichiersGeneresSpotBatch() + " fichier SpotBatch genere");
							myLog.info("       - " + myDFA_out34.getiNbFichiersExecutesSpotBatch() + " fichier SpotBatch execute");
						}
						catch ( TrsException e )
						{
							myLog.fatal("PROBLEME lors de la prise en compte des donnees de MLV " + sParamMlv[i] + " pour le site web : " + e.toString());
						}
					}
					else
					{
						myLog.info("  - NON demande");
					}
					// 16/02/2021 - FIN modif
				}
					
					
					myLog.info("");
					myLog.info("*** 12. RECUPERATION des SAV a transmettre au site web *************************");
					
					DoFileAttenteOutSav myDFA_out35 = null;
					
					iNbFichiers = 0;
	
					if ( sParam.indexOf("9") > -1 )
					{
						try
						{
							myDFA_out35 = new DoFileAttenteOutSav(myProps);
							iNbFichiers = myDFA_out35.generateFile();
							
							myLog.info("       - " + iNbFichiers + " fichier SAV genere pour le Web");
							myLog.info("       - " + myDFA_out35.getiNbFichiersGeneresSpotBatch() + " fichier SpotBatch genere");
							myLog.info("       - " + myDFA_out35.getiNbFichiersExecutesSpotBatch() + " fichier SpotBatch execute");
						}
						catch ( TrsException e )
						{
							myLog.fatal("PROBLEME lors de la prise en compte des donnees de SAV pour le site web : " + e.toString());
						}
					}
					else
					{
						myLog.info("  - NON demande");
					}
					// 16/02/2021 - FIN modif
				
    	
					myLog.info("");
					myLog.info("*** 13. RECUPERATION des LIV a transmettre au site web *************************");
							
					DoFileAttenteOutLiv myDFA_out36 = null;
					
					iNbFichiers = 0;
	
					if ( sParam.indexOf("9") > -1 )
					{
						try
						{
							myDFA_out36 = new DoFileAttenteOutLiv(myProps);
							iNbFichiers = myDFA_out36.generateFile();
							
							myLog.info("       - " + iNbFichiers + " fichier SAV genere pour le Web");
							myLog.info("       - " + myDFA_out36.getiNbFichiersGeneresSpotBatch() + " fichier SpotBatch genere");
							myLog.info("       - " + myDFA_out36.getiNbFichiersExecutesSpotBatch() + " fichier SpotBatch execute");
						}
						catch ( TrsException e )
						{
							myLog.fatal("PROBLEME lors de la prise en compte des donnees de LIV pour le site web : " + e.toString());
						}
					}
					else
					{
						myLog.info("  - NON demande");
					}
					// 16/02/2021 - FIN modif
					
					
					myLog.info("");
					myLog.info("*** 4. TRANSFERT des Fichiers et Documents en attente de transfert FTP a PREVA ***************************");
					
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