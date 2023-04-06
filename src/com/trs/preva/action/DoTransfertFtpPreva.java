/**
 * 16/01/2023 - Création
 */
package com.trs.preva.action;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.Protocol;
import com.enterprisedt.net.ftp.SecureFileTransferClient;
import com.enterprisedt.net.ftp.WriteMode;
import com.trs.action.ProprieteCommune;
import com.trs.exception.AppliException;
import com.trs.exception.TrsException;
import com.trs.utils.fileSystem.FileUtil;
import com.trs.utils.format.DateTimeConversion;
import com.trs.utils.format.FormatException;
import com.trs.utils.java.JavaUtil;
import com.trs.utils.string.StringUtil;

import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Classe de transfert par FTP des fichiers à transmettre à PREVA
 * 
 * @author Jean-Noël CATTIN
 */
public class DoTransfertFtpPreva 
{
	// Log
	static final Logger myLog = LogManager.getLogger(DoTransfertFtpPreva.class.getName());
	
	private ProprieteCommune _myPC = null;
	
	public DoTransfertFtpPreva(ProprieteCommune aPC) 
	{
		this._myPC = aPC;
		
		// Licence FTP achetée le 25/01/2019
		com.enterprisedt.util.license.License.setLicenseDetails("TRANSPORTROUTESERVICES", "371-3545-6633-501248");
	}
    
