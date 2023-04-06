package com.trs.preva.metier;

import java.util.ArrayList;
import java.util.List;

import com.trs.dbAccess.DBBean;
import com.trs.exception.TrsException;
import com.trs.wintrans.dbAccess.HistoSavAccess;
import com.trs.wintrans.dbAccess.P_OPAccess;
import com.trs.wintrans.metier.Client;
import com.trs.wintrans.metier.HistoSav;
import com.trs.wintrans.metier.P_OP;
import com.trs.preva.metier.*;

public class hP_OPAccess extends P_OPAccess {

	// 28/11/2022 - PREVA LOT 5 - en-tete commande
	public final static String	PCH = "PCH";
	public final static String	CONFIMPLICITE = "CONFIMPLICITE";
	public final static String	CONFEXPLICITE = "CONFEXPLICITE";
	public final static String	PREVA = "PREVA";
	public final static String  ANNULATION = "ANNULATION";
	public final static String  CREATION = "CREATION";
	
	public hP_OPAccess(String asPropFileName, String asDbName) throws TrsException {
		super(asPropFileName, asDbName);
		// TODO Auto-generated constructor stub
	}

	
	
	public List<hP_OP> getSqlforTraction(String asDateCreation,String asReqConf, String asTableRef, int aiNbJours) throws TrsException {

		List<hP_OP>	  myLcList = new ArrayList<hP_OP>();
		hP_OP	      myP_OP = null;
		hOT 		  myOT = null;
	
		String		  sComment = "";
		String		  sValeur = "";
		String 		  sSql = "";
		String 		  sNumCdeTemp = "";
		int 		  iTempNumOtBefore = 0;
		int 		  iTempNumOtAfter = 0;
		myLog.info("Selection des commandes eligibles au Traction");

		switch ( asReqConf )  
		{
	
			case PCH :
				sSql = "with agence_liv as (\r\n"
						+ "select ot.no_ligne_commande,ot.no_ot, ot.code_expediteur \r\n"
						+ "from ot \r\n"
						+ "inner join ligcomm lc on lc.no_ligne_commande = ot.no_ligne_commande\r\n"
						+ "where no_ot_apres is null \r\n"
						+ "and lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') and ot.etape = 'LIVRAISON'\r\n"
						+ "\r\n"
						+ "),\r\n"
						+ " numero_ot as ( "
						+ " select CASE WHEN ROW_NUMBER () OVER (PARTITION BY lc.no_ligne_commande ORDER BY ot.no_ot asc) = 1  then 0 else ROW_NUMBER () OVER (PARTITION BY lc.no_ligne_commande ORDER BY ot.no_ot asc) END as num_ot, lc.no_ligne_commande, ot.no_ot,ot.code_expediteur,COUNT(*) OVER (PARTITION BY ot.no_ligne_commande) as MAX_OT "				
				        + "    from ot "
						+ "				inner join ligcomm lc on lc.no_ligne_commande = ot.no_ligne_commande  "
						+ "				where lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
						+ "				and ot.etape in ('LIVRAISON') "
				        + "    and lc.no_ligne_commande not in (select lc.no_ligne_commande from ligcomm lc inner join ot on ot.no_ligne_commande=lc.no_ligne_commande where ot.etape = 'TRACTION') "
				        + "    UNION "
						+ " select CASE WHEN ROW_NUMBER () OVER (PARTITION BY ot.no_ligne_commande ORDER BY ot.no_ot asc) = 1 AND ot.code_expediteur = al.code_expediteur then 0 else ROW_NUMBER () OVER (PARTITION BY ot.no_ligne_commande ORDER BY ot.no_ot asc) END as num_ot, ot.no_ligne_commande, ot.no_ot,ot.code_expediteur,COUNT(*) OVER (PARTITION BY ot.no_ligne_commande) as MAX_OT	"			
				        + "    from ot "
						+ "	left join agence_liv al on al.no_ot = ot.no_ot  "
						+ "	inner join ligcomm lc on lc.no_ligne_commande = ot.no_ligne_commande "
						+ "	where lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
						+ "	and ot.etape in ('TRACTION') "
						+ "	ORDER by no_ligne_commande, no_ot) "
						+ "SELECT distinct(ot.NO_OT), \r\n"
						+ "				 ot.no_ligne_commande,\r\n"
						+ "				 cast(p_op.date_de_debut_reelle AS timestamp) as date_debut_reelle, \r\n"
						+ "				 cast(p_op.date_de_fin_reelle AS timestamp) as date_fin_reelle, \r\n"
						+ "         CASE WHEN n_ot.num_ot = 0 then '' WHEN n_ot.num_ot > 0 then ot.code_destinataire END as AGENCE_OPERATION,\r\n"
						+ "				 p_op.AGENCE_OT,\r\n"
						+ "				 p_op.type,\r\n"
						+ "				 ot.ETAPE,\r\n"
						+ "				 case when ot.code_destinataire = al.code_expediteur  and n_ot.num_ot >= 1 then 'OUI' WHEN n_ot.num_ot = 0 and ot.code_expediteur = al.code_expediteur then 'OUI' else 'NON' END as AgenceDeLivraison,\r\n"
						+ "         n_ot.num_ot\r\n"
						+ "		--		 Case  WHEN replace(ot.code_expediteur,'Q','TRS') =  p_op.AGENCE_OT  AND ot.code_destinataire = al.valeur3_chaine THEN 0 WHEN replace(ot.code_expediteur,'Q','TRS')= p_op.AGENCE_OT AND ot.ETAPE = 'TRACTION'  THEN 1 WHEN replace(ot.code_destinataire,'Q','TRS') <> p_op.AGENCE_OT AND ot.ETAPE = 'TRACTION' THEN 2 else 3  END Numero_ot\r\n"
						+ "				 FROM p_op\r\n"
						+ "				 inner join ot on  p_op.no_ot = ot.no_ot \r\n"
						+ "         RIGHT join numero_ot n_ot on n_ot.NO_OT = ot.NO_OT\r\n"
						+ "				 inner join ligcomm lc on ot.no_ligne_commande = lc.no_ligne_commande\r\n"
						+ "				 inner join a_proprietes aprop on lc.NO_LIGNE_COMMANDE = aprop.VALEUR1_DOUBLE \r\n"
						+ "         inner join agence_liv al on lc.no_ligne_commande = al.no_ligne_commande\r\n"
						+ "				 AND p_op.agence_ot = ot.agence_ot\r\n"
						+ "				 and p_op.type <> 'ENLEVEMENT'\r\n"
						+ "         and (ot.etape in ('TRACTION') OR ot.ETAPE = 'LIVRAISON' AND n_ot.num_ot <= 1 )\r\n"
						//+ "				 AND lc.date_saisie >= to_date('01/01/23','dd/mm/yy')\r\n"
						+ " and  lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
						+ "				 and aprop.NOM = 'TRS_PCH_PREVA'\r\n"
						+ " and lc.no_ligne_commande not in (select no_ref from a_proprietes aProp1 where aProp1.nom = 'TRS_TRACTION_PREVA' AND aProp1.table_ref = 'P_OP' AND aProp1.no_ref = lc.no_ligne_commande AND aProp1.VALEUR1_DOUBLE = ot.NO_OT AND VALEUR2_CHAINE = 'PCH')"
						+ "				 order by ot.no_ligne_commande, ot.NO_OT"
				;  
				break;
				
			case PREVA :
				sSql = "with agence_liv as (\r\n"
						+ "select ot.no_ligne_commande,ot.no_ot, ot.code_expediteur \r\n"
						+ "from ot \r\n"
						+ "inner join ligcomm lc on lc.no_ligne_commande = ot.no_ligne_commande\r\n"
						+ "where no_ot_apres is null \r\n"
						+ "and lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') and ot.etape = 'LIVRAISON'\r\n"
						+ "\r\n"
						+ "),\r\n"
						+ " numero_ot as ( "
						+ " select CASE WHEN ROW_NUMBER () OVER (PARTITION BY lc.no_ligne_commande ORDER BY ot.no_ot asc) = 1  then 0 else ROW_NUMBER () OVER (PARTITION BY lc.no_ligne_commande ORDER BY ot.no_ot asc) END as num_ot, lc.no_ligne_commande, ot.no_ot,ot.code_expediteur,COUNT(*) OVER (PARTITION BY ot.no_ligne_commande) as MAX_OT "				
				        + "    from ot "
						+ "				inner join ligcomm lc on lc.no_ligne_commande = ot.no_ligne_commande  "
						+ "				where lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
						+ "				and ot.etape in ('LIVRAISON') "
				        + "    and lc.no_ligne_commande not in (select lc.no_ligne_commande from ligcomm lc inner join ot on ot.no_ligne_commande=lc.no_ligne_commande where ot.etape = 'TRACTION') "
				        + "    UNION "
						+ " select CASE WHEN ROW_NUMBER () OVER (PARTITION BY ot.no_ligne_commande ORDER BY ot.no_ot asc) = 1 AND ot.code_expediteur = al.code_expediteur then 0 else ROW_NUMBER () OVER (PARTITION BY ot.no_ligne_commande ORDER BY ot.no_ot asc) END as num_ot, ot.no_ligne_commande, ot.no_ot,ot.code_expediteur,COUNT(*) OVER (PARTITION BY ot.no_ligne_commande) as MAX_OT	"			
				        + "    from ot "
						+ "	left join agence_liv al on al.no_ot = ot.no_ot  "
						+ "	inner join ligcomm lc on lc.no_ligne_commande = ot.no_ligne_commande "
						+ "	where lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
						+ "	and ot.etape in ('TRACTION') "
						+ "	ORDER by no_ligne_commande, no_ot) "
						+ "SELECT distinct(ot.NO_OT), \r\n"
						+ "	ot.no_ligne_commande,\r\n"
						+ "	cast(p_op.date_de_debut_reelle AS timestamp) as date_debut_reelle, \r\n"
						+ "	cast(p_op.date_de_fin_reelle AS timestamp) as date_fin_reelle, \r\n"
						+ " CASE WHEN n_ot.num_ot = 0 then '' WHEN n_ot.num_ot > 0 then ot.code_destinataire END as AGENCE_OPERATION,\r\n"
						+ "	p_op.AGENCE_OT,\r\n"
						+ "	p_op.type,\r\n"
						+ "	ot.ETAPE,\r\n"
						+ "	case when ot.code_destinataire = al.code_expediteur  and n_ot.num_ot >= 1  then 'OUI' WHEN n_ot.num_ot = 0 and ot.code_expediteur = al.code_expediteur then 'OUI' else 'NON' END as AgenceDeLivraison,\r\n"
						+ " n_ot.num_ot\r\n"
						+ "	--Case  WHEN replace(ot.code_expediteur,'Q','TRS') =  p_op.AGENCE_OT  AND ot.code_destinataire = al.valeur3_chaine THEN 0 WHEN replace(ot.code_expediteur,'Q','TRS')= p_op.AGENCE_OT AND ot.ETAPE = 'TRACTION'  THEN 1 WHEN replace(ot.code_destinataire,'Q','TRS') <> p_op.AGENCE_OT AND ot.ETAPE = 'TRACTION' THEN 2 else 3  END Numero_ot\r\n"
						+ "	FROM p_op "
						+ " inner join ot on  p_op.no_ot = ot.no_ot "
						+ " RIGHT join numero_ot n_ot on n_ot.NO_OT = ot.NO_OT "
						+ "	inner join ligcomm lc on ot.no_ligne_commande = lc.no_ligne_commande\r\n"
						+ " inner join agence_liv al on lc.no_ligne_commande = al.no_ligne_commande\r\n"
						+ " INNER JOIN histosav h on h.no_ot = ot.no_ot "
						+ "	WHERE p_op.agence_ot = ot.agence_ot\r\n"
						+ "	and p_op.type <> 'ENLEVEMENT'\r\n"
						+ " and (ot.etape in ('TRACTION') OR ot.ETAPE = 'LIVRAISON' AND n_ot.num_ot <= 1 )\r\n"
					//	+ "	AND lc.date_saisie >= to_date('01/01/23','dd/mm/yy')\r\n"
						+ " and  lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
						+ " AND h.CODE_JUSTIFICATION = 'INS' "
						+ " AND h.CODE_SITUATION = 'RST' "
						+ " AND h.libelle like 'RdV web proposé%' "
						+ " and lc.no_ligne_commande not in (select no_ref from a_proprietes aProp1 where aProp1.nom = 'TRS_TRACTION_PREVA' AND aProp1.table_ref = 'P_OP' AND aProp1.no_ref = lc.no_ligne_commande AND aProp1.VALEUR1_DOUBLE = ot.NO_OT AND VALEUR2_CHAINE = 'PREVA')"
						+ " order by ot.no_ligne_commande, ot.NO_OT"
				;  
				break;
			
			case CONFIMPLICITE :
				sSql = "with agence_liv as (\r\n"
						+ "select ot.no_ligne_commande,ot.no_ot, ot.code_expediteur \r\n"
						+ "from ot \r\n"
						+ "inner join ligcomm lc on lc.no_ligne_commande = ot.no_ligne_commande\r\n"
						+ "where no_ot_apres is null \r\n"
						+ "and lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') and ot.etape = 'LIVRAISON'\r\n"
						+ "\r\n"
						+ "),\r\n"
						+ " numero_ot as ( "
						+ " select CASE WHEN ROW_NUMBER () OVER (PARTITION BY lc.no_ligne_commande ORDER BY ot.no_ot asc) = 1  then 0 else ROW_NUMBER () OVER (PARTITION BY lc.no_ligne_commande ORDER BY ot.no_ot asc) END as num_ot, lc.no_ligne_commande, ot.no_ot,ot.code_expediteur,COUNT(*) OVER (PARTITION BY ot.no_ligne_commande) as MAX_OT "				
				        + "    from ot "
						+ "				inner join ligcomm lc on lc.no_ligne_commande = ot.no_ligne_commande  "
						+ "				where lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
						+ "				and ot.etape in ('LIVRAISON') "
				        + "    and lc.no_ligne_commande not in (select lc.no_ligne_commande from ligcomm lc inner join ot on ot.no_ligne_commande=lc.no_ligne_commande where ot.etape = 'TRACTION') "
				        + "    UNION "
						+ " select CASE WHEN ROW_NUMBER () OVER (PARTITION BY ot.no_ligne_commande ORDER BY ot.no_ot asc) = 1 AND ot.code_expediteur = al.code_expediteur then 0 else ROW_NUMBER () OVER (PARTITION BY ot.no_ligne_commande ORDER BY ot.no_ot asc) END as num_ot, ot.no_ligne_commande, ot.no_ot,ot.code_expediteur,COUNT(*) OVER (PARTITION BY ot.no_ligne_commande) as MAX_OT	"			
				        + "    from ot "
						+ "	left join agence_liv al on al.no_ot = ot.no_ot  "
						+ "	inner join ligcomm lc on lc.no_ligne_commande = ot.no_ligne_commande "
						+ "	where lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
						+ "	and ot.etape in ('TRACTION') "
						+ "	ORDER by no_ligne_commande, no_ot) "
						+ "SELECT distinct(ot.NO_OT), \r\n"
						+ "	ot.no_ligne_commande,\r\n"
						+ "	cast(p_op.date_de_debut_reelle AS timestamp) as date_debut_reelle, \r\n"
						+ "	cast(p_op.date_de_fin_reelle AS timestamp) as date_fin_reelle, \r\n"
						+ " CASE WHEN n_ot.num_ot = 0 then '' WHEN n_ot.num_ot > 0 then ot.code_destinataire END as AGENCE_OPERATION,\r\n"
						+ "	p_op.AGENCE_OT,\r\n"
						+ "	p_op.type,\r\n"
						+ "	ot.ETAPE,\r\n"
						+ "	case when ot.code_destinataire = al.code_expediteur  and n_ot.num_ot >= 1  then 'OUI' WHEN n_ot.num_ot = 0 and ot.code_expediteur = al.code_expediteur then 'OUI' else 'NON' END as AgenceDeLivraison,\r\n"
						+ " n_ot.num_ot\r\n"
						+ "	--Case  WHEN replace(ot.code_expediteur,'Q','TRS') =  p_op.AGENCE_OT  AND ot.code_destinataire = al.valeur3_chaine THEN 0 WHEN replace(ot.code_expediteur,'Q','TRS')= p_op.AGENCE_OT AND ot.ETAPE = 'TRACTION'  THEN 1 WHEN replace(ot.code_destinataire,'Q','TRS') <> p_op.AGENCE_OT AND ot.ETAPE = 'TRACTION' THEN 2 else 3  END Numero_ot\r\n"
						+ "	FROM p_op "
						+ " inner join ot on  p_op.no_ot = ot.no_ot "
						+ " RIGHT join numero_ot n_ot on n_ot.NO_OT = ot.NO_OT "
						+ "	inner join ligcomm lc on ot.no_ligne_commande = lc.no_ligne_commande\r\n"
						+ " inner join agence_liv al on lc.no_ligne_commande = al.no_ligne_commande\r\n"
						+ " INNER JOIN histosav h on h.no_ot = ot.no_ot "
						+ "	WHERE p_op.agence_ot = ot.agence_ot\r\n"
						+ "	and p_op.type <> 'ENLEVEMENT'\r\n"
						+ " and (ot.etape in ('TRACTION') OR ot.ETAPE = 'LIVRAISON' AND n_ot.num_ot <= 1 )\r\n"
						//+ "	AND lc.date_saisie >= to_date('01/01/23','dd/mm/yy')\r\n"	
						+ " and  lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
						+ " AND (h.CODE_JUSTIFICATION = 'LDF' "
						+ " AND h.CODE_SITUATION = 'RST' "
						+ " and lc.no_ligne_commande not in (select no_ref from a_proprietes aProp1 where aProp1.nom = 'TRS_TRACTION_PREVA' AND aProp1.table_ref = 'P_OP' AND aProp1.no_ref = lc.no_ligne_commande AND aProp1.VALEUR1_DOUBLE = ot.NO_OT AND VALEUR2_CHAINE = 'CONFIMPLICITE')"
						+ " AND h.CHAMPS1 is not null) "

				;  
				break;
				
			case CONFEXPLICITE :
				sSql = "with agence_liv as (\r\n"
						+ "select ot.no_ligne_commande,ot.no_ot, ot.code_expediteur \r\n"
						+ "from ot \r\n"
						+ "inner join ligcomm lc on lc.no_ligne_commande = ot.no_ligne_commande\r\n"
						+ "where no_ot_apres is null \r\n"
						+ "and lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') and ot.etape = 'LIVRAISON'\r\n"
						+ "\r\n"
						+ "),\r\n"
						+ " numero_ot as ( "
						+ " select CASE WHEN ROW_NUMBER () OVER (PARTITION BY lc.no_ligne_commande ORDER BY ot.no_ot asc) = 1  then 0 else ROW_NUMBER () OVER (PARTITION BY lc.no_ligne_commande ORDER BY ot.no_ot asc) END as num_ot, lc.no_ligne_commande, ot.no_ot,ot.code_expediteur,COUNT(*) OVER (PARTITION BY ot.no_ligne_commande) as MAX_OT "				
				        + "    from ot "
						+ "				inner join ligcomm lc on lc.no_ligne_commande = ot.no_ligne_commande  "
						+ "				where lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
						+ "				and ot.etape in ('LIVRAISON') "
				        + "    and lc.no_ligne_commande not in (select lc.no_ligne_commande from ligcomm lc inner join ot on ot.no_ligne_commande=lc.no_ligne_commande where ot.etape = 'TRACTION') "
				        + "    UNION "
						+ " select CASE WHEN ROW_NUMBER () OVER (PARTITION BY ot.no_ligne_commande ORDER BY ot.no_ot asc) = 1 AND ot.code_expediteur = al.code_expediteur then 0 else ROW_NUMBER () OVER (PARTITION BY ot.no_ligne_commande ORDER BY ot.no_ot asc) END as num_ot, ot.no_ligne_commande, ot.no_ot,ot.code_expediteur,COUNT(*) OVER (PARTITION BY ot.no_ligne_commande) as MAX_OT	"			
				        + "    from ot "
						+ "	left join agence_liv al on al.no_ot = ot.no_ot  "
						+ "	inner join ligcomm lc on lc.no_ligne_commande = ot.no_ligne_commande "
						+ "	where lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
						+ "	and ot.etape in ('TRACTION') "
						+ "	ORDER by no_ligne_commande, no_ot) "
						+ "SELECT distinct(ot.NO_OT), \r\n"
						+ "	ot.no_ligne_commande,\r\n"
						+ "	cast(p_op.date_de_debut_reelle AS timestamp) as date_debut_reelle, \r\n"
						+ "	cast(p_op.date_de_fin_reelle AS timestamp) as date_fin_reelle, \r\n"
						+ " CASE WHEN n_ot.num_ot = 0 then '' WHEN n_ot.num_ot > 0 then ot.code_destinataire END as AGENCE_OPERATION,\r\n"
						+ "	p_op.AGENCE_OT,\r\n"
						+ "	p_op.type,\r\n"
						+ "	ot.ETAPE,\r\n"
						+ "	case when ot.code_destinataire = al.code_expediteur  and n_ot.num_ot >= 1  THEN 'OUI' WHEN n_ot.num_ot = 0 and ot.code_expediteur = al.code_expediteur then 'OUI' else 'NON' END as AgenceDeLivraison,\r\n"
						+ " n_ot.num_ot\r\n"
						+ "	--Case  WHEN replace(ot.code_expediteur,'Q','TRS') =  p_op.AGENCE_OT  AND ot.code_destinataire = al.valeur3_chaine THEN 0 WHEN replace(ot.code_expediteur,'Q','TRS')= p_op.AGENCE_OT AND ot.ETAPE = 'TRACTION'  THEN 1 WHEN replace(ot.code_destinataire,'Q','TRS') <> p_op.AGENCE_OT AND ot.ETAPE = 'TRACTION' THEN 2 else 3  END Numero_ot\r\n"
						+ "	FROM p_op "
						+ " inner join ot on  p_op.no_ot = ot.no_ot "
						+ " RIGHT join numero_ot n_ot on n_ot.NO_OT = ot.NO_OT "
						+ "	inner join ligcomm lc on ot.no_ligne_commande = lc.no_ligne_commande\r\n"
						+ " inner join agence_liv al on lc.no_ligne_commande = al.no_ligne_commande\r\n"
						+ " INNER JOIN histosav h on h.no_ot = ot.no_ot "
						+ "	WHERE p_op.agence_ot = ot.agence_ot\r\n"
						+ "	and p_op.type <> 'ENLEVEMENT'\r\n"
						+ " and (ot.etape in ('TRACTION') OR ot.ETAPE = 'LIVRAISON' AND n_ot.num_ot <= 1 )\r\n"
						//+ "	AND lc.date_saisie >= to_date('01/01/23','dd/mm/yy')\r\n"
						+ " and  lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
						+ " AND (h.CODE_JUSTIFICATION = 'CFM' "
						+ " AND h.CODE_SITUATION = 'MLV' "
						+ " AND h.CHAMPS1 is not null )"
						+ " and lc.no_ligne_commande not in (select no_ref from a_proprietes aProp1 where aProp1.nom = 'TRS_TRACTION_PREVA' AND aProp1.table_ref = 'P_OP' AND aProp1.no_ref = lc.no_ligne_commande AND aProp1.VALEUR1_DOUBLE = ot.NO_OT AND VALEUR2_CHAINE = 'CONFEXPLICITE') "
						+ " order by ot.no_ligne_commande, ot.NO_OT"
						;  
				break;
			default : 
				throw new TrsException(HistoSavAccess.class.getEnclosingMethod().getName() + " : Parametre asReqConf [" + asReqConf
					+ "] INCONNU : valeurs attendues = PCH,PREVA,CONFIMPLICITE,CONFEXPLICITE " ); 	
		
	}

		int iRetour = this._DBBean.executeSQL(sSql);
		
		if ( iRetour < 0 )
			return myLcList;
		
	

		while ( this._DBBean.next() )
		{
			
		
		
		myP_OP = new hP_OP(this._DBBean.getString("no_ligne_commande"));
		myLog.info("Commande : " + myP_OP.get_refCommande() );
		myP_OP.set_refCommande(this._DBBean.getString("no_ligne_commande"));
		myP_OP.set_agenceEnlevement(this._DBBean.getString("AGENCE_OT"));
		myP_OP.set_numeroTraction(this._DBBean.getInt("num_ot"));	
		myP_OP.set_heureDeDebutReelle(this._DBBean.getString("date_debut_reelle"));
		myP_OP.set_heureDeFinReelle(this._DBBean.getString("date_fin_reelle"));


		myOT = new hOT(this._DBBean.getString("NO_LIGNE_COMMANDE"));
		myOT.set_agenceDestinataire(this._DBBean.getString("AGENCE_OPERATION"));
		myOT.set_agenceLivraison(this._DBBean.getString("AgenceDeLivraison"));
		myOT.setNoOt(this._DBBean.getInt("NO_OT"));
		iTempNumOtBefore = this._DBBean.getInt("NO_OT");
		
		myLog.info("Agence de livraison : " + myOT.get_agenceLivraison());
		
		

		myLog.debug("  - commande = " + myP_OP.get_refCommande());

		// Date prévisionnelle de livraison
		// Au format dd/mm/yyyy HH:MM:SS stocké dans HISTO_SAV
		// A passer au format yyyy-mm-dd HH:MM:SS
		myP_OP.set_OT(myOT);
		myLcList.add(myP_OP);
	
		}
		return myLcList;	
		
}
	
public List<hP_OP> getSqlforMLV(String asDateCreation,String asReqConf, String asTableRef, int aiNbJours) throws TrsException {
		
		List<hP_OP> MyP_OPList = new ArrayList<hP_OP>();
		hOT myOT;
		hHistoSav myHS;
		hP_OP myP_OP;
		String sRequete = "";
		String sEnregistrement = "";
		String sTempOuvre = "";
		
		
		switch(asReqConf) {
		case CREATION : 
			sRequete =  "select ot.no_ligne_commande,p_op.no_ot, p_op.agence_ot, DATE_DE_DEBUT, DATE_DE_DEBUT_REELLE,'CREATION' as Enregistrement,h.NOID from p_op"
				+ " inner join ot on ot.NO_OT = P_OP.NO_OT"
				+ " inner join HISTOSAV h on h.NO_OT = P_OP.NO_OT"
				+ " inner join LIGCOMM lc on lc.no_ligne_commande = ot.no_ligne_commande "
				+ " where ot.NO_OT_APRES is null"
				+ " and p_op.type = 'LIVRAISON'"
				+ " and h.CODE_SITUATION = 'MLV' and h.CODE_JUSTIFICATION = 'CFM'"
			//	+ " and lc.date_saisie >= to_date('01/01/2023','dd/mm/yyyy')"
			+ " and  lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
			+ " and lc.no_ligne_commande not in (select no_ref from a_proprietes aProp1 where aProp1.nom = 'TRS_MLV_PREVA' AND aProp1.table_ref = 'P_OP' AND aProp1.no_ref = lc.no_ligne_commande AND aProp1.VALEUR1_DOUBLE = ot.NO_OT AND VALEUR2_CHAINE = 'CREATION' and VALEUR2_double = NOLIGNE) "
			
				;  
				break;
		case ANNULATION :
			sRequete =  " select ot.no_ligne_commande,p_op.no_ot, p_op.agence_ot, DATE_DE_DEBUT, DATE_DE_DEBUT_REELLE,'ANNULATION' as Enregistrement,h.NOID from p_op"
					+ " inner join ot on ot.NO_OT = P_OP.NO_OT"
					+ " inner join HISTOSAV h on h.NO_OT = P_OP.NO_OT"
					+ " inner join LIGCOMM lc on lc.no_ligne_commande = ot.no_ligne_commande "
					+ " where ot.NO_OT_APRES is null"
					+ " and p_op.type = 'LIVRAISON'"
					+ " and h.CODE_SITUATION = 'RST' and h.CODE_JUSTIFICATION = 'AUT'"
					//+ " and lc.date_saisie >= to_date('01/01/2023','dd/mm/yyyy')"
					+ " and  lc.date_saisie >= to_date('" + asDateCreation + "', 'dd/mm/yyyy') "
					+ " and lc.no_ligne_commande not in (select no_ref from a_proprietes aProp1 where aProp1.nom = 'TRS_MLV_PREVA' AND aProp1.table_ref = 'P_OP' AND aProp1.no_ref = lc.no_ligne_commande AND aProp1.VALEUR1_DOUBLE = ot.NO_OT AND VALEUR2_CHAINE = 'ANNULATION' and VALEUR2_double = NOLIGNE) "
					;  
					break;
				
		default : 
			throw new TrsException(HistoSavAccess.class.getEnclosingMethod().getName() + " : Parametre asReqConf [" + asReqConf
				+ "] INCONNU : valeurs attendues = CREATION,ANNULATION " ); 		
		
		}
		int iRetour = this._DBBean.executeSQL(sRequete);
		
		if ( iRetour < 0 )
			return MyP_OPList;
		while ( this._DBBean.next() )
		{
		myP_OP = new hP_OP(this._DBBean.getString("no_ligne_commande"));
		myP_OP.set_refCommande(this._DBBean.getString("no_ligne_commande"));
		myP_OP.set_agenceEnlevement(this._DBBean.getString("agence_ot"));
		
		sTempOuvre = this._DBBean.getString("DATE_DE_DEBUT");
		sTempOuvre = sTempOuvre.substring(8, 10) + "/" + sTempOuvre.substring(5, 7) + "/" +  sTempOuvre.substring(0, 4);
		//sTempOuvre = sTempOuvre.substring(6, 10) + "-" + sTempOuvre.substring(3, 5) + "-" + sTempOuvre.substring(0, 2) + " " + sTempOuvre.substring(11); 
		myLog.info("Date" + sTempOuvre);
		myLog.info("Date" + this._DBBean.getString("DATE_DE_DEBUT"));
		myP_OP.setDateMLVJourOuvre(sTempOuvre);
		
		myP_OP.set_dateDeDebut(this._DBBean.getString("DATE_DE_DEBUT"));
		myP_OP.set_heureDeDebut(this._DBBean.getString("DATE_DE_DEBUT").substring(11,this._DBBean.getString("DATE_DE_DEBUT").length()-2 ));
		
		

		myP_OP.set_enregistrement(this._DBBean.getString("Enregistrement"));
		
		myOT = new hOT(this._DBBean.getString("NO_LIGNE_COMMANDE"));
		myOT.setNoOt(this._DBBean.getInt("NO_OT"));
		
		myHS = new hHistoSav(this._DBBean.getInt("NO_OT"));
		myHS.set_noID(this._DBBean.getInt("NOID"));
	
		myOT.set_hHistoSav(myHS);
		myP_OP.set_OT(myOT);
		MyP_OPList.add(myP_OP);
		}
		return MyP_OPList;
		
	}

}
