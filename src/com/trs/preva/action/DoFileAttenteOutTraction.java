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
import com.trs.utils.calendar.CalendarHelper;
import com.trs.utils.fileSystem.FileUtil;
import com.trs.utils.java.JavaUtil;
import com.trs.utils.string.StringUtil;
import com.trs.wintrans.dbAccess.HistoSavAccess;
import com.trs.wintrans.dbAccess.LigCommAccess;
import com.trs.wintrans.dbAccess.SpotBatch;
import com.trs.wintrans.metier.AProprietes;
import com.trs.wintrans.metier.HistoSav;
import com.trs.wintrans.metier.LigComm;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Lecture des fichiers dans "fileAttente" 
 * 
 * @author Dimitri Buffard
 *
 */
public class DoFileAttenteOutTraction 
{
	// 16/02/2021 - Crï¿½ation pour lot 3 PREVA : prises en charge
	
	// Log
	static final Logger myLog = LogManager.getLogger(DoFileAttenteOutTraction.class.getName());
	
    ProprieteCommune myPC = null;
    private	String iProp = "";
    private	int iNbFichiersGeneresSpotToWeb = 0;
    private	int iNbFichiersGeneresSpotBatch = 0;
    private int iNbFichiersExecutesSpotBatch = 0;
 
    
    private final String _tableRef = "HISTOSAV";

    
    public DoFileAttenteOutTraction(String iProp,ProprieteCommune aPC)
    {
    	// TODO Ajouter passage de paramï¿½tres : asPropFileName, dbName
    	
    	this.myPC = aPC;
    	this.iProp = iProp;
    	
		
		// Licence FTP achetï¿½e le 25/01/2019
		com.enterprisedt.util.license.License.setLicenseDetails("TRANSPORTROUTESERVICES", "371-3545-6633-501248");
    }

