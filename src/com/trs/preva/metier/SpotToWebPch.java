package com.trs.preva.metier;

import com.trs.action.ProprieteCommune;
import com.trs.constant.TrsConstant;
import com.trs.exception.TrsException;
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


public class SpotToWebPch 
{
	// 19/02/2021 - Version pour lot 3 PREVA
	
	private String	_fichierDate = "";
	private String	_targetDir = "";
	
	private Date _date = null;
	
	static final Logger myLog = LogManager.getLogger(SpotToWebPch.class.getName());
	
	private List<HistoSav>		_myHSList = null;
	private ProprieteCommune	_PC = null;
	private StringUtil			sUtil;
	private SpotToWebHelper		_mySTWHelper = null;
 
	
	/**
	 * <p>Initialisation puis génération du fichier</p>
	 * 
	 * @throws TrsException
	 */
	public SpotToWebPch(List<HistoSav> aSavList, ProprieteCommune aPC) throws TrsException
	{
		String	sPropKey = "";
		
		this._myHSList = aSavList;
		this._PC = aPC;
		this._mySTWHelper = new SpotToWebHelper();
		
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
			myLog.info("Il y a des lignes a ecrire dans le fichier des Prises en Charge pour le site web PREVA");
			
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
		String 	sValeur2 = "";
		String	sRetour = "";
		
		List<String>	sListValeur = null;
		
		myLog.info("PREPARATION ecriture fichier dans [" + asTarget + "]");
		
		// On prépare l'écriture du fichier, dans le répertoire cible
		sFileName = this.getFileName(asTarget, false);
		
		// Création du répertoire utilisé
		myFU.makeDir(asTarget);
		
		try { myPWFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sFileName), "UTF-8")); }
        catch ( IOException e ) { throw new TrsException("ERREUR lors de la préparation en écriture du fichier [" + sFileName + "]"); }
        
		myLog.info("DEBUT ecriture fichier [" + sFileName + "]");
		
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
	        	
	        	// Limitation longueur à 255 caractères : si présence de références destinataire issues du commentaire exploitation,
	        	// cela peut être très long !
	        	if ( sValeur.length() > 255 )
	        	{
	        		myLog.info("    - Reference destinataire TROP longue : on tronque a 255 caracteres");
	        		sValeur = sValeur.substring(0, 255);
	        	}
	        	
    			sColonne = "reference destinataire";
    			// Valeur entre " si présence ;
	        	sLigne += "\"" + sValeur + "\"" + ";";
	        	
    			sHeader += "sav_number;";
	        	sColonne = "mySav.get_noID()";
	        	sLigne += mySav.get_noID() + ";";
	        	
    			sHeader += "creation_date;";
	        	sColonne = "mySav.get_calDteHistoSav()";
	        	sValeur = CalendarHelper.getFormattedDate(mySav.get_calDteHistoSav(), CalendarHelper.FORMAT_YYYYMMDD_HHMMSS_tiret);
	        	myLog.debug("  - " + sColonne + " = " + sValeur);
	        	sLigne += sValeur + ";";
	        	
    			sHeader += "operator_username;";
	        	sColonne = "mySav.getUserIns()";
	        	sLigne += mySav.getUserIns() + ";";
	        	
	        	// Agence de livraison
    			sHeader += "delivery_agency_code;";
	        	sColonne = "mySav.get_agenceLivraison()";
	        	sLigne += mySav.get_agenceLivraison() + ";";
	        	
	        	// Agence d'enlèvement 
    			sHeader += "pickup_agency_code;";
	        	sColonne = "mySav.get_agenceOt()";
	        	sLigne += mySav.get_agenceOt() + ";";

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
	        	
	        	// Plusieurs numéros de téléphone portable
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
	        	
	        	// Appel de la méthode qui tient compte des propriétés pour forcer la valeur si nécessaire
	        	sValeur = this._mySTWHelper.getTelephoneDestinataire(sValeur, this._PC);
	        	
	        	sValeur = sValeur.replaceAll("\\.", "");
	        	sValeur = sValeur.replaceAll("-", "");
	        	sValeur = sValeur.replaceAll(" ", "");
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
	        	
	        	// TODO : Faut-il ajouter le type de commande get_typeCommande() ?  OU dans ce cas get_typePreva() = get_typeCommande()
	        	
	        	// Type de Commande : LDD | NON-LDD
    			sHeader += "proposal_type;";
	        	sColonne = "mySav.getLigComm().get_typePreva()";
	        	sLigne += mySav.getLigComm().get_typePreva() + ";";
	        	
	        	// Type LDD : {vide} | COMFOUR
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

	        	// Annonce de prise en charge à faire
    			sHeader += "announce_pch_to_do;";
	        	sColonne = "mySav.getLigComm().get_variableString1()";
	        	sLigne += mySav.getLigComm().get_variableString1() + ";";

	        	// Delai de prise de contact
    			sHeader += "contact_delay;";
	        	sColonne = "mySav.getLigComm().get_variableString2()";
	        	sLigne += mySav.getLigComm().get_variableString2() + ";";
    		}
    		catch ( StringIndexOutOfBoundsException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebPch : StringIndexOutOfBoundsException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( NullPointerException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebPch : NullPointerException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( TrsException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebPch : TrsException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( Exception e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebPch : Exception avec la colonne [" + sColonne + "] : " + e.toString();
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
		
		String sTexteFileName = asTargetDir + "/preva-spot-pch-" + this._fichierDate + ".csv";
		
		return sTexteFileName;
	}
}
