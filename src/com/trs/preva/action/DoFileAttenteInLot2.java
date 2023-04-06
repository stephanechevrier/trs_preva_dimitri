package com.trs.preva.action;

import com.trs.action.ProprieteCommune;
import com.trs.constant.TrsConstant;
import com.trs.exception.TrsException;
import com.trs.preva.TrsPrevaConstant;
import com.trs.preva.metier.*;
//import com.trs.utils.calendar.CalendarHelper;
import com.trs.utils.fileSystem.FileUtil;
import com.trs.utils.java.JavaUtil;
//import com.trs.utils.properties.Properties;
import com.trs.utils.string.StringUtil;
//import com.trs.utils.system.ExecutePrg;
//import com.trs.wintrans.dbAccess.HistoSavAccess;
//import com.trs.wintrans.dbAccess.P_OPAccess;
import com.trs.wintrans.dbAccess.SpotBatch;
//import com.trs.wintrans.metier.HistoSav;

//import com.enterprisedt.net.ftp.FileTransferClient;
//import com.enterprisedt.net.ftp.FTPClientInterface;
//import com.enterprisedt.net.ftp.FTPConnectMode;
//import com.enterprisedt.net.ftp.FTPException;
//import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.SecureFileTransferClient;
//import com.enterprisedt.net.ftp.ssh.SSHFTPAlgorithm;
import com.enterprisedt.net.ftp.ssh.SSHFTPClient;
//import com.enterprisedt.net.ftp.ssl.SSLFTPClient;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.Protocol;
//import com.enterprisedt.util.debug.Logger;




import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Lecture des fichiers dans "fileAttente"
 * 
 * @author Jean-Noël CATTIN
 *
 */
public class DoFileAttenteInLot2 
{
	// Log
	static final Logger myLog = LogManager.getLogger(DoFileAttenteInLot2.class.getName());
	
    ProprieteCommune myPC = null;
    
    //private int _nbLignesFichier = 0;
    //private int _nbCommandesFichier = 0;
    //private int _nbCommandesFichierGroup = 0;
    
    //private int _nbFichiers = 0;
    
    private WebToSpotReportManagerLot2 _WTSRManager = null;    

    
    public DoFileAttenteInLot2(ProprieteCommune aPC) throws TrsException
    {
    	this.myPC = aPC;
    	
    	// Initialisation pour reporting exploitants
		// Préparation rapport exploitant
		_WTSRManager = new WebToSpotReportManagerLot2(this.myPC);
		
		// Licence FTP achetée le 25/01/2019
		com.enterprisedt.util.license.License.setLicenseDetails("TRANSPORTROUTESERVICES", "371-3545-6633-501248");
    }
    
    /**
     * <p>On boucle sur les fichiers à traiter : pour chaque fichier, on met à jour HISTOSAV.</p>
     * <p>Les fichiers à traiter passent dans un répertoire de transit.</p>
     * 
     * @return
     */
    public int readFileAndUpdate() throws TrsException
    {
    	int iRetour = 0;

    	String	sAnnee = "";
    	String	sDirArchive = "";
    	String	sDirConf = "";
    	String	sDirWorking = "";
    	String	sDirSource = "";
    	String	sFileName = "";
    	String	sMois = "";

        FileUtil 		myTRSFU = new FileUtil();
        
        WebToSpotLot2	myWTS = null;
		
		if ( this.myPC == null )
			throw new TrsException("Le fichier de proprietes N'est PAS initialise : traitement IMPOSSIBLE !  Appeler le constructeur avant d'utiliser cette methode");

		
		// Répertoire où trouver les fichiers à traiter
    	// **************************************************************************************************
    	sDirSource = this.myPC.getProperty(ProprieteCommune.dir_in_source_propKey, "");
    	
		if ( sDirSource.equals("") )
		{
        	myLog.fatal(this.myPC.getMessageErreur(1, ProprieteCommune.dir_in_source_propKey));
        	return -1;
		}
		
		myLog.info("LECTURE   dans [" + sDirSource + "]");
		
		
		// Répertoire des fichiers en cours de traitement
    	// **************************************************************************************************
		sDirWorking = this.myPC.getProperty(ProprieteCommune.dir_in_working_propKey, "");
    	
		if ( sDirWorking.equals("") )
		{
        	myLog.fatal(this.myPC.getMessageErreur(1, ProprieteCommune.dir_in_working_propKey));
        	return -1;
		}
		
		myTRSFU.makeDir(sDirWorking);
		
		myLog.info("TRANSIT   dans [" + sDirWorking + "] : fichiers en cours de lecture");

		
		// Répertoire d'archivage
    	// **************************************************************************************************
		sDirArchive = this.myPC.getProperty(ProprieteCommune.dir_in_archive_propKey, "");
		
		if ( sDirArchive.equals("") )
		{
        	myLog.fatal(this.myPC.getMessageErreur(1, ProprieteCommune.dir_in_archive_propKey));
        	return -1;
		}
		
		myTRSFU.makeDir(sDirArchive);
		
		myLog.info("ARCHIVAGE dans [" + sDirArchive + "] : fichiers en cours de lecture");
		
		
        // Propriété décrivant le répertoire de configuration
    	// **************************************************************************************************
        try { sDirConf = this.myPC.getDirConf(); }
        catch ( TrsException e )
        {
        	myLog.fatal(e.toString());
        	return -1;
        }
        
        sDirConf = StringUtil.getDirPath(true, sDirConf);
		myLog.info("CONFIGURATION dans [" + sDirConf + "]");

		
        // Boucle sur le répertoire Source pour copie des fichiers dans Working
        // *********************************************************************************************
		myLog.info("Debut lecture dans [" + sDirSource + "] pour deplacement fichiers dans [" + sDirWorking + "]");
        
        String sFichiers[] = myTRSFU.getFichiers(sDirSource, "");

        // S'il y a des fichiers à traiter
        // *********************************************************************************************
		if ( sFichiers != null ) 
		{
			for ( int j = 0; j < sFichiers.length; j++ ) 
			{
				// Nom du fichier en cours de traitement (sans le répertoire)
				sFileName = sFichiers[j];
				
				myLog.info("Deplacement du fichier [" + sDirSource + "/" + sFileName + "]");
				
				// Déplacement dans working
				myTRSFU.copyBytes(sDirSource + "/" + sFileName, sDirWorking + "/" + sFileName, true);
			}
		}
		
        
        // Boucle sur le répertoire Working pour prise en compte des fichiers
        // *********************************************************************************************
		myLog.info("Debut lecture dans [" + sDirWorking + "]");
        
        String sFichiers2[] = myTRSFU.getFichiers(sDirWorking, "");

        // S'il y a des fichiers à traiter
        // *********************************************************************************************
		if ( sFichiers2 != null ) 
		{
			for ( int j = 0; j < sFichiers2.length; j++ ) 
			{
				// Nom du fichier en cours de traitement (sans le répertoire)
				sFileName = sFichiers2[j];
				
				myLog.info("Traitement du fichier [" + sDirWorking + "/" + sFileName + "]");
				
				try
				{
					// Lecture du fichier
					myWTS = new WebToSpotLot2(sDirWorking + "/" + sFileName, this.myPC, sDirConf);
				}
				catch ( TrsException e )
				{
					myLog.fatal("ERREUR avec lecture [" + sDirWorking + "/" + sFileName + "] : ARRET du traitement : " + e.toString());
					
					// Il ne faut pas aller plus loin pour ne pas archiver (sinon au prochain cycle, le fichier non traité sera oublié
					return -1;
				}
				
				try
				{
					// Analyse du fichier pour sauvegarde en base de données
					this.updateHistoSav(myWTS, sDirConf);
					
				}
				catch ( TrsException e )
				{
					myLog.fatal("ERREUR avec traitement des donnees du fichier [" + sDirWorking + "/" + sFileName + "] "
						+ "pour mise a jour en base de donnees : ARRET du traitement : " + e.toString());
					
					// Il ne faut pas aller plus loin pour ne pas archiver (sinon au prochain cycle, le fichier non traité sera oublié
					return -1;
				}
				
				// Dans tous les cas (même SI erreur étape précédente), on déplace dans archives pour ne pas jouer 2 fois le même fichier
		        sAnnee = StringUtil.getStringIn(StringUtil.getStringIn(sFileName, "-", 3), "-", -1);
		        sMois = StringUtil.getStringIn(StringUtil.getStringIn(sFileName, "-", 4), "-", -1);
		        myTRSFU.makeDir(sDirArchive + "/" + sAnnee + sMois);
		        
				myTRSFU.copyBytes(sDirWorking + "/" + sFileName, sDirArchive + "/" + sAnnee + sMois + "/" + sFileName, true);
				
				iRetour ++;
			}
		}
    	
    	return iRetour;
    }
  
