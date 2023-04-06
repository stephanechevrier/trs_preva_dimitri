/**
 * 
 */
package com.trs.preva.action;

import com.trs.action.ProprieteCommune;
import com.trs.exception.AppliException;
import com.trs.exception.TrsException;
import com.trs.utils.fileSystem.FileUtil;
import com.trs.utils.format.DateTimeConversion;
import com.trs.utils.format.FormatException;
import com.trs.utils.string.StringUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Lecture des fichiers à prendre en compte
 * 
 * @author Jean-Noël CATTIN
 */
public class DirReceiver 
{
	// Log
	static final Logger myLog = LogManager.getLogger(DirReceiver.class.getName());
	
	private ProprieteCommune _proprieteCommune = null;
	
	public DirReceiver() 
	{
	}

    /**
     * <p>Boucle sur les répertoires dans lesquels on trouve les fichiers à prendre en compte.</p>
     * 
     * @param asPropFile Fichier de propriétés à lire
     */
    public void downloadFiles(String asPropFile) 
    {
    	String	sFileName	= "";
    	String	sProp 		= null;
    	String	sValeur 	= "";
    	
    	String 	sChrono = "";
    	String	sDirFtp = "";
    	String	sEdiList = "";
    	String	sTargetFile = "";
    	String	sTargetFileWithDir = "";
    	
    	FileUtil myFileUtil = new FileUtil();
    	
    	int iIndexFileToSave = 0;
    	int iRetour = 0;
    	
    	myLog.info("** DEBUT de la recuperation des fichiers sur le serveur FTP interne");
    	
    	try { _proprieteCommune = new ProprieteCommune(asPropFile); }
    	catch ( TrsException e )
    	{
    		myLog.fatal(e.toString());
    		return;
    	}
    	
    	sEdiList = _proprieteCommune.getProperty(ProprieteCommune.FTP_EDI_LIST, "").trim();
    	
    	// Liste vide : pas d'EDI concerné
    	if ( sEdiList.equals("") )
    	{
    		myLog.info("Pas d'EDI a prendre en compte (" + _proprieteCommune.getKeyWithEnvironment(ProprieteCommune.FTP_EDI_LIST)
    			+ " vide dans fichier de proprietes)");
    		return;
    	}
    	
        // Lecture de la propriété décrivant le répertoire où stocker les fichiers récupérés sur le serveur FTP
        sProp = ProprieteCommune.dir_filAttente_propKey;
        String sDirFileAttente = _proprieteCommune.getProperty(sProp);
        if ( sDirFileAttente == null )
        {
        	myLog.fatal("Repertoire de stockage des fichiers EDI - propriete [" + sProp + "] - NON decrit dans [" + asPropFile + "]"); 
        	return;
        }
        sDirFileAttente = StringUtil.getDirPath(true, sDirFileAttente);
        
        // Propriété décrivant le répertoire de configuration
        String sDirConf = "";
        try { sDirConf = _proprieteCommune.getDirConf(); }
        catch ( TrsException e )
        {
        	myLog.fatal(e.toString()); 
        	return;
        }
        sDirConf = StringUtil.getDirPath(true, sDirConf);
        
        // Découpage de la liste des EDI à traiter
        String sTabEdi[] = sEdiList.split(",");
        
        for ( int iEdi = 0; iEdi < sTabEdi.length; iEdi ++ )
        {
        	//sProp = sTabEdi[iEdi] + ".ftp.dir" + "." + ProprieteCommune._sEnvironment;
        	sProp = sTabEdi[iEdi] + ".ftp.dir" + "." + _proprieteCommune._sEnvironment;
        	sDirFtp = _proprieteCommune.getProperty(sProp, "");
        	
        	if ( sDirFtp.equals("") )
        	{
            	myLog.fatal("Propriete [" + sProp + "] NON defini dans fichier de proprietes [" + asPropFile + "]"); 
            	return;
        	}
        	
        	String sTabFichiersEdi[] = myFileUtil.getFichiers(sDirFtp, "");
        	
        	for ( int jFichier = 0; jFichier < sTabFichiersEdi.length; jFichier ++ )
        	{
    	    	iIndexFileToSave ++;
    	    	
	        	try
		    	{
		    		DateTimeConversion myDateTimeConversion = new DateTimeConversion();
		    		sChrono = myDateTimeConversion.getCurrentDateTime(2) + "-" + iIndexFileToSave;
		    	}
		    	catch ( FormatException e ) { sChrono = "00000000_0000" + "-" + iIndexFileToSave; }
	        	
	        	sTargetFile = sTabEdi[iEdi] + "__" + sChrono + "__" + sTabFichiersEdi[jFichier];
	        	sTargetFileWithDir = sDirFileAttente + sTargetFile;
	        	
	        	myLog.info("ESSAI deplacement [" + sDirFtp + "/" + sTabFichiersEdi[jFichier] + "] vers [" + sTargetFileWithDir + "]") ;
	        	iRetour = myFileUtil.copyBytes(sDirFtp + "/" + sTabFichiersEdi[jFichier], sTargetFileWithDir, true);
	        	
	        	// 16/03/2015 - En production, le fichier ne disparait pas
	        	if ( myFileUtil.fileExist(sDirFtp + "/" + sTabFichiersEdi[jFichier]) )
	        	{
	        		try { myFileUtil.deleteFile(sDirFtp + "/" + sTabFichiersEdi[jFichier]); }
	        		catch ( AppliException e ) { myLog.fatal("ERREUR suppression fichier [" + sDirFtp + "/" + sTabFichiersEdi[jFichier] + "] : " + e.toString()); }
	        	}
	        	
	        	if ( iRetour < 0 )
	        	{
	            	myLog.fatal("ERREUR lors de la recuperation des fichiers a lire sur le serveur FTP interne"); 
	            	return;
	        	}
        	}
        }
    }
}
