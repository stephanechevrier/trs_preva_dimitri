package com.trs.preva.action;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.Protocol;
import com.enterprisedt.net.ftp.SecureFileTransferClient;
import com.enterprisedt.net.ftp.WriteMode;
import com.trs.action.ProprieteCommune;
import com.trs.constant.TrsConstant;
import com.trs.exception.AppliException;
import com.trs.exception.TrsException;
import com.trs.preva.metier.*;
import com.trs.wintrans.dbAccess.CommandesPlakardsAccess;
import com.trs.utils.calendar.CalendarHelper;
import com.trs.utils.fileSystem.FileUtil;
import com.trs.utils.string.StringUtil;
import com.trs.wintrans.dbAccess.HistoSavAccess;
import com.trs.wintrans.dbAccess.SpotBatch;
import com.trs.wintrans.metier.AProprietes;
import com.trs.wintrans.metier.HistoSav;
import com.trs.wintrans.metier.CommandesPlakards;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Lecture des fichiers dans "fileAttente" 
 * 
 * @author Jean-Noël CATTIN
 *
 */
public class DoFileAttenteOutBL 
{
	// 16/02/2021 - Création pour lot 3 PREVA : prises en charge
	
	// Log
	static final Logger myLog = LogManager.getLogger(DoFileAttenteOutBL.class.getName());
	
    ProprieteCommune myPC = null;
    private	int iNbFichiersGeneresSpotToWeb = 0;
    private	int iNbFichiersGeneresSpotBatch = 0;
    private int iNbFichiersExecutesSpotBatch = 0;
 
    

    
    public DoFileAttenteOutBL(ProprieteCommune aPC)
    {
    	// TODO Ajouter passage de paramètres : asPropFileName, dbName
    	
    	this.myPC = aPC;
    	
		
		// Licence FTP achetée le 25/01/2019
		com.enterprisedt.util.license.License.setLicenseDetails("TRANSPORTROUTESERVICES", "371-3545-6633-501248");
    }