    /**
     * <p>Mise à jour de HISTOSAV, P_OP et LIGCOMM</p>
     * 
     */
	public void updateHistoSav(WebToSpotLot2 aWTS, String asDirConf) throws TrsException
	{
		int		iNbMaxRepNegatives = 0;
		int		iNbSqlOrder = 0;

		String	sReponse = "";
		String	sReponse70car = ""; // 11/09/2020 - Réponse limitée à 70 caractères
		String	sSqlDateRdv = "";
		String	sSqlDirectTemplate = "GetSession.SqlDirect('REQUETE', true);";
		String	sSqlLibelleSavRstIns = "";
		String	sSqlLibelleSavRstLdf = "";
		String	sTemp = "";
		String	sUpdateOrder = "";
		String	sUpdateOrderItem = "";
		String	sUpdateTemplate = "";
		String	sValeur = "";
		
		SpotBatch			mySP = null;
		
		myLog.info(JavaUtil.getMethodeName() + " - DEBUT");
		
		if ( this.myPC == null )
			throw new TrsException("Le fichier de proprietes N'est PAS initialise : traitement IMPOSSIBLE !  Appeler le constructeur avant d'utiliser cette methode");

		
		// Initialisations
		// *****************************************************************
		sSqlLibelleSavRstIns = "SELECT s.libelle_situation || '' - '' || j.libelle_justification AS libelle "
			+ "FROM situatio s, justific j "
			+ "WHERE s.code_message = j.code_message "
			+ "AND s.code_situation = j.code_situation "
			+ "AND s.code_situation = ''RST'' "
			;
		
		sSqlLibelleSavRstLdf = sSqlLibelleSavRstIns
			+ "AND j.code_justification = ''LDF'' "
			;
		
		sSqlLibelleSavRstIns += "AND j.code_justification = ''INS'' "
			;
		
		sSqlDateRdv = "SELECT op.date_de_debut "
			+ "FROM p_op op "
			+ "WHERE op.no_op = @@@NO_OP@@@ "
			+ "AND op.no_ot = @@@NO_OT@@@ "
			+ "AND op.no_plan = @@@NO_PLAN@@@ "
			;
		
		iNbMaxRepNegatives = Integer.parseInt(this.myPC.getProperty(TrsPrevaConstant.REP_NEGATIVES_NB_MAXI, "3"));
		myLog.info(TrsPrevaConstant.REP_NEGATIVES_NB_MAXI + " = " + iNbMaxRepNegatives);
		
		// Classe pour exécution SpotBatch
		mySP = new SpotBatch(this.myPC);

		// Modèle de script SpotBatch
		// *****************************************************************
		try
		{
			sUpdateTemplate = mySP.getSpotBatchTemplate("webToSpot.update");
		}
		catch ( TrsException e )
		{
			throw new TrsException(e.toString());
		}

		// Boucle sur les lignes pour générer les ordres SQL
		// ********************************************************************************************************************
		for ( WebToSpotLineLot2 myWTSLine : aWTS.getWTSLineList() )
		{
			myLog.info("Traitement reponse " + myWTSLine.get_reponse() + " pour la commande " + myWTSLine.get_noLigComm());
			
			// Mise à jour du SAV RST-(INS|LDF) dans tous les cas de réponse
			// ****************************************************************************
			myLog.debug("INSERT INTO histosav pour RST-INS|LDF");
			
			sReponse = "Reponse web = " + myWTSLine.get_reponse()
					+ ( ! myWTSLine.get_reponse().equals(TrsPrevaConstant.REP_OUI) 
					? " (nb reponses negatives = " + myWTSLine.get_nbReponsesNegatives() + ")" : "" )
					;

			// 11/09/2020 - Limite à 70 caractères
			sReponse70car = ( sReponse.length() > 70 ? sReponse.substring(0, 70) : sReponse);
			// 11/09/2020 - FIN modif

			// Mise à jour du code pour prendre en compte une OT disparu pendant le délai de prise en compte de l'INSERT
			// Pour éviter un plantage du code qui stoppe la suite du script SQL
			sTemp = "INSERT INTO histosav ( no_ot, agence_ot, code_situation, code_justification, libelle, champs8, commentaire, histosav_date, heure, date_rdv ) "
				+ "SELECT " + myWTSLine.get_noOt() + ", "
				+ "''" + myWTSLine.get_agenceEnlevement() + "'', " // Au lieu de .get_agenceOt() = identique
				+ "''RST'', "
				;
			
			// En fonction de la réponse, le code justification change
			if ( myWTSLine.get_reponse().equals(TrsPrevaConstant.REP_OUI) )
				sTemp += "''LDF'', "
					// 11/09/2020 - On affiche la réponse "courte" car seule cette zone "libelle" est affichable dans Wintrans
					+ "''" + sReponse70car + "'', "
					//+ "''Livraison a jour fixe'', " // PAS d'accent pour être sûr de la restitution
					// 11/09/2020 - FIN modif
					;
			else
				sTemp += "''INS'', "
					// 11/09/2020 - On affiche la réponse "courte" car seule cette zone "libelle" est affichable dans Wintrans
					+ "''" + sReponse70car + "'', "
					//+ "''En attente instructions'', "
					// 11/09/2020 - FIN modif
					;
			
			sTemp += "''PREVA'', "
				+ "''" + sReponse + "'', "
				+ "to_date( ''" + myWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // histosav_date
				+ "to_date( ''" + myWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // heure
				;
			
			// 23/12/2020 - On initialise la date de rendez-vous quelle que soit la réponse
			// La date de rendez-vous doit avoir l'heure car il s'agit d'un rendez-vous conf avec heure (comme dans Spot)
			// 07/10/2020 - Pour répons NON et SANS_REPONSE, date_rdv = NULL
			/*
			if ( myWTSLine.get_reponse().equals(TrsPrevaConstant.REP_OUI) )
				sTemp += "to_date( ''" + myWTSLine.get_datePrevLivraison().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ) ";
			else
				sTemp += "NULL ";
			*/
			
			sTemp += "to_date( ''" + myWTSLine.get_datePrevLivraison().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ) ";
			// 07/10/2020 - FIN modif
			// 23/12/2020 - FIN modif
			
			sTemp += "FROM dual "
				// Condition pour ne faire l'INSERT que si l'OT est présente
				+ "WHERE EXISTS ( SELECT no_ot FROM ot WHERE no_ot = " + myWTSLine.get_noOt() + " AND agence_ot = ''" + myWTSLine.get_agenceOt() + "'' ) ";

			sUpdateOrderItem = sSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
			
			// Ajout d'une trace pour identifier la ligne en erreur quand erreur
			iNbSqlOrder ++;
			sUpdateOrder += "Trace('Ordre SQL # " + iNbSqlOrder 
				+ " pour commande " + myWTSLine.get_noLigComm() + " / " + myWTSLine.get_agenceEnlevement() + "');\r\n";
			
			sUpdateOrder += sUpdateOrderItem;
			
			sUpdateOrder += "\r\n";
			
			// Ajout du SAV MLV-CFM comme dans Spot
			// On ne vérifie pas qu'un SAV MLV-CFM n'existe pas avant : dans le processus PREVA, il n'y a pas de MLV-CFM auparavant
			// On ajoute uniquement pour les Réponses OUI (pour contrôler un retrait accidentel dans Spot)
			// ****************************************************************************
			if ( myWTSLine.get_reponse().equals(TrsPrevaConstant.REP_OUI) ) 
			{
				myLog.debug("INSERT INTO histosav pour MLV|CFM");

				sTemp = "INSERT INTO histosav ( no_ot, agence_ot, code_situation, code_justification, libelle, champs8, "
					+ "commentaire, histosav_date, heure, date_rdv ) "
						
					//+ "VALUES ( "
					+ "SELECT "
					
					+ myWTSLine.get_noOt() + ", "
					+ "''" + myWTSLine.get_agenceOt() + "'', "
					+ "''MLV'', "
					+ "''CFM'', "
					;
				
				sReponse = "Suite reponse web = " + myWTSLine.get_reponse();
					
				// En fonction de la réponse, le code justification change
				sTemp += "''" + sReponse + "'', "
					;
				
				sTemp += "''PREVA'', "
					+ "''" + sReponse + "'', "
					+ "to_date( ''" + myWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // histosav_date
					+ "to_date( ''" + myWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // heure
					;
				
				// La date de rendez-vous doit avoir l'heure car il s'agit d'un rendez-vous conf avec heure (comme dans Spot)
				// Faut-il l'heure lorsque c'est un MLV-CFM ?
				// TODO 03/02/2019 <<<<<<<<<<<<<<<<<<<<   - Utiliser nouvelle zone qui contient la date et l'heure du rendez-vous (celle fournie dans SpotToWeb)
				//sTemp += "( " + sSqlDateRdv.replaceFirst("@@@NO_OP@@@", myWTSLine.get_noOp())
				//		.replaceFirst("@@@NO_OT@@@", myWTSLine.get_noOt()).replaceFirst("@@@NO_PLAN@@@", myWTSLine.get_noPlan())
				sTemp += "to_date( ''" + myWTSLine.get_datePrevLivraison().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ) "
					;
				
				sTemp += "FROM dual "
					// Condition pour ne faire l'INSERT que si l'OT est présente
					+ "WHERE EXISTS ( SELECT no_ot FROM ot WHERE no_ot = " + myWTSLine.get_noOt() + " AND agence_ot = ''" + myWTSLine.get_agenceOt() + "'' ) ";
	
				sUpdateOrderItem = sSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
				
				// Ajout d'une trace pour identifier la ligne en erreur quand erreur
				iNbSqlOrder ++;
				sUpdateOrder += "Trace('Ordre SQL # " + iNbSqlOrder
						+ " pour commande " + myWTSLine.get_noLigComm() + " / " + myWTSLine.get_agenceEnlevement() + "');\r\n";
				sUpdateOrder += sUpdateOrderItem;
				sUpdateOrder += "\r\n";
			}
			
