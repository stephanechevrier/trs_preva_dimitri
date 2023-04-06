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


public class SpotToWebLiv 
{
	// 19/02/2021 - Version pour lot 5 PREVA
	
	private String	_fichierDate = "";
	private String	_targetDir = "";
	private String _sProp = "";
	private Date _date = null;
	
	static final Logger myLog = LogManager.getLogger(SpotToWebLiv.class.getName());
	
	private List<LigComm>		_myLCList = null;
	private ProprieteCommune	_PC = null;
	private StringUtil			sUtil;
	private SpotToWebHelper		_mySTWHelper = null;
 
	
	/**
	 * <p>Initialisation puis  du fichier</p>
	 * 
	 * @throws TrsException
	 */
	public SpotToWebLiv(List<LigComm> aLCList, ProprieteCommune aPC,String asPropFileName, String asDbName) throws TrsException
	{
		String	sPropKey = "";
		
		this._myLCList = aLCList;
		this._PC = aPC;
		this._mySTWHelper = new SpotToWebHelper();
	
		// Rpertoire de stockage des fichiers  
		// ********************************************************************************************************************
		this._targetDir = aPC.getProperty(ProprieteCommune.dir_out_target_propKey, "");
		
		if ( this._targetDir.equals("") )
		{
        	throw new TrsException(aPC.getMessageErreur(1, ProprieteCommune.dir_out_target_propKey));
		}
		
		// SAV  exporter
		// ****************************************
		if ( aLCList.size() > 0 )
		{
			myLog.info("Il y a des lignes a ecrire dans le fichier des tractions pour le site web PREVA");
			
			// G du fichier
			this.generateFile(this._targetDir, aLCList);
		}
	}

	
	public void generateFile(String asTarget, List<LigComm> aLCList) throws TrsException
	{
		FileUtil	myFU = new FileUtil();
		
		PrintWriter	myPWFile = null;
		Calendar aCalendarDebut;
		Calendar aCalendarFin;
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
		
		// On   du fichier, dans pertoire cible
		sFileName = this.getFileName(asTarget, false);
		
		// Cration du rpertoire utilis
		myFU.makeDir(asTarget);
		
		try { myPWFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sFileName), "UTF-8")); }
        catch ( IOException e ) { throw new TrsException("ERREUR lors de la préparation en écriture du fichier [" + sFileName + "]"); }
        
		myLog.info("DEBUT ecriture fichier [" + sFileName + "]");
		
        // Boucle sur les SAV
        for ( LigComm myLC : aLCList )
        {
        	iNbLignes ++;
    		myLog.info("PREPARATION ecriture ligne #" + iNbLignes);
        	
    		try
    		{
    			sHeader = "";
    			sHeader += "order_number;";
    			
    			sColonne = "myLG.get_refCommande()";
	        	sLigne = myLC.get_refCommande() + ";";
	        	
	        	myLog.info("Numéro de commande : " + myLC.get_refCommande());
				
	        	
	        	// Agence d'enlèvement 
    			sHeader += "pickup_agency_code;";
	        	sColonne = "myLG.get_agenceEnlevement()";
	        	sLigne += myLC.get_agenceEnlevement() + ";";
	        	
	        	
	        	
	        	
	        	
	        	
	        	// Date début reelle de l'opération
    			sHeader += "real_delivery_date;";
	        	sColonne = "";
	        	//aCalendarDebut = CalendarHelper.getCalendar(myLC.get_heureLivraison(), "");
	        	sLigne += myLC.get_heureReelleLivraison().substring(0, myLC.get_heureReelleLivraison().length()-2) + ";";
	            
	 
	        	
	        	
	        	
	        	
	        	// Anomalie de livraison
    			sHeader += "delivery_anomaly;";
	        	sColonne = "";
	        	if(myLC.get_commentaire() == null)
	        		sLigne += ";";
	        	else
	        		sLigne += myLC.get_commentaire() + ";";
    		
	        	
	        	
	        	
	        	
	        

	
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
		
		// Mémorisation date et heure utilisé pour le nom du fichier
		this._fichierDate = myDf.format(myDate);
		
		String sTexteFileName = asTargetDir + "/preva-spot-liv-" + this._fichierDate + ".csv";
		
		return sTexteFileName;
	}
}