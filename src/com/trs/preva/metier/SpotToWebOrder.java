
package com.trs.preva.metier;

import com.trs.action.ProprieteCommune;
import com.trs.constant.TrsConstant;
import com.trs.exception.TrsException;
import com.trs.utils.calendar.CalendarHelper;
import com.trs.utils.fileSystem.FileUtil;
import com.trs.utils.format.StringMgt;
import com.trs.utils.string.StringUtil;
import com.trs.wintrans.metier.HistoSav;
import com.trs.wintrans.metier.LigComm;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
//import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
//import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SpotToWebOrder 
{
	// 19/02/2021 - Version pour lot 5 PREVA
	
	private String	_fichierDate = "";
	private String	_targetDir = "";
	private String _sProp = "";
	private Date _date = null;
	
	static final Logger myLog = LogManager.getLogger(SpotToWebOrder.class.getName());
	
	private List<hLigcomm>		_myLGList = null;
	private ProprieteCommune	_PC = null;
	private StringUtil			sUtil;
	private SpotToWebHelper		_mySTWHelper = null;
 
	
	/**
	 * <p>Initialisation puis g�n�ration du fichier</p>
	 * 
	 * @throws TrsException
	 */
	public SpotToWebOrder(String sProp,List<hLigcomm> aLGList, ProprieteCommune aPC,String asPropFileName, String asDbName) throws TrsException
	{
		String	sPropKey = "";
		
		this._myLGList = aLGList;
		this._PC = aPC;
		this._mySTWHelper = new SpotToWebHelper();
		this._sProp = sProp;
		
		// R�pertoire de stockage des fichiers � g�n�rer
		// ********************************************************************************************************************
		this._targetDir = aPC.getProperty(ProprieteCommune.dir_out_target_propKey, "");
		
		if ( this._targetDir.equals("") )
		{
        	throw new TrsException(aPC.getMessageErreur(1, ProprieteCommune.dir_out_target_propKey));
		}
		
		// SAV � exporter
		// ****************************************
		if ( aLGList.size() > 0 )
		{
			myLog.info("Il y a des lignes a ecrire dans le fichier des en-tetes commandes pour le site web PREVA");
			
			// G�n�ration du fichier
			this.generateFile(sProp,this._targetDir, aLGList);
		}
	}

	
	public void generateFile(String sProp,String asTarget, List<hLigcomm> aLGList) throws TrsException
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
		
		// On pr�pare l'�criture du fichier, dans le r�pertoire cible
		sFileName = this.getFileName(sProp,asTarget, false);
		
		// Cr�ation du r�pertoire utilis�
		myFU.makeDir(asTarget);
		
		try { myPWFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sFileName), "UTF-8")); }
        catch ( IOException e ) { throw new TrsException("ERREUR lors de la pr�paration en �criture du fichier [" + sFileName + "]"); }
        
		myLog.info("DEBUT ecriture fichier [" + sFileName + "]");
		
        // Boucle sur les SAV
        for ( hLigcomm myLG : aLGList )
        {
        	iNbLignes ++;
    		myLog.info("PREPARATION ecriture ligne #" + iNbLignes);
        	
    		try
    		{
    			sHeader = "";
    			sHeader += "order_number;";
    			
    			sColonne = "myLG.get_refCommande()";
	        	sLigne = myLG.get_refCommande() + ";";
	        	
	        	myLog.info("Num�ro de commande : " + myLG.get_refCommande());
				myLog.info("Agence : " + myLG.get_agenceEnlevement());
	        	
	        	// Agence d'enl�vement 
    			sHeader += "pickup_agency_code;";
	        	sColonne = "myLG.get_agenceEnlevement()";
	        	sLigne += myLG.get_agenceEnlevement() + ";";
	        	
	        	
	        	sHeader += "preva_order_id;";
	        	sColonne = "mySav.getLigComm().get_refCommande()";
	        	sLigne += ";";
	        	
	        	sHeader += "manufacturer_code;";
	        	sColonne = "myLG.get_codeClient()";
	        	sLigne += myLG.get_codeClient() + ";";
	        	
	        	sHeader += "manufacturer_name;";
	        	sColonne = "myLG.get_nomClient()";
	        	sLigne += myLG.get_nomClient() + ";";
	        	
	        	
	        	sHeader += "manufacturer_activity;";
	        	sColonne = "myLG.get_Client().get_qualification()";
	        	sLigne += myLG.getClient().get_qualification() + ";";
	        	
	        	

	        	sHeader += "recipient_name;";
	        	sColonne = "myLG.get_nomDestinataire()";
	        	sLigne += myLG.get_nomDestinataire().replaceAll(";", "_") + ";";
	        			
	        	
	        	// Num�ro de t�l�phone mobile destinataire 
    			sHeader += "recipient_mobile_phone;";
	        	sColonne = "mySav.getLigComm().get_mobileDestinataire()";
	        	
	        	// Ne transmettre le num�ro que si mobile !!! Identification possible uniquement si mobile fran�ais
	        	sValeur = this._mySTWHelper.getNumeroMobile(myLG.get_mobileDestinataire());
	        	
	        	// Si pas de num�ro retourn�, on essaie avec le num�ro de t�l�phone fixe
	        	if ( sValeur.equals("") )
	        	{
	        		myLog.info("PAS de numero mobile dans get_mobileDestinataire() ( = [" 
	        			+ myLG.get_mobileDestinataire() + "] ), on essaie avec get_telDestinataire() ( = ["
	        			+ myLG.get_telDestinataire() + "] )"
	        			);
	        		sValeur = this._mySTWHelper.getNumeroMobile(myLG.get_telDestinataire());
	        		
		        	if ( sValeur.equals("") )
		        		myLog.info("  - PAS de numero mobile trouve dans la zone specifiquement prevu pour cela dans Wintrans");
	        	}
	        	
	        	// SI le num�ro est celui d'un t�l�phone portable fran�ais, on peut le passer
	        	if ( StringMgt.isMobilePhone(sValeur) )
	        	{
	        		// On formate le num�ro au cas o�
	        		sValeur = StringMgt.formatPhoneNumer(sValeur);

	        		myLog.info("  - [" + sValeur + "] conserve CAR numero de telephone mobile francais");
	        	}
	        	// Pour les autres, il faut ne pas passer si cela correspond � un num�ro de fixe fran�ais
	        	else
	        	{
	        		sValeur2 = StringMgt.formatPhoneNumer(sValeur);
	        		
	        		// SI le num�ro d�bute par 00, il faut le passer : c'est un num�ro �tranger
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
	        	
	        	// M�morisation du premier num�ro de t�l�phone mobile 
	        	sNumTelDestinataire = sValeur;
	        	
	        	// Plusieurs num�ros de t�l�phone portable
	        	if ( this._PC.getProperty("numPortable.plusieurs", "non").equals("oui") )
	        	{
	        		// 1 num�ro d�j� pr�sent ET des num�ros suppl�mentaires
	        		if ( ! sValeur.equals("") && ! myLG.get_mobileDestinataireSup().equals("") )
	        		{
	        			myLog.info("Utilisation des numeros de telephone portable supplementaires");
	        			//myLog.fatal("Utilisation des numeros de telephone portable supplementaires");
	        			sValeur += ",";
	        		}
	        		
	        		if ( ! myLG.get_mobileDestinataireSup().equals("") )
	        			sValeur += myLG.get_mobileDestinataireSup().replaceAll(";", ",");
	        	}
	        	
	        	// Appel de la m�thode qui tient compte des propri�t�s pour forcer la valeur si n�cessaire
	        	sValeur = this._mySTWHelper.getTelephoneDestinataire(sValeur, this._PC);
	        	
	        	sValeur = sValeur.replaceAll("\\.", "");
	        	sValeur = sValeur.replaceAll("-", "");
	        	sValeur = sValeur.replaceAll(" ", "");
	        	sLigne += sValeur + ";";
	        	
	        	// Mail destinataire 
    			sHeader += "recipient_email;";
	        	sColonne = "myLG.get_eMailDestinataire()";
	        	
	        	sValeur = myLG.get_eMailDestinataire();

	        	// PRENDRE en compte plusieurs adresses mail : s�parateur = , : fait dans la m�thode appel�e
	        	// La m�thode ci-dessous prend en compte le for�age indiqu� dans le fichier de propri�t�
	        	sValeur = this._mySTWHelper.getEmailDestinataire(sValeur, myLG.get_commentaire(), this._PC);
	        	
	        	sLigne += sValeur + ";";
	        	
	        	
	        	
    			sHeader += "address_line_1;";
	        	sColonne = "myLG.get_adresseDestLigne1()";
	        	sLigne += myLG.get_adresseDestLigne1().replaceAll(";", "_") + ";";
	        	
    			sHeader += "address_line_2;";
	        	sColonne = "myLG.get_adresseDestLigne2()";
	        	sLigne += myLG.get_adresseDestLigne2().replaceAll(";", "_") + ";";
	        		
    			sHeader += "address_zip_code;";
	        	sColonne = "myLG.get_cpDestinataire()";
	        	sLigne += myLG.get_cpDestinataire() + ";";
	        			
    			sHeader += "address_city;";
	        	sColonne = "myLG.get_villeDestinataire()";
	        	sLigne += myLG.get_villeDestinataire().replaceAll(";", "_") + ";";
	        	
	        	sHeader += "address_country;";
	        	sColonne = "myLG.get_paysDestinataire()";
	        	sLigne += myLG.get_paysDestinataire() + ";";
	        	

	        			
	        	sHeader += "number_of_packages_to_transport;";
	        	sColonne = "myLG.get_nbColisReel()";
	        	sLigne += myLG.get_nbColisReel() + ";";
	        	myLog.info("NB_COLIS : " + myLG.get_nbColisReel() );
	        	
	        	// TODO : Faut-il ajouter le type de commande get_typeCommande() ?  OU dans ce cas get_typePreva() = get_typeCommande()
	        	
	        	sHeader += "exploitation_comment;";
	        	sColonne = "mySav.get_commentaire()";
	        	sLigne += myLG.get_commentaire().replaceAll("[\r\n]+", "").replaceAll(";", ",") + ";";
	        	
	        	
	        	sHeader += "creation_date;";
	        	sColonne = "mySav.get_calDteHistoSav()";
	        	//sValeur = CalendarHelper.getFormattedDate(myLG.get_dateCreation(), CalendarHelper.FORMAT_YYYYMMDD_HHMMSS_tiret);
	        	//sValeur = myLG.get_dateCreation();
	        	Calendar aCalendar = CalendarHelper.getCalendar(myLG.get_dateCreation(), "");
	        	sLigne += CalendarHelper.getFormattedDate(aCalendar, CalendarHelper.FORMAT_YYYYMMDD_HHMMSS_tiret) + ";";
	        	
	        	

	        	sHeader += "delivery_estimated_date;";
	        	sColonne = "mySav.get_calDteHistoSav()";
	        	sLigne += ";";
	        	
	        	sHeader += "order_manufacturer_reference;";
	        	sValeur = StringUtil.getBloc(myLG.get_commentaire(), "CDES(");
	        	
	        	myLog.debug("  - Reference destinataire issu du commentaire exploitation = [" + sValeur + "]");
	        	
	        	myLog.info("1 - Reference commande " + sValeur);
	        	if ( sValeur.equals(StringUtil.ERROR_BLOC_DEBUT) )
	        		sValeur = "";
	        	if ( sValeur.equals("ERROR@CDES(") )
	        		sValeur = "";
	        	myLog.info("2 - Reference commande " + sValeur);
	        	if ( sValeur.equals("") )
	        	{
	        		sValeur = myLG.get_refClient();
	        		
	        		if ( sValeur == null )
	        			sValeur = "";
	        		
		        	myLog.debug("  - Reference destinataire issu de LIGCOMM.REF_CLIENT = [" + sValeur + "]");
	        	}
	        	myLog.info("3 - Reference commande " + sValeur);
	        	// Limitation longueur � 255 caract�res : si pr�sence de r�f�rences destinataire issues du commentaire exploitation,
	        	// cela peut etre tr�s long !
	        	if ( sValeur.length() > 255 )
	        	{
	        		myLog.info("    - Reference destinataire TROP longue : on tronque a 255 caracteres");
	        		sValeur = sValeur.substring(0, 255);
	        	}
	        	myLog.info("4 - Reference commande " + sValeur);
	        	sColonne = "mySav.get_commentaire()";
	        	sLigne += sValeur + ";";
	        	
	        	sHeader += "order_recipient_reference;";
	        	sValeur = StringUtil.getBloc(myLG.get_commentaire(), "RefDest(");
	        	
	        	myLog.debug("  - Reference destinataire issu du commentaire exploitation = [" + sValeur + "]");
	        	
	        	if ( sValeur.equals(StringUtil.ERROR_BLOC_DEBUT) )
	        		sValeur = "";
	        	
	        	if ( sValeur.equals("") )
	        	{
	        		sValeur = myLG.get_refClient();
	        		
	        		if ( sValeur == null )
	        			sValeur = "";
	        		
		        	myLog.debug("  - Reference destinataire issu de LIGCOMM.REF_CLIENT = [" + sValeur + "]");
	        	}
	        	
	        	// Limitation longueur � 255 caract�res : si pr�sence de r�f�rences destinataire issues du commentaire exploitation,
	        	// cela peut etre tr�s long !
	        	if ( sValeur.length() > 255 )
	        	{
	        		myLog.info("    - Reference destinataire TROP longue : on tronque a 255 caracteres");
	        		sValeur = sValeur.substring(0, 255);
	        	}
	        	
    			sColonne = "reference destinataire";
    			// Valeur entre " si pr�sence ;
	        	sLigne += "\"" + sValeur + "\"" + ";";
	    
	        	
	        
	        

	
    		}
    		catch ( StringIndexOutOfBoundsException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebOrder : StringIndexOutOfBoundsException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( NullPointerException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebOrder : NullPointerException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( TrsException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebOrder : TrsException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( Exception e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebOrder : Exception avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		
    		// Ecriture de la ligne d'en-tete : uniquement sur la 1ere ligne
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
	
	private String getFileName(String sprop,String asTargetDir, boolean abExtensionCsv)
	{
		Date myDate = new Date(System.currentTimeMillis());
		this._date = myDate;
		
		DateFormat myDf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		
		// M�morisation date et heure utilis�es pour le nom du fichier
		this._fichierDate = myDf.format(myDate);
		
		String sTexteFileName = asTargetDir + "/preva-spot-order-" + this._fichierDate + ".csv";
		
		return sTexteFileName;
	}
}