			// SAV RST-INS : livraison ascenseur ou  escalier
			// ----------------------------------------------
			sUpdateOrderItem = this.getHistoSavSqlInsert(myWTSLine, sSqlDirectTemplate, iNbSqlOrder, "LIVASCESC");
			
			if ( ! sUpdateOrderItem.equals("") )
			{
				sUpdateOrder += sUpdateOrderItem;
				iNbSqlOrder ++;
				
			}

			/*
			if ( myWTSLine.get_reponseComplement().toUpperCase().indexOf("LIVASC") > -1 || myWTSLine.get_reponseComplement().toUpperCase().indexOf("LIVESC") > -1 )
			{
				sValeur = "ascenseur";
				if ( myWTSLine.get_reponseComplement().toUpperCase().indexOf("LIVESC") > -1 ) { sValeur = "escalier"; }
				
				myLog.debug("INSERT INTO histosav pour RST-INS " + myWTSLine.get_reponseComplement().toUpperCase() + " (livraison " + sValeur + ")");
				sReponse = "Reponse web = livraison par " + sValeur.toUpperCase() + " à l'étage " + myWTSLine.get_etage();

				// Mise à jour du code pour prendre en compte une OT disparu pendant le délai de prise en compte de l'INSERT
				// Pour éviter un plantage du code qui stoppe la suite du script SQL
				sTemp = "INSERT INTO histosav ( no_ot, agence_ot, code_situation, code_justification, libelle, champs8, commentaire, histosav_date, heure, date_rdv ) "
					+ "SELECT " + myWTSLine.get_noOt() + ", "
					+ "''" + myWTSLine.get_agenceOt() + "'', " // Au lieu de .get_agenceOt() = identique
					+ "''RST'', "
					+ "''INS'', "
					;
				
				sTemp += "''PREVA'', "
					+ "''" + sReponse + "'', "
					+ "to_date( ''" + myWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // histosav_date
					+ "to_date( ''" + myWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // heure
					+ "to_date( ''" + myWTSLine.get_datePrevLivraison().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ) "
					;
				
				sTemp += "FROM dual "
					// Condition pour ne faire l'INSERT que si l'OT est présente
					+ "WHERE EXISTS ( SELECT no_ot FROM ot WHERE no_ot = " + myWTSLine.get_noOt() + " AND agence_ot = ''" + myWTSLine.get_agenceOt() + "'' ) ";

				sUpdateOrderItem = sSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
				
				// Ajout d'une trace pour identifier la ligne en erreur quand erreur
				iNbSqlOrder ++;
				sUpdateOrder += "Trace('Ordre SQL # " + iNbSqlOrder 
					+ " pour commande " + myWTSLine.get_noLigComm() + " / " + myWTSLine.get_agenceEnlevement() + "');\r\n";
				sUpdateOrder += sUpdateOrderItem;
				sUpdateOrder += "\r\n";
			}
			*/
			
