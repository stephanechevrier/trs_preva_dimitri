package com.trs.preva.metier;

import com.trs.action.ProprieteCommune;
import java.io.File;
import com.trs.constant.TrsConstant;
import com.trs.exception.TrsException;
import com.trs.utils.calendar.CalendarHelper;
import com.trs.utils.fileSystem.FileUtil;
import com.trs.utils.format.StringMgt;
import com.trs.utils.properties.Properties;
import com.trs.utils.string.StringUtil;
import com.trs.wintrans.metier.HistoSav;
import com.trs.wintrans.dbAccess.CalendrierTrsAccess;
import com.trs.metier.AgenceHelper;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
//import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import com.trs.utils.email.*;
import java.util.ArrayList;
//import java.util.ArrayList;
import java.util.Date;
import java.util.List;
//02/05/22 MAJ Propriete commune
//import com.trs.preva.metier.CommandesPlakardsBLToPreva;
import com.trs.wintrans.metier.CommandesPlakards;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SpotToWebBL 
{
	// 19/02/2021 - Version pour lot 3 PREVA
	
	
	private String	_targetDir = "";

	
	static final Logger myLog = LogManager.getLogger(SpotToWebBL.class.getName());
	
	private List<CommandesPlakards>		_myCPList = null;
	private ProprieteCommune	_PC = null;
	private SpotToWebHelper		_mySTWHelper = null;
	private MailNotification	_myMailNotification = null;
	private String _dirConf ="";
	
	String asPropFileName;
	String asDbName;
	private List<CommandesPlakards> myCPListBlNotFound = null; 
	
	
	
	/**
	 * <p>Initialisation puis génération du fichier</p>
	 * 
	 * @throws TrsException
	 */
	public SpotToWebBL(List<CommandesPlakards> aCPList, ProprieteCommune aPC,String asPropFileName, String asDbName) throws TrsException
	{
		String	sPropKey = "";
		
		// TODO C'est le SpotToWeb qui a besoin du Jour Ouvré Précédent
		// Besoin d'avoir les paramètres de connexion : String asPropFileName, String asDbName
		
		
		this._myCPList = aCPList;
		this._PC = aPC;
		this._mySTWHelper = new SpotToWebHelper();
		this.asDbName = asDbName;
		this.asPropFileName = asPropFileName;
		 
		try { this._dirConf = this._PC.getDirConf(); }
	        catch ( TrsException e )
	        {
	        	throw new TrsException("PROBLEME lors de l'initalisation du répertoire de configuration : " + e.toString());
	        }
		
		
		// Répertoire de stockage des fichiers à générer
		// ********************************************************************************************************************
		this._targetDir = aPC.getProperty(ProprieteCommune.dir_out_target_propKey, "");
		
		if ( this._targetDir.equals("") )
		{
        	throw new TrsException(aPC.getMessageErreur(1, ProprieteCommune.dir_out_target_propKey));
        
		}
		// SAV à exporter
		// ****************************************
		if ( aCPList.size() > 0 )
		{
			myLog.info("Il y a des BL a transmettre au site web PREVA");
			
			//this.copyFile(this._targetDir, aCPList);
			
		}
	}

	
	
	public List<CommandesPlakards> copyFile(String asTarget, List<CommandesPlakards> aCPList) throws TrsException
	{
		FileUtil	myFU = new FileUtil();
		
		PrintWriter	myPWFile = null;
	    List<CommandesPlakards> myCPListCopy = new ArrayList<CommandesPlakards>(); 
		String 	sFileName = "";
		String sCodeClient ="";
		String _sourceDir ="";
		String sNotFoundBl = "";
		String _sourceDirChemins ="";
		String sBL = "";
		String sCde = "";
		String codeClientErreur = ""; 
		String codeClientTemp = "";
		boolean bFileExist = false;
		int test = 0;
		
		myLog.info("PREPARATION copie fichier dans [" + asTarget + "]");
		
		myLog.info("asPropFileName = " + this.asPropFileName + " | dbName = " + this.asDbName );
		
		
		// Création du répertoire utilisé
		myFU.makeDir(asTarget);
		
		myLog.info("DEBUT Copie fichier [" + sFileName + "]");
		
		myLog.info("  - " + aCPList.size() + " SAV trouve(s)");
		
        // Boucle sur les SAV
        for ( CommandesPlakards myCp : aCPList )
        {

        	sCodeClient = myCp.getLigComm().get_codeClient();
        	sCodeClient = sCodeClient.toLowerCase();
        	sBL = myCp.get_numBl();
        	sCde = myCp.getLigComm().get_refCommande();
        	_sourceDir = "";
        	sNotFoundBl = "";
        	
        	// Si le code_client correspond à celui n'ayant pas de chemins d'accès au bl renseigné on passe au prochain BL
        	if(!sCodeClient.equals(codeClientErreur)) {
        		
        		//SI le code client de la commande correspond au code client du dernier BL traité pas besoin d'initialisé le dossier source on garde le précedent.
        		if(!sCodeClient.equals(codeClientTemp))
        			_sourceDirChemins = _PC.getProperty(this._PC.getEnvironnement() + "." + ProprieteCommune.preva_spotToWeb_bl_directory_propKey + '.' + sCodeClient, "");
        			_sourceDir = _sourceDirChemins + "\\" + sBL + ".pdf";
        		codeClientTemp = sCodeClient;
        		
        		
        	  // Si le repertoire source des BL n'existe pas on envoie une erreur
        		if (_sourceDir.equals("\\" + sBL + ".pdf")) {
        		myLog.info("Le chemins d''acces au BL n''a pas ete renseigne pour ce client : " + sCodeClient + " | Il faut le renseigner dans le fichier de propriete " +  _PC.getDirConf() + "trsPreva.properties");
        		codeClientErreur = sCodeClient; 
        		}else 
        		{
        			try {
        				bFileExist =myFU.fileExist(_sourceDir);
        				myLog.info("Le fichier " + _sourceDir +" existe : " + bFileExist);
        			//test = myFU.copyAllBytes(_sourceDir, asTarget , sBL, false,false,false,false) ;
        				if(bFileExist==true) {
        				test = myFU.copyBytes(_sourceDir,asTarget + "\\"  + "BL_" + sCde + "_" + sBL + ".pdf",false);
        				myCPListCopy.add(myCp);
        				}
        				else {
        					myCPListBlNotFound.add(myCp);
        					
        				}
        			
        			}
        			
        			catch( Exception e) {
        			//throw new TrsException("ERREUR lors de la copie du BL, veuillez vérifier le chemins source : " + _sourceDir);
        			myLog.info("ERREUR lors de la copie du BL, veuillez vérifier le chemins source : " + _sourceDir);
        			
        		};
        		
        		}		
        		
        		
        	}
        }
        
		myLog.info("FIN Copie fichier [" + asTarget + "]");
		
		return myCPListCopy;
	}
	
	public void SendNotifInformatique()
	{
		String sValeur = "";
		String	sContent = "";
		String	sPropKey = "";
		String sPropDestKey = ".informatique.mail";
		
		
		
		if (myCPListBlNotFound.isEmpty())
		{
			myLog.info( "PAS d'envoi de notification pour l'informatique : PAS de BL en erreur");
			return;
		}
		
		myLog.info("ENVOI du rapport au SI");
		
		
		try {
			Properties myProp = new Properties();
			myProp.load(this._dirConf + "/" + "trsPreva.notifFonctionnel.properties");
			MailNotification myMN = new MailNotification(myProp, this._PC.getEnvironnement() + ".", "notif_informatique");
			sContent = myMN.getContent();
			sPropKey = this._PC.getEnvironnement() + sPropDestKey;
			sValeur = myProp.getProperty(sPropKey, "");
			if ( ! sValeur.equals("") )
			{
				myMN.setMailTo(sValeur);
				myLog.info("  - Prise en compte des destinataires supplementaires : " + sValeur);
			}
			else
			{
				myLog.fatal("ATTENTION, le parametrage [" + sPropKey + "] est absent dans [" 
					+ this._dirConf + "/" + "trsPreva.notifFonctionnel.properties" + "]");
			}
			
			myLog.info("  - Envoi a [" + myMN.getMailTo() + "]");
			
			for(CommandesPlakards myCpBlNotFound : myCPListBlNotFound ) {
				
				// Remplacement des variables
				// ******************************************************************************************************
	
				sContent = sContent.replaceFirst("@@@CODE_CLIENT@@@", myCpBlNotFound.getLigComm().get_codeClient());
				
				
				sContent = sContent.replaceFirst("@@@NUMERO_DE_COMMANDE@@@", myCpBlNotFound.getLigComm().get_refCommande());
						
				
				sContent = sContent.replaceFirst("@@@NUMERO_DE_BL@@@", myCpBlNotFound.get_numBl());
						
				
				myMN.setContent(sContent);
				
		    	
	        }
			myMN.sendHTMLMail();
		}
		catch ( Exception e ) 
		{
			myLog.info(e.toString() 
					+ "\r\n" + "sContent = [" + sContent + "]"
					); 
			myLog.error(e.toString() 
				+ "\r\n" + "sContent = [" + sContent + "]"
				); 
		}
				
}
		
		
		
}
	
	
	

