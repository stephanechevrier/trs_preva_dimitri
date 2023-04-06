package com.trs.preva.metier;

import java.util.ArrayList;
import java.util.List;

import com.trs.constant.TrsConstant;
import com.trs.exception.TrsException;
import com.trs.wintrans.dbAccess.HistoSavAccess;
import com.trs.wintrans.dbAccess.P_OPAccess;
import com.trs.wintrans.metier.Client;
import com.trs.wintrans.metier.LigComm;
import com.trs.preva.metier.*;

public class hHistosavAccess extends HistoSavAccess {

	public final static String RST_INS  = "En attente d'information";
	public final static String REN_NRV  = "Nouveau rendez-vous";
	public final static String REN_LNA  = "Livraison à nouvelle adresse ou retour Fabricant ";
	public final static String MLV2_CFM = "Chargement pour livraison (flashage)";
	public final static String PCH2_CFM = "Ramasse";
	public final static String PCH3_CFM = "Enlèvement / Reprise (flashage)";
	public final static String LIV_MQP  = "Manquant partiel à la livraison";
	
	
	

	
	public hHistosavAccess(String asPropFileName, String asDbName) throws TrsException {
		super(asPropFileName, asDbName);
		// TODO Auto-generated constructor stub
	}

	
	
	public List<hHistoSav> getSqlforSav(String asDateCreation,String asTableRef, int aiNbJours) throws TrsException {

		List<hHistoSav>	myHSList = new ArrayList<hHistoSav>();
		hHistoSav	myHS = null;
		hOT 		myOT = null;
		LigComm		myLC = null;
		String		sComment = "";
		String		sValeur = "";
		String 		sSql = "";
		String 		sRST_INS  = "RST_INS";
		String 		sREN_NRV  = "REN_NRV";
		String 		sREN_LNA  = "REN_LNA";
		String 		sMLV2_CFM = "MLV2_CFM";
		String 		sPCH2_CFM = "PCH2_CFM";
		String 		sPCH3_CFM = "PCH3_CFM";
		String 		sLIV_MQP  = "LIV_MQP";
		
		
		myLog.info("Selection des commandes eligibles au sav");

		sSql = " select distinct ot.NO_OT,ot.NO_LIGNE_COMMANDE,LC.AGENCE,CODE_SITUATION,CODE_JUSTIFICATION,h.libelle, h.HISTOSAV_DATE from histosav h"
				+ " inner join ot on ot.NO_OT = h.NO_OT"
				+ " inner join ligcomm lc on lc.NO_LIGNE_COMMANDE = ot.NO_LIGNE_COMMANDE "
			//	+ " where lc.date_saisie >= to_date('01/01/2023','dd/mm/yyyy')"
			    + " and  lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
				+ " AND ((code_situation = 'RST' AND code_justification = 'INS' and (libelle not like 'RdV web proposé%' AND libelle not like 'Reponse web%' AND libelle not like '%Nb reponses%' ))"
				+ " OR (code_situation = 'REN'  AND code_justification  = 'NRV')"
				+ " OR (code_situation = 'REN'  AND code_justification  = 'LNA')"
				+ " OR (code_situation = 'MLV2' AND code_justification  = 'CFM')"
				+ " OR (code_situation = 'PCH2' AND code_justification  = 'CFM')"
				+ " OR (code_situation = 'PCH3' AND code_justification  = 'CFM')"
				+ " OR (code_situation = 'LIV'  AND code_justification  = 'MQP')"
				+ ")"
				+ " and lc.no_ligne_commande not in (select no_ref from a_proprietes aProp1 where aProp1.nom = 'TRS_SAV_PREVA' AND aProp1.table_ref = 'HISTOSAV' AND aProp1.no_ref = lc.no_ligne_commande AND aProp1.VALEUR1_DOUBLE = ot.NO_OT)";
				

		int iRetour = this._DBBean.executeSQL(sSql);
		
		if ( iRetour < 0 )
			return myHSList;
		
		while ( this._DBBean.next() )
		{
			
		
		
		myHS = new hHistoSav(this._DBBean.getInt("no_ot"));
		
		myHS.set_codeSituation(this._DBBean.getString("CODE_SITUATION"));
		myHS.set_codeJustification(this._DBBean.getString("CODE_JUSTIFICATION"));
		myHS.set_libelle(this._DBBean.getString("LIBELLE"));
		
		myHS.set_heureHistoSav(this._DBBean.getString("HISTOSAV_DATE"));
		
		
		sValeur = this._DBBean.getString("CODE_SITUATION") + "_" + this._DBBean.getString("CODE_JUSTIFICATION");
		
		
		switch(sValeur){
			
			case "RST_INS" : myHS.set_LibelleSav(RST_INS);
			break;
			
			case "REN_NRV" : myHS.set_LibelleSav(REN_NRV);
			break;
			
			case "REN_LNA" : myHS.set_LibelleSav(REN_LNA);
			break;
			
			case "MLV2_CFM" : myHS.set_LibelleSav(MLV2_CFM);
			break;
			
			case "PCH2_CFM" : myHS.set_LibelleSav(PCH2_CFM);
			break;
			
			case "PCH3_CFM" : myHS.set_LibelleSav(PCH3_CFM);
			break;
			
			case "LIV_MQP" : myHS.set_LibelleSav(LIV_MQP);
			break;
			
			default : myLog.error("Pas de Libelle SAV correspondant, il faut l'ajouter dans la classe HistoSavAccess");
			
		}
		
		
		myLC = new LigComm(this._DBBean.getString("NO_LIGNE_COMMANDE"));
		myLC.set_refCommande(this._DBBean.getString("NO_LIGNE_COMMANDE"));
		myLC.set_agenceEnlevement(this._DBBean.getString("AGENCE"));
		
		

		

		// Date prévisionnelle de livraison
		// Au format dd/mm/yyyy HH:MM:SS stocké dans HISTO_SAV
		// A passer au format yyyy-mm-dd HH:MM:SS
		myHS.setLigComm(myLC);
		myHSList.add(myHS);
	
		}
		return myHSList;	
		
}
	
}
