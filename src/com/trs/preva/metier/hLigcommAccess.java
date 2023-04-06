
package com.trs.preva.metier;

import java.util.ArrayList;
import java.util.List;

import com.trs.exception.TrsException;
import com.trs.wintrans.dbAccess.HistoSavAccess;
import com.trs.wintrans.dbAccess.LigCommAccess;
import com.trs.wintrans.metier.Client;
import com.trs.wintrans.metier.LigComm;
import com.trs.wintrans.metier.OT;

public class hLigcommAccess extends LigCommAccess {

	// 28/11/2022 - PREVA LOT 5 - en-tete commande
	public final static String	PCH = "PCH";
	public final static String	LIV = "LIV";
	public final static String	MLV = "MLV";
	public final static String	CONF = "CONF";
	public final static String	PREVA = "PREVA";
	
	public hLigcommAccess(String asPropFileName, String asDbName) throws TrsException {
		super(asPropFileName, asDbName);
		// TODO Auto-generated constructor stub
	}

	public List<hLigcomm> getSqlForOrder(String asDateCreation,String asReqConf, String asTableRef, int aiNbJours) throws TrsException
	{
		// 23/11/2021 - Création
		
		
		List<hLigcomm>	myLcList = new ArrayList<hLigcomm>();
		hLigcomm		myLC 	 = null;
		Client 			MyClient = null;
		OT				MyOT 	 = null;
	
		String		sComment = "";
		String		sValeur = "";
		String 		sSql = "";
		
		myLog.info("Selection des commandes eligibles a l'en-tete de commandes");

		switch ( asReqConf )  
		{
			// Commande ayant fait l'objet d'une PREVA acceptée ou d'une confirmation manuelle (confirmation de rendez-vous), la confirmation peut etre transmise 
			// dès que possible
			case PCH :
				sSql = "SELECT distinct Lc.NO_LIGNE_COMMANDE, "
		            + "Lc.REF_CLIENT, "
		            + "Lc.CODE_CLIENT, "
		            + "cli.NOM_CLIENT, "
		            + "ot.NO_OT, "
		            + "Lc.NOM_DESTINATAIRE, "
		            + "Lc.VILLE_DESTINATAIRE,"
		            + "Lc.telephone_destinataire, "
		            + "Lc.COLIS, "
		            + "LC.PAYS_DESTINATAIRE, "
		            + "LC.DATE_CREATION, "
		            + "Lc.CP_DESTINATAIRE, "
		            + "Lc.ADR1_DESTINATAIRE, "
		            + "Lc.ADR2_DESTINATAIRE, "
		            + "lc.DATE_DECHARGEMENT,"
		            + "cli.QUALIFICATION,"
		            + "Concat(Concat(lc.COMMENTAIRE,' |Champs3 = '),lc.champs3) as commentaire, "                
		            + "Lc.AGENCE, "  
		            + "replace(replace(Lc.CHAMPS6, chr(10)), chr(13)) AS MAIL_DEST, "
		            + "CASE WHEN lc.commentaire IS NOT NULL "
		            + "THEN CASE WHEN INSTR( upper(lc.commentaire), 'EMAILDEST(') > 0 THEN SUBSTR( lc.commentaire, INSTR( Upper(lc.commentaire), 'EMAILDEST(' ) + 10, "
		            + "INSTR( lc.commentaire, ')', INSTR( Upper(lc.commentaire), 'EMAILDEST(' ) + 1)  - INSTR( UPPER(lc.commentaire), 'EMAILDEST(' ) - 10 ) "
		            + "ELSE '' END "
		            + "ELSE '' END AS MAIL_DEST2, "
		            + "Lc.CHAMPS7 AS SMS_DEST, "
		            + "Lc.TELEPHONE_DESTINATAIRE AS SMS_DEST_1, "
		            + "Lc.TELECOPIE_DESTINATAIRE AS SMS_DEST_2, "
		            + "CASE WHEN lc.commentaire IS NOT NULL "
		            + "THEN CASE WHEN INSTR( upper(lc.commentaire), 'TELMOBILEDEST(') > 0 THEN SUBSTR( lc.commentaire, INSTR( Upper(lc.commentaire), 'TELMOBILEDEST(' ) + 14, "
		            + "INSTR( lc.commentaire, ')', INSTR( Upper(lc.commentaire), 'TELMOBILEDEST(' ) + 1 ) - INSTR( UPPER(lc.commentaire), 'TELMOBILEDEST(' ) - 14 ) "
		            + "ELSE '' END "
		            + "ELSE '' END AS SMS_DEST_3, "
		            + "CASE WHEN INSTR( lc.commentaire, 'Allegro(') > 0 THEN SUBSTR( lc.commentaire, INSTR( lc.commentaire, 'Allegro(' ) + 8, "
		            + "INSTR( lc.commentaire, ')', INSTR( lc.commentaire, 'Allegro(' ) + 1 ) - INSTR( lc.commentaire, 'Allegro(' ) - 8 ) "
		            + "ELSE '' END AS numeroAllegro "
		            + "FROM LIGCOMM LC "
		            + "INNER JOIN a_proprietes aprop ON lc.NO_LIGNE_COMMANDE = aprop.VALEUR1_DOUBLE "
		            + "INNER JOIN CLIENT cli on cli.CODE_CLIENT = LC.CODE_CLIENT " 
		            + "INNER JOIN ot  ON lc.NO_LIGNE_COMMANDE = ot.NO_LIGNE_COMMANDE "
		            + "INNER JOIN histosav h ON h.NO_OT = ot.NO_OT "
		            + "WHERE aprop.NOM = 'TRS_PCH_PREVA' "
		            + " AND h.CODE_SITUATION ='PCH' and h.CODE_JUSTIFICATION = 'CFM' "
		            + "and lc.no_ligne_commande not in (select no_ref from a_proprietes aProp1 where aProp1.nom = 'TRS_ORDER_PREVA' AND aProp1.table_ref = 'LIGCOMM' AND aProp1.no_ref = lc.no_ligne_commande AND aProp1.VALEUR1_DOUBLE = ot.NO_OT AND VALEUR2_CHAINE = 'PCH') "
		            // Ajouter condition sur a_proprietes pour ne pas retraiter des commandes
		            
					//+ " AND lc.DATE_SAISIE >= to_date('01/01/2023','dd/mm/yyyy') "
					+ " and  lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy')"
					;
				break;
				
			case PREVA :
				sSql = " SELECT distinct Lc.NO_LIGNE_COMMANDE, "
		            + " Lc.REF_CLIENT, "
		            + " ot.NO_OT, "
		            + " Lc.CODE_CLIENT, "
		            + " cli.NOM_CLIENT, "
		            + " Lc.NOM_DESTINATAIRE, "
		            + " Lc.VILLE_DESTINATAIRE,"
		            + " Lc.telephone_destinataire, "
		            + " Lc.COLIS, "
		            + " LC.PAYS_DESTINATAIRE, "
		            + " LC.DATE_CREATION, "
		            + " Lc.CP_DESTINATAIRE, "
		            + " Lc.ADR1_DESTINATAIRE, "
		            + " Lc.ADR2_DESTINATAIRE, "
		            + " lc.DATE_DECHARGEMENT,"
		            + " cli.QUALIFICATION,"
		            + " Concat(Concat(lc.COMMENTAIRE,' |Champs3 = '),lc.champs3) as commentaire, "                
		            + " Lc.AGENCE, "  
		            + " replace(replace(Lc.CHAMPS6, chr(10)), chr(13)) AS MAIL_DEST, "
		            + " CASE WHEN lc.commentaire IS NOT NULL "
		            + " THEN CASE WHEN INSTR( upper(lc.commentaire), 'EMAILDEST(') > 0 THEN SUBSTR( lc.commentaire, INSTR( Upper(lc.commentaire), 'EMAILDEST(' ) + 10, "
		            + " INSTR( lc.commentaire, ')', INSTR( Upper(lc.commentaire), 'EMAILDEST(' ) + 1)  - INSTR( UPPER(lc.commentaire), 'EMAILDEST(' ) - 10 ) "
		            + " ELSE '' END "
		            + " ELSE '' END AS MAIL_DEST2, "
		            + " Lc.CHAMPS7 AS SMS_DEST, "
		            + " Lc.TELEPHONE_DESTINATAIRE AS SMS_DEST_1, "
		            + " Lc.TELECOPIE_DESTINATAIRE AS SMS_DEST_2, "
		            + " CASE WHEN lc.commentaire IS NOT NULL "
		            + " THEN CASE WHEN INSTR( upper(lc.commentaire), 'TELMOBILEDEST(') > 0 THEN SUBSTR( lc.commentaire, INSTR( Upper(lc.commentaire), 'TELMOBILEDEST(' ) + 14, "
		            + " INSTR( lc.commentaire, ')', INSTR( Upper(lc.commentaire), 'TELMOBILEDEST(' ) + 1 ) - INSTR( UPPER(lc.commentaire), 'TELMOBILEDEST(' ) - 14 ) "
		            + " ELSE '' END "
		            + " ELSE '' END AS SMS_DEST_3, "
		            + " CASE WHEN INSTR( lc.commentaire, 'Allegro(') > 0 THEN SUBSTR( lc.commentaire, INSTR( lc.commentaire, 'Allegro(' ) + 8, "
		            + " INSTR( lc.commentaire, ')', INSTR( lc.commentaire, 'Allegro(' ) + 1 ) - INSTR( lc.commentaire, 'Allegro(' ) - 8 ) "
		            + " ELSE '' END AS numeroAllegro "
		            + " FROM LIGCOMM LC "
		            + " INNER JOIN ot  ON lc.NO_LIGNE_COMMANDE = ot.NO_LIGNE_COMMANDE "
		            + " INNER JOIN histosav h on ot.no_ot = h.no_ot "
		            + " INNER JOIN CLIENT cli on cli.CODE_CLIENT = LC.CODE_CLIENT " 
		            + " WHERE h.CODE_JUSTIFICATION = 'INS' "
		            + " AND h.CODE_SITUATION = 'RST' "
		            + " AND h.libelle like 'RdV web proposé%' "
		            + " and lc.no_ligne_commande not in (select no_ref from a_proprietes aProp1 where aProp1.nom = 'TRS_ORDER_PREVA' AND aProp1.table_ref = 'LIGCOMM' AND aProp1.no_ref = lc.no_ligne_commande AND aProp1.VALEUR1_DOUBLE = ot.NO_OT AND VALEUR2_CHAINE = 'PREVA')"
		            // Ajouter condition sur a_proprietes pour ne pas retraiter des commandes
		            
					//+ " AND lc.DATE_SAISIE >= to_date('01/01/2023','dd/mm/yyyy') "
					+ " and  lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy')"
					;
				break;
				
			case CONF :
				sSql = " SELECT distinct Lc.NO_LIGNE_COMMANDE, "
			            + " Lc.REF_CLIENT, "
			            + " ot.NO_OT, "
			            + " Lc.CODE_CLIENT, "
			            + " cli.NOM_CLIENT, "
			            + " Lc.NOM_DESTINATAIRE, "
			            + " Lc.VILLE_DESTINATAIRE,"
			            + " Lc.telephone_destinataire, "
			            + " Lc.COLIS, "
			            + " LC.PAYS_DESTINATAIRE, "
			            + " LC.DATE_CREATION, "
			            + " Lc.CP_DESTINATAIRE, "
			            + " Lc.ADR1_DESTINATAIRE, "
			            + " Lc.ADR2_DESTINATAIRE, "
			            + " lc.DATE_DECHARGEMENT,"
			            + " cli.QUALIFICATION,"
			            + " Concat(Concat(lc.COMMENTAIRE,' |Champs3 = '),lc.champs3) as commentaire, "                
			            + " Lc.AGENCE, "  
			            + " replace(replace(Lc.CHAMPS6, chr(10)), chr(13)) AS MAIL_DEST, "
			            + " CASE WHEN lc.commentaire IS NOT NULL "
			            + " THEN CASE WHEN INSTR( upper(lc.commentaire), 'EMAILDEST(') > 0 THEN SUBSTR( lc.commentaire, INSTR( Upper(lc.commentaire), 'EMAILDEST(' ) + 10, "
			            + " INSTR( lc.commentaire, ')', INSTR( Upper(lc.commentaire), 'EMAILDEST(' ) + 1)  - INSTR( UPPER(lc.commentaire), 'EMAILDEST(' ) - 10 ) "
			            + " ELSE '' END "
			            + " ELSE '' END AS MAIL_DEST2, "
			            + " Lc.CHAMPS7 AS SMS_DEST, "
			            + " Lc.TELEPHONE_DESTINATAIRE AS SMS_DEST_1, "
			            + " Lc.TELECOPIE_DESTINATAIRE AS SMS_DEST_2, "
			            + " CASE WHEN lc.commentaire IS NOT NULL "
			            + " THEN CASE WHEN INSTR( upper(lc.commentaire), 'TELMOBILEDEST(') > 0 THEN SUBSTR( lc.commentaire, INSTR( Upper(lc.commentaire), 'TELMOBILEDEST(' ) + 14, "
			            + " INSTR( lc.commentaire, ')', INSTR( Upper(lc.commentaire), 'TELMOBILEDEST(' ) + 1 ) - INSTR( UPPER(lc.commentaire), 'TELMOBILEDEST(' ) - 14 ) "
			            + " ELSE '' END "
			            + " ELSE '' END AS SMS_DEST_3, "
			            + " CASE WHEN INSTR( lc.commentaire, 'Allegro(') > 0 THEN SUBSTR( lc.commentaire, INSTR( lc.commentaire, 'Allegro(' ) + 8, "
			            + " INSTR( lc.commentaire, ')', INSTR( lc.commentaire, 'Allegro(' ) + 1 ) - INSTR( lc.commentaire, 'Allegro(' ) - 8 ) "
			            + " ELSE '' END AS numeroAllegro "
			            + " FROM LIGCOMM LC "
			            + " INNER JOIN ot  ON lc.NO_LIGNE_COMMANDE = ot.NO_LIGNE_COMMANDE "
			            + " INNER JOIN histosav h on ot.no_ot = h.no_ot "
			            + " INNER JOIN CLIENT cli on cli.CODE_CLIENT = LC.CODE_CLIENT " 
			            + " LEFT OUTER JOIN a_proprietes aProp1  ON aProp1.nom = 'TRS_ORDER_PREVA' AND aProp1.table_ref = 'LIGCOMM' AND aProp1.no_ref = lc.no_ligne_commande AND aProp1.VALEUR1_DOUBLE = ot.NO_OT  "
			            + " WHERE (h.CODE_JUSTIFICATION = 'LDF' "
			            + " AND h.CODE_SITUATION = 'RST' "
			            + " AND h.CHAMPS1 is not null "
			           // + " AND lc.DATE_SAISIE >= to_date('01/01/2022','dd/mm/yyyy')"
			            + " and  lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
			            + " and lc.no_ligne_commande not in (select no_ref from a_proprietes aProp1 where aProp1.nom = 'TRS_ORDER_PREVA' AND aProp1.table_ref = 'LIGCOMM' AND aProp1.no_ref = lc.no_ligne_commande AND aProp1.VALEUR1_DOUBLE = ot.NO_OT AND VALEUR2_CHAINE = 'CONF')) "
			            + " OR (h.CODE_JUSTIFICATION = 'CFM' "
					    + " AND h.CODE_SITUATION = 'MLV' "
					    + " AND h.CHAMPS1 is not null "
					   
					
					  + " and  lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy')"
					  + " and lc.no_ligne_commande not in (select no_ref from a_proprietes aProp1 where aProp1.nom = 'TRS_ORDER_PREVA' AND aProp1.table_ref = 'LIGCOMM' AND aProp1.no_ref = lc.no_ligne_commande AND aProp1.VALEUR1_DOUBLE = ot.NO_OT AND VALEUR2_CHAINE = 'CONF'))"
			            
					;
				break;
				
			case MLV :
				sSql = " SELECT distinct Lc.NO_LIGNE_COMMANDE, "
			            + " Lc.REF_CLIENT, "
			            + " ot.NO_OT, "
			            + " Lc.CODE_CLIENT, "
			            + " cli.NOM_CLIENT, "
			            + " Lc.NOM_DESTINATAIRE, "
			            + " Lc.VILLE_DESTINATAIRE,"
			            + " Lc.telephone_destinataire, "
			            + " Lc.COLIS, "
			            + " LC.PAYS_DESTINATAIRE, "
			            + " LC.DATE_CREATION, "
			            + " Lc.CP_DESTINATAIRE, "
			            + " Lc.ADR1_DESTINATAIRE, "
			            + " Lc.ADR2_DESTINATAIRE, "
			            + " lc.DATE_DECHARGEMENT,"
			            + " cli.QUALIFICATION,"
			            + " Concat(Concat(lc.COMMENTAIRE,' |Champs3 = '),lc.champs3) as commentaire, "                
			            + " Lc.AGENCE, "  
			            + " replace(replace(Lc.CHAMPS6, chr(10)), chr(13)) AS MAIL_DEST, "
			            + " CASE WHEN lc.commentaire IS NOT NULL "
			            + " THEN CASE WHEN INSTR( upper(lc.commentaire), 'EMAILDEST(') > 0 THEN SUBSTR( lc.commentaire, INSTR( Upper(lc.commentaire), 'EMAILDEST(' ) + 10, "
			            + " INSTR( lc.commentaire, ')', INSTR( Upper(lc.commentaire), 'EMAILDEST(' ) + 1)  - INSTR( UPPER(lc.commentaire), 'EMAILDEST(' ) - 10 ) "
			            + " ELSE '' END "
			            + " ELSE '' END AS MAIL_DEST2, "
			            + " Lc.CHAMPS7 AS SMS_DEST, "
			            + " Lc.TELEPHONE_DESTINATAIRE AS SMS_DEST_1, "
			            + " Lc.TELECOPIE_DESTINATAIRE AS SMS_DEST_2, "
			            + " CASE WHEN lc.commentaire IS NOT NULL "
			            + " THEN CASE WHEN INSTR( upper(lc.commentaire), 'TELMOBILEDEST(') > 0 THEN SUBSTR( lc.commentaire, INSTR( Upper(lc.commentaire), 'TELMOBILEDEST(' ) + 14, "
			            + " INSTR( lc.commentaire, ')', INSTR( Upper(lc.commentaire), 'TELMOBILEDEST(' ) + 1 ) - INSTR( UPPER(lc.commentaire), 'TELMOBILEDEST(' ) - 14 ) "
			            + " ELSE '' END "
			            + " ELSE '' END AS SMS_DEST_3, "
			            + " CASE WHEN INSTR( lc.commentaire, 'Allegro(') > 0 THEN SUBSTR( lc.commentaire, INSTR( lc.commentaire, 'Allegro(' ) + 8, "
			            + " INSTR( lc.commentaire, ')', INSTR( lc.commentaire, 'Allegro(' ) + 1 ) - INSTR( lc.commentaire, 'Allegro(' ) - 8 ) "
			            + " ELSE '' END AS numeroAllegro "
			            + " FROM LIGCOMM LC "
			            + " INNER JOIN ot  ON lc.NO_LIGNE_COMMANDE = ot.NO_LIGNE_COMMANDE "
			            + " INNER JOIN histosav h on ot.no_ot = h.no_ot "
			            + " INNER JOIN CLIENT cli on cli.CODE_CLIENT = LC.CODE_CLIENT " 
			            + " WHERE  h.CODE_JUSTIFICATION = 'CFM' "
					    + " AND h.CODE_SITUATION = 'MLV' "
			            + " and lc.no_ligne_commande not in (select no_ref from a_proprietes aProp1 where aProp1.nom = 'TRS_ORDER_PREVA' AND aProp1.table_ref = 'LIGCOMM' AND aProp1.no_ref = lc.no_ligne_commande AND aProp1.VALEUR1_DOUBLE = ot.NO_OT AND VALEUR2_CHAINE = 'MLV')"
					  //  + " AND lc.DATE_SAISIE >= to_date('01/01/2023','dd/mm/yyyy') "
					  + " and  lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy')"
			            
					;
				break;
				
			case LIV :
				sSql = " SELECT distinct Lc.NO_LIGNE_COMMANDE, "
			            + " Lc.REF_CLIENT, "
			            + " ot.NO_OT, "
			            + " Lc.CODE_CLIENT, "
			            + " cli.NOM_CLIENT, "
			            + " Lc.NOM_DESTINATAIRE, "
			            + " Lc.VILLE_DESTINATAIRE,"
			            + " Lc.telephone_destinataire, "
			            + " Lc.COLIS, "
			            + " LC.PAYS_DESTINATAIRE, "
			            + " LC.DATE_CREATION, "
			            + " Lc.CP_DESTINATAIRE, "
			            + " Lc.ADR1_DESTINATAIRE, "
			            + " Lc.ADR2_DESTINATAIRE, "
			            + " lc.DATE_DECHARGEMENT,"
			            + " cli.QUALIFICATION,"
			            + " Concat(Concat(lc.COMMENTAIRE,' |Champs3 = '),lc.champs3) as commentaire, "                
			            + " Lc.AGENCE, "  
			            + " replace(replace(Lc.CHAMPS6, chr(10)), chr(13)) AS MAIL_DEST, "
			            + " CASE WHEN lc.commentaire IS NOT NULL "
			            + " THEN CASE WHEN INSTR( upper(lc.commentaire), 'EMAILDEST(') > 0 THEN SUBSTR( lc.commentaire, INSTR( Upper(lc.commentaire), 'EMAILDEST(' ) + 10, "
			            + " INSTR( lc.commentaire, ')', INSTR( Upper(lc.commentaire), 'EMAILDEST(' ) + 1)  - INSTR( UPPER(lc.commentaire), 'EMAILDEST(' ) - 10 ) "
			            + " ELSE '' END "
			            + " ELSE '' END AS MAIL_DEST2, "
			            + " Lc.CHAMPS7 AS SMS_DEST, "
			            + " Lc.TELEPHONE_DESTINATAIRE AS SMS_DEST_1, "
			            + " Lc.TELECOPIE_DESTINATAIRE AS SMS_DEST_2, "
			            + " CASE WHEN lc.commentaire IS NOT NULL "
			            + " THEN CASE WHEN INSTR( upper(lc.commentaire), 'TELMOBILEDEST(') > 0 THEN SUBSTR( lc.commentaire, INSTR( Upper(lc.commentaire), 'TELMOBILEDEST(' ) + 14, "
			            + " INSTR( lc.commentaire, ')', INSTR( Upper(lc.commentaire), 'TELMOBILEDEST(' ) + 1 ) - INSTR( UPPER(lc.commentaire), 'TELMOBILEDEST(' ) - 14 ) "
			            + " ELSE '' END "
			            + " ELSE '' END AS SMS_DEST_3, "
			            + " CASE WHEN INSTR( lc.commentaire, 'Allegro(') > 0 THEN SUBSTR( lc.commentaire, INSTR( lc.commentaire, 'Allegro(' ) + 8, "
			            + " INSTR( lc.commentaire, ')', INSTR( lc.commentaire, 'Allegro(' ) + 1 ) - INSTR( lc.commentaire, 'Allegro(' ) - 8 ) "
			            + " ELSE '' END AS numeroAllegro "
			            + " FROM LIGCOMM LC "
			            + " INNER JOIN ot  ON lc.NO_LIGNE_COMMANDE = ot.NO_LIGNE_COMMANDE "
			            + " INNER JOIN histosav h on ot.no_ot = h.no_ot "
			            + " INNER JOIN CLIENT cli on cli.CODE_CLIENT = LC.CODE_CLIENT " 
			            + " WHERE  h.CODE_JUSTIFICATION = 'CFM' "
					    + " AND h.CODE_SITUATION = 'LIV' "
			            + " and lc.no_ligne_commande not in (select no_ref from a_proprietes aProp1 where aProp1.nom = 'TRS_ORDER_PREVA' AND aProp1.table_ref = 'LIGCOMM' AND aProp1.no_ref = lc.no_ligne_commande AND aProp1.VALEUR1_DOUBLE = ot.NO_OT AND VALEUR2_CHAINE = 'LIV')"
					  //  + " AND lc.DATE_SAISIE >= to_date('01/01/2023','dd/mm/yyyy') "
					  + " and  lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy')"
			            
					;
				break;
	                 
			default : 
				throw new TrsException(HistoSavAccess.class.getEnclosingMethod().getName() + " : Parametre asReqConf [" + asReqConf
					+ "] INCONNU : valeurs attendues = PCH,PREVA,CONF,MLV,LIV " ); 
		} 
		
		int iRetour = this._DBBean.executeSQL(sSql);
		
		if ( iRetour < 0 )
			return myLcList;
		
		while ( this._DBBean.next() )
		{
			
			
			// Le SELECT doit faire un ORDER BY pour regrouper par date de livraison
			// Ainsi l'appel de myCTAccess.get_prevJourOuvre() permet d'optimiser le cache
			
						
			myLC = new hLigcomm(this._DBBean.getString("NO_LIGNE_COMMANDE"));
			myLC.set_refCommande(this._DBBean.getString("NO_LIGNE_COMMANDE"));
			myLC.set_agenceEnlevement(this._DBBean.getString("AGENCE"));
			
			myLC.set_codeClient(this._DBBean.getString("code_client"));
			myLC.set_nomClient(this._DBBean.getString("NOM_CLIENT"));
			
			MyClient = new Client(this._DBBean.getString("CODE_CLIENT"));
			MyClient.set_qualification(this._DBBean.getString("QUALIFICATION"));
			myLC.set_nomDestinataire(this._DBBean.getString("nom_destinataire"));
			myLC.set_adresseDestLigne1(this._DBBean.getString("adr1_destinataire"));
			myLC.set_adresseDestLigne2(this._DBBean.getString("adr2_destinataire"));
			myLC.set_villeDestinataire(this._DBBean.getString("VILLE_DESTINATAIRE"));
			myLC.set_cpDestinataire(this._DBBean.getString("CP_DESTINATAIRE"));
			myLC.set_paysDestinataire(this._DBBean.getString("PAYS_DESTINATAIRE"));
			myLC.set_eMailDestinataire(this._DBBean.getString("MAIL_DEST"));
			myLC.set_eMailDestinataireSup(this._DBBean.getString("MAIL_DEST2"));
			myLC.set_mobileDestinataire(this._DBBean.getString("SMS_DEST"));
			myLC.set_mobileDestinataireSup(this._DBBean.getString("SMS_DEST_1"));
			myLC.set_telDestinataire(this._DBBean.getString("telephone_destinataire"));
			myLC.set_nbColisReel(this._DBBean.getInt("COLIS"));
			myLC.set_dateCreation(this._DBBean.getString("DATE_CREATION"));
		
			MyOT = new OT(this._DBBean.getString("NO_LIGNE_COMMANDE"));
			MyOT.setNoOt(this._DBBean.getInt("NO_OT"));
			sComment = this._DBBean.getString("commentaire");
			myLC.set_commentaire(sComment);
	
			myLog.debug("  - commande = " + myLC.get_refCommande());
	
			// Date prévisionnelle de livraison
			// Au format dd/mm/yyyy HH:MM:SS stocké dans HISTO_SAV
			// A passer au format yyyy-mm-dd HH:MM:SS
			myLC.setClient(MyClient);
			myLC.set_ot(MyOT);
			myLcList.add(myLC);
		}
	
		return myLcList;
	}
	
	
	public  List<LigComm> getSqlLiv(String asDateCreation,String asTableRef, int aiNbJours) throws TrsException{
		
		String sql = "";
		List<LigComm> myLCList = new ArrayList<LigComm>();
		LigComm myLc = null;
		String sLibelleTemp = "";
		
		
		sql = " select distinct lc.agence,lc.no_ligne_commande,VALEUR1_DATE,h.libelle,h.CODE_SITUATION,h.CODE_JUSTIFICATION from ligcomm lc "
				+ " inner join a_proprietes a_prop on a_prop.no_ref = lc.no_ligne_commande "
				+ " inner join ot on ot.no_ligne_commande = lc.no_ligne_commande "
				+ " inner join histosav h on h.no_ot = ot.no_ot "
				+ " where a_prop.NOM = 'DATE_LIV_REELLE' "
				//+ "and lc.date_saisie >= to_date('01/01/2023','dd/mm/yyyy') "
				+ " and  lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
				+ " AND (lc.CODE_ETAT in ('FAC','LIV') and h.CODE_SITUATION = 'LIV')"
				+ " and lc.no_ligne_commande not in (select no_ref from a_proprietes aProp1 where aProp1.nom = 'TRS_LIV_PREVA' AND aProp1.table_ref = 'LIGCOMM' AND aProp1.no_ref = lc.no_ligne_commande)";
		
		int iRetour = this._DBBean.executeSQL(sql);
		
		if ( iRetour < 0 )
			return myLCList;
		
		while ( this._DBBean.next() )
		{
		
			myLc = new LigComm(this._DBBean.getString("no_ligne_commande"));
			myLc.set_refCommande(this._DBBean.getString("no_ligne_commande"));
			myLc.set_agenceEnlevement(this._DBBean.getString("agence"));
			myLc.set_heureReelleLivraison(this._DBBean.getString("valeur1_date"));
			
			sLibelleTemp = this._DBBean.getString("CODE_SITUATION") + "-" + this._DBBean.getString("CODE_JUSTIFICATION") ;
			
			myLog.info("Code : " + sLibelleTemp);
			
			if( !"LIV-CFM".equals(sLibelleTemp)) {
				myLc.set_commentaire(this._DBBean.getString("Libelle"));
			}
			
			
			myLCList.add(myLc);
		
		
			
		}
		return myLCList;
	}
	
}