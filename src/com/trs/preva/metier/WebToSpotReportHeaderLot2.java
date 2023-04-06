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

/**
 * <p>En-tête du report envoyé à chaque exploitation</p>
 * 
 * @author Jean-Noël CATTIN
 *
 */
public class WebToSpotReportHeaderLot2 
{
	private List<WebToSpotReportLineLot2> _myWTSRLineList = new ArrayList<WebToSpotReportLineLot2>();
	
	private ProprieteCommune _myProps = null;
	
	static final Logger myLog = LogManager.getLogger(WebToSpotReportHeaderLot2.class.getName());
	
	private	String	_agenceLivraison = "";
	
	
	public WebToSpotReportHeaderLot2(ProprieteCommune aProps, String asCodeAgence) throws TrsException
	{
		this._myProps = aProps;
		this._agenceLivraison = asCodeAgence;
	}
	
	
	public String getAgenceLivraison()
	{
		return this._agenceLivraison;
	}

	
	public ProprieteCommune getProperties()
	{
		return this._myProps;
	}
	
	
	public List<WebToSpotReportLineLot2> getWTSRLineList()
	{
		return this._myWTSRLineList;
	}
	
	
	public void addWebToSpotReportHeaderLine(String asNoLigComm, String asNoPlan, String asReponse, String asDateReponse, 
		String asTypeAcces, int aiNbReponsesNegatives, String asLoginWeb,
		String asComplementReponse, String asTypePreva,
		String asTypeLdd, String asEtage, String asNumTelLivraison, String asNumTelLivraisonOld		
		)
	{
		WebToSpotReportLineLot2 myWTSRL = null;
		
		myWTSRL = new WebToSpotReportLineLot2(asNoLigComm, asNoPlan, asReponse, asDateReponse, asTypeAcces, aiNbReponsesNegatives,
			asLoginWeb,
			asComplementReponse, asTypePreva,
			asTypeLdd, asEtage, asNumTelLivraison, asNumTelLivraisonOld		
			);
		
		this.getWTSRLineList().add(myWTSRL);
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