			// Livraison par l'ascenseur
			// -------------------------
			if ( myWTSLine.get_reponseComplement().toUpperCase().indexOf("LIVASC") > -1 )
			{
				myLog.debug("Prise en compte livraison Ascenseur : UPDATE ligcomm");

				// 25/06/2020 - Simplification contenu + Mise à jour ligcomm.champs3
				sTemp = "UPDATE ligcomm "
					//+ "SET commentaire = commentaire || ''|Ascenseur(Oui)|Etage(" + myWTSLine.get_etage().trim() + ")'' "
					+ "SET commentaire = commentaire || ''|Ascenseur(" + myWTSLine.get_etage().trim() + ")'', "
					+ "champs3 = Substr(''Ascenseur(" + myWTSLine.get_etage().trim() + ")|'' || Nvl(champs3, '' ''), 1, 35) "
					+ "WHERE no_ligne_commande = " + myWTSLine.get_noLigComm() + " "
					+ "AND agence = ''" + myWTSLine.get_agenceEnlevement() + "'' "
					;

				sUpdateOrderItem = sSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
				
				// Ajout d'une trace pour identifier la ligne en erreur quand erreur
				iNbSqlOrder ++;
				sUpdateOrder += "Trace('Ordre SQL # " + iNbSqlOrder 
					+ " pour commande " + myWTSLine.get_noLigComm() + " / " + myWTSLine.get_agenceEnlevement() + "');\r\n";
				sUpdateOrder += sUpdateOrderItem;
				sUpdateOrder += "\r\n";
			}
			
			// Livraison par l'escalier
			// ------------------------
			if ( myWTSLine.get_reponseComplement().toUpperCase().indexOf("LIVESC") > -1 )
			{
				myLog.debug("Prise en compte livraison Escalier : UPDATE ligcomm");

				// 25/06/2020 - Simplification contenu
				sTemp = "UPDATE ligcomm "
					+ "SET commentaire = commentaire || ''|Escalier(" + myWTSLine.get_etage().trim() + ")'', "
					+ "champs3 = Substr(''Escalier(" + myWTSLine.get_etage().trim() + ")|'' || Nvl(champs3, '' ''), 1, 35) "
					+ "WHERE no_ligne_commande = " + myWTSLine.get_noLigComm() + " "
					+ "AND agence = ''" + myWTSLine.get_agenceEnlevement() + "'' "
					;

				sUpdateOrderItem = sSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
				
				// Ajout d'une trace pour identifier la ligne en erreur quand erreur
				iNbSqlOrder ++;
				sUpdateOrder += "Trace('Ordre SQL # " + iNbSqlOrder 
					+ " pour commande " + myWTSLine.get_noLigComm() + " / " + myWTSLine.get_agenceEnlevement() + "');\r\n";
				sUpdateOrder += sUpdateOrderItem;
				sUpdateOrder += "\r\n";
			}
			
			// SAV RST-INS : numéro de téléphone livraison
			// -------------------------------------------
			sUpdateOrderItem = this.getHistoSavSqlInsert(myWTSLine, sSqlDirectTemplate, iNbSqlOrder, "TELLIV");
			
			if ( ! sUpdateOrderItem.equals("") )
			{
				sUpdateOrder += sUpdateOrderItem;
				iNbSqlOrder ++;
				
			}
			
			/*
			if ( myWTSLine.get_reponseComplement().toUpperCase().indexOf("TELLIV") > -1 )
			{
				myLog.debug("INSERT INTO histosav pour RST-INS TELLIV (numero telephone livraison)");
				sReponse = "Reponse web = numéro de téléphone pour le contact à la livraison = " + myWTSLine.get_numTelLivraison();

				// Mise à jour du code pour prendre en compte une OT disparu pendant le délai de prise en compte de l'INSERT
				// Pour éviter un plantage du code qui stoppe la suite du script SQL
				sTemp = "INSERT INTO histosav ( no_ot, agence_ot, code_situation, code_justification, libelle, champs8, commentaire, histosav_date, heure, date_rdv ) "
					+ "SELECT " + myWTSLine.get_noOt() + ", "
					+ "''" + myWTSLine.get_agenceOt() + "'', "
					+ "''RST'', "
					+ "''INS'', "
					;
				
				sTemp += "''PREVA'', "
					+ "''" + sReponse + "'', "
					+ "to_date( ''" + myWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // histosav_date
					+ "to_date( ''" + myWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // heure
					+ "to_date( ''" + myWTSLine.get_datePrevLivraison().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ) "
					;
				
				sTemp += "FROM dual "
					// Condition pour ne faire l'INSERT que si l'OT est présente
					+ "WHERE EXISTS ( SELECT no_ot FROM ot WHERE no_ot = " + myWTSLine.get_noOt() + " AND agence_ot = ''" + myWTSLine.get_agenceOt() + "'' ) ";

				sUpdateOrderItem = sSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
				
				// Ajout d'une trace pour identifier la ligne en erreur quand erreur
				iNbSqlOrder ++;
				sUpdateOrder += "Trace('Ordre SQL # " + iNbSqlOrder 
					+ " pour commande " + myWTSLine.get_noLigComm() + " / " + myWTSLine.get_agenceEnlevement() + "');\r\n";
				sUpdateOrder += sUpdateOrderItem;
				sUpdateOrder += "\r\n";
			}
			*/
			
