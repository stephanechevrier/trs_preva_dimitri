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


public class SpotToWebHelper 
{
	// 19/02/2021 - Création pour le lot 3 PREVA
	
	static final Logger myLog = LogManager.getLogger(SpotToWebHelper.class.getName());
	
	public String getNumeroMobile(String asValeur)
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
	
	
	public String getEmailDestinataire(String asValeur, String asCommentaire, ProprieteCommune aPC) throws TrsException, Exception
	{
		String			sErreur = "";
		String			sPropKey = "";
		String			sProperty = "";
		String			sRetour = "";
		
		List<String>	sListValeur = null;

		sPropKey = aPC.getEnvironnement() + ".destinataire.eMail.force";
		sProperty = aPC.getProperty(sPropKey, "non");
		
		if ( sProperty.equals("oui") )
		{
			myLog.info("Mail destinataire FORCE comme l'indique le fichier de proprietes [" + aPC.getPropFileName() + "] "
				+ "avec propriete " + sPropKey
				);
			
			sPropKey = aPC.getEnvironnement() + ".destinataire.eMail";
			sProperty = aPC.getProperty(sPropKey, "");
			
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

	
	public String getTelephoneDestinataire(String asValeur, ProprieteCommune aPC) throws TrsException
	{
		String	sErreur = "";
		String	sPropKey = "";
		String	sProperty = "";
		String	sRetour = asValeur;
		
		sPropKey = aPC.getEnvironnement() + ".destinataire.telephone.force";
		sProperty = aPC.getProperty(sPropKey, "non");
		
		if ( sProperty.equals("oui") )
		{
			myLog.info("Telephone destinataire FORCE comme l'indique le fichier de proprietes [" + aPC.getPropFileName() + "] "
				+ "avec propriete " + sPropKey
				);
			
			sPropKey = aPC.getEnvironnement() + ".destinataire.telephone";
			sProperty = aPC.getProperty(sPropKey, "");
			
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