    public int run(String asDirFileAttente, String asDirArchive) throws TrsException
    {
		SecureFileTransferClient 	mySFtpClient = null;
        FileUtil 					myFileUtil = new FileUtil();
        ProprieteCommune			myPC = null;
        
        int		iNbFichiers = 0;

        String	sAnnee = "";
        String	sDate = "";
        String	sDirArchive = "";
        String	sDirFileAttente = "";
        String	sFileName = "";
        String 	sMois = "";
        String	sPrefixe = "";
        String	sPropKey = "";
        
        String	sFtpHost = "ftp.trs49.fr";
        String	sFtpLogin = "testjnc";
        String	sFtpPassword = "cnjtset";
        String	sFtpPort = "21";
        String	sFtpRepertoire = ".";
        

		if ( this._myPC == null )
			throw new TrsException(JavaUtil.getMethodeFullName() + " - Le fichier de proprietes N'est PAS initialise : traitement IMPOSSIBLE !"
				+ "APPELER le constructeur avant d'utiliser cette methode");

		
		myLog.info("");
		myLog.info(JavaUtil.getMethodeFullName() + " - DEBUT du traitement");
		
		
		// Répertoire des fichiers à envoyer
		// ********************************************************************************************************************
		sDirFileAttente = asDirFileAttente;

		
		// Répertoire d'archivage des fichiers à envoyer
		// ********************************************************************************************************************
		sDirArchive = asDirArchive;
		
		// Création du répertoire d'archivage principal
		myFileUtil.makeDir(sDirArchive);
		
		myLog.info("");
		myLog.info("LECTURE dans [" + sDirFileAttente + "]");
        
        // Boucle sur le répertoire <fileAttente>
        String sFichiers[] = myFileUtil.getFichiers(sDirFileAttente, "");
        
        
        // S'il y a des fichiers à traiter
        // *********************************************************************************************
		if ( sFichiers.length > 0 ) 
		{
	        // SI environnement de production ou de recette, on récupére les informations dans le fichier de propriétés
			// SINON on conserve les valeurs définies en dur
			if ( this._myPC.getEnvironnement().equals("prod") || this._myPC.getEnvironnement().equals("rec") )
			{
				myLog.info("");
				
				// Host FTP
				// --------------------------------------------------
				sPropKey = "spotToWeb.ftp.host";
				myLog.info("LECTURE de [" + sPropKey + "] dans " + this._myPC.getPropFileName());
				sPropKey = this._myPC.getEnvironnement() + "." + sPropKey;
	
	            sFtpHost = this._myPC.getProperty(sPropKey, "");
		            
	            if ( sFtpHost.equals("") )
	            	throw new TrsException(JavaUtil.getMethodeFullName() + this._myPC.getMessageErreur(1, sPropKey));
		            	
	            // Login FTP
				// --------------------------------------------------
				sPropKey = "spotToWeb.ftp.login";
				myLog.info("LECTURE de [" + sPropKey + "] dans " + this._myPC.getPropFileName());
				sPropKey = this._myPC.getEnvironnement() + "." + sPropKey;
	
	            sFtpLogin = this._myPC.getProperty(sPropKey, "");
		            
	            if ( sFtpLogin.equals("") )
	            	throw new TrsException(this._myPC.getMessageErreur(1, sPropKey));
	
	            // Password FTP
				// --------------------------------------------------
				sPropKey = "spotToWeb.ftp.password";
				myLog.info("LECTURE de [" + sPropKey + "] dans " + this._myPC.getPropFileName());
				sPropKey = this._myPC.getEnvironnement() + "." + sPropKey;
	            
	            sFtpPassword = this._myPC.getProperty(sPropKey, "");
		            
	            if ( sFtpPassword.equals("") )
	            	throw new TrsException(this._myPC.getMessageErreur(1, sPropKey));
	
	            // Port FTP
				// --------------------------------------------------
				sPropKey = "spotToWeb.ftp.port";
				myLog.info("LECTURE de [" + sPropKey + "] dans " + this._myPC.getPropFileName());
				sPropKey = this._myPC.getEnvironnement() + "." + sPropKey;
	            
	            sFtpPort = this._myPC.getProperty(sPropKey, "");
		            
	            if ( sFtpPort.equals("") )
	            	throw new TrsException(this._myPC.getMessageErreur(1, sPropKey));
	            
	            // Répertoire FTP
				// --------------------------------------------------
				sPropKey = "spotToWeb.ftp.repertoire";
				myLog.info("LECTURE de [" + sPropKey + "] dans " + this._myPC.getPropFileName());
				sPropKey = this._myPC.getEnvironnement() + "." + sPropKey;
	            
				sFtpRepertoire = this._myPC.getProperty(sPropKey, "");
		            
	            if ( sFtpRepertoire.equals("") )
	            	throw new TrsException(this._myPC.getMessageErreur(1, sPropKey));
			}
			
			myLog.info("");
			myLog.info("Transfert FTP a [" + sFtpHost + ";" + sFtpPort + ";" + sFtpLogin + "]");
			
			try 
			{ 
				myLog.info("");
	            myLog.info("Creating SFTP client");
	            mySFtpClient = new SecureFileTransferClient();
			}
			catch ( Exception e )
			{
				throw new TrsException(JavaUtil.getMethodeFullName() + " - ERREUR lors de la creation du service FTP " + " : " + e.toString());
			}
				
			try
			{
				mySFtpClient.setRemoteHost(sFtpHost);
				mySFtpClient.setUserName(sFtpLogin);
				mySFtpClient.setPassword(sFtpPassword);
				mySFtpClient.setRemotePort(Integer.parseInt(sFtpPort));
				mySFtpClient.setProtocol(Protocol.SFTP);
				
				myLog.info("");
				myLog.info("  - Avant connect()");
				mySFtpClient.connect();
				myLog.info("  - Connected and logged in to server " + sFtpHost);
				
				myLog.info("");
				myLog.info("  - Changement de repertoire : " + sFtpRepertoire);
				mySFtpClient.changeDirectory(sFtpRepertoire);
	
				for ( int j = 0; j < sFichiers.length; j ++ ) 
				{
				    try 
					{
						myLog.info("");
						myLog.info("Fichier [" + sFichiers[j] + "]");
				        
				        myLog.info("  - avant PUT");
				
				        sFileName = sDirFileAttente + "/" + sFichiers[j];
				        mySFtpClient.uploadFile(sFileName, sFichiers[j], WriteMode.OVERWRITE);
				        
						myLog.info("  - SUCCES Envoi FTP");
				
						myLog.info("  - Archivage du fichier transfere");
						
				        // Format du nom des fichiers :
						// - preva-spot-2018-10-05-08-00-00.csv
						// - BL_10068874_87399420.pdf
						// - preva-spot-conf-2022-12-30-13-30-03.csv
						// - preva-spot-pch-2022-12-27-08-45-08.csv
						// - nc-9396381-2021-09-03-07-54-00.pdf
						
						// Le répertoire d'archivage est de la forme "{1er mot du nom de fichier}{Année du fichier sur 2 chiffres}{Mois du fichier sur 2 chiffres}"
						sPrefixe = StringUtil.getStringIn(sFichiers[j], "-", -1);
						
						// Lecture de la date dans le nom du fichier : date toujours au format yyyy-mm-dd-HH-MM-SS
						sDate = getDate(sFileName);
				        sAnnee = sDate.substring(0, 4);
				        sMois = sDate.substring(4, 6);
				        
				        // PAS le type d'objet dans le nom du répertoire
				        //myFileUtil.makeDir(sDirArchive + "/" + sPrefixe + "/" + sAnnee + sMois);
						//myFileUtil.copyBytes(sFileName, sDirArchive + "/" + sPrefixe + "/" + sAnnee + sMois + "/" + sFichiers[j], true);
				        myFileUtil.makeDir(sDirArchive + "/" + sAnnee + sMois);
						myFileUtil.copyBytes(sFileName, sDirArchive + "/" + sAnnee + sMois + "/" + sFichiers[j], true);
						
						iNbFichiers ++;
				    } 
				    catch ( Exception e ) 
				    {
						throw new TrsException(JavaUtil.getMethodeFullName() + " - ERREUR lors de l'envoi par FTP du fichier [" + sFileName 
							+ "] "
							+ ( mySFtpClient == null ? "" :
							"(" + mySFtpClient.getRemoteHost() + " / " + mySFtpClient.getUserName() + " / " + mySFtpClient.getRemotePort() + ")"
							)
							+ " : " + e.toString());
				    }
				}

	            // Shutdown client
	            myLog.info("  - ARRET de la connexion FTP");
	            mySFtpClient.disconnect();
			}
			catch ( FTPException e )
			{
				throw new TrsException(JavaUtil.getMethodeFullName() + " - ERREUR lors de l'etablissement de la connexion FTP "
					+ ( mySFtpClient == null ? "" :
					"(" + mySFtpClient.getRemoteHost() + " / " + mySFtpClient.getUserName() + " / " + mySFtpClient.getRemotePort() + ")"
					)
					+ " : " + e.toString());
			}
			catch ( Exception e1 )
			{
				throw new TrsException(JavaUtil.getMethodeFullName() + " - ERREUR lors de l'etablissement de la connexion FTP "
					+ " : " + e1.toString());
			}
		}
		else
			myLog.info("PAS de fichier a envoyer par FTP");
		
		return iNbFichiers;
    }

    
    /**
     * <p>Retourne la premiere date au format yyyy-mm-dd-HH-MM-SS trouvée dans la chaine de caractères</p>
     * <p>Si cette date n'existe pas dans ce format, on retourne la premiere date au format yyyymmdd-HHMM</p>
     * 
     * @param asValeur
     * @return
     */
	public static String getDate(String asValeur)
	{
		String sRetour = null;
		String sPattern = "\\d\\d\\d\\d-\\d\\d-\\d\\d-\\d\\d-\\d\\d-\\d\\d";
		
		int iPos = 0;
		
		if ( asValeur == null )
			asValeur = "";
		
		if ( asValeur == "" ) 
			return "";
		
		Pattern myPattern = Pattern.compile(sPattern, Pattern.CASE_INSENSITIVE);
		Matcher myMatcher = myPattern.matcher(asValeur);
		
		if( myMatcher.find() )
			sRetour = asValeur.substring(myMatcher.start(), myMatcher.end());
		
		if ( sRetour == null )
			sRetour = "";
		
		if ( sRetour.equals("") )
		{
			sPattern = "\\d\\d\\d\\d[01]\\d[0123]\\d_[012]\\d[012345]\\d";
			
			myPattern = Pattern.compile(sPattern, Pattern.CASE_INSENSITIVE);
			myMatcher = myPattern.matcher(asValeur);
			
			if( myMatcher.find() )
				sRetour = asValeur.substring(myMatcher.start(), myMatcher.end());
		}
		
		return sRetour;
	}
}
