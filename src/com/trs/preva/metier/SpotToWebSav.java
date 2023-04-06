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


public class SpotToWebSav 
{
	// 19/02/2021 - Version pour lot 5 PREVA
	
	private String	_fichierDate = "";
	private String	_targetDir = "";
	private String _sProp = "";
	private Date _date = null;
	
	static final Logger myLog = LogManager.getLogger(SpotToWebSav.class.getName());
	
	private List<hHistoSav>		_myHSList = null;
	private ProprieteCommune	_PC = null;
	private StringUtil			sUtil;
	private SpotToWebHelper		_mySTWHelper = null;
 
	
	/**
	 * <p>Initialisation puis g�n�ration du fichier</p>
	 * 
	 * @throws TrsException
	 */
	public SpotToWebSav(List<hHistoSav> aHSList, ProprieteCommune aPC,String asPropFileName, String asDbName) throws TrsException
	{
		String	sPropKey = "";
		
		this._myHSList = aHSList;
		this._PC = aPC;
		this._mySTWHelper = new SpotToWebHelper();
	
		
		// R�pertoire de stockage des fichiers � g�n�rer
		// ********************************************************************************************************************
		this._targetDir = aPC.getProperty(ProprieteCommune.dir_out_target_propKey, "");
		
		if ( this._targetDir.equals("") )
		{
        	throw new TrsException(aPC.getMessageErreur(1, ProprieteCommune.dir_out_target_propKey));
		}
		
		// SAV � exporter
		// ****************************************
		if ( aHSList.size() > 0 )
		{
			myLog.info("Il y a des lignes a ecrire dans le fichier des tractions pour le site web PREVA");
			
			// G�n�ration du fichier
			this.generateFile(this._targetDir, aHSList);
		}
	}

	
	public void generateFile(String asTarget, List<hHistoSav> aHSList) throws TrsException
	{
		FileUtil	myFU = new FileUtil();
		
		PrintWriter	myPWFile = null;
		Calendar aCalendarSav;
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
		sFileName = this.getFileName(asTarget, false);
		
		// Cr�ation du r�pertoire utilis�
		myFU.makeDir(asTarget);
		
		try { myPWFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sFileName), "UTF-8")); }
        catch ( IOException e ) { throw new TrsException("ERREUR lors de la pr�paration en �criture du fichier [" + sFileName + "]"); }
        
		myLog.info("DEBUT ecriture fichier [" + sFileName + "]");
		
        // Boucle sur les SAV
        for ( hHistoSav myHS : aHSList )
        {
        	iNbLignes ++;
    		myLog.info("PREPARATION ecriture ligne #" + iNbLignes);
        	
    		try
    		{
    			sHeader = "";
    			sHeader += "order_number;";
    			
    			sColonne = "myLG.get_refCommande()";
	        	sLigne = myHS.getLigComm().get_refCommande() + ";";
	        
				
	        	
	        	// Agence d'enl�vement 
    			sHeader += "pickup_agency_code;";
	        	sColonne = "myLG.get_agenceEnlevement()";
	        	sLigne += myHS.getLigComm().get_agenceEnlevement() + ";";
	        	
	        	
	        	// Code SAV
    			sHeader += "event_code;";
	        	sColonne = "myLG.get_agenceEnlevement()";
	        	sLigne +=  myHS.get_codeSituation() + "-" + myHS.get_codeJustification() +";";
	        	
	        	
	        	// Code SAV
    			sHeader += "event_label;";
	        	sColonne = "myLG.get_agenceEnlevement()";
	        	sLigne +=   myHS.get_LibelleSav() + ";";
	        	
	        	
	        	// Code SAV
    			sHeader += "event_comment;";
	        	sColonne = "myLG.get_agenceEnlevement()";
	        	sLigne +=   myHS.get_libelle().replaceAll("[\r\n]+", "").replaceAll(";", ",") + ";";
	        	
	        	
	        	
	        	// Date SAV
    			sHeader += "event_date;";
	        	sColonne = "";
	        	//aCalendarSav = CalendarHelper.getCalendar(myHS.get_sDateHistoSav() + " " + myHS.get_sHeureHistoSav(), CalendarHelper.FORMAT_YYYYMMDD_HHMMSS_tiret);
	        	//sLigne += CalendarHelper.getFormattedDate(aCalendarSav, CalendarHelper.FORMAT_YYYYMMDD_HHMMSS_tiret) + ";";
	        	sLigne +=  myHS.get_sHeureHistoSav().substring(0, myHS.get_sHeureHistoSav().length()-2);
	 
	        	
	        	
	        	
	        	
	        
    		
	        	
	        	
	        	
	        	
	        

	
    		}
    		catch ( StringIndexOutOfBoundsException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebTraction : StringIndexOutOfBoundsException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( NullPointerException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebTraction : NullPointerException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( Exception e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebTraction : Exception avec la colonne [" + sColonne + "] : " + e.toString();
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
	
	private String getFileName(String asTargetDir, boolean abExtensionCsv)
	{
		Date myDate = new Date(System.currentTimeMillis());
		this._date = myDate;
		
		DateFormat myDf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		
		// M�morisation date et heure utilis�es pour le nom du fichier
		this._fichierDate = myDf.format(myDate);
		
		String sTexteFileName = asTargetDir + "/preva-spot-sav-" + this._fichierDate + ".csv";
		
		return sTexteFileName;
	}
}