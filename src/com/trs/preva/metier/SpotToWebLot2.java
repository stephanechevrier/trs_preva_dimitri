package com.trs.preva.metier;

import com.trs.action.ProprieteCommune;
import com.trs.constant.TrsConstant;
import com.trs.exception.TrsException;
import com.trs.metier.AgenceHelper;
import com.trs.utils.calendar.CalendarHelper;
import com.trs.utils.fileSystem.FileUtil;
import com.trs.utils.format.StringMgt;
import com.trs.utils.string.StringUtil;
import com.trs.wintrans.metier.HistoSav;

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


public class SpotToWebLot2 
{
	// 28/12/2019 - Version pour lot 2 PREVA
	
	private String	_fichierDate = "";
	private String	_targetDir = "";
	String asPropFileName;
	String asDbName;
	
	private Date _date = null;
	
	static final Logger myLog = LogManager.getLogger(SpotToWebLot2.class.getName());
	
	private List<HistoSav>		_myHSList = null;
	private ProprieteCommune	_PC = null;
	private StringUtil			sUtil;
	/**
	 * <p>Initialisation puis génération du fichier</p>
	 * 
	 * @throws TrsException
	 */
	public SpotToWebLot2(List<HistoSav> aSavList, ProprieteCommune aPC,String asPropFileName, String asDbName) throws TrsException
	{
		String	sPropKey = "";
		
		this._myHSList = aSavList;
		this._PC = aPC;
		this.asDbName = asDbName;
		this.asPropFileName = asPropFileName;
		myLog.info("Connexion à asDbName :" + asDbName);
		myLog.info("Connexion à asPropFileName :" + asPropFileName);
		
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
			myLog.info("Il y a des lignes a ecrire dans un fichier pour le site web");
			
			// Génération du fichier
			this.generateFile(this._targetDir, aSavList);
		}
	}
	
	public void generateFile(String asTarget, List<HistoSav> aSavList) throws TrsException
	{
		FileUtil	myFU = new FileUtil();
		//PrintWriter	myFile =  null;
		
		PrintWriter	myPWFile = null;
		
		boolean	bHeader = true;
		
		int	   	iNbLignes = 0;

		String 	sColonne = "";
		String	sDatePrevLivraison = "";
		String 	sFileName = "";
		String	sHeader = "";
		String 	sLigne = "";
		String  sCodeClient = "";
		String  sCommentaire ="";
		String	sNumTelDestinataire = "";
		String 	sValeur = "";
		String 	sValeur2 = "";
		String	sRetour = "";
		String sNumTemp ="";
		List<String>	sListValeur = null; // 15/02/2019
		AgenceHelper myAgenceHelper = new AgenceHelper(this.asPropFileName,this.asDbName);
		myLog.info("PREPARATION ecriture fichier dans [" + asTarget + "]");
		
		// On prépare l'écriture du fichier, dans le répertoire cible
		sFileName = this.getFileName(asTarget, false);
		
		// Création du répertoire utilisé
		myFU.makeDir(asTarget);
		
		// Forçage de l'encodage
		//System.setProperty( "file.encoding", "UTF-8" );
		
        //try { myFile =  new PrintWriter(new BufferedWriter(new FileWriter(sFileName))); }
        //catch ( IOException e ) { throw new TrsException("ERREUR lors de la préparation en écriture du fichier [" + sFileName + "]"); }
        
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
	        	
	        	myLog.info("  - Commande " + mySav.getLigComm().get_refCommande());
	        	
	        	// 18/12/2018 - Ajout de la référence destinataire
	        	// ********************************************************************************************************
    			sHeader += "order_recipient_number;";
	        	sValeur = StringUtil.getBloc(mySav.getLigComm().get_commentaire(), "RefDest(");
	        	
	        	myLog.debug("  - Reference destinataire issu du commentaire exploitation = [" + sValeur + "]");
	        	
	        	if ( sValeur.equals(StringUtil.ERROR_BLOC_DEBUT) )
	        		sValeur = "";
	        	
	        	if ( sValeur.equals("") )
	        	{
	        		sValeur = mySav.getLigComm().get_refClient();
	        		
	        		if ( sValeur == null )
	        			sValeur = "";
	        		
		        	myLog.debug("  - Reference destinataire issu de LIGCOMM.REF_CLIENT = [" + sValeur + "]");
	        	}
	        	
	        	// 06/03/2019 - Limitation longueur à 255 caractères : si présence de références destinataire issues du commentaire exploitation,
	        	//				cela peut être très long !
	        	if ( sValeur.length() > 255 )
	        	{
	        		myLog.info("    - Reference destinataire TROP longue : on tronque a 255 caracteres");
	        		sValeur = sValeur.substring(0, 255);
	        	}
	        	// 06/03/2019 - FIN modif
	        	
    			sColonne = "reference destinataire";
    			// 26/06/2020 - Valeur entre " si présence ;
	        	//sLigne += sValeur + ";";
	        	sLigne += "\"" + sValeur + "\"" + ";";
	        	// 26/06/2020 - FIN modif
	        	// ********************************************************************************************************
	        	// 18/12/2018 - FIN modif
	        	
	        	// 18/12/2018 - Il faut aussi fournir le numéro de l'OT
	        	
    			sHeader += "ot_number;";
	        	sColonne = "mySav.get_noOT()";
	        	sLigne += mySav.get_noOT() + ";";
	        	// 18/12/2018 - FIN modif
	        	
    			sHeader += "op_number;";
	        	sColonne = "mySav.get_noOP()";
	        	sLigne += mySav.get_noOP() + ";"; // 13/11/2018 - noOP au lieu de noOT, par rapport au cdc
	        	
    			sHeader += "plan_number;";
	        	sColonne = "mySav.getNumPlan()";
	        	sLigne += mySav.getNumPlan() + ";";

    			sHeader += "sav_number;";
	        	sColonne = "mySav.get_noID()";
	        	sLigne += mySav.get_noID() + ";";
	        	
	        	// Date heure de création
	        	// ********************************************************
    			sHeader += "creation_date;";
	        	sColonne = "mySav.get_calDteHistoSav()";
	        	sValeur = CalendarHelper.getFormattedDate(mySav.get_calDteHistoSav(), CalendarHelper.FORMAT_YYYYMMDD_HHMMSS_tiret);
	        	myLog.debug("  - " + sColonne + " = " + sValeur);
	        	sLigne += sValeur + ";";
	        	
    			sHeader += "operator_username;";
	        	sColonne = "mySav.getUserIns()";
	        	sLigne += mySav.getUserIns() + ";";

	        	// 04/02/2018 - On remplace par 2 codes agence
	        	/*
    			sHeader += "agency_code;";
	        	sColonne = "mySav.get_agenceOt()";
	        	sLigne += mySav.get_agenceOt() + ";";
	        	*/
    			sHeader += "delivery_agency_code;";
	        	sColonne = "mySav.get_agenceLivraison()";
	        	sLigne += mySav.get_agenceLivraison() + ";";
	        	
    			sHeader += "pickup_agency_code;";
	        	sColonne = "mySav.get_agenceOt()";
	        	sLigne += mySav.get_agenceOt() + ";";
	        	// 04/02/2019 - FIN modif
	        	
    			sHeader += "manufacturer_code;";
	        	sColonne = "mySav.getLigComm().get_codeClient()";
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
	        	// ***********************************************************************************
    			sHeader += "recipient_mobile_phone;";
	        	sColonne = "mySav.getLigComm().get_mobileDestinataire()";
	        	
	        	// Ne transmettre le numéro que si mobile !!! Identification possible uniquement si mobile français
	        	sValeur = this.getNumeroMobile(mySav.getLigComm().get_mobileDestinataire());
	        	
	        	// Si pas de numéro retourné, on essaie avec le numéro de téléphone fixe
	        	if ( sValeur.equals("") )
	        	{
	        		myLog.info("PAS de numero mobile dans get_mobileDestinataire() ( = [" 
	        			+ mySav.getLigComm().get_mobileDestinataire() + "] ), on essaie avec get_telDestinataire() ( = ["
	        			+ mySav.getLigComm().get_telDestinataire() + "] )"
	        			);
	        		sValeur = this.getNumeroMobile(mySav.getLigComm().get_telDestinataire());
	        		
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
	        	
	        	// 21/09/2020 - Mémorisation du premier numéro de téléphone mobile 
	        	sNumTelDestinataire = sValeur;
	        	
	        	// TODO Il faudra tester cette variable : si VIDE et si présence de numéro supplémentaire, il faudrait utiliser
	        	//      le premier numéro supplémentaire
	        	// 21/09/2020 - FIN modif

	        	// 15/09/2020 - Plusieurs numéros de téléphone portable
	        	if ( this._PC.getProperty("numPortable.plusieurs", "non").equals("oui") )
	        	{
	        		// 1 numéro déjà présent ET des numéros supplémentaires
	        		if ( ! sValeur.equals("") && ! mySav.getLigComm().get_mobileDestinataireSup().equals("") )
	        		{
	        			myLog.info("Utilisation des numeros de telephone portable supplementaires");
	        			//myLog.fatal("Utilisation des numeros de telephone portable supplementaires");
	        			sValeur += ",";
	        		}
	        		
	        		if ( ! mySav.getLigComm().get_mobileDestinataireSup().equals("") )
	        			sValeur += mySav.getLigComm().get_mobileDestinataireSup().replaceAll(";", ",");
	        	}
	        	// 15/09/2020 - FIN modif
	        	
	        	// Appel de la méthode qui tient compte des propriétés pour forcer la valeur si nécessaire
	        	sValeur = this.getTelephoneDestinataire(sValeur);
	        	
	        	sValeur = sValeur.replaceAll("\\.", "");
	        	sValeur = sValeur.replaceAll("-", "");
	        	sValeur = sValeur.replaceAll(" ", "");
	        	sLigne += sValeur + ";";
	        	
	        	// Mail destinataire 
	        	// ***********************************************************************************
    			sHeader += "recipient_email;";
	        	sColonne = "mySav.getLigComm().get_eMailDestinataire()";
	        	
	        	sValeur = mySav.getLigComm().get_eMailDestinataire();

	        	// 08/10/2020 - La méthode après ce bloc prend en compte la présence d'adresses mail dans le commentaire de la commande
	        	//				mySav.getLigComm().get_eMailDestinataireSup() n'est donc PAS utile
	        	/*
	        	// 15/09/2020 - Plusieurs adresses mail
	        	if ( this._PC.getProperty("adresseMail.plusieurs", "non").equals("oui") )
	        	{
	        		// 1 adresse déjà présente ET des adresses supplémentaires
	        		if ( ! sValeur.equals("") && ! mySav.getLigComm().get_eMailDestinataireSup().equals("") )
	        		{
	        			myLog.info("Utilisation des adresses mail supplementaires");
	        			sValeur += ",";
	        		}
	        		
	        		if ( ! mySav.getLigComm().get_eMailDestinataireSup().equals("") )
	        			sValeur += mySav.getLigComm().get_eMailDestinataireSup().replaceAll(";", ",");
	        	}
	        	// 15/09/2020 - FIN modif
	        	*/
	        	// 08/10/2020 - FIN modif

	        	// 06/05/2020 - PRENDRE en compte plusieurs adresses mail : séparateur = ,
	        	//				Fait dans la méthode appelée
	        	// La méthode ci-dessous prend en compte le forçage indiqué dans le fichier de propriétés
	        	sValeur = this.getEmailDestinataire(sValeur, mySav.getLigComm().get_commentaire());
	        	// 06/05/2020 - FIN modif
	        	
	        	sLigne += sValeur + ";";
	        	
	        	// Date de livraison prévisionnelle
	        	// ***********************************************************************************
    			sHeader += "delivery_estimated_date;";
	        	sColonne = "mySav.getLigComm().get_datePrevLivraison()";
	        	myLog.debug("  - AVANT " + sColonne);
	        	sDatePrevLivraison = mySav.getLigComm().get_datePrevLivraison();
	        	myLog.debug("  - Date previsionnelle de livraison = [" + sDatePrevLivraison + "]");
	        	// Format source = dd/mm/yyyy | Format cible = yyyy-mm-dd
	        	sValeur = sDatePrevLivraison.substring(6) + "-" + sDatePrevLivraison.substring(3, 5) + "-" + sDatePrevLivraison.substring(0, 2);
	        	myLog.debug("  - Date previsionnelle de livraison au format PREVA = [" + sValeur + "]");
	        	
	        	// 03/02/2019 - Ajout de l'heure (pour prise en compte dans le sens WebToSpot pour mettre à jour l'heure dans HISTOSAV.DATE_RDV
	        	sValeur += " " + mySav.getLigComm().get_heurePrevLivraison();
	        	myLog.debug("  - Date previsionnelle de livraison au format PREVA avec heure = [" + sValeur + "]");
	        	sLigne += sValeur + ";";
	        			
	        	// Plage de livraison prévisionnelle
	        	// ***********************************************************************************
    			sHeader += "delivery_estimated_time_slot;";
	        	sColonne = "mySav.getLigComm().getPlageHorairePrevLivraison()";
	        	myLog.debug("Recuperation plage horaire pour [" + mySav.getLigComm().get_datePrevLivraison() + "]");
	        	sLigne += mySav.getLigComm().getPlageHorairePrevLivraison() + ";";
	        	
    			sHeader += "number_of_packages;";
	        	sColonne = "mySav.getLigComm().get_nbColisReel()";
	        	sLigne += mySav.getLigComm().get_nbColisReel() + ";";
	        	
	        	// 06/05/2020 - Ajout pour LDD
	        	// ***************************
	        	
	        	// Type de PREVA : LDD | NON-LDD
	        	// - Information à ajouter dans le SELECT en fonction de la fiche client : LDD dans Client.Champs2
    			sHeader += "proposal_type;";
	        	sColonne = "mySav.getLigComm().get_typePreva()";
	        	sLigne += mySav.getLigComm().get_typePreva() + ";";
	        	
	        	// Type LDD : {vide} | COMFOUR
    			sHeader += "home_delivery_type;";
	        	sColonne = "mySav.getLigComm().get_typeLdd()";
	        	sLigne += mySav.getLigComm().get_typeLdd() + ";";
	        	
	        	// Livraison à l'étage : Oui | Non
	        	// - Client.Champs2 contient Etage(oui)
    			sHeader += "upstairs_delivery;";
	        	sColonne = "mySav.getLigComm().get_livraisonEtage()";
	        	sLigne += mySav.getLigComm().get_livraisonEtage() + ";";
	        	
	        	// Numéro Allegro : « [{numéro magasin sur 3 chiffres}-]{numéro Allegro sur 8 chiffres} »
    			sHeader += "allegro_number;";
	        	sColonne = "mySav.getLigComm().get_numAllegro()";
	        	
	        	// 22/02/2021 - Le bon test sur COMFOUR !
	        	// 22/06/2020 - Ne pas tenir compte du numéro si NON-LDD
	        	//if ( mySav.getLigComm().get_typeLdd().equals("LDD") )
		        if ( mySav.getLigComm().get_typeLdd().equals("COMFOUR") )
	        	{
	        		if ( mySav.getLigComm().get_numAllegro().length() > 48 )
		        		sLigne += mySav.getLigComm().get_numAllegro().substring(0, 48) + ";";
	        		else
	        			sLigne += mySav.getLigComm().get_numAllegro() + ";";
	        	}
	        	else
	        		sLigne += ";";
	        	// 22/06/2020 - FIN modif
		        // 22/02/2021 - FIN modif
	        	
	        	// URL suivi de commande
    			sHeader += "order_tracking_url;";
	        	sColonne = "mySav.getLigComm().get_urlSuiviCommande()";
	        	sLigne += mySav.getLigComm().get_urlSuiviCommande() + ";";
	        	
	        	// Montant contre-remboursement
    			sHeader += "cash_on_delivery_amount;";
	        	sColonne = "mySav.getLigComm().get_montantContreRemboursement()";
	        	sLigne += mySav.getLigComm().get_montantContreRemboursement() + ";";
	        	
	        	// Numéro de téléphone pour la livraison : numéro de téléphone portable de la commande en priorité
    			sHeader += "delivery_phone;";
	        	sColonne = "mySav.getLigComm().get_mobileDestinataire()";
	        	
	        	// 21/09/2020 - Même valeur que pour recipient_mobile_phone avec UNIQUEMENT le premier numéro
	        	sValeur = sNumTelDestinataire;
	        	
	        	// Appel de la méthode qui tient compte des propriétés pour forcer la valeur si nécessaire
	        	sValeur = this.getTelephoneDestinataire(sValeur);
	        	// 21/09/2020 - FIN modif
	        	
	        	
	        	/*
	        	sValeur = mySav.getLigComm().get_mobileDestinataire();
	        	
	        	// Si pas de numéro retourné, on essaie avec le numéro de téléphone fixe
	        	if ( sValeur.equals("") )
	        	{
	        		myLog.info("Numero telephone livraison : PAS de numero mobile dans get_mobileDestinataire() ( = [" 
	        			+ mySav.getLigComm().get_mobileDestinataire() + "] ), on essaie avec get_telDestinataire() ( = ["
	        			+ mySav.getLigComm().get_telDestinataire() + "] )"
	        			);
	        		sValeur = mySav.getLigComm().get_telDestinataire();
	        		
		        	if ( sValeur.equals("") )
		        		myLog.info("  - PAS de numero disponible");
	        	}
	        	
	        	sValeur = sValeur.replaceAll("\\.", "");
	        	sValeur = sValeur.replaceAll("-", "");
	        	sValeur = sValeur.replaceAll(" ", "");
	        	*/
	        	sLigne += sValeur + ";";
	        	
	        	// Numéro de téléphone de l'agence de livraison
    			sHeader += "delivery_agency_phone;";
	        	sColonne = "myAgenceHelper.getNumTel(sAgence)";
	        	sNumTemp = myAgenceHelper.getNumTel(mySav.get_agenceLivraison().replaceFirst("TRS", "Q"));
	        	sLigne += sNumTemp.replace(".","");
	        	// 06/05/2020 - FIN modif

    		}
    		catch ( StringIndexOutOfBoundsException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWeb : StringIndexOutOfBoundsException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( NullPointerException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWeb : NullPointerException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( TrsException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWeb : TrsException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( Exception e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWeb : Exception avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		
    		// Ecriture de la ligne d'en-tête : uniquement sur la 1ère ligne
    		if ( bHeader )
    		{
    	    	myLog.info("sLigne EnTete = " + sHeader);
    	    	
    	    	// On ne veut pas CRLF en fin de ligne : on veut uniquement LF
    	    	try
    	    	{
    	    		//.getBytes("ISO-8859-1")
	    	        //myFile.print(sHeader);
	    	        //myFile.print("\n");
	    	        
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
	    		//.getBytes("ISO-8859-1")
		        //myFile.print(sLigne);
		        //myFile.print("\n");
    	        
    	        myPWFile.write(sLigne);
    	        myPWFile.write("\n");
	    	}
	    	/*
	    	catch ( UnsupportedEncodingException e )
	    	{
	    		sRetour = "Probleme de conversion de [" + sHeader + "] en ISO-8859-1 : " + e.toString();
	    		throw new TrsException(sRetour);
	    	}
	    	*/
	    	catch ( Exception e )
	    	{
	    		sRetour = "Probleme de conversion de [" + sHeader + "] en ISO-8859-1 : " + e.toString();
	    		throw new TrsException(sRetour);
	    	}
        }
		
        //myFile.close();
        
        //try { 
        	myPWFile.close(); 
        //	}
        //catch ( IOException e )
        //{ myLog.info("Fermeture de myBWFile avec ERREUR"); }
        
		myLog.info("FIN ecriture fichier [" + asTarget + "]");
	}
	
	private String getFileName(String asTargetDir, boolean abExtensionCsv)
	{
		Date myDate = new Date(System.currentTimeMillis());
		this._date = myDate;
		
		DateFormat myDf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		
		// Mémorisation date et heure utilisées pour le nom du fichier
		this._fichierDate = myDf.format(myDate);
		
		String sTexteFileName = asTargetDir + "/preva-spot-" + this._fichierDate + ".csv";
		
		return sTexteFileName;
	}
	
	private String getNumeroMobile(String asValeur)
	{
		String	sRetour = asValeur;
		String	sValeur = "";
		
    	// SI le numéro est celui d'un téléphone portable français, on peut le passer
    	if ( StringMgt.isMobilePhone(sRetour) )
    	{
    		// On ne fait rien
    	}
    	// Pour les autres, il faut ne pas passer si cela correspond à un numéro de fixe français
    	else
    	{
    		sValeur = StringMgt.formatPhoneNumer(sRetour);
    		
    		// SI le numéro débute par 00, il faut le passer : c'est un numéro étranger
    		if ( sValeur.length() > 2 && sValeur.substring(0, 2).equals("00") )
    		{
    			// On ne fait rien
    		}
    		else
    		{
    			// On vide pour ne pas passer de valeur
    			sRetour = "";
    		}
    	}

    	return sRetour;
	}
	
	
	private String getEmailDestinataire(String asValeur, String asCommentaire) throws TrsException, Exception
	{
		String			sErreur = "";
		String			sPropKey = "";
		String			sProperty = "";
		String			sRetour = "";
		
		List<String>	sListValeur = null;

		sPropKey = this._PC.getEnvironnement() + ".destinataire.eMail.force";
		sProperty = this._PC.getProperty(sPropKey, "non");
		
		if ( sProperty.equals("oui") )
		{
			myLog.info("Mail destinataire FORCE comme l'indique le fichier de proprietes [" + this._PC.getPropFileName() + "] "
				+ "avec propriete " + sPropKey
				);
			
			sPropKey = this._PC.getEnvironnement() + ".destinataire.eMail";
			sProperty = this._PC.getProperty(sPropKey, "");
			
			if ( sProperty.equals("") )
			{
				sErreur = "PAS de valeur pour la propriete, ou pas de propriete [" + sProperty + "] : forcage eMail destinataire IMPOSSIBLE";
				myLog.info("  - " + sErreur);
				throw new TrsException(sErreur);
			}
			
			sRetour = sProperty;
			
			myLog.info("  - Valeur prise en compte = [" + sProperty + "] au lieu de [" + asValeur + "]");
		}
		else
		{
			myLog.debug("  - PAS de forcage de l'adresse mail : on conserve [" + asValeur + "]");
			
			// On prend en compte toutes les adresses mail inscrites dans le paramètre (cas peu probable, uniquement si saisie manuelle TRS)
        	sListValeur = StringMgt.getEMailList(asValeur, false);
        	
        	if ( sListValeur.size() > 0 )
        	{
        		for ( String mySEmail : sListValeur )
        			sRetour += mySEmail + ",";
        		
        		// On supprimer le dernier ","
        		sRetour = sRetour.substring(0, sRetour.length() - 1);
        	}
        	else
        		sRetour = "";
        	
        	myLog.info("    - APRES prise en compte adresse mail sur la commande : [" + sRetour + "]");
        	
        	// Prise en compte d'adresse mail dans le commentaire
        	sListValeur = StringMgt.getEMailList(StringUtil.getBloc(asCommentaire, TrsConstant.COMEXPLOIT_EMAILDEST + "("), false);
        	
        	if ( sListValeur.size() > 0 )
        	{
        		if ( ! sRetour.equals("") )
        			sRetour = sRetour + ",";
        		
        		for ( String mySEmail : sListValeur )
        			sRetour += mySEmail + ",";
        		
        		// On supprimer le dernier ","
        		sRetour = sRetour.substring(0, sRetour.length() - 1);
        	}
        	
        	myLog.info("    - APRES prise en compte adresse mail dans le commentaire de la commande : [" + sRetour + "]");
		}
		
		return sRetour;
	}

	
	private String getTelephoneDestinataire(String asValeur) throws TrsException
	{
		String	sErreur = "";
		String	sPropKey = "";
		String	sProperty = "";
		String	sRetour = asValeur;
		
		sPropKey = this._PC.getEnvironnement() + ".destinataire.telephone.force";
		sProperty = this._PC.getProperty(sPropKey, "non");
		
		if ( sProperty.equals("oui") )
		{
			myLog.info("Telephone destinataire FORCE comme l'indique le fichier de proprietes [" + this._PC.getPropFileName() + "] "
				+ "avec propriete " + sPropKey
				);
			
			sPropKey = this._PC.getEnvironnement() + ".destinataire.telephone";
			sProperty = this._PC.getProperty(sPropKey, "");
			
			if ( sProperty.equals("") )
			{
				sErreur = "PAS de valeur pour la propriete, ou pas de propriete [" + sProperty + "] : forcage telephone destinataire IMPOSSIBLE";
				myLog.info("  - " + sErreur);
				throw new TrsException(sErreur);
			}
			
			sRetour = sProperty;
			
			myLog.info("  - Valeur prise en compte = [" + sProperty + "] au lieu de [" + asValeur + "]");
		}
		else
			myLog.debug("  - PAS de forcage du numero de telephone : on conserve [" + asValeur + "]");
		
		return sRetour;
	}
}