			// NOUVEAU numéro de téléphone livraison
			// -------------------------------------
			if ( myWTSLine.get_reponseComplement().toUpperCase().indexOf("TELLIV") > -1 )
			{
				myLog.debug("Prise en compte nouveau numero de telephone destinataire sur LIGCOMM");

				sTemp = "UPDATE ligcomm "
					+ "SET commentaire = commentaire || ''|AncienNumTel(" + myWTSLine.get_numTelLivraisonOld().trim() + ")'', "
					
					// Mise à jour de champs7 : on considère que le numéro saisi est un numéro de portable 
					//+ "telephone_destinataire = ''" + myWTSLine.get_numTelLivraison().trim() + "'' "
					+ "champs7 = ''" + myWTSLine.get_numTelLivraison().trim() + "'' "
					
					+ "WHERE no_ligne_commande = " + myWTSLine.get_noLigComm() + " " 
					+ "AND agence = ''" + myWTSLine.get_agenceEnlevement() + "'' "
					;

				sUpdateOrderItem = sSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
				
				// Ajout d'une trace pour identifier la ligne en erreur quand erreur
				iNbSqlOrder ++;
				sUpdateOrder += "Trace('Ordre SQL # " + iNbSqlOrder 
					+ " pour commande " + myWTSLine.get_noLigComm() + " / " + myWTSLine.get_agenceEnlevement() + "');\r\n";
				sUpdateOrder += sUpdateOrderItem;
				sUpdateOrder += "\r\n";
			}
			
			// SAV RST-INS : nouvelle adresse livraison
			// -------------------------------------------
			sUpdateOrderItem = this.getHistoSavSqlInsert(myWTSLine, sSqlDirectTemplate, iNbSqlOrder, "COORD");
			
			if ( ! sUpdateOrderItem.equals("") )
			{
				sUpdateOrder += sUpdateOrderItem;
				iNbSqlOrder ++;
				
			}
			
			/*
			if ( myWTSLine.get_reponseComplement().toUpperCase().indexOf("COORD") > -1 )
			{
				myLog.debug("INSERT INTO histosav pour RST-INS COORD (nouvelle adresse livraison)");
				sReponse = "Reponse web = NOUVELLE adresse de livraison a RECUPERER aupres du Destinataire";

				// Mise à jour du code pour prendre en compte une OT disparu pendant le délai de prise en compte de l'INSERT
				// Pour éviter un plantage du code qui stoppe la suite du script SQL
				sTemp = "INSERT INTO histosav ( no_ot, agence_ot, code_situation, code_justification, libelle, champs8, commentaire, histosav_date, heure, date_rdv ) "
					+ "SELECT " + myWTSLine.get_noOt() + ", "
					+ "''" + myWTSLine.get_agenceOt() + "'', "
					+ "''RST'', "
					+ "''INS'', "
					;
				
				sTemp += "''PREVA'', "
					+ "''" + sReponse + "'', "
					+ "to_date( ''" + myWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // histosav_date
					+ "to_date( ''" + myWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // heure
					+ "to_date( ''" + myWTSLine.get_datePrevLivraison().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ) "
					;
				
				sTemp += "FROM dual "
					// Condition pour ne faire l'INSERT que si l'OT est présente
					+ "WHERE EXISTS ( SELECT no_ot FROM ot WHERE no_ot = " + myWTSLine.get_noOt() + " AND agence_ot = ''" + myWTSLine.get_agenceOt() + "'' ) ";

				sUpdateOrderItem = sSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
				
				// Ajout d'une trace pour identifier la ligne en erreur quand erreur
				iNbSqlOrder ++;
				sUpdateOrder += "Trace('Ordre SQL # " + iNbSqlOrder 
					+ " pour commande " + myWTSLine.get_noLigComm() + " / " + myWTSLine.get_agenceEnlevement() + "');\r\n";
				sUpdateOrder += sUpdateOrderItem;
				sUpdateOrder += "\r\n";
			}
			*/
			
			// SAV RST-INS : livraison inaccessible
			// ------------------------------------
			sUpdateOrderItem = this.getHistoSavSqlInsert(myWTSLine, sSqlDirectTemplate, iNbSqlOrder, "INACCE");
			
			if ( ! sUpdateOrderItem.equals("") )
			{
				sUpdateOrder += sUpdateOrderItem;
				iNbSqlOrder ++;
				
			}
			
			/*
			if ( myWTSLine.get_reponseComplement().toUpperCase().indexOf("INACCE") > -1 )
			{
				myLog.debug("INSERT INTO histosav pour RST-INS INACCE (livraison inaccessible)");
				sReponse = "Reponse web = livraison INACCESSIBLE";

				// Mise à jour du code pour prendre en compte une OT disparu pendant le délai de prise en compte de l'INSERT
				// Pour éviter un plantage du code qui stoppe la suite du script SQL
				sTemp = "INSERT INTO histosav ( no_ot, agence_ot, code_situation, code_justification, libelle, champs8, commentaire, histosav_date, heure, date_rdv ) "
					+ "SELECT " + myWTSLine.get_noOt() + ", "
					+ "''" + myWTSLine.get_agenceOt() + "'', "
					+ "''RST'', "
					+ "''INS'', "
					;
				
				sTemp += "''PREVA'', "
					+ "''" + sReponse + "'', "
					+ "to_date( ''" + myWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // histosav_date
					+ "to_date( ''" + myWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // heure
					+ "to_date( ''" + myWTSLine.get_datePrevLivraison().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ) "
					;
				
				sTemp += "FROM dual "
					// Condition pour ne faire l'INSERT que si l'OT est présente
					+ "WHERE EXISTS ( SELECT no_ot FROM ot WHERE no_ot = " + myWTSLine.get_noOt() + " AND agence_ot = ''" + myWTSLine.get_agenceOt() + "'' ) ";

				sUpdateOrderItem = sSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
				
				// Ajout d'une trace pour identifier la ligne en erreur quand erreur
				iNbSqlOrder ++;
				sUpdateOrder += "Trace('Ordre SQL # " + iNbSqlOrder 
					+ " pour commande " + myWTSLine.get_noLigComm() + " / " + myWTSLine.get_agenceEnlevement() + "');\r\n";
				sUpdateOrder += sUpdateOrderItem;
				sUpdateOrder += "\r\n";
			}
			*/
			
