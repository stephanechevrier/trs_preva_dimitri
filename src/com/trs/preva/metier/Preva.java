package com.trs.preva.metier;

//import com.trs.constant.TechnicalConstant;
import com.trs.action.ProprieteCommune;
import com.trs.constant.TrsConstant;
import com.trs.exception.AppliException;
import com.trs.exception.TrsException;
import com.trs.inOrder.InOrderFileLine;
import com.trs.utils.fileSystem.FileUtil;
import com.trs.utils.format.StringMgt;
//import com.trs.preva.rapport.SpotToWebLineComparable;
//import com.trs.utils.email.MailNotification;
//import com.trs.utils.properties.Properties;
//import com.trs.wintrans.dbAccess.HistoSavAccess;
//import com.trs.wintrans.dbAccess.LigColisAccess;
import com.trs.wintrans.metier.HistoSav;



import com.trs.wintrans.scontr.ScontrLineMessage;

import java.io.BufferedReader;
//import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.FileReader;
import java.io.FileWriter;
//import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
//import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Preva 
{
	private String 	_alerteFormat = "";
	private String	_enTete = "";
	private String	_fichierDate = "";
	private String	_targetDir = "";
	
	private Date 	_date = null;

    private	BufferedReader	_BR = null;
    private FileReader 		_FR = null;


	private ProprieteCommune _PC = null;
	
	static final Logger myLog = LogManager.getLogger(Preva.class.getName());
	
	// 01/10/2021 - WebToSpotLine > WebToSpotLineLot2
	private List<WebToSpotLineLot2>		_myWTSList = null;
	private List<String>				_myWTSLignesList = null;
	
	/**
	 * <p>Initialisation puis génération du fichier</p>
	 * 
	 * @throws TrsException
	 */
	public Preva(ProprieteCommune aPC) throws TrsException
	{
		this._PC = aPC;
	}
	
	public void readFileWebToSpot(String asFileName) throws TrsException
	{
        String	sAlerteFormat = "";
        String 	sLigne = "";
        String	sRetour = "";
        String 	sValeur = "";
        String 	sValeurDebug = "";
        
    	// 01/10/2021 - WebToSpotLine > WebToSpotLineLot2
		WebToSpotLineLot2	myWTS = null;
		
		int iIndexLigne = 0;
		int iNbColonnes = 0;
		int iNbLignesVides = 0;
		
		boolean		bColInconnue = false;
		boolean		bDebug = false;
		boolean		bFinFichier = false;
		boolean		bTreatLigne = false;

		try { this._FR = new FileReader( new File(asFileName) ); }
		catch ( FileNotFoundException e ) { throw new TrsException("ERREUR ouverture fichier [" + asFileName + "] : " + e.toString()); }
		catch ( Exception e ) { throw new TrsException(e); }
		
		this._BR = new BufferedReader(this._FR);
		
		// Lecture de l'en-tête
		try
		{
			iIndexLigne ++;
			sLigne = this._BR.readLine();
		}
		catch ( IOException e ) 
		{
			this.close();
			throw new TrsException("ERREUR lecture fichier [" + asFileName + "] : " + e.toString()); 
		}
		
		// Interprétation de l'en-tête
		if ( sLigne == null | sLigne.equals("" ) )
		{
			this.close();
			throw new TrsException("ERREUR lecture fichier [" + asFileName + "] : ligne d'en-tête absente"); 
		}
			
		String sTabEnTete[] = sLigne.split(";", -1);
		iNbColonnes = sTabEnTete.length;
		
		// Format l'en-tête
		if ( iNbColonnes < 5 )
		{
			this.close();
			throw new TrsException("ERREUR lecture fichier [" + asFileName + "] : ligne d'en-tête "
				+ "avec NON suffisamment de colonnes"); 
		}
		
		while ( sLigne != null && ! bFinFichier )
        {
			iIndexLigne ++;
			
			if ( sLigne.equals("") )
        		bTreatLigne = false;
        	// Présence d'une ligne vide
        	else if ( this.isLigneVide(sLigne) )
        	{
        		bTreatLigne = false;
        		iNbLignesVides ++;
        	}
        	else
        		bTreatLigne = true;
        		
        	if ( bTreatLigne )
        	{
        	// On mémorise la ligne
        	this.addRowWTS(sLigne);
        			
        	// 01/10/2021 - WebToSpotLine > WebToSpotLineLot2
        	myWTS = new WebToSpotLineLot2();
	
	        String[] sData = sLigne.split(";", -1);
	
	    	int iColCounts = sData.length;
		    int iIndexCol = -1;
		        	
		    // TANT QUE dernière colonne du fichier EDI non atteinte
		    // ET fin du fichier non atteinte
		    // ET fin du tableau des correspondances non atteinte
		    while ( iIndexCol < iColCounts && ! bFinFichier && iIndexCol < iNbColonnes - 1 ) 
		    {		        
		    	iIndexCol ++;
		    	bColInconnue = true;
	        			
	        	try { sValeur = sData[iIndexCol].trim(); }
	        	catch ( ArrayIndexOutOfBoundsException e)
	        	{
			        throw new TrsException("ERREUR lecture fichier [" + asFileName + "],<br> impossible de lire la colonne "
				    	+ ( iIndexCol + 1 ) + " [" + sTabEnTete[iIndexCol].toLowerCase() + "] : "
				        + "<b><u>vérifier</u></b> le contenu de la <b>ligne " + ( iIndexLigne + 1 ) + "</b> dans un programme tel que Notepad++ : "
				        + "Est-ce que la ligne est complète ? Y-a t'il un saut de ligne qui interrompt la ligne ?");
	        	}
       			catch ( Exception e ) 
       			{ 
            		this.close();
		            		
            		throw new TrsException("ERREUR lecture fichier [" + asFileName + "],<br> impossible de lire la colonne "
            			+ ( iIndexCol + 1 ) + " ["
            			+ sTabEnTete[iIndexCol].toLowerCase() + "],<br> ligne " + ( iIndexLigne + 1 ) + " : <b><u>vérifier</u></b> le <b>format du fichier</b>"
            			+ "ou la <b>longueur de la ligne indiquée en erreur</b> (ouvrir le fichier dans Notepad++)"
            			+ " : Erreur = " + e.toString());
	        			}
	        			
		            sValeurDebug = sValeur;
		            	
	            	if ( bDebug & ! sTabEnTete[iIndexCol].equals("") )
		            	myLog.debug("  Ligne [" + (iIndexLigne + 1) + "], colonne [" + (iIndexCol + 1) + ", '" + sTabEnTete[iIndexCol].toLowerCase() + "']");
		            	
		            try
		            {
				       	switch ( sTabEnTete[iIndexCol].toLowerCase() )
				       	{
				        	case "access_type":
				        		myWTS.set_typeAcces(sValeur);
				        			
			        			bColInconnue = false;
			        			break;

				        	case "number_of_negative_responses":
				        		myWTS.set_nbReponsesNegatives(sValeur);
				        			
			        			bColInconnue = false;
			        			break;
		        			
				       		// op_number = ot_number
				        	case "op_number":
				        		myWTS.set_noOp(sValeur);
				        			
			        			bColInconnue = false;
			        			break;

				        	case "operator_username":
				        		myWTS.set_loginWeb(sValeur);
				        			
			        			bColInconnue = false;
			        			break;
			        			
				        	case "order_number":
				        		myWTS.set_noLigComm(sValeur);
				        			
			        			bColInconnue = false;
			        			break;
				        			
				       		// op_number = ot_number
				        	case "ot_number":
				        		myWTS.set_noOp(sValeur);
				        			
			        			bColInconnue = false;
			        			break;
			        			
				        	case "plan_number":
				        		myWTS.set_noPlan(sValeur);
				        			
			        			bColInconnue = false;
			        			break;
			        			
				        	case "response":
				        		myWTS.set_reponse(sValeur);
				        			
			        			bColInconnue = false;
			        			break;
			        			
				        	case "response_date":
				        		myWTS.set_dateReponse(sValeur);
				        			
			        			bColInconnue = false;
			        			break;
			        			
				        	case "sav_number":
				        		myWTS.set_noSav(sValeur);
				        			
			        			bColInconnue = false;
			        			break;

			        			// Colonne non utilisée
					        	case "":
					        		bColInconnue = false;
					        		break;

				       	}
		            }
	            	catch ( TrsException e1 )
	            	{
	            		this.close();
		            		
	            		sRetour = "<p>ERREUR lecture fichier [" + asFileName + "],<br><br> ligne [" + ( iIndexLigne + 1 ) + "],<br> "
		            		+ "colonne [" + (iIndexCol + 1) + ", " + sTabEnTete[iIndexCol].toLowerCase() + "],<br> "
		            		+ "dernière valeur lue = [<b>" + sValeurDebug + "</b>],<br><br> ligne original = [" 
		            		+ sLigne + "],<br><br> erreur = [" 
		            		+ e1.getContentForNotification() + "]</p>";

	            		throw new TrsException(sRetour);
		            }
	            	catch ( Exception e2 )
	            	{
	            		this.close();
	            		
	            		sRetour = "<p>ERREUR lecture fichier [" + asFileName + "],<br><br> ligne [" + ( iIndexLigne + 1 ) + "],<br> "
		            		+ "colonne [" + ( iIndexCol + 1 ) + ", " + sTabEnTete[iIndexCol].toLowerCase() + "],<br> "
		            		+ "dernière valeur lue = [<b>" + sValeurDebug + "</b>],<br><br> ligne original = [" 
		            		+ sLigne + "],<br><br> erreur = [" 
		            		+ e2.toString() + "]</p>";

	            		throw new TrsException(sRetour);
	            	}
		            
	            	if ( bColInconnue )
	            	{
	            		// 22/05/2015
	            		this.close();
	            		
	            		throw new TrsException("ERREUR lecture fichier [" + asFileName + "],<br> colonne ["
	            			+ sTabEnTete[iIndexCol].toLowerCase() + "] INCONNUE,<br> ligne " + (iIndexLigne + 1));
	            	}
	            }
		    
		    	this._myWTSList.add(myWTS);
        	}
		            
        	// Ligne suivante
    		try { sLigne = this._BR.readLine().trim(); }
    		catch ( IOException e ) { throw new TrsException("ERREUR lecture ligne " + ( iIndexLigne + 1 ) + " du fichier [" + asFileName + "] : " + e.toString()); }
    		catch ( NullPointerException e )
    		{
    			myLog.debug("FIN de fichier");
    			bFinFichier = true;
    		}
    		
    		// SI la ligne est vide
    		if ( sLigne == null )
    			sLigne = "";

    		// SI la ligne est vide ALORS fin de fichier
    		if ( sLigne.equals("") )
    			bFinFichier = true;
	    }

		// Fermeture du fichier
		this.close();
	    
		// Lignes vides
		if ( iNbLignesVides > 0 )
			sAlerteFormat += "<p><b>" + iNbLignesVides + "</b> ligne(s) vide(s) trouvée(s) sur les <b>" + ( iIndexLigne + 1 ) 
				+ "</b> que contient le fichier EDI.</p>";

	    this.setAlerteFormat(sAlerteFormat);
	}
	
	public void close()
	{
		try
	    {
		    this._BR.close();
		    this._FR.close();
	    }
	    catch ( IOException e )
	    {
		    myLog.fatal("EXCEPTION fermeture fichier : " + e.toString());
	    }
	}
	
	private boolean isLigneVide(String asLigne)
	{
		// Si la ligne ne contient aucun caractère, la ligne est vide
		if ( asLigne.trim().equals("") ) return true;
		
		// Si la ligne ne contient que le séparateur, la ligne est vide
		asLigne = asLigne.replaceAll(";", "");
		if ( asLigne.trim().equals("") ) return true;
		
		return false;
	}
	
	public void closeFile(BufferedReader aBr)
	{
		try { aBr.close(); }
		catch ( IOException e ) 
		{ 
			// On ne fait rien car si erreur importante, déjà signalée avant  
		}
	}
	
	public void addRowWTS(String asLigne)
	{
		this._myWTSLignesList.add(asLigne);
	}
	
	public void setAlerteFormat(String asValeur)
	{
		this._alerteFormat = asValeur;
	}
}
