package com.trs.preva.metier;

import com.trs.action.ProprieteCommune;
import com.trs.exception.TrsException;
import com.trs.utils.email.MailNotification;
import com.trs.utils.properties.Properties;
import com.trs.utils.string.StringUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebToSpotLot2 
{
	private List<WebToSpotLineLot2> _myWTSLineList = new ArrayList<WebToSpotLineLot2>();
	
	private ProprieteCommune _myProps = null;
	
	private Date _date = new Date(System.currentTimeMillis());
	
    private	BufferedReader 	_myBr = null;
    private	FileReader 		_myFr = null;
    
    private String			_myHeader = "";
    
	static final Logger myLog = LogManager.getLogger(WebToSpotLot2.class.getName());
	
	public WebToSpotLot2(String asFileName, ProprieteCommune aProps, String asDirConf) throws TrsException
	{
		this._myProps = aProps;
		
		// Lecture en-tête
		this.init(asFileName);
		// Lecture des données
		this.loadFile(asFileName, asDirConf);
	}
	
	/**
	 * <p>Lecture de la ligne d'en-tête pour contrôle de format</p>
	 * 
	 * @param asFichier
	 * @throws TrsException
	 */
	private void init(String asFichier) throws TrsException
	{
		String 	sHeader = "";
        String 	sLigne = "";
        String	sPropKey = "webToSpotLot2.header";
        
		// Lecture du format de ligne d'en-tête attendue
		sHeader = this._myProps.getProperty(sPropKey, "").trim().toLowerCase();
		
		if ( sHeader.equals("") )
        	throw(new TrsException(this._myProps.getMessageErreur(1, sPropKey)));
		
		// Lecture du fichier à traiter
		try { _myFr = new FileReader(new File(asFichier)); }
		catch ( FileNotFoundException e ) { throw new TrsException("ERREUR ouverture fichier [" + asFichier + "] : " + e.toString()); }
		catch ( Exception e ) { throw new TrsException(e); }
		
		_myBr = new BufferedReader(_myFr);
		
		// 1ère ligne du fichier
		// ******************************************************************
		try { sLigne = _myBr.readLine(); }
		catch ( IOException e ) 
		{
			this.closeFile(_myBr);
			throw new TrsException("ERREUR lecture fichier [" + asFichier + "] : " + e.toString()); 
		}
		
		sLigne = sLigne.trim().toLowerCase();
		
		// SI la ligne lue ne correspond pas au bon format, ERREUR
		if ( ! sHeader.equals(sLigne) )
		{
			this.closeFile(_myBr);
			throw new TrsException("ERREUR lecture fichier [" + asFichier + "] : ligne d'en-tete [" + sLigne 
				+ "] DIFFERENTE de celle attendue " 
				+ "[" + sHeader + "] : valeur lue dans [" + this._myProps.getPropFileName() + "]"); 
		}
		
		this._myHeader = sHeader;
	}
	
	private void loadFile(String asFileName, String asDirConf) throws TrsException
	{
		boolean	bColInconnue = false;
		
		String	sColHeader = "";
        String 	sLigne = "";
        
		WebToSpotLineLot2	myWTSLine = null;
		
		int iIndexLigne = 0;
		
		// Analyse du Header
		String[] sTabHeader = this._myHeader.split(";");

		// Lecture des données à formater
		try
		{
			while ( this._myBr.ready() )
			{
				sLigne = this._myBr.readLine();
				String sTabData[] = sLigne.split(";", -1);

				iIndexLigne ++;
				
				// Nouvelle ligne
				myWTSLine = new WebToSpotLineLot2();
				
				// Boucle sur les types de données
				for ( int i = 0 ; i < sTabHeader.length ; i ++ )
				{
					bColInconnue = true;
					sColHeader = sTabHeader[i].toLowerCase();
					myLog.debug(sColHeader + " = " + sTabData[i]);
					
					switch ( sColHeader )
					{
						// Type d'accès au site web : le destinataire ou TRS a répondu
						case "access_type":
							myWTSLine.set_typeAcces(sTabData[i]);
							bColInconnue = false;
							break;

						// 01/04/2020 - Nouvelle information lot 2
						case "additional_response":
							myWTSLine.set_reponseComplement(sTabData[i]);
							bColInconnue = false;
							break;
						// 01/04/2020 - FIN modif

						case "agency_code":
							myWTSLine.set_agence(sTabData[i]);
							bColInconnue = false;
							break;
							
						case "creation_date":
							myWTSLine.set_dateCreation(sTabData[i]);
							bColInconnue = false;
							break;

						// Agence de livraison
						case "delivery_agency_code":
							myWTSLine.set_agenceLivraison(sTabData[i]);
							bColInconnue = false;
							break;
							
						case "delivery_estimated_date":
							myWTSLine.set_datePrevLivraison(sTabData[i]);
							bColInconnue = false;
							break;
							
						// 01/04/2020 - Nouvelle information lot 2
						//case "delivery_phone":
						case "updated_delivery_phone":
							myWTSLine.set_numTelLivraison(sTabData[i]);
							bColInconnue = false;
							break;
						// 01/04/2020 - FIN modif
							
						// 01/04/2020 - Nouvelle information lot 2
						case "floor_number":
							myWTSLine.set_etage(sTabData[i]);
							bColInconnue = false;
							break;
						// 01/04/2020 - FIN modif
							
						// 01/04/2020 - Nouvelle information lot 2
						case "home_delivery_type":
							myWTSLine.set_typeLdd(sTabData[i]);
							bColInconnue = false;
							break;
						// 01/04/2020 - FIN modif
							
						// Code client
						/*
						case "manufacturer_code":
							// On n'en fait rien
							bColInconnue = false;
							break;
						*/
							
						// Nom client
						/*
						case "manufacturer_name":
							// On n'en fait rien
							bColInconnue = false;
							break;
						*/
							
						case "number_of_negative_responses":
							myWTSLine.set_nbReponsesNegatives(sTabData[i]);
							bColInconnue = false;
							break;
							
						// 01/04/2020 - Nouvelle information lot 2
						//case "old_delivery_phone":
						case "delivery_phone":
							myWTSLine.set_numTelLivraisonOld(sTabData[i]);
							bColInconnue = false;
							break;
						// 01/04/2020 - FIN modif
							
						case "op_number":
							myWTSLine.set_noOp(sTabData[i]);
							bColInconnue = false;
							break;
	
						case "operator_username":
							myWTSLine.set_loginWeb(sTabData[i]);
							bColInconnue = false;
							break;
	
						case "order_number":
							myWTSLine.set_noLigComm(sTabData[i]);
							bColInconnue = false;
							break;
							
						// Référence commande destinataire
						/*
						case "order_recipient_number":
							// On n'en fait rien
							bColInconnue = false;
							break;
						*/
					
						case "ot_number":
							myWTSLine.set_noOt(sTabData[i]);
							bColInconnue = false;
							break;

						// Agence d'enlèvement / Agence OT
						case "pickup_agency_code":
							myWTSLine.set_agenceEnlevement(sTabData[i]);
							bColInconnue = false;
							break;
							
						case "plan_number":
							myWTSLine.set_noPlan(sTabData[i]);
							bColInconnue = false;
							break;
							
						// Information lot 2
						case "proposal_type":
							myWTSLine.set_typePreva(sTabData[i]);
							bColInconnue = false;
							break;
							
						// Nom destinataire
						/*
						case "recipient_name":
							// On n'en fait rien
							bColInconnue = false;
							break;
						*/
							
						case "response":
							myWTSLine.set_reponse(sTabData[i]);
							bColInconnue = false;
							break;
							
						case "response_date":
							myWTSLine.set_dateReponse(sTabData[i]);
							bColInconnue = false;
							break;
							
						case "sav_number":
							myWTSLine.set_noSav(sTabData[i]);
							bColInconnue = false;
							break;
							
						// Information lot 2
						case "upstairs_delivery":
							myLog.info("  - " + sColHeader + " = " + sTabData[i] + " (donnee non memorisee)");
							bColInconnue = false;
							break;

						// 01/04/2020 - Information lot 2
						case "type":
							// Zone à confirmer : on n'en fait rien pour l'instant ?
							
							bColInconnue = false;
							break;
					
						default:
							bColInconnue = true;
							break;
					}
					
	            	if ( bColInconnue )
	            	{
	            		this.close();
	            		
	            		throw new TrsException("ERREUR lecture fichier [" + asFileName + "],<br> colonne ["
	            			+ sColHeader + "] NON decrite dans le traitement,<br> ligne " + (iIndexLigne + 1));
	            	}
				}
				
				// Ajout de la ligne
				_myWTSLineList.add(myWTSLine);
			}
		}
		catch ( IOException e ) 
		{
			this.close();
			throw new TrsException("ERREUR lecture fichier [" + asFileName + "] : " + e.toString()); 
		}
		catch ( TrsException e ) 
		{
			this.close();
			throw new TrsException("ERREUR lecture contenu fichier [" + asFileName + "] : " + e.toString()); 
		}
		
		this.close();
	}

	public void closeFile(BufferedReader aBr)
	{
		try { aBr.close(); }
		catch ( IOException e ) 
		{ 
			// On ne fait rien car si erreur importante, déjà signalée avant  
		}
	}
	
	public void close()
	{
		try
	    {
		    this._myBr.close();
		    this._myFr.close();
	    }
	    catch ( IOException e )
	    {
		    myLog.fatal("EXCEPTION fermeture fichier : " + e.toString());
	    }
	}
	
	public List<WebToSpotLineLot2> getWTSLineList()
	{
		return this._myWTSLineList;
	}

	
	public ProprieteCommune getProperties()
	{
		return this._myProps;
	}
	
	private void sendNotifAlerteFormat(String asContenu, String asDirConf)
	{
		// 28/11/2017 - Création
		
		String	sContent = "";
		String	sValeur = "";
		
		try 
		{
			Properties myProp = new Properties();
	    	//myProp.load(asDirConf + "/" + ProprieteCommune.notifFonctionnel_properties_fileName); 23/10/2018
	    	myProp.load(asDirConf + "/" + this._myProps.ediOut_notifFonctionnel_prop_fileName);
	
	    	//MailNotification myMN = new MailNotification(myProp, ProprieteCommune._sEnvironment + ".", "notif_rdl_alerteFormat"); 16/07/2018
	    	//MailNotification myMN = new MailNotification(myProp, ProprieteCommune.get_sEnvironment() + ".", "notif_rdl_alerteFormat"); 23/10/2018
	    	MailNotification myMN = new MailNotification(myProp, this._myProps.getEnvironnement() + ".", "notif_rdl_alerteFormat");
			
			sContent = myMN.getContent();
			sContent += asContenu;
			
			// Récupération destinataires
			sValeur = this._myProps.getProperty("erreurFormatCommentaireExploitation.mailCci", "");
			if ( ! sValeur.equals("") )
				myMN.setMailCci(sValeur);
			
			myLog.info("  Envoi Cci à [" + myMN.getMailCci() + "]");
			
			sValeur = this._myProps.getProperty("erreurFormatCommentaireExploitation.mailTo", "");
			if ( ! sValeur.equals("") )
				myMN.setMailTo(sValeur);
			
			myLog.info("  Envoi à [" + myMN.getMailTo() + "]");
			
			myMN.setContent(sContent); 
			
	    	myMN.sendHTMLMail();
		}
		catch ( Exception e ) 
		{
			myLog.error(e.toString() 
				+ "\r\n" + "sContent = [" + sContent + "]"
				); 
		}
	}
}
