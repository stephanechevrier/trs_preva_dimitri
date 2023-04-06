
package com.trs.preva.metier;

import com.trs.wintrans.metier.P_OP;

import java.util.Calendar;

import com.trs.exception.AppliException;
import com.trs.exception.TrsException;
import com.trs.utils.calendar.CalendarHelper;
import com.trs.wintrans.dbAccess.CalendrierTrsAccess;
import com.trs.wintrans.metier.HistoSav;
import com.trs.wintrans.metier.OT;

public class hP_OP extends P_OP {

	String _agenceEnlevement = "";
	String _enregistrement = "";
	int _numeroTraction;
	Calendar _calPrevJourOuvre = Calendar.getInstance();;
	private hOT aOt;
	
	
	
	
	
	
	public hP_OP(String asRefCommande) {
		super(asRefCommande);
		// TODO Auto-generated constructor stub
	}

public void set_OT(hOT NumOt) {
	
	this.aOt = NumOt;
}

public hOT get_OT() {
	
	return this.aOt;
}

public String get_agenceEnlevement() {
	
	return this._agenceEnlevement;	
}

public void set_agenceEnlevement(String agenceEnlevement) {
	this._agenceEnlevement = agenceEnlevement;
}

public int get_numeroTraction() {
	return this._numeroTraction;
}

public void set_numeroTraction(int numeroTraction) {
	this._numeroTraction = numeroTraction;
}

public void set_enregistrement(String enregistrement) {
	this._enregistrement = enregistrement;
}

public String get_enregistrement() {
	return this._enregistrement;
}

public Calendar getDatePrevJourOuvre() 
{
	// 23/11/2021 - Création
	
	return this._calPrevJourOuvre;
}

public void setDateMLVJourOuvre(String asDate) throws TrsException 
{
	// 23/11/2021 - Création
	
	try { _calPrevJourOuvre = CalendarHelper.getCalendar(asDate,CalendarHelper.FORMAT_DDMMYYYY);}
	catch ( AppliException e )
	{
		throw new TrsException(HistoSav.class.getEnclosingMethod().getName() + " : ERREUR lors de la transformation en Calendar de [" 
			+ asDate + "] : "+ e.toString());
	}
}

}