			// Mise à jour de P_OP dans tous les cas de réponse
			// Dans le cas d'une annulation, il faut vider le contenu
			// ****************************************************************************
			myLog.debug("UPDATE P_OP");

			sTemp = "UPDATE P_OP set CHAMPS7_OP = "
				+ ( myWTSLine.get_reponse().equals(TrsPrevaConstant.REP_ANNULATION) ? "''''" : "''PREVA=" + myWTSLine.get_reponse() + "'' " )
				;
			
			// Livraison par l'ascenseur
			// -------------------------
			if ( myWTSLine.get_reponseComplement().toUpperCase().indexOf("LIVASC") > -1 )
			{
				// On indique l'étage sur P_OP pour que cela apparaisse sur la feuille de tournée (document papier remis au conducteur)
				myLog.debug("Prise en compte livraison Ascenseur : UPDATE p_op");

				// 25/06/2020 - Simplification contenu
				sTemp += ", champs2_OP = Substr(Trim(''Ascenseur(" + myWTSLine.get_etage().trim() + ")|'' || champs2_OP), 1, 50) "
					;
			}	
			
			// Livraison par l'escalier
			// ------------------------
			if ( myWTSLine.get_reponseComplement().toUpperCase().indexOf("LIVESC") > -1 )
			{
				// On indique l'étage sur P_OP pour que cela apparaisse sur la feuille de tournée (document papier remis au conducteur)
				myLog.debug("Prise en compte livraison Escalier : UPDATE p_op");

				// 25/06/2020 - Simplification contenu
				sTemp += ", champs2_OP = Substr(Trim(''Escalier(" + myWTSLine.get_etage().trim() + ")|'' || champs2_OP), 1, 50) "
					;
			}
			
			sTemp += "WHERE no_op = " + myWTSLine.get_noOp() + " "
				+ "AND no_plan = " + myWTSLine.get_noPlan() + " "
				+ "AND no_ot = " + myWTSLine.get_noOt() + " "
				;

			sUpdateOrderItem = sSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
			
			// Ajout d'une trace pour identifier la ligne en erreur quand erreur
			iNbSqlOrder ++;
			sUpdateOrder += "Trace('Ordre SQL # " + iNbSqlOrder 
				+ " pour commande " + myWTSLine.get_noLigComm() + " / " + myWTSLine.get_agenceEnlevement() + "');\r\n";
			
			sUpdateOrder += sUpdateOrderItem;
			
			sUpdateOrder += "\r\n";
			
			// Mise à jour de LIGCOMM.CHAMPS3 pour les réponses = NON avec nombre de réponses négatives à la limite
			// Aussi pour les sans réponses
			// VERIFIER que la commande n'est pas facturée !!
			myLog.debug("Nb reponses negatives = " + myWTSLine.get_nbReponsesNegatives());
			
			if ( ( myWTSLine.get_reponse().equals(TrsPrevaConstant.REP_NON) || myWTSLine.get_reponse().equals(TrsPrevaConstant.REP_SANS_REPONSE) )
				&& myWTSLine.get_nbReponsesNegatives() >= iNbMaxRepNegatives 
				)
			{
				myLog.info("UPDATE LigComm.Champs3 : trop de reponses negatives successives : limite = " + iNbMaxRepNegatives);
				
				sTemp = "UPDATE LigComm SET Champs3 = "
					+ "CASE WHEN ( Champs3 IS NULL OR Champs3 NOT LIKE ''%PREVA OFF%'' ) THEN "
					+ "  CASE WHEN Champs3 IS NOT NULL AND Length(Champs3) > 0 "
					+ "  THEN " 
					+ "    CASE WHEN length(''PREVA OFF|'' || Trim(champs3)) > 35 THEN Substr(''PREVA OFF|'' || Trim(champs3), 1, 35) ELSE ''PREVA OFF|'' || Trim(champs3) END "
					+ "  ELSE "
					+ "    ''PREVA OFF'' "
					+ "  END "
					+ "END "
					+ "WHERE no_ligne_commande = " + myWTSLine.get_noLigComm() + " "
					+ "AND agence = ''" + myWTSLine.get_agenceEnlevement() + "'' "
					+ "AND no_facture IS NULL " // Commande non facturée
					;

				sUpdateOrderItem = sSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
					
				// Ajout d'une trace pour identifier la ligne en erreur quand erreur
				iNbSqlOrder ++;
				sUpdateOrder += "Trace('Ordre SQL # " + iNbSqlOrder 
					+ " pour commande " + myWTSLine.get_noLigComm() + " / " + myWTSLine.get_agenceEnlevement() + "');\r\n";
					
				sUpdateOrder += sUpdateOrderItem;
					
				sUpdateOrder += "\r\n";
				
				// 11/05/2021 - Insertion RST-INS pour mémoriser PREVA OFF
				// Mise à jour du code pour prendre en compte une OT disparue pendant le délai de prise en compte de l'INSERT
				// Pour éviter un plantage du code qui stoppe la suite du script SQL
				sTemp = "INSERT INTO histosav ( no_ot, agence_ot, code_situation, code_justification, libelle, champs8, "
					+ "commentaire, histosav_date, heure, date_rdv ) "
						
					+ "SELECT " + myWTSLine.get_noOt() + ", "
					+ "''" + myWTSLine.get_agenceEnlevement() + "'', "
					+ "''RST'', "
					+ "''INS'', "
					+ "''PREVA OFF : Nb reponses negatives = " + myWTSLine.get_nbReponsesNegatives() + "'', "
					+ "''PREVA'', "
					+ "''PREVA OFF : Nb reponses negatives = " + myWTSLine.get_nbReponsesNegatives() + "'', "
					+ "to_date( ''" + myWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // histosav_date
					+ "to_date( ''" + myWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // heure
					+ "to_date( ''" + myWTSLine.get_datePrevLivraison().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ) "
					;
				
				sTemp += "FROM dual "
					// Condition pour ne faire l'INSERT que si l'OT est présente
					+ "WHERE EXISTS ( SELECT no_ot FROM ot WHERE no_ot = " + myWTSLine.get_noOt() + " AND agence_ot = ''" + myWTSLine.get_agenceOt() + "'' ) ";

				sUpdateOrderItem = sSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
				