    /**
	 * <p>Gï¿½nï¿½ration du fichier des prises en charge</p>
     * 
     * @return -1 si erreur, -2 si erreur ï¿½ la gï¿½nï¿½ration du fichier, SINON le nombre de fichiers traitï¿½s
     */
	public int generateFile() throws TrsException
	{
		
		int		iNbJours = 0;
		int		iNbSqlOrder = 0;
		String  sDateCreation = "";
		String	sDateExport = "";
		String	sDbName = "";
		String	sDbPropFileName = "";		
        String 	sDirConf = "";
        String	sGrosVolume = "";
        String	sUpdateOrderSendTraction = "";
        String	sPropKey = "";
		String	sRetour = "";
		String	sSqlDirectTemplate = "GetSession.SqlDirect('REQUETE', true);";
		String	sTemp = "";
		String	sUpdateOrder = "";
		String	sUpdateOrderItem = "";
		String	sUpdateTemplate = "";
		//String	sValeur = "";
		
		sUpdateOrderSendTraction=    	"MERGE INTO a_proprietes aProp "
				+ "USING ( "
				+ "  SELECT "
	       		+ "    @NO_REF@ AS no_ref, "
	       		+ "    ''TRS_TRACTION_PREVA''  AS nom, "
	       		+ "		@NO_OT@ as VALEUR1_DOUBLE, "
	       		+ "    ''@EVENEMENT@'' as VALEUR3_CHAINE"	
				+ "  FROM dual "
				+ "  ) virtuel on ( "
				+ "   aProp.no_ref = virtuel.no_ref "
				+ "  AND aProp.nom = virtuel.nom "
				+ "  AND aProp.VALEUR1_DOUBLE = @NO_OT@ "	
				+ "  AND aProp.VALEUR3_CHAINE = virtuel.VALEUR3_CHAINE "
				+ "  ) "
				
				+ "WHEN MATCHED THEN "
				+ "UPDATE SET "
				// Date de mise ï¿½ jour 
				+ "VALEUR1_DATE = @VALEUR1_DATE@ "
				
				+ "WHEN NOT MATCHED THEN "
				+ "INSERT( "
				+ "TABLE_REF, NO_REF, NOM, "
				// Date de mise ï¿½ jour 
				+ "VALEUR1_DATE, "
				// Numï¿½ro commande concernï¿½e
				+ "VALEUR1_DOUBLE, "
				+ "VALEUR2_CHAINE, "
				+ "VALEUR3_CHAINE "

				+ ") "
				+ "VALUES ("
				+ "''P_OP'', "
	       		+ "@NO_REF@, "
	       		+ "''TRS_TRACTION_PREVA'', "
				// Date de mise ï¿½ jour 
	       		+ "@VALEUR1_DATE@, "
				// Numï¿½ro commande concernï¿½e
	       		+ "@NO_OT@, "
	       		+ "''@EVENEMENT@'', "
	       		+ " ''VALEUR1_DOUBLE = Numï¿½ro OT | VALEUR2_CHAINE = EVENEMENT''"
	       		

				+ ") "
				;
		
		
		if ( this.myPC == null )
			throw new TrsException("Le fichier de proprietes N'est PAS initialise : traitement IMPOSSIBLE !  Appeler le constructeur avant d'utiliser cette methode");
		
		
		Date 				myDate = new Date(System.currentTimeMillis());
		SimpleDateFormat	mySDF = null;
		
		hP_OPAccess	myP_OPAccess = null;
		List<hP_OP>	myP_OPList = null;
		
		SpotBatch			mySB = null;
		SpotToWebTraction		mySTW = null; // 18/02/2021

		
        // Propriï¿½tï¿½ dï¿½crivant le rï¿½pertoire de configuration
		// ********************************************************************************************************************
        try { sDirConf = this.myPC.getDirConf(); }
        catch ( TrsException e )
        {
        	myLog.fatal(e.toString());
        	return -1;
        }
        
        sDirConf = StringUtil.getDirPath(true, sDirConf);
		
        
		// Connection ï¿½ la base de donnï¿½es Wintrans
		// ********************************************************************************************************************
    	sDbName = "wintrans";
    	
    	// Fichier de propriï¿½tï¿½s dï¿½crivant la base de donnï¿½es
		sPropKey = "dbPropFileName";
		sDbPropFileName = this.myPC.getProperty(sPropKey, "");
		
		if ( sDbPropFileName.equals("") )
    	{
    		sRetour = "VERIFIER propriete [" + this.myPC.getKeyWithEnvironment(sPropKey) + "] dans " + this.myPC.getPropFileName();
			myLog.fatal(sRetour);
			return -1;
    	}
		// Date de saisie des commandes à partir duquelle on prend en compte les données à envoyer
		// ********************************************************************************************************************
		sPropKey = "commande.dateCreation.mini";
		sDateCreation = this.myPC.getProperty(sPropKey, "", true);
		
		if ( sDateCreation.equals("") )
			throw new TrsException(JavaUtil.getMethodeFullName() + " : la propriete [" + this.myPC.getEnvironnement() + "." + sPropKey + "] doit exister "
				+ "dans le fichier de proprietes [" + this.myPC.getPropFileName() + "]"
				);
		// Accï¿½s pour les SAV Wintrans
    	try { myP_OPAccess = new hP_OPAccess(sDbPropFileName, sDbName); }
    	catch ( TrsException e )
    	{
    		sRetour = "ERREUR connection base de donnees [" + sDbName + "] pour LigComm : " + e.toString();
			myLog.fatal(sRetour);
			return -1;
    	}
        
        // Lecture des SAV ï¿½ prendre en compte
    	// **********************************************************************************
		myLog.info("LECTURE des Tractions a prendre en compte");
		
        
        	try {
        		myLog.info("argument : " + iProp);
				myP_OPList = myP_OPAccess.getSqlforTraction(sDateCreation,iProp,this._tableRef, iNbJours);
			} catch (AppliException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} 
        	
        	if ( myP_OPList == null )
        	{
    			myLog.info("  - AUCUNE commande a prendre en compte");
    			myLog.info("    - ATTENTION myLGList = null !");
        	}
        	else
        	{
    			myLog.info("  - " + myP_OPList.size() + " SAV trouve(s)");
    			
    			if ( myP_OPList.size() > 0 )
    			{
    				// Si des SAV, le fichier est ï¿½crit
    				
    				try { mySTW = new SpotToWebTraction(iProp,myP_OPList, this.myPC, sDbPropFileName, sDbName); }
    				catch ( Exception e )
    				{ 
    					myLog.fatal(e.toString());
    					return -2;
    				}
    				
    				this.setiNbFichiersGeneresSpotToWeb(1);
    				//Mise a jour dans la base Wintrans ï¿½ ajouter  via SpotBatch
    			}
    			
    			mySB = new SpotBatch(this.myPC);
    			
    			// Modï¿½le de script SpotBatch
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
    			
    	        for ( hP_OP myP_OP : myP_OPList )
    	        {
    	        	myLog.debug("P_OP #" + myP_OP.get_refCommande()  + " : MERGE a_proprietes TRS_TRACTION_PREVA correspondant");

    				// Script de mise ï¿½ jour SQL pour SpotBatch : table A_PROPRIETES
    				// *************************************************************************************
    				sTemp = sUpdateOrderSendTraction.replaceAll("@NO_REF@", "" + myP_OP.get_refCommande());
    				
    				sTemp = sTemp.replaceAll("@NO_OT@", "" + myP_OP.get_OT().getNoOt());
    				// Date de mise ï¿½ jour
    				sTemp = sTemp.replaceAll("@VALEUR1_DATE@", "sysdate");
    				sTemp = sTemp.replaceAll("@EVENEMENT@", iProp);

    				sUpdateOrderItem = sSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
    				
    				// Ajout d'une trace pour identifier la ligne en erreur quand erreur
    				iNbSqlOrder ++;
    				sUpdateOrder += "Trace('Ordre SQL # " + iNbSqlOrder + "');\r\n";
    				
    				sUpdateOrder += sUpdateOrderItem;
    				
    				sUpdateOrder += "\r\n";
    			} 
        	
        	
        	// Si une mise ï¿½ jour ï¿½ faire en base de donnï¿½es, on gï¿½nï¿½re le fichier pour SpotBatch
        	this.setiNbFichiersGeneresSpotBatch( mySB.writeFile("spotToWeb.update", sUpdateOrder, sUpdateTemplate, "") );
        				
        				// Exï¿½cution du script
        	 if ( ! sUpdateOrder.equals("") )
        	this.setiNbFichiersExecutes(mySB.execute("spotToWeb.update")); 
        			} 

        return this.getiNbFichiersGeneresSpotToWeb();
		
	}
	
	
	/**
	 * <p>Transfert FTP des fichiers prï¿½sents + Archivage pour chaque transfert OK</p>
	 * 
	 * @return
	 * @throws TrsException
	 * @deprecated NE PAS UTILISER : ï¿½ supprimer : Cette mï¿½thode est inutile ici : FAIRE une classe DoFileAttenteOutFtp uniquement pour le transfert FTP
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
		
		
		// Rï¿½pertoire de stockage des fichiers ï¿½ envoyer
		// ********************************************************************************************************************
		sDirFileAttente = this.myPC.getProperty(ProprieteCommune.dir_out_target_propKey, "");
		
		if ( sDirFileAttente.equals("") )
		{
			myLog.fatal(this.myPC.getMessageErreur(1, ProprieteCommune.dir_out_target_propKey));
			return -1;
		}
		
		
		// Rï¿½pertoire d'archivage des fichiers ï¿½ envoyer
		// Racine : on ajoute un rï¿½pertoire avec le mois du fichier yyyymm
		// ********************************************************************************************************************
		sDirArchive = this.myPC.getProperty(ProprieteCommune.dir_out_archive_propKey, "");
		
		if ( sDirArchive.equals("") )
		{
        	myLog.fatal(this.myPC.getMessageErreur(1, ProprieteCommune.dir_out_archive_propKey));
        	return -1;
		}
		
		// TODO 09/11/2018 - AJOUTER le mois d'archivage yyyymm dans le nom du rï¿½pertoire d'archivage
		
		// Crï¿½ation du rï¿½pertoire d'archivage principal
		myFileUtil.makeDir(sDirArchive);
		
		myLog.info("LECTURE dans [" + sDirFileAttente + "]");
        
        // Boucle sur le rï¿½pertoire <fileAttente>
        String sFichiers[] = myFileUtil.getFichiers(sDirFileAttente, "");
        

        // S'il y a des fichiers ï¿½ traiter
        // *********************************************************************************************
		if ( sFichiers.length > 0 ) 
		{
	        // Si environnement de production ou de recette, on rï¿½cupï¿½re les informations dans le fichier de propriï¿½tï¿½s
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
	            

	            // Rï¿½pertoire FTP
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