    /**
	 * <p>Génération du fichier des prises en charge</p>
     * 
     * @return -1 si erreur, -2 si erreur à la génération du fichier, SINON le nombre de fichiers traités
     */
	public int generateFile() throws TrsException
	{
		
		int		iNbJours = 0;
		int		iNbSqlOrder = 0;
		
		String	sDateExport = "";
		String	sDbName = "";
		String	sDbPropFileName = "";		
        String 	sDirConf = "";
        String	sUpdateOrderSendBL = "";
        String	sPropKey = "";
		String	sRetour = "";
		String	sSqlDirectTemplate = "GetSession.SqlDirect('REQUETE', true);";
		String	sTemp = "";
		String	sUpdateOrder = "";
		String	sUpdateOrderItem = "";
		String	sUpdateTemplate = "";
		String  sTarget = "";
		//String	sValeur = "";
		
		sUpdateOrderSendBL =    		"  UPDATE COMMANDES_PLAKARDS  "
				+ "                       SET DATE_FTP_PREVA = to_char(@VALEUR1_DATE@)  "
				+ "                       WHERE NO_LIGNE_COMMANDE = @NO_LIGNE_COMMANDE@  "
			;
		
		
		if ( this.myPC == null )
			throw new TrsException("Le fichier de proprietes N'est PAS initialise : traitement IMPOSSIBLE !  Appeler le constructeur avant d'utiliser cette methode");
		
		
		Date 				myDate = new Date(System.currentTimeMillis());
		SimpleDateFormat	mySDF = null;
		
		CommandesPlakardsAccess	myCPAccess = null;
		List<CommandesPlakards>	myCPList = null;
		List<CommandesPlakards>	myCPListCopy = null;
		
		SpotBatch		mySB = null;
		SpotToWebBL		mySTW = null; // 18/02/2021

		
        // Propriété décrivant le répertoire de configuration
		// ********************************************************************************************************************
        try { sDirConf = this.myPC.getDirConf(); }
        catch ( TrsException e )
        {
        	myLog.fatal(e.toString());
        	return -1;
        }
        
        sDirConf = StringUtil.getDirPath(true, sDirConf);
        
        myLog.info("Chemins du fichier de configuration : " + sDirConf);
		
        
		// Connection à la base de données Wintrans
		// ********************************************************************************************************************
    	sDbName = "wintrans";
    	
    	// Fichier de propriétés décrivant la base de données
		sPropKey = "dbPropFileName";
		sDbPropFileName = this.myPC.getProperty(sPropKey, "");
		
		if ( sDbPropFileName.equals("") )
    	{
    		sRetour = "VERIFIER propriete [" + this.myPC.getKeyWithEnvironment(sPropKey) + "] dans " + this.myPC.getPropFileName();
			myLog.fatal(sRetour);
			return -1;
    	}
    	
		// Accès pour les SAV Wintrans
    	try { myCPAccess = new CommandesPlakardsAccess(sDbPropFileName, sDbName); }
    	catch ( TrsException e )
    	{
    		sRetour = "ERREUR connection base de donnees [" + sDbName + "] pour Commande_Plakards : " + e.toString();
			myLog.fatal(sRetour);
			return -1;
    	}
        
        // Lecture des SAV à prendre en compte
    	// **********************************************************************************
		myLog.info("LECTURE des BL a prendre en compte");
		
        
        		myCPList = myCPAccess.getBlForPreva();
        	
        	if ( myCPList == null )
        	{
    			myLog.info("  - AUCUN SAV a prendre en compte");
    			myLog.info("  - ATTENTION myCPList = null !");
        	}
        	else
        	{
    			myLog.info("  - " + myCPList.size() + " SAV trouve(s)");
    			
    			if ( myCPList.size() > 0 )
    			{
    				// Si des SAV, le fichier est copié
    				
    				try { mySTW = new SpotToWebBL(myCPList, this.myPC, sDbPropFileName, sDbName); }
    				catch ( Exception e )
    				{ 
    					myLog.fatal(e.toString());
    					return -2;
    				}
    				
    				this.setiNbFichiersGeneresSpotToWeb(1);
    				//Mise a jour dans la base Wintrans à ajouter  via SpotBatch
    			}
    			
    			//sTarget =  myPC.getProperty(ProprieteCommune.preva_spotToWeb_bl_directory_propKey + ".target", "");
    			sTarget =  myPC.getProperty(this.myPC.getEnvironnement() + "." + ProprieteCommune.preva_spotToWeb_bl_directory_propKey + ".target", "");
    			
    			
    			myLog.info("Chemins destination BL : " + sTarget);
    			
    			
    			if ( myCPList.size() > 0 )
    			{
    				
    			myCPListCopy = mySTW.copyFile(sTarget, myCPList);
    			
    			
    				
    			mySB = new SpotBatch(this.myPC);
    			
    			// Modèle de script SpotBatch
    			// *****************************************************************
    			try
    			{
    				sUpdateTemplate = mySB.getSpotBatchTemplate("spotToWeb.update");
    			}
    			catch ( TrsException e )
    			{
    				throw new TrsException(e.toString());
    			}
    			
    			// Marquer l'export sur chaque A_PROPRIETES
    			// *************************************************************************
    			mySDF = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
    			sDateExport = mySDF.format(myDate);
    			
    			
    	        for ( CommandesPlakards myCP : myCPListCopy )
    	        {
    	        	myLog.debug("Commandes_Plakards #" + myCP.getLigComm().get_refCommande() + " : MERGE a_proprietes TRS_BL_PREVA correspondant");

    				// Script de mise à jour SQL pour SpotBatch : table A_PROPRIETES
    				// *************************************************************************************
    				sTemp = sUpdateOrderSendBL.replaceAll("@NO_LIGNE_COMMANDE@", "" + myCP.getLigComm().get_refCommande());

    				// Date de mise à jour
    				sTemp = sTemp.replaceAll("@VALEUR1_DATE@", "sysdate");

    				sUpdateOrderItem = sSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
    				
    				// Ajout d'une trace pour identifier la ligne en erreur quand erreur
    				iNbSqlOrder ++;
    				sUpdateOrder += "Trace('Ordre SQL # " + iNbSqlOrder + "');\r\n";
    				
    				sUpdateOrder += sUpdateOrderItem;
    				
    				sUpdateOrder += "\r\n";
    			}
    	        
        	
        	
        	// Si une mise à jour à faire en base de données, on génère le fichier pour SpotBatch
        	this.setiNbFichiersGeneresSpotBatch( mySB.writeFile("spotToWeb.update", sUpdateOrder, sUpdateTemplate, "") );
        				
        				// Exécution du script
        	 if ( ! sUpdateOrder.equals("") )
        	this.setiNbFichiersExecutes(mySB.execute("spotToWeb.update")); 
        			 

        
        	}
    		
    			
        
        	else {
        		myLog.info("Il n'y a pas de BL à traiter");
        	}
    			
        	}
        	mySTW.SendNotifInformatique();
        	return this.getiNbFichiersGeneresSpotToWeb();
	}
	
	
	
	/**
	 * <p>Transfert FTP des fichiers présents + Archivage pour chaque transfert OK</p>
	 * 
	 * @return
	 * @throws TrsException
	 * @deprecated NE PAS UTILISER : à supprimer : Cette méthode est inutile ici : FAIRE une classe DoFileAttenteOutFtp uniquement pour le transfert FTP
	 * 
	 */
	public int putFile() throws TrsException
	{
		SecureFileTransferClient 	mySFtpClient = null;
        FileUtil 					myFileUtil = new FileUtil();
        ProprieteCommune			myPC = null;
        
        int		iNbFichiers = 0;

        String	sAnnee = "";
        String	sDirArchive = "";
        String	sDirFileAttente = "";
        String	sFileName = "";
        String 	sMois = "";
        String	sPropKey = "";
        
        String	sFtpHost = "ftp.trs49.fr";
        String	sFtpLogin = "testjnc";
        String	sFtpPassword = "cnjtset";
        String	sFtpPort = "21";
        String	sFtpRepertoire = ".";
        

		if ( this.myPC == null )
			throw new TrsException("Le fichier de proprietes N'est PAS initialise : traitement IMPOSSIBLE !  Appeler le constructeur avant d'utiliser cette methode");
		
		
		// Répertoire de stockage des fichiers à envoyer
		// ********************************************************************************************************************
		sDirFileAttente = this.myPC.getProperty(ProprieteCommune.dir_out_target_propKey, "");
		
		if ( sDirFileAttente.equals("") )
		{
			myLog.fatal(this.myPC.getMessageErreur(1, ProprieteCommune.dir_out_target_propKey));
			return -1;
		}
		
		
		// Répertoire d'archivage des fichiers à envoyer
		// Racine : on ajoute un répertoire avec le mois du fichier yyyymm
		// ********************************************************************************************************************
		sDirArchive = this.myPC.getProperty(ProprieteCommune.dir_out_archive_propKey, "");
		
		if ( sDirArchive.equals("") )
		{
        	myLog.fatal(this.myPC.getMessageErreur(1, ProprieteCommune.dir_out_archive_propKey));
        	return -1;
		}
		
		// TODO 09/11/2018 - AJOUTER le mois d'archivage yyyymm dans le nom du répertoire d'archivage
		
		// Création du répertoire d'archivage principal
		myFileUtil.makeDir(sDirArchive);
		
		myLog.info("LECTURE dans [" + sDirFileAttente + "]");
        
        // Boucle sur le répertoire <fileAttente>
        String sFichiers[] = myFileUtil.getFichiers(sDirFileAttente, "");
        

        // S'il y a des fichiers à traiter
        // *********************************************************************************************
		if ( sFichiers.length > 0 ) 
		{
	        // Si environnement de production ou de recette, on récupère les informations dans le fichier de propriétés
			if ( this.myPC.getEnvironnement().equals("prod") || this.myPC.getEnvironnement().equals("rec") )
			{
				// Host FTP
				sPropKey = "spotToWeb.ftp.host";
				sPropKey = this.myPC.getEnvironnement() + "." + sPropKey;
	
	            sFtpHost = this.myPC.getProperty(sPropKey, "");
		            
	            if ( sFtpHost.equals("") )
	            	throw new TrsException(this.myPC.getMessageErreur(1, sPropKey));
		            	
	            // Login FTP
				sPropKey = "spotToWeb.ftp.login";
				sPropKey = this.myPC.getEnvironnement() + "." + sPropKey;
	
	            sFtpLogin = this.myPC.getProperty(sPropKey, "");
		            
	            if ( sFtpLogin.equals("") )
	            	throw new TrsException(this.myPC.getMessageErreur(1, sPropKey));
	
	            // Password FTP
				sPropKey = "spotToWeb.ftp.password";
				sPropKey = this.myPC.getEnvironnement() + "." + sPropKey;
	            
	            sFtpPassword = this.myPC.getProperty(sPropKey, "");
		            
	            if ( sFtpPassword.equals("") )
	            	throw new TrsException(this.myPC.getMessageErreur(1, sPropKey));
	
	            // Port FTP
				sPropKey = "spotToWeb.ftp.port";
				sPropKey = this.myPC.getEnvironnement() + "." + sPropKey;
	            
	            sFtpPort = this.myPC.getProperty(sPropKey, "");
		            
	            if ( sFtpPort.equals("") )
	            	throw new TrsException(this.myPC.getMessageErreur(1, sPropKey));
	            

	            // Répertoire FTP
				sPropKey = "spotToWeb.ftp.repertoire";
				sPropKey = this.myPC.getEnvironnement() + "." + sPropKey;
	            
				sFtpRepertoire = this.myPC.getProperty(sPropKey, "");
		            
	            if ( sFtpRepertoire.equals("") )
	            	throw new TrsException(this.myPC.getMessageErreur(1, sPropKey));
			}
			
			myLog.info("Tentative envoi FTP a [" + sFtpHost + ";" + sFtpPort + ";" + sFtpLogin + ";" + sFtpPassword + "]");
			
			try 
			{ 
	            myLog.info("Creating SFTP client");
	            mySFtpClient = new SecureFileTransferClient();
			}
			catch ( Exception e )
			{
				throw new TrsException("ERREUR lors de la creation du service FTP " + " : " + e.toString());
			}
				
			try
			{
				mySFtpClient.setRemoteHost(sFtpHost);
				mySFtpClient.setUserName(sFtpLogin);
				mySFtpClient.setPassword(sFtpPassword);
				mySFtpClient.setRemotePort(Integer.parseInt(sFtpPort));
				mySFtpClient.setProtocol(Protocol.SFTP);
				
				myLog.info("  - Avant connect()");
				mySFtpClient.connect();
				myLog.info("  - Connected and logged in to server " + sFtpHost);
				
				myLog.info("  - Changement de repertoire : " + sFtpRepertoire);
				mySFtpClient.changeDirectory(sFtpRepertoire);
	
				for ( int j = 0; j < sFichiers.length; j ++ ) 
				{
				    try 
					{
						myLog.info("");
						myLog.info("Fichier [" + sFichiers[j] + "]");
				        
				        myLog.info("  - avant PUT");
				
				        sFileName = sDirFileAttente + "/" + sFichiers[j];
				        mySFtpClient.uploadFile(sFileName, sFichiers[j], WriteMode.OVERWRITE);
				        
						myLog.info("  - SUCCES Envoi FTP");
				
						// Archivage du fichier transmis
				        // Format du fichier = preva-spot-2018-10-05-08-00-00.csv
				        sAnnee = StringUtil.getStringIn(StringUtil.getStringIn(sFileName, "-", 3), "-", -1);
				        sMois = StringUtil.getStringIn(StringUtil.getStringIn(sFileName, "-", 4), "-", -1);
				        myFileUtil.makeDir(sDirArchive + "/" + sAnnee + sMois);
						myFileUtil.copyBytes(sFileName, sDirArchive + "/" + sAnnee + sMois + "/" + sFichiers[j], true);
						
						iNbFichiers ++;
				    } 
				    catch ( Exception e ) 
				    {
						throw new TrsException("ERREUR lors de l'envoi par FTP du fichier [" + sFileName 
							+ "] "
							+ ( mySFtpClient == null ? "" :
							"(" + mySFtpClient.getRemoteHost() + " / " + mySFtpClient.getUserName() + " / " + mySFtpClient.getRemotePort() + ")"
							)
							+ " : " + e.toString());
				    }
				}

	            // Shut down client
	            myLog.info("  - ARRET de la connexion FTP");
	            mySFtpClient.disconnect();
			}
			catch ( FTPException e )
			{
				throw new TrsException("ERREUR lors de l'etablissement de la connexion FTP "
					+ ( mySFtpClient == null ? "" :
					"(" + mySFtpClient.getRemoteHost() + " / " + mySFtpClient.getUserName() + " / " + mySFtpClient.getRemotePort() + ")"
					)
					+ " : " + e.toString());
			}
			catch ( Exception e1 )
			{
				throw new TrsException("ERREUR lors de l'etablissement de la connexion FTP "
					+ " : " + e1.toString());
			}
		}
		else
			myLog.info("PAS de fichier a envoyer par FTP");
		
		return iNbFichiers;
	}

	public int getiNbFichiersGeneresSpotBatch() {
		return iNbFichiersGeneresSpotBatch;
	}

	public void setiNbFichiersGeneresSpotBatch(int iNbFichiersGeneres) {
		this.iNbFichiersGeneresSpotBatch = iNbFichiersGeneres;
	}

	public int getiNbFichiersGeneresSpotToWeb() {
		return iNbFichiersGeneresSpotToWeb;
	}

	public void setiNbFichiersGeneresSpotToWeb(int aiValeur) {
		this.iNbFichiersGeneresSpotToWeb = aiValeur;
	}

	public int getiNbFichiersExecutesSpotBatch() {
		return iNbFichiersExecutesSpotBatch;
	}

	public void setiNbFichiersExecutes(int aiValeur) {
		this.iNbFichiersExecutesSpotBatch = aiValeur;
	}
}