				// Ajout d'une trace pour identifier la ligne en erreur quand erreur
				iNbSqlOrder ++;
				sUpdateOrder += "Trace('Ordre SQL # " + iNbSqlOrder 
					+ " pour commande " + myWTSLine.get_noLigComm() + " / " + myWTSLine.get_agenceEnlevement() + "');\r\n";
				
				sUpdateOrder += sUpdateOrderItem;
				
				sUpdateOrder += "\r\n";
				// 11/05/2021 - FIN modif
			}
			// 28/02/2019 - FIN modif
			
			// En parallèle de la mise à jour, il faut préparer le rapport qui doit être généré par mail à chaque exploitation
			_WTSRManager.init(myWTSLine.get_agenceLivraison(), 
				myWTSLine.get_noLigComm(),
				myWTSLine.get_noPlan(),
				myWTSLine.get_reponse(),
				myWTSLine.get_dateReponse(),
				myWTSLine.get_typeAcces(),
				myWTSLine.get_nbReponsesNegatives(),
				myWTSLine.get_loginWeb(),
				myWTSLine.get_reponseComplement(),
				myWTSLine.get_typePreva(),
				myWTSLine.get_typeLdd(),
				myWTSLine.get_etage(),
				myWTSLine.get_numTelLivraison(),
				myWTSLine.get_numTelLivraisonOld()
				);
		}
		
		// Si une mise à jour à faire en base de données, on génère le fichier pour SpotBatch
		mySP.writeFile("webToSpot.update", sUpdateOrder, sUpdateTemplate, "");
		
		// Exécution du script
		mySP.execute("webToSpot.update");
		
		myLog.info(JavaUtil.getMethodeName() + " - FIN");
	}
	
	
	public WebToSpotReportManagerLot2 getWTSRManager()
	{
		return this._WTSRManager;
	}
	
	
	public int getFile() throws TrsException
	{
		SSHFTPClient 				mySFtpService = null;
		SecureFileTransferClient 	mySFtpClient = null;
		
        FileUtil 			myFileUtil = new FileUtil();
        
        int		iNbFichiers = 0;

        //String	sDirArchive = "";
        String	sDirFileAttente = "";
        //String	sFileName = "";
        String	sPropKey = "";
        
        String	sFtpHost 		= "ftp.trs49.fr";
        String	sFtpLogin 		= "testjnc";
        String	sFtpPassword 	= "cnjtset";
        String	sFtpPort 		= "21";
        String	sFtpRepertoire 	= "data"; // Répertoire dans lequel il faut lire 

		if ( this.myPC == null )
			throw new TrsException("Le fichier de proprietes N'est PAS initialise : traitement IMPOSSIBLE !  Appeler le constructeur avant d'utiliser cette methode");
		
		
		// Répertoire de stockage des fichiers à récupérer
		// ********************************************************************************************************************
		sDirFileAttente = this.myPC.getProperty(ProprieteCommune.dir_in_source_propKey, "");
		
		if ( sDirFileAttente.equals("") )
		{
			myLog.fatal(this.myPC.getMessageErreur(1, ProprieteCommune.dir_in_source_propKey));
			return -1;
		}
		
		// Création du répertoire de stockage
		myFileUtil.makeDir(sDirFileAttente);

		
		myLog.info("STOCKAGE dans [" + sDirFileAttente + "]");
        
        // Boucle sur le répertoire <fileAttente>
        //String sFichiers[] = myFileUtil.getFichiers(sDirFileAttente, "");
        

        // Lecture des fichiers sur le serveur FTP PREVA
        // *********************************************************************************************
        // Si environnement de production ou de recette, on récupère les informations dans le fichier de propriétés
		if ( this.myPC.getEnvironnement().equals("prod") || this.myPC.getEnvironnement().equals("rec") )
		{
			// Host FTP
			sPropKey = "webToSpot.ftp.host";
			sPropKey = this.myPC.getEnvironnement() + "." + sPropKey;

            sFtpHost = this.myPC.getProperty(sPropKey, "");
	            
            if ( sFtpHost.equals("") )
            	throw new TrsException(this.myPC.getMessageErreur(1, sPropKey));
	            	
            // Login FTP
			sPropKey = "webToSpot.ftp.login";
			sPropKey = this.myPC.getEnvironnement() + "." + sPropKey;

            sFtpLogin = this.myPC.getProperty(sPropKey, "");
	            
            if ( sFtpLogin.equals("") )
            	throw new TrsException(this.myPC.getMessageErreur(1, sPropKey));

            // Password FTP
			sPropKey = "webToSpot.ftp.password";
			sPropKey = this.myPC.getEnvironnement() + "." + sPropKey;
            
            sFtpPassword = this.myPC.getProperty(sPropKey, "");
	            
            if ( sFtpPassword.equals("") )
            	throw new TrsException(this.myPC.getMessageErreur(1, sPropKey));

            // Port FTP
			sPropKey = "webToSpot.ftp.port";
			sPropKey = this.myPC.getEnvironnement() + "." + sPropKey;
            
            sFtpPort = this.myPC.getProperty(sPropKey, "");
	            
            if ( sFtpPort.equals("") )
            	throw new TrsException(this.myPC.getMessageErreur(1, sPropKey));

            // Répertoire FTP
			sPropKey = "webToSpot.ftp.repertoire";
			sPropKey = this.myPC.getEnvironnement() + "." + sPropKey;
            
			sFtpRepertoire = this.myPC.getProperty(sPropKey, "");
	            
            if ( sFtpRepertoire.equals("") )
            	throw new TrsException(this.myPC.getMessageErreur(1, sPropKey));
		}
		
		myLog.info("Tentative lecture FTP avec [" + sFtpHost + ";" + sFtpPort + ";" + sFtpLogin + ";" + sFtpPassword + "]");
		
		try 
		{ 
            myLog.info("Creating SFTP client");
            mySFtpClient = new SecureFileTransferClient();
		}
		catch ( Exception e )
		{
			new TrsException("ERREUR lors de la creation du service FTP " + " : " + e.toString());
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

            myLog.info("  - Getting current directory listing");
            FTPFile[] myFTPFile = mySFtpClient.directoryList(".");
            
			myLog.info("");
			myLog.info("  - Nombre de fichiers trouves = " + myFTPFile.length);
			
			// Une premiere boucle pour lister les fichiers à traiter
            for ( int i = 0 ; i < myFTPFile.length; i ++ ) 
				myLog.info("    - Fichier [" + myFTPFile[i].getName() + "]");
			
			myLog.info("");
            
            for ( int i = 0 ; i < myFTPFile.length; i ++ ) 
            {
            	if ( myFTPFile[i].getName().equals(".") | myFTPFile[i].getName().equals("..") )
            	{
            		// On ne fait rien
            	}
            	else
            	{
					myLog.info("      - Fichier [" + myFTPFile[i].getName() + "] en cours de download");
					mySFtpClient.downloadFile(sDirFileAttente + "/" + myFTPFile[i].getName(), myFTPFile[i].getName());
					myLog.info("        - Fichier recupere");
					
					myLog.info("        - Fichier sur le serveur FTP en cours de suppression");
					mySFtpClient.deleteFile(myFTPFile[i].getName());
					myLog.info("        - Fichier supprime sur le serveur FTP");
					
					iNbFichiers ++;
            	}
            }
            
			myLog.info("");
			myLog.info("  - Nombre de fichiers traites = " + iNbFichiers);

            // Shut down client
            myLog.info("  - ARRET de la connexion FTP");
            mySFtpClient.disconnect();

		} 
	    catch ( Exception e ) 
	    {
			myLog.fatal("ERREUR lors de la recuperation par FTP des fichiers PREVA "
				+ ( mySFtpClient == null ? "" :
				"(" + mySFtpClient.getRemoteHost() + " / " + mySFtpClient.getRemotePort() + ")"
				)
				+ " : " + e.toString());
	    }
		
		return iNbFichiers;
	}
	
	private String getHistoSavSqlInsert(WebToSpotLineLot2 aWTSLine, String asSqlDirectTemplate, int aiNbSqlOrder, String asReponseComplement)
	{
		String 	sReponse = "";
		String 	sReponseCourte = "";
		String 	sTemp = "";
		String 	sUpdateOrder = "";
		String 	sUpdateOrderItem = "";
		String	sValeur = "";
		
		// 07/10/2020
		boolean bDateRdvNull = false;
		
		if ( asReponseComplement.equals("COORD") && aWTSLine.get_reponseComplement().toUpperCase().indexOf("COORD") > -1 )
		{
			myLog.debug("INSERT INTO histosav pour RST-INS COORD (nouvelle adresse livraison)");
			sReponse = "Reponse web = NOUVELLE adresse de livraison a RECUPERER aupres du Destinataire";
			sReponseCourte = "PREVA = NOUVELLE adresse livraison a RECUPERER aupres du Destinataire";
			
			// 07/10/2020 - Date RDV
			bDateRdvNull = true;
		}
		else if ( asReponseComplement.equals("INACCE") && aWTSLine.get_reponseComplement().toUpperCase().indexOf("INACCE") > -1 )
		{
			myLog.debug("INSERT INTO histosav pour RST-INS INACCE (livraison inaccessible)");
			sReponse = "Reponse web = livraison INACCESSIBLE";
			sReponseCourte = "PREVA = livraison INACCESSIBLE";
			
			// 07/10/2020 - Date RDV
			bDateRdvNull = true;
		}
		else if ( asReponseComplement.equals("LIVASCESC") && ( aWTSLine.get_reponseComplement().toUpperCase().indexOf("LIVASC") > -1 || aWTSLine.get_reponseComplement().toUpperCase().indexOf("LIVESC") > -1 ) )
		{
			sValeur = "ascenseur";
			if ( aWTSLine.get_reponseComplement().toUpperCase().indexOf("LIVESC") > -1 ) { sValeur = "escalier"; }
			
			myLog.debug("INSERT INTO histosav pour RST-INS " + aWTSLine.get_reponseComplement().toUpperCase() + " (livraison " + sValeur + ")");
			sReponse = "Reponse web = livraison par " + sValeur.toUpperCase() + ", Etage " + aWTSLine.get_etage();
			sReponseCourte = "PREVA = livraison par " + sValeur.toUpperCase() + ", Etage " + aWTSLine.get_etage();
			
			// 07/10/2020 - Date RDV
			bDateRdvNull = false;
		}
		else if ( asReponseComplement.equals("TELLIV") && aWTSLine.get_reponseComplement().toUpperCase().indexOf("TELLIV") > -1 )
		{
			myLog.debug("INSERT INTO histosav pour RST-INS TELLIV (numero telephone livraison)");
			sReponse = "Reponse web = nouveau numero telephone contact livraison = " + aWTSLine.get_numTelLivraison();
			sReponseCourte = "PREVA = nouveau numero telephone contact livraison = " + aWTSLine.get_numTelLivraison();
			
			// 07/10/2020 - Date RDV
			bDateRdvNull = false;
		}
		
		
		if ( sReponse.equals("") )
			return "";

		// Mise à jour du code pour prendre en compte une OT disparue pendant le délai de prise en compte de l'INSERT
		// Pour éviter un plantage du code qui stoppe la suite du script SQL
		sTemp = "INSERT INTO histosav ( no_ot, agence_ot, code_situation, code_justification, libelle, champs8, commentaire, histosav_date, heure, date_rdv ) "
			+ "SELECT " + aWTSLine.get_noOt() + ", "
			+ "''" + aWTSLine.get_agenceOt() + "'', "
			+ "''RST'', "
			+ "''INS'', "
			+ "''" + sReponseCourte + "'', " // Cette zone est visible dans Wintrans = Description
			;
		
		sTemp += "''PREVA'', "
			+ "''" + sReponse + "'', " // Cette zone n'est pas visible dans Wintrans
			+ "to_date( ''" + aWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // histosav_date
			+ "to_date( ''" + aWTSLine.get_dateReponse().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ), " // heure
			;
		
		// 07/10/2020 - Date_RDV NULL
		if ( bDateRdvNull )
			sTemp += "NULL ";
		else
			sTemp += "to_date( ''" + aWTSLine.get_datePrevLivraison().replaceAll("\"", "") + "'', ''yyyy-mm-dd HH24:MI:SS'' ) ";
		// 07/10/2020 - FIN modif
		
		sTemp += "FROM dual "
			// Condition pour ne faire l'INSERT que si l'OT est présente
			+ "WHERE EXISTS ( SELECT no_ot FROM ot WHERE no_ot = " + aWTSLine.get_noOt() + " AND agence_ot = ''" + aWTSLine.get_agenceOt() + "'' ) ";

		sUpdateOrderItem = asSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
		
		// Ajout d'une trace pour identifier la ligne en erreur quand erreur
		aiNbSqlOrder ++;
		sUpdateOrder += "Trace('Ordre SQL # " + aiNbSqlOrder 
			+ " pour commande " + aWTSLine.get_noLigComm() + " / " + aWTSLine.get_agenceEnlevement() + "');\r\n";
		sUpdateOrder += sUpdateOrderItem;
		sUpdateOrder += "\r\n";
		
		return sUpdateOrder;
	}
}
