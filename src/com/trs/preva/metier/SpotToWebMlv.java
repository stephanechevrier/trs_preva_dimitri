
package com.trs.preva.metier;

import com.trs.action.ProprieteCommune;
import com.trs.constant.TrsConstant;
import com.trs.exception.TrsException;
import com.trs.utils.calendar.CalendarHelper;
import com.trs.utils.fileSystem.FileUtil;
import com.trs.utils.format.StringMgt;
import com.trs.utils.string.StringUtil;
import com.trs.wintrans.dbAccess.CalendrierTrsAccess;
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


public class SpotToWebMlv 
{
	// 19/02/2021 - Version pour lot 5 PREVA
	
	private String	_fichierDate = "";
	private String	_targetDir = "";
	private String _sProp = "";
	private String	asDbName = "";
	private String asPropFileName = "";
	private Date _date = null;


	static final Logger myLog = LogManager.getLogger(SpotToWebMlv.class.getName());
	
	private List<hP_OP>		_myP_OPList = null;
	private ProprieteCommune	_PC = null;
	private StringUtil			sUtil;
	private SpotToWebHelper		_mySTWHelper = null;
 
	
	/**
	 * <p>Initialisation puis génération du fichier</p>
	 * 
	 * @throws TrsException
	 */
	public SpotToWebMlv(String sProp,List<hP_OP> aP_OPList, ProprieteCommune aPC,String asPropFileName, String asDbName) throws TrsException
	{
		String	sPropKey = "";
		
		this._myP_OPList = aP_OPList;
		this._PC = aPC;
		this._mySTWHelper = new SpotToWebHelper();
		this._sProp = sProp;
		this.asDbName = asDbName;
		this.asPropFileName = asPropFileName;
		
		// Répertoire de stockage des fichiers  généré
		// ********************************************************************************************************************
		this._targetDir = aPC.getProperty(ProprieteCommune.dir_out_target_propKey, "");
		
		if ( this._targetDir.equals("") )
		{
        	throw new TrsException(aPC.getMessageErreur(1, ProprieteCommune.dir_out_target_propKey));
		}
		
		// SAV à exporter
		// ****************************************
		if ( aP_OPList.size() > 0 )
		{
			myLog.info("Il y a des lignes a ecrire dans le fichier des tractions pour le site web PREVA");
			
			// Génération du fichier
			this.generateFile(sProp,this._targetDir, aP_OPList);
		}
	}

	
	public void generateFile(String sProp,String asTarget, List<hP_OP> aP_OPList) throws TrsException
	{
		FileUtil	myFU = new FileUtil();
		
		PrintWriter	myPWFile = null;
		Calendar cCalendarDebut;
		
		boolean	bHeader = true;
		CalendrierTrsAccess myCalTrs = new CalendrierTrsAccess(this.asPropFileName,this.asDbName);
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
		sFileName = this.getFileName(sProp,asTarget, false);
		
		// Création du répertoire utilisé
		myFU.makeDir(asTarget);
		
		try { myPWFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sFileName), "UTF-8")); }
        catch ( IOException e ) { throw new TrsException("ERREUR lors de la préparation en écriture du fichier [" + sFileName + "]"); }
        
		myLog.info("DEBUT ecriture fichier [" + sFileName + "]");
		
        // Boucle sur les SAV
        for ( hP_OP myP_OP : aP_OPList )
        {
        	iNbLignes ++;
    		myLog.info("PREPARATION ecriture ligne #" + iNbLignes);
        	
    		try
    		{
    			sHeader = "";
    			sHeader += "order_number;";
    			
    			sColonne = "myLG.get_refCommande()";
	        	sLigne = myP_OP.get_refCommande() + ";";
	        	
	        	myLog.info("Numéro de commande : " + myP_OP.get_refCommande());
				
	        	
	        	// Agence d'enlèvement 
    			sHeader += "pickup_agency_code;";
	        	sColonne = "myLG.get_agenceEnlevement()";
	        	sLigne += myP_OP.get_agenceEnlevement() + ";";
	        	
	        	
	        	// Agence d'enlèvement 
    			sHeader += "preva_order_id;";
	        	sColonne = "myLG.get_agenceEnlevement()";
	        	sLigne +=  ";";
	        	
	        	
	        	
	        	// Date début reelle de l'opération
    			sHeader += "pre_shipping_date;";
	        	sColonne = "";
	        	cCalendarDebut = myCalTrs.getPrevJourOuvre(myP_OP.getDatePrevJourOuvre());
	        	sLigne += CalendarHelper.getFormattedDate(cCalendarDebut, CalendarHelper.FORMAT_YYYYMMDD_HHMMSS_tiret).substring(0,11) + " " + myP_OP.get_sHeureDeDebut() + ";";
	        	
	        	
	
	        	// Agence de Livraison
    			sHeader += "type;";
	        	sColonne = "";
	        	sLigne += myP_OP.get_enregistrement() + ";";
	        	
	        	
	        	
	        	
	        	
	        

	
    		}
    		catch ( StringIndexOutOfBoundsException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebMlv : StringIndexOutOfBoundsException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( NullPointerException e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebMlv : NullPointerException avec la colonne [" + sColonne + "] : " + e.toString();
    			throw new TrsException(sRetour);
    		}
    		catch ( Exception e )
    		{
    			sRetour = "ERREUR dans la generation du fichier SpotToWebMlv : Exception avec la colonne [" + sColonne + "] : " + e.toString();
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
		
		// Mémorisation date et heure utilisées pour le nom du fichier
		this._fichierDate = myDf.format(myDate);
		
		String sTexteFileName = asTargetDir + "/preva-spot-mlv-" + this._fichierDate + ".csv";
		
		return sTexteFileName;
	}
}