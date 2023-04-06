package com.trs.preva.metier;

import com.trs.action.ProprieteCommune;
import com.trs.exception.TrsException;
import com.trs.utils.calendar.CalendarHelper;
import com.trs.utils.fileSystem.FileUtil;
//import com.trs.preva.rapport.SpotToWebLineComparable;
import com.trs.wintrans.metier.HistoSav;

import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class WebToSpotReportLineLot2 
{
	private	String	_loginWeb = "";
	private String	_noLigComm = "";
	private String	_noPlan = "";
	private String	_reponse = "";
	private String	_dateReponse = "";
	private String	_typeAcces = "";
	
	private String	_reponseComplement = "";
	private String	_typePreva = "";
	private String	_typeLdd = "";
	private String	_etage = "";
	private String	_numTelLivraison = "";
	private String	_numTelLivraisonOld = "";
	
	private	int		_nbReponsesNegatives = 0;
	
	static final Logger myLog = LogManager.getLogger(WebToSpotReportLineLot2.class.getName());
	
	private List<HistoSav>		_myHSList = null;
	
	public WebToSpotReportLineLot2(String asNoLigComm, String asNoPlan, String asReponse, String asDateReponse, String asTypeAcces, 
		int aiNbReponsesNegatives, String asLoginWeb,
		String asComplementReponse, String asTypePreva,
		String asTypeLdd, String asEtage, String asNumTelLivraison, String asNumTelLivraisonOld		
		)
	{
		this.set_noLigComm(asNoLigComm);
		this.set_noPlan(asNoPlan);
		this.set_reponse(asReponse);
		this.set_dateReponse(asDateReponse);
		this.set_typeAcces(asTypeAcces);
		
		this.set_nbReponsesNegatives(aiNbReponsesNegatives);
		
		this.set_loginWeb(asLoginWeb);
		
		this.set_reponseComplement(asComplementReponse);
		this.set_typePreva(asTypePreva);
		this.set_typeLdd(asTypeLdd);
		this.set_etage(asEtage);
		this.set_numTelLivraison(asNumTelLivraison);
		this.set_numTelLivraisonOld(asNumTelLivraisonOld);
	}

	public String get_loginWeb() {
		return _loginWeb;
	}

	public void set_loginWeb(String asValeur) {
		this._loginWeb = asValeur;
	}

	public String get_noLigComm() {
		return _noLigComm;
	}

	public void set_noLigComm(String _noLigComm) {
		this._noLigComm = _noLigComm;
	}

	public String get_noPlan() {
		return _noPlan;
	}

	public void set_noPlan(String _noPlan) {
		this._noPlan = _noPlan;
	}

	public String get_reponse() {
		return _reponse;
	}

	public void set_reponse(String _reponse) {
		this._reponse = _reponse;
	}

	/**
	 * <p>Format = "yyyy/mm/dd HH:MM:SS"</p>
	 * 
	 * @return
	 */
	public String get_dateReponse() 
	{
		return _dateReponse;
	}

	/**
	 * <p>Format interne = "yyyy-mm-dd HH:MM:SS"</p>
	 * <p>Même si non précisé dans le format, l'heure est retournée.</p>
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
				// Il faut passe de yyyy/mm/dd à dd/mm/yyyy
				sRetour = sDate.substring(8, 10) + "/"
					+ sDate.substring(5, 7) + "/"
					+ sDate.substring(0, 4);
				
				// Ajout de l'heure
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
		this._dateReponse = _dateReponse.replaceAll("\"", "");
	}

	public String get_typeAcces() {
		return _typeAcces;
	}

	public void set_typeAcces(String _typeAcces) {
		this._typeAcces = _typeAcces;
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

	public void set_reponseComplement(String _reponseComplement) {
		this._reponseComplement = _reponseComplement;
	}

	public String get_typePreva() {
		return _typePreva;
	}

	public void set_typePreva(String _typePreva) {
		this._typePreva = _typePreva;
	}

	public String get_typeLdd() {
		return _typeLdd;
	}

	public void set_typeLdd(String _typeLdd) {
		this._typeLdd = _typeLdd;
	}

	public String get_etage() {
		return _etage;
	}

	public void set_etage(String _etage) {
		this._etage = _etage;
	}

	public String get_numTelLivraison() {
		return _numTelLivraison;
	}

	public void set_numTelLivraison(String _numTelLivraison) {
		this._numTelLivraison = _numTelLivraison;
	}

	public String get_numTelLivraisonOld() {
		return _numTelLivraisonOld;
	}

	public void set_numTelLivraisonOld(String _numTelLivraisonold) {
		this._numTelLivraisonOld = _numTelLivraisonOld;
	}
}
