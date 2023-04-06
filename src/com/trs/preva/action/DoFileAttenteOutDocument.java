package com.trs.preva.action;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.Protocol;
import com.enterprisedt.net.ftp.SecureFileTransferClient;
import com.enterprisedt.net.ftp.WriteMode;

import com.trs.action.ProprieteCommune;

import com.trs.constant.TrsConstant;

import com.trs.exception.AppliException;
import com.trs.exception.TrsException;

import com.trs.preva.TrsPrevaConstant;
import com.trs.preva.metier.*;

import com.trs.utils.calendar.CalendarHelper;
import com.trs.utils.fileSystem.FileUtil;
import com.trs.utils.java.JavaUtil;
import com.trs.utils.string.StringUtil;

import com.trs.wintrans.dbAccess.DocumentAccess;
import com.trs.wintrans.dbAccess.LigCommAccess;
import com.trs.wintrans.dbAccess.SpotBatch;
import com.trs.wintrans.metier.AProprietes;
import com.trs.wintrans.metier.Document;

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
public class DoFileAttenteOutDocument 
{
	// 09/01/2023 - Création pour lot 5 PREVA : ajout de nouveaux documents
	
	// Log
	static final Logger myLog = LogManager.getLogger(DoFileAttenteOutDocument.class.getName());
	
    ProprieteCommune _myPC = null;
    
    //private String	_sTypeDocument = "";
    //private String  _sTypesDocument = "";
    
    private int		_iNbNC = 0;
    private int		_iNbPhotos = 0;
    private int		_iNbRecep = 0;
    private int		_iNbDocErreur = 0;
 
    
    public DoFileAttenteOutDocument(ProprieteCommune aPC) throws TrsException
    {
    	this._myPC = aPC;
    	
    	// TODO 09/01/2023 - A passer dans 1Password
		com.enterprisedt.util.license.License.setLicenseDetails("TRANSPORTROUTESERVICES", "371-3545-6633-501248");
    }

    /**
	 * <p>Transfert du type de document demandé</p>
     * 
     * @return -1 si erreur, -2 si erreur au transfert des fichiers , SINON le nombre de fichiers traités
     */
	public int generateFile() throws TrsException
	{
		Date 				myDate = new Date(System.currentTimeMillis());

		SimpleDateFormat	mySDF = null;
		
		int		iLimite = 0;
		int		iNbJours = 0;
		int		iNbSqlOrder = 0;
		
		String	sDateCreation = "";
		String	sDateExport = "";
		String	sDbName = "";
		String	sDbPropFileName = "";		
        String 	sDirConf = "";
        String	sPropKey = "";
		String	sRetour = "";
		String	sSqlDirectTemplate = "GetSession.SqlDirect('REQUETE', true);";
		String  sTarget = "";
		String	sTemp = "";
        String	sUpdateOrderDocumentSent = "";
		String	sUpdateOrder = "";
		String	sUpdateOrderItem = "";
		String	sUpdateTemplate = "";
		
		SpotBatch			mySB = null;
		SpotToWebDocument	mySTW = null;
		
		LigCommAccess	myLCAccess = null;
		DocumentAccess	myDocAccess = null;
		
		List<Document>	myDocList = null;
		List<Document>	myRecepList = null;
		
		
		// no_ref doit être un nombre...
		// Il faut stocker le nom du fichier dans VALEUR1_CHAINE et faire passer dateExport dans VALEUR2
		sUpdateOrderDocumentSent = "MERGE INTO a_proprietes aProp "
			+ "USING ( "
			+ "  SELECT "
			+ "    ''" + AProprietes.TABLE_REF_DOCUMENT + "'' AS table_ref, "
       		+ "    @NO_REF@ AS no_ref, " // NO_LIGNE_COMMANDE
       		+ "    ''" + AProprietes.NOM_TRS_PREVA_DOCUMENT + "'' AS nom, "
       		+ "    @VALEUR1_CHAINE@ AS valeur1_chaine " // Nom du document
       		//+ "    @VALEUR1_ENTIER@ AS valeur1_entier, " // NO_LIGNE_COMMANDE
       		//+ "    @VALEUR2_ENTIER@ AS valeur2_entier " // NO_OT
			+ "  FROM dual "
			+ "  ) virtuel on ( "
			+ "  aProp.table_ref = virtuel.table_ref "
			+ "  AND aProp.no_ref = virtuel.no_ref "
			+ "  AND aProp.nom = virtuel.nom "
			+ "  AND aProp.valeur1_chaine = virtuel.valeur1_chaine "
			//+ "  AND aProp.valeur2_entier = virtuel.valeur2_entier "
			+ "  ) "
			
			+ "WHEN MATCHED THEN "
			+ "UPDATE SET "

			// Date de prise en compte
			+ "VALEUR2_CHAINE = @VALEUR2_CHAINE@, "
			+ "VALEUR2_DATE = @VALEUR2_DATE@, "
			
			// Nom du fichier
			//+ "VALEUR1_CHAINE = @VALEUR1_CHAINE@, "
			
			// NO_LIGNE_COMMANDE
			//+ "VALEUR1_ENTIER = @VALEUR1_ENTIER@, "
			
			// NO_OT
			//+ "VALEUR2_ENTIER = @VALEUR2_ENTIER@, "
			
			// Commentaire pour documenter
			+ "VALEUR4_CHAINE = ''Interface PREVA : Document Photo, NC ou Récépissé Emargé'', "
			+ "VALEUR5_CHAINE = ''VALEUR2_CHAINE + VALEUR2_DATE = Date prise en compte | VALEUR1_CHAINE = no_ligne_commande'' "
			
			+ "WHEN NOT MATCHED THEN "
			+ "INSERT( "

			+ "TABLE_REF, NO_REF, NOM, "

			// Date de prise en compte
			+ "VALEUR2_CHAINE, VALEUR2_DATE, "
			
			// Nom du fichier
			+ "VALEUR1_CHAINE, "

			// NO_LIGNE_COMMANDE
			//+ "VALEUR1_ENTIER, "

			// NO_OT
			//+ "VALEUR2_ENTIER, "

			// Commentaire pour documenter
			+ "VALEUR4_CHAINE, "
			+ "VALEUR5_CHAINE "

			+ ") "

			+ "VALUES ("
			+ "''" + AProprietes.TABLE_REF_DOCUMENT + "'', "
       		+ "@NO_REF@, "
       		+ "''" + AProprietes.NOM_TRS_PREVA_DOCUMENT + "'', "

			// Date prise en compte
			+ "@VALEUR2_CHAINE@, "
       		+ "@VALEUR2_DATE@, "
			
			// Nom du fichier
			+ "@VALEUR1_CHAINE@, "
			
			// no_ligne_commande
			//+ "@VALEUR1_ENTIER@, "
			
			// no_ot
			//+ "@VALEUR2_ENTIER@, "
			
			// Commentaire pour documenter
			+ "''Interface PREVA : Document Photo, NC ou Récépissé Emargé'', "
			+ "''VALEUR2_CHAINE + VALEUR2_DATE = Date prise en compte | VALEUR1_CHAINE = no_ligne_commande'' "

			+ ") "
			;
		
		
		if ( this._myPC == null )
			throw new TrsException(JavaUtil.getMethodeFullName() + " - le fichier de proprietes N'est PAS initialise : traitement IMPOSSIBLE !  Appeler le constructeur avant d'utiliser cette methode");

		
        // Propriété décrivant le répertoire de configuration
		// ********************************************************************************************************************
        try { sDirConf = this._myPC.getDirConf(); }
        catch ( TrsException e )
        {
        	myLog.fatal(JavaUtil.getMethodeFullName() + " - " + e.toString());
        	return -1;
        }
        
        sDirConf = StringUtil.getDirPath(true, sDirConf);
        
        myLog.info("");
        myLog.info("Repertoire de configuration = " + sDirConf);
        
        
        // Date de création des documents à partir de laquelle on prend en compte les documents
		// ********************************************************************************************************************
		sPropKey = "document.dateCreation.dateMini";
		sDateCreation = this._myPC.getProperty(sPropKey, "", true);
		
		if ( sDateCreation.equals("") )
			throw new TrsException(JavaUtil.getMethodeFullName() + " : la propriete [" + this._myPC.getEnvironnement() + "." + sPropKey + "] doit exister "
				+ "dans le fichier de proprietes [" + this._myPC.getPropFileName() + "]"
				);
		
        myLog.info("");
        myLog.info(this._myPC.getEnvironnement() + "." + sPropKey + " = " + sDateCreation);
        

		// Nombre de jours dans le passé vis-à-vis de la date de création des documents
		// ********************************************************************************************************************
		sPropKey = "document.dateCreation.nbJours";
		iNbJours = Integer.parseInt(_myPC.getProperty(sPropKey, "0", true));
		
        myLog.info("");
        myLog.info(this._myPC.getEnvironnement() + "." + sPropKey + " = " + iNbJours);
        

		// Limite sur le nombre de documents à prendre en compte
		// ********************************************************************************************************************
		sPropKey = "document.limite";
		iLimite = Integer.parseInt(_myPC.getProperty(sPropKey, "0", true));
		
        myLog.info("");
        myLog.info(this._myPC.getEnvironnement() + "." + sPropKey + " = " + iLimite);
		
        
		// Connection à la base de données Wintrans
		// ********************************************************************************************************************
    	sDbName = "wintrans";
    	
    	// Fichier de propriétés décrivant la base de données
		sPropKey = "dbPropFileName";
		sDbPropFileName = this._myPC.getProperty(sPropKey, "");
		
		if ( sDbPropFileName.equals("") )
    	{
    		sRetour = "VERIFIER propriete [" + this._myPC.getKeyWithEnvironment(sPropKey) + "] dans " + this._myPC.getPropFileName();
			myLog.fatal(JavaUtil.getMethodeFullName() + " - " + sRetour);
			return -1;
    	}
		
		
		// On veut lire dans la base de données :
		// - Les commandes pour le récépissé émargé dans ligcomm.url_local
		// - Les DOCUMENTs pour les photos, les NCs et les Récépissés Emargés (ceux qui n'auraient pas être pris en compte dans Ligcomm - rotation si plus d'un récépissé
		//   émargé)
    	
		// Accès pour les Commandes
    	try { myLCAccess = new LigCommAccess(sDbPropFileName, sDbName); }
    	catch ( TrsException e )
    	{
    		sRetour = "ERREUR connection base de donnees [" + sDbName + "] pour les LIGCOMM : " + e.toString();
			myLog.fatal(JavaUtil.getMethodeFullName() + " - " + sRetour);
			return -1;
    	}
    	
		// Accès pour les Documents
    	try { myDocAccess = new DocumentAccess(sDbPropFileName, sDbName); }
    	catch ( TrsException e )
    	{
    		sRetour = "ERREUR connection base de donnees [" + sDbName + "] pour les LIGCOMM : " + e.toString();
			myLog.fatal(JavaUtil.getMethodeFullName() + " - " + sRetour);
			return -1;
    	}

    	
        // Lecture des Documents à prendre en compte (fichiers présents dans la table DOCUMENT)
    	// **************************************************************************************
		myLog.info("");
		myLog.info("LECTURE des Documents a prendre en compte");
		
		myDocList = myDocAccess.getDocumentsForPreva(sDateCreation, iNbJours, iLimite, this._myPC);
		
		
		// Lectures des récépissés émargés dans Ligcomm à prendre en compte
    	// **************************************************************************************
		myLog.info("");
		myLog.info("LECTURE des Recepisses Emarges a prendre en compte");
		
		myRecepList = myLCAccess.getRecepForPreva(sDateCreation, iNbJours, iLimite, this._myPC);
		
		// Ajout de ces éléments à la 1ère liste
		for ( Document myDocument : myRecepList )
			myDocList.add(myDocument);
		
    	if ( myDocList == null )
    	{
    		myLog.info("");
			myLog.info("  - AUCUN Document a prendre en compte");
    	}
    	else
    	{
    		myLog.info("");
			myLog.info("  - " + myDocList.size() + " Document(s) trouve(s)");
			
			if ( myDocList.size() > 0 )
			{
				// Les documents trouvés doivent être mis à disposition dans le répertoire pour transfert FTP à PREVA
				try { mySTW = new SpotToWebDocument(myDocList, this._myPC); }
				catch ( Exception e )
				{ 
					myLog.fatal(e.toString());
					return -2;
				}
			}
			
			
			// Mémorisation en base de données
			// *****************************************************************
			mySB = new SpotBatch(this._myPC, this._myPC.getEnvironnement());
			
			
			// Modèle de script SpotBatch
			// *****************************************************************
			sPropKey = "spotToWeb.update";
			
			try { sUpdateTemplate = mySB.getSpotBatchTemplate(sPropKey); }
			catch ( TrsException e )
			{
				throw new TrsException(JavaUtil.getMethodeFullName() + " - ERREUR a la lecture de [" + sPropKey + "] : " + e.toString());
			}
			
			// Marquer l'export sur chaque A_PROPRIETES
			// *************************************************************************
			mySDF = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			sDateExport = mySDF.format(myDate);
			
			myLog.info("");
			myLog.info("MEMORISATION des Documents pris en compte dans A_PROPRIETES");
			
	        for ( Document myDoc : myDocList )
	        {
	        	myLog.info("  - Document [" + myDoc.get_nomFichier() + "] pour la commande [" + myDoc.get_noLigneCommande() + "]");
	        	
	        	// Pour les fichiers non stockés en base de données, il faut vérifier que la copie a pu se faire
	        	if ( myDoc.get_fichier() == null && myDoc.get_notExistingOnDisk() )
	        	{
	        		this._iNbDocErreur ++;
		        	myLog.info("    - NON copie : ERREUR = " + myDoc.get_sValeur1());
		        	
		        	// TODO 20/01/2023 - FAIRE une notification
		        	//					 VERIFIER si la demande de régénération du document permet de régler le problème
		        	//					 Combien de fichiers dans ce cas ?
	        	}
	        	else
	        	{
		        	// Il faut prendre le nom du fichier sans le chemin (cas pour les récépissés émargés dans LIGCOMM)
					sTemp = sUpdateOrderDocumentSent;
					
					// TODO 19/01/2023 - no_ref doit être un nombre...
					//					 Il faut stocker le nom du fichier dans VALEUR1_CHAINE et faire passer dateExport dans VALEUR2
					sTemp = sTemp.replaceAll("@NO_REF@", 		 "" + myDoc.get_noLigneCommande());
					
					sTemp = sTemp.replaceAll("@VALEUR1_CHAINE@", "''" + StringUtil.getLastString(myDoc.get_nomFichier(), "\\\\") + "''");
					
					//sTemp = sTemp.replaceAll("@VALEUR1_ENTIER@", "" + myDoc.get_noLigneCommande());
					//sTemp = sTemp.replaceAll("@VALEUR2_ENTIER@", "" + myDoc.get_noOt());
					
					sTemp = sTemp.replaceAll("@VALEUR2_CHAINE@", "''" + sDateExport + "''");
					sTemp = sTemp.replaceAll("@VALEUR2_DATE@", "to_date(''" + sDateExport + "'', ''dd/mm/yyyy HH24:MI:SS'')");
					
					sUpdateOrderItem = sSqlDirectTemplate.replaceFirst("REQUETE", sTemp); 
					
					// Ajout d'une trace pour identifier la ligne en erreur quand erreur
					iNbSqlOrder ++;
					sUpdateOrder += "Trace('Ordre SQL # " + iNbSqlOrder + "');\r\n";
					
					sUpdateOrder += sUpdateOrderItem;
					
					sUpdateOrder += "\r\n";
					
					if ( myDoc.get_nomFichierPreva().toLowerCase().indexOf("nc") > -1 )
						this._iNbNC ++;
					
					if ( myDoc.get_nomFichierPreva().toLowerCase().indexOf("photo-") > -1 )
						this._iNbPhotos ++;
					
					if ( myDoc.get_nomFichierPreva().toLowerCase().indexOf("recep-") > -1 )
						this._iNbRecep ++;
					
					if ( myDoc.get_nomFichierPreva().toLowerCase().indexOf("recepliv-") > -1 )
						this._iNbRecep ++;
	        	}
			}
    	
	    	// Génération du script SpotBatch
	    	mySB.writeFile("spotToWeb.update", sUpdateOrder, sUpdateTemplate, "");
    				
			// Exécution du script SpotBatch
	    	if ( ! sUpdateOrder.equals("") )
	    		mySB.execute("spotToWeb.update"); 
    	}

    	return myDocList.size();
	}
	
	
	public int getNbNc()
	{
		return this._iNbNC;
	}
	
	
	public int getNbPhotos()
	{
		return this._iNbPhotos;
	}
	
	
	public int getNbRecep()
	{
		return this._iNbRecep;
	}
	
	public int getNbDocErreur()
	{
		return this._iNbDocErreur;
	}
}
