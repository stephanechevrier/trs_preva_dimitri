package com.trs.preva.metier;

import com.trs.action.ProprieteCommune;
import com.trs.constant.TrsConstant;
import com.trs.exception.TrsException;
import com.trs.utils.calendar.CalendarHelper;
import com.trs.utils.fileSystem.FileUtil;
import com.trs.utils.format.StringMgt;
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
//import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SpotToWebConf 
{
	// 19/02/2021 - Version pour lot 3 PREVA
	
	private String	_fichierDate = "";
	private String	_targetDir = "";
	private String sAgence ="";
	
	private Date _date = null;
	
	static final Logger myLog = LogManager.getLogger(SpotToWebConf.class.getName());
	
	private List<HistoSav>		_myHSList = null;
	private ProprieteCommune	_PC = null;
	private StringUtil			sUtil;
	private SpotToWebHelper		_mySTWHelper = null;
	String asPropFileName;
	String asDbName;
	
	
	
	/**
	 * <p>Initialisation puis génération du fichier</p>
	 * 
	 * @throws TrsException
	 */
	public SpotToWebConf(List<HistoSav> aSavList, ProprieteCommune aPC,String asPropFileName, String asDbName) throws TrsException
	{
		String	sPropKey = "";
		
		// TODO C'est le SpotToWeb qui a besoin du Jour Ouvré Précédent
		// Besoin d'avoir les paramètres de connexion : String asPropFileName, String asDbName
		
		
		this._myHSList = aSavList;
		this._PC = aPC;
		this._mySTWHelper = new SpotToWebHelper();
		this.asDbName = asDbName;
		this.asPropFileName = asPropFileName;
		
		
		// Répertoire de stockage des fichiers à générer
		// ********************************************************************************************************************
		this._targetDir = aPC.getProperty(ProprieteCommune.dir_out_target_propKey, "");
		
		if ( this._targetDir.equals("") )
		{
        	throw new TrsException(aPC.getMessageErreur(1, ProprieteCommune.dir_out_target_propKey));
		}
		
		// SAV à exporter
		// ****************************************
		if ( aSavList.size() > 0 )
		{
			myLog.info("Il y a des lignes a ecrire dans le fichier des Confirmations de RDV pour le site web PREVA");
			
			// Génération du fichier
			this.generateFile(this._targetDir, aSavList);
		}
	}

	
	public void generateFile(String asTarget, List<HistoSav> aSavList) throws TrsException
	{
		FileUtil	myFU = new FileUtil();
		
		PrintWriter	myPWFile = null;
		
		boolean	bHeader = true;
		
		int	   	iNbLignes = 0;

		String 	sColonne = "";
		String	sDatePrevLivraison = "";
		String 	sFileName = "";
		String	sHeader = "";
		String 	sLigne = "";
		String	sNumTelDestinataire = "";
		String  sCodeClient = "";
		String  sCommentaire = "";
		String 	sValeur = "";
		String  sHeureLiv = "";
		String  sNumTelSup ="";
		String 	sValeur2 = "";
		String  sTemp2 = "";
		String	sRetour = "";
		CalendrierTrsAccess myCTAccess = new CalendrierTrsAccess(this.asPropFileName,this.asDbName);
		AgenceHelper myAgenceHelper = new AgenceHelper(this.asPropFileName,this.asDbName);
		String sNumTemp ="";
		
		
		List<String>	sListValeur = null;
		
		myLog.info("PREPARATION ecriture fichier dans [" + asTarget + "]");
		
		myLog.info("asPropFileName = " + this.asPropFileName + " | dbName = " + this.asDbName );
		// On prépare l'écriture du fichier, dans le répertoire cible
		sFileName = this.getFileName(asTarget, false);
		
		// Création du répertoire utilisé
		myFU.makeDir(asTarget);
		
		try { myPWFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sFileName), "UTF-8")); }
        catch ( IOException e ) { throw new TrsException("ERREUR lors de la préparation en écriture du fichier [" + sFileName + "]"); }
        
		myLog.info("DEBUT ecriture fichier [" + sFileName + "]");
		
		myAgenceHelper.init();
        // Boucle sur les SAV
        for ( HistoSav mySav : aSavList )
        {
        	iNbLignes ++;
    		myLog.info("PREPARATION ecriture ligne #" + iNbLignes);
        	
    		try
    		{
    			sHeader = "";
    			sHeader += "order_number;";
    			
    			sColonne = "mySav.getLigComm().get_refCommande()";
	        	sLigne = mySav.getLigComm().get_refCommande() + ";";
	        	
	        	sHeader += "delivery_estimated_date;";
	        	sColonne = "mySav.getLigComm().get_datePrevLivraison()";
	        	myLog.debug("  - AVANT " + sColonne);
	        	sDatePrevLivraison = mySav.getLigComm().get_datePrevLivraison();
	        	myLog.debug("  - Date previsionnelle de livraison = [" + sDatePrevLivraison + "]");
	        	// Format source = dd/mm/yyyy | Format cible = yyyy-mm-dd
	        	sValeur = sDatePrevLivraison.substring(6) + "-" + sDatePrevLivraison.substring(3, 5) + "-" + sDatePrevLivraison.substring(0, 2);
	        	myLog.debug("  - Date previsionnelle de livraison au format PREVA = [" + sValeur + "]");
	        	
	        	// 03/02/2019 - Ajout de l'heure (pour prise en compte dans le sens WebToSpot pour mettre à jour l'heure dans HISTOSAV.DATE_RDV
	        	sHeureLiv = mySav.getLigComm().get_heurePrevLivraison();
	        	sValeur += " " + mySav.getLigComm().get_heurePrevLivraison();
	        	myLog.info(sHeureLiv);
	        	myLog.debug("  - Date previsionnelle de livraison au format PREVA avec heure = [" + sValeur + "]");
	        	sLigne += sValeur + ";";
	        	if (!sHeureLiv.equals("00:00:00")) {
	        	sHeader += "delivery_estimated_time_slot;";
	        	sColonne = "mySav.getLigComm().get_plageHorairePrevLivraison()";
	        	sLigne += mySav.getLigComm().getPlageHorairePrevLivraison() + ";";
	        	}else {
	        		sHeader += "delivery_estimated_time_slot;";
		        	sColonne = "mySav.getLigComm().get_plageHorairePrevLivraison()";
		        	sLigne +=  ";";
	        	}
	        	
	        	/*sHeader += "delivery_estimated_time_slot;";
	        	sColonne = "mySav.getLigComm().getPlageHorairePrevLivraison()";
	        	myLog.debug("Recuperation plage horaire pour [" + mySav.getLigComm().get_datePrevLivraison() + "]");
	        	sLigne += mySav.getLigComm().getPlageHorairePrevLivraison() + ";";*/
	        	
	        	
	      
	        	sHeader += "working_date_before_delivery;";
	        	sColonne = "myCTAccess.getPrevJourOuvre(mySav.getLigComm().get_dateCalPrevLivraison())";
	        	sLigne += CalendarHelper.getFormattedDate(myCTAccess.getPrevJourOuvre(mySav.getDatePrevJourOuvre()), CalendarHelper.FORMAT_YYYYMMDD_HHMMSS_tiret) + ";";
	        	
	        	
	        	sHeader += "recipient_name;";
	        	sColonne = "mySav.getLigComm().get_nomDestinataire()";
	        	sLigne += mySav.getLigComm().get_nomDestinataire().replaceAll(";", "_") + ";";
    			
	        			
    			sHeader += "address_line_1;";
	        	sColonne = "mySav.getLigComm().get_adresseDestLigne1()";
	        	sLigne += mySav.getLigComm().get_adresseDestLigne1().replaceAll(";", "_") + ";";
	        	
    			sHeader += "address_line_2;";
	        	sColonne = "mySav.getLigComm().get_adresseDestLigne2()";
	        	sLigne += mySav.getLigComm().get_adresseDestLigne2().replaceAll(";", "_") + ";";
	        		
    			sHeader += "address_zip_code;";
	        	sColonne = "mySav.getLigComm().get_cpDestinataire()";
	        	sLigne += mySav.getLigComm().get_cpDestinataire() + ";";
	        			
    			sHeader += "address_city;";
	        	sColonne = "mySav.getLigComm().get_villeDestinataire()";
	        	sLigne += mySav.getLigComm().get_villeDestinataire().replaceAll(";", "_") + ";";
	        	
	        	// Numéro de téléphone mobile destinataire 
    			sHeader += "recipient_mobile_phone;";
	        	sColonne = "mySav.getLigComm().get_mobileDestinataire()";
	        	
	        	// Ne transmettre le numéro que si mobile !!! Identification possible uniquement si mobile français
	        	sValeur = this._mySTWHelper.getNumeroMobile(mySav.getLigComm().get_mobileDestinataire());
	        	
	        	// Si pas de numéro retourné, on essaie avec le numéro de téléphone fixe
	        	if ( sValeur.equals("") )
	        	{
	        		myLog.info("PAS de numero mobile dans get_mobileDestinataire() ( = [" 
	        			+ mySav.getLigComm().get_mobileDestinataire() + "] ), on essaie avec get_telDestinataire() ( = ["
	        			+ mySav.getLigComm().get_telDestinataire() + "] )"
	        			);
	        		sValeur = this._mySTWHelper.getNumeroMobile(mySav.getLigComm().get_telDestinataire());
	        		
		        	if ( sValeur.equals("") )
		        		myLog.info("  - PAS de numero mobile trouve dans la zone specifiquement prevu pour cela dans Wintrans");
	        	}
	        	
	        	// SI le numéro est celui d'un téléphone portable français, on peut le passer
	        	if ( StringMgt.isMobilePhone(sValeur) )
	        	{
	        		// On formate le numéro au cas où
	        		sValeur = StringMgt.formatPhoneNumer(sValeur);

	        		myLog.info("  - [" + sValeur + "] conserve CAR numero de telephone mobile francais");
	        	}
	        	// Pour les autres, il faut ne pas passer si cela correspond à un numéro de fixe français
	        	else
	        	{
	        		sValeur2 = StringMgt.formatPhoneNumer(sValeur);
	        		
	        		// SI le numéro débute par 00, il faut le passer : c'est un numéro étranger
	        		if ( sValeur2.length() > 2 && sValeur2.substring(0, 2).equals("00") )
	        		{
	        			// On ne fait rien
	        			myLog.info("  - Numero NON francais : on le passe tel quel (cela pourrait etre un numero de mobile)");
	        		}
	        		else
	        		{
	        			myLog.info("  - Numero fixe francais [" + sValeur + "] : on ne passe pas de numero de telephone");

	        			// On vide pour ne pas passer de valeur
	        			sValeur = "";
	        		}
	        	}
	        	
	        	// Mémorisation du premier numéro de téléphone mobile 
	        	sNumTelDestinataire = sValeur;
	        	sNumTelSup = mySav.getLigComm().get_mobileDestinataireSup();
	        	sValeur = sValeur.replaceAll(" ", "");
	        	sValeur = sValeur.replaceAll("-", ""); 
	        	sValeur = sValeur.replaceAll("\\.", ""); 
	        	myLog.info("Tel dest = " + sValeur);
	        	sNumTelSup = sNumTelSup.replaceAll(" ", ""); 
	        	sNumTelSup = sNumTelSup.replaceAll("-", ""); 
	        	sNumTelSup = sNumTelSup.replaceAll("\\.", ""); 
	        	myLog.info("Tel dest sup = " + sNumTelSup);
	        	
	        	// Plusieurs numéros de téléphone portable
	        	if ( this._PC.getProperty("numPortable.plusieurs", "non").equals("oui") )
	        	{
	        		// 1 numéro déjà présent ET des numéros supplémentaires
	        		if ( ! sValeur.equals("") && ! mySav.getLigComm().get_mobileDestinataireSup().equals("") )
	        		{
	        			myLog.info("Utilisation des numeros de telephone portable supplementaires");
	        			//myLog.fatal("Utilisation des numeros de telephone portable supplementaires");
	        			
	        		}
	        		
	        		if ( ! mySav.getLigComm().get_mobileDestinataireSup().equals("") &&  (! sValeur.equals(sNumTelSup)))
					{
	        			if ( ! sValeur.equals("") ) {
	        			sValeur += ",";
	        			sValeur += sNumTelSup.replaceAll(";", ",");
	        			}
	        			else 
	        				sValeur += sNumTelSup.replaceAll(";", ",");
					}
	        		
	        	}
	        	
	        	// Appel de la méthode qui tient compte des propriétés pour forcer la valeur si nécessaire
	        	sValeur = this._mySTWHelper.getTelephoneDestinataire(sValeur, this._PC);
	        	sLigne += sValeur + ";"; 
	        	
	        	
	        	// Mail destinataire 
    			sHeader += "recipient_email;";
	        	sColonne = "mySav.getLigComm().get_eMailDestinataire()";
	        	
	        	sValeur = mySav.getLigComm().get_eMailDestinataire();
	        	
	        	// PRENDRE en compte plusieurs adresses mail : séparateur = , : fait dans la méthode appelée
	        	// La méthode ci-dessous prend en compte le forçage indiqué dans le fichier de propriétés
	        	sValeur = this._mySTWHelper.getEmailDestinataire(sValeur, mySav.getLigComm().get_commentaire(), this._PC);
	        	
	        	sLigne += sValeur + ";";
    			sHeader += "number_of_packages;";
	        	sColonne = "mySav.getLigComm().get_nbColisReel()";
	        	sLigne += mySav.getLigComm().get_nbColisReel() + ";";
	        	
	        	
	        	// Annonce de Confirmation à faire
    			sHeader += "confirmation_announce;";
    			 sColonne = "mySav.getLigComm().get_variableString1()";
	        	sLigne += mySav.getLigComm().get_variableString1() + ";";
	        	

	        	// URL suivi de commande
    			sHeader += "order_tracking_url;";
	        	sColonne = "mySav.getLigComm().get_urlSuiviCommande()";
	        	sLigne += mySav.getLigComm().get_urlSuiviCommande() + ";";
	        	
	        	
	        	
	   
	        	// Numéro de téléphone de l'agence de livraison
    			sHeader += "delivery_agency_phone;";
	        	sColonne = "myAgenceHelper.getNumTel(sAgence)";
	        	sNumTemp = myAgenceHelper.getNumTel(mySav.get_agenceLivraison());
	        	sLigne += sNumTemp.replace(".","")  + ";";
	        	
	        	sHeader += "home_delivery_type;";
	        	sColonne = "mySav.getLigComm().get_typeLdd()";
	        	sLigne += mySav.getLigComm().get_typeLdd() + ";";
	        	
	        	
	        	// Numéro Allegro : « [{numéro magasin sur 3 chiffres}-]{numéro Allegro sur 8 chiffres} »
    			sHeader += "allegro_number;";
	        	sColonne = "mySav.getLigComm().get_numAllegro()";
	        	
	        	// Ne pas tenir compte du numéro si NON Comfour
	        	if ( mySav.getLigComm().get_typeLdd().equals("COMFOUR") )
	        	{
	        		if ( mySav.getLigComm().get_numAllegro().length() > 48 )
		        		sLigne += mySav.getLigComm().get_numAllegro().substring(0, 48) + ";";
	        		else
	        			sLigne += mySav.getLigComm().get_numAllegro() + ";";
	        	}
	        	else
	        		sLigne += ";";
	        	
	        	sHeader += "manufacturer_code;";
	        	sColonne = "mySav.getLigComm().get_codeClient()";
	        //	sLigne += mySav.getLigComm().get_codeClient() + ";";
	        	sCodeClient = mySav.getLigComm().get_codeClient();
	        	/*if (sCodeClient.equals("TOU31CE"))
	        	{
	        		sCommentaire = mySav.getLigComm().get_commentaire();
	        		sCommentaire = sUtil.getBloc(sCommentaire,"FabPreva(");
	        		if ( ! sCommentaire.equals(""))
	        		sLigne += sCommentaire + ";";
	        		else
	        		sLigne += mySav.getLigComm().get_codeClient() + ";";	
	        	}
	        	else*/
	        		sLigne += mySav.getLigComm().get_codeClient() + ";";
	        	
    			sHeader += "manufacturer_name;";
	        	sColonne = "mySav.getLigComm().get_nomClient()";
	        	sLigne += mySav.getLigComm().get_nomClient().replaceAll(";", "_") + ";";
	        	
	        
	        
	        	
    		}
    		
    		catch ( StringIndexOutOfBoundsException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebConf : StringIndexOutOfBoundsException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( NullPointerException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebConf : NullPointerException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( TrsException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebConf : TrsException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( Exception e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebConf : Exception avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		
    		// Ecriture de la ligne d'en-tête : uniquement sur la 1ère ligne
    		if ( bHeader )
    		{
    	    	myLog.info("sLigne EnTete = " + sHeader);
    	    	
    	    	// On ne veut pas CRLF en fin de ligne : on veut uniquement LF
    	    	try
    	    	{
	    	        myPWFile.write(sHeader);
	    	        myPWFile.write("\n");
    	    	}
    	    	catch ( Exception e )
    	    	{
    	    		sRetour = "Probleme de conversion de [" + sHeader + "] en ISO-8859-1 : " + e.toString();
    	    		throw new TrsException(sRetour);
    	    	}
    	        
    			bHeader = false;
    		}
        	
        	myLog.info("sLigne = " + sLigne);
        		
	    	// On ne veut pas CRLF en fin de ligne : on veut uniquement LF
	    	try
	    	{
    	        myPWFile.write(sLigne);
    	        myPWFile.write("\n");
	    	}
	    	catch ( Exception e )
	    	{
	    		sRetour = "Probleme de conversion de [" + sHeader + "] en ISO-8859-1 : " + e.toString();
	    		throw new TrsException(sRetour);
	    	}
        }
		
       	myPWFile.close(); 
        
		myLog.info("FIN ecriture fichier [" + asTarget + "]");
	}
	
	private String getFileName(String asTargetDir, boolean abExtensionCsv)
	{
		Date myDate = new Date(System.currentTimeMillis());
		this._date = myDate;
		
		DateFormat myDf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		
		// Mémorisation date et heure utilisées pour le nom du fichier
		this._fichierDate = myDf.format(myDate);
		
		String sTexteFileName = asTargetDir + "/preva-spot-conf-" + this._fichierDate + ".csv";
		
		return sTexteFileName;
	}
}
