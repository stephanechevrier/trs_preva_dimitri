package com.trs.preva.metier;

//import com.trs.constant.TechnicalConstant;
import com.trs.action.ProprieteCommune;
import com.trs.exception.TrsException;
import com.trs.utils.calendar.CalendarHelper;
import com.trs.utils.fileSystem.FileUtil;
//import com.trs.preva.rapport.SpotToWebLineComparable;
//import com.trs.utils.email.MailNotification;
//import com.trs.utils.properties.Properties;
//import com.trs.wintrans.dbAccess.HistoSavAccess;
//import com.trs.wintrans.dbAccess.LigColisAccess;
import com.trs.wintrans.metier.HistoSav;



//import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class WebToSpotLineLot2 
{
	private String	_agence = "";
	private	String	_agenceOt = ""; // 03/02/2019
	private String	_agenceLivraison = ""; // 03/02/2019
	private String	_noLigComm = "";
	private String	_noOp = "";
	private String	_noOt = "";
	private String	_noPlan = "";
	private String	_noSav = "";
	private String	_reponse = "";
	private String	_dateCreation = ""; // 29/01/2019
	private String	_datePrevLivraison = ""; // 04/02/2019 
	private String	_dateReponse = "";
	private String	_typeAcces = "";
	private String	_loginWeb = "";
	
	private	int		_nbReponsesNegatives = 0;
	
	private String	_enTete = "";
	private String	_fichierDate = "";
	private String	_targetDir = "";
	
	private Date _date = null;
	
	static final Logger myLog = LogManager.getLogger(WebToSpotLineLot2.class.getName());
	
	private List<HistoSav>		_myHSList = null;
	
	// 01/04/2020 - Données lot 2
	private String	_reponseComplement = "";
	private String	_typePreva = "";
	private String	_typeLdd = "";
	private	String	_etage = "";
	private String	_numTelLivraison = "";
	private String	_numTelLivraisonOld = "";
	
	public WebToSpotLineLot2()
	{
		
	}
	
	public WebToSpotLineLot2(String asNoLigComm, String asNoOp, String asNoPlan, String asNoSav, String asReponse, String asDateReponse, String asTypeAcces, String asLoginWeb,
		int aiNbReponsesNegatives, String asDateCreation, String asDatePrevLivraison)
	{
		this.set_noLigComm(asNoLigComm);
		this.set_noOp(asNoOp);
		this.set_noPlan(asNoPlan);
		this.set_noSav(asNoSav);
		this.set_dateCreation(asDateCreation);
		this.set_datePrevLivraison(asDatePrevLivraison); // 04/02/2019
		this.set_dateReponse(asDateReponse);
		this.set_typeAcces(asTypeAcces);
		this.set_loginWeb(asLoginWeb);
		
		this.set_nbReponsesNegatives(aiNbReponsesNegatives);
	}

	public String get_agence() {
		return this._agence;
	}

	/**
	 * <p>Agence OT = Agence d'enlèvement : utiliser set_agenceEnlevement</p>
	 * 
	 * @deprecated
	 */
	public void set_agenceOt(String asValeur) {
		this._agenceOt = asValeur;
	}
	
	public void set_agenceEnlevement(String asValeur) {
		this._agenceOt = asValeur;
	}

	/**
	 * <p>Agence OT = Agence d'enlèvement</p>
	 * 
	 * @return
	 */
	public String get_agenceOt() 
	{
		return this.get_agenceEnlevement();
	}

	public String get_agenceEnlevement() {
		return this._agenceOt;
	}

	public void set_agenceLivraison(String asValeur) {
		this._agenceLivraison = asValeur;
	}

	public String get_agenceLivraison() {
		return this._agenceLivraison;
	}

	/**
	 * <p>Si un séparateur, alors on a "{Agence Livraison}+{Agence OT}".</p>
	 * @param asValeur
	 */
	public void set_agence(String asValeur) 
	{
		this._agence = asValeur;
		
		// 03/02/2019 - SI séparateur +
		if ( asValeur.indexOf("+") > -1 )
		{
			String sTabAgence[] = asValeur.split("\\+");
			
			this.set_agenceLivraison(sTabAgence[0]);
			this.set_agenceOt(sTabAgence[1]);
		}
		else
		{
			this.set_agenceLivraison(asValeur);
			this.set_agenceOt("");
		}
		// 03/02/2019 - FIN modif
	}

	public String get_noLigComm() {
		return _noLigComm;
	}

	public void set_noLigComm(String _noLigComm) {
		this._noLigComm = _noLigComm;
	}

	public String get_noOp() {
		return _noOp;
	}

	public void set_noOp(String _noOp) {
		this._noOp = _noOp;
	}

	public String get_noOt() {
		return _noOt;
	}

	public void set_noOt(String _noOt) {
		this._noOt = _noOt;
	}

	public String get_noPlan() {
		return _noPlan;
	}

	public void set_noPlan(String _noPlan) {
		this._noPlan = _noPlan;
	}

	public String get_noSav() {
		return _noSav;
	}

	public void set_noSav(String _noSav) {
		this._noSav = _noSav;
	}

	public String get_reponse() {
		return _reponse;
	}

	public void set_reponse(String _reponse) {
		this._reponse = _reponse;
	}

	/**
	 * <p>Format = "yyyy-mm-dd HH:MM:SS"</p>
	 * 
	 * @return
	 */
	public String get_dateCreation() 
	{
		return _dateCreation;
	}

	/**
	 * 
	 * @param asFormat dd/mm/yyyy ou yyyy/mm/dd
	 * @return
	 */
	public String get_dateCreation(String asFormat) 
	{
		String	sDate = this.get_dateCreation();
		String	sRetour = "";
		
		if ( asFormat == null )
			asFormat = "";
		
		if ( asFormat.equals("") )
			return this.get_dateCreation();
		
		if ( CalendarHelper.FORMAT_DDMMYYYY.equals(asFormat) )
		{
			try
			{
				// Il faut passe de yyyy/mm/dd à dd/mm/yyyy
				sRetour = sDate.substring(8, 10) + "/"
					+ sDate.substring(5, 7) + "/"
					+ sDate.substring(0, 4);
				
				sRetour += " " + sDate.substring(11);
			}
			catch ( Exception e )
			{
				myLog.fatal("PROBLEME lors de la conversion de [" + sDate + "]");
			}
			
			return sRetour;
		}
		
		return this.get_dateCreation();
	}

	/**
	 * 
	 * @param asDateCreation : format "yyyy-mm-dd HH:MM:SS"
	 */
	public void set_dateCreation(String asDateCreation) 
	{
		this._dateCreation = asDateCreation;
	}

	/**
	 * <p>Format = "yyyy-mm-dd HH:MM:SS"</p>
	 * 
	 * @return
	 */
	public String get_datePrevLivraison() 
	{
		return _datePrevLivraison;
	}

	/**
	 * 
	 * @param asFormat dd/mm/yyyy HH:MM:SS ou yyyy/mm/dd HH:MM:SS
	 * @return
	 */
	public String get_datePrevLivraison(String asFormat) 
	{
		String	sDate = this.get_datePrevLivraison();
		String	sRetour = "";
		
		if ( asFormat == null )
			asFormat = "";
		
		if ( asFormat.equals("") || CalendarHelper.FORMAT_YYYYMMDD_HHMMSS_tiret.equals(asFormat) )
			return this.get_datePrevLivraison();
		
		if ( CalendarHelper.FORMAT_DDMMYYYY_HHMMSS.equals(asFormat) )
		{
			try
			{
				// Il faut passe de yyyy-mm-dd à dd/mm/yyyy
				sRetour = sDate.substring(8, 10) + "/"
					+ sDate.substring(5, 7) + "/"
					+ sDate.substring(0, 4);
				
				// On ajoute l'heure
				sRetour += " " + sDate.substring(11);
			}
			catch ( Exception e )
			{
				myLog.fatal("PROBLEME lors de la conversion de [" + sDate + "]");
			}
			
			return sRetour;
		}
		
		return this.get_dateReponse();
	}

	/**
	 * 
	 * @param asDate : format "yyyy-mm-dd HH:MM:SS"
	 */
	public void set_datePrevLivraison(String asDate) 
	{
		this._datePrevLivraison = asDate;
	}

	/**
	 * <p>Format = "yyyy-mm-dd HH:MM:SS"</p>
	 * 
	 * @return
	 */
	public String get_dateReponse() 
	{
		return _dateReponse;
	}

	/**
	 * 
	 * @param asFormat dd/mm/yyyy ou yyyy/mm/dd
	 * @return
	 */
	public String get_dateReponse(String asFormat) 
	{
		String	sDate = this.get_dateReponse();
		String	sRetour = "";
		
		if ( asFormat == null )
			asFormat = "";
		
		if ( asFormat.equals("") )
			return this.get_dateReponse();
		
		if ( CalendarHelper.FORMAT_DDMMYYYY.equals(asFormat) )
		{
			try
			{
				// Il faut passe de yyyy-mm-dd à dd/mm/yyyy
				sRetour = sDate.substring(8, 10) + "/"
					+ sDate.substring(5, 7) + "/"
					+ sDate.substring(0, 4);
				
				sRetour += " " + sDate.substring(11);
			}
			catch ( Exception e )
			{
				myLog.fatal("PROBLEME lors de la conversion de [" + sDate + "]");
			}
			
			return sRetour;
		}
		
		return this.get_dateReponse();
	}

	/**
	 * 
	 * @param _dateReponse : format "yyyy-mm-dd HH:MM:SS"
	 */
	public void set_dateReponse(String _dateReponse) 
	{
		this._dateReponse = _dateReponse;
	}

	public String get_typeAcces() {
		return _typeAcces;
	}

	public void set_typeAcces(String _typeAcces) {
		this._typeAcces = _typeAcces;
	}

	public String get_loginWeb() {
		return _loginWeb;
	}

	public void set_loginWeb(String _loginWeb) {
		this._loginWeb = _loginWeb;
	}

	public int get_nbReponsesNegatives() {
		return _nbReponsesNegatives;
	}

	public void set_nbReponsesNegatives(int _nbReponsesNegatives) {
		this._nbReponsesNegatives = _nbReponsesNegatives;
	}

	public void set_nbReponsesNegatives(String asValeur) throws TrsException
	{
		int iValeur = 0;
		
		try { iValeur = Integer.parseInt(asValeur); }
		catch ( Exception e )
		{
			throw new TrsException("PROBLEME de conversion du nombre de reponses negatives : valeur = [" + asValeur + "] : "
				+ "erreur = " + e.toString()
				);
		}
		
		this._nbReponsesNegatives = iValeur;
	}

	public String get_reponseComplement() {
		return _reponseComplement;
	}

	/**
	 * <p>Complément de réponse fourni par le site web parmi COORD, INACCE, LIVASC, LIVESC, TELLIV</p>
	 * <p>Les compléments peuvent être multiples avec séparateur = ,</p>
	 * <p>Valeurs non contrôlées : c'est le site web PREVA qui gère</p>
	 * 
	 * @param _reponseComplement
	 */
	public void set_reponseComplement(String _reponseComplement) {
		this._reponseComplement = _reponseComplement;
	}

	public String get_typePreva() {
		return _typePreva;
	}

	/**
	 * <p>Type de PREVA - LDD ou NON-LDD - fourni par le site web PREVA</p>
	 * <p>Valeurs non contrôlées</p>
	 * 
	 * @param _typePreva
	 */
	public void set_typePreva(String _typePreva) 
	{
		this._typePreva = _typePreva;
	}

	public String get_typeLdd() {
		return _typeLdd;
	}

	/**
	 * <p>Type de LDD - vide ou COMFOUR - fourni par le site web PREVA</p>
	 * <p>Valeurs non contrôlées</p>
	 * 
	 * @param _typeLdd
	 */
	public void set_typeLdd(String _typeLdd) {
		this._typeLdd = _typeLdd;
	}

	public String get_etage() {
		return _etage;
	}

	/**
	 * <p>Numéro de l’étage indiqué par le Destinataire via le questionnaire en ligne du site PREVA</p>
	 * 
	 * @param _etage
	 */
	public void set_etage(String _etage) {
		this._etage = _etage;
	}

	public String get_numTelLivraison() {
		return _numTelLivraison;
	}

	/**
	 * <p>Nouveau numéro de téléphone du destinataire à utiliser pour la livraison</p>
	 * 
	 * @param _numTelLivraison
	 */
	public void set_numTelLivraison(String _numTelLivraison) {
		this._numTelLivraison = _numTelLivraison;
	}

	public String get_numTelLivraisonOld() {
		return _numTelLivraisonOld;
	}

	/**
	 * <p>Numéro de téléphone du destinataire initialement fourni dans la PREVA</p>
	 * 
	 * @param _numTelLivraison
	 */
	public void set_numTelLivraisonOld(String _numTelLivraisonOld) {
		this._numTelLivraisonOld = _numTelLivraisonOld;
	}
}
