
package com.trs.preva.metier;

import com.trs.wintrans.metier.HistoSav;
import com.trs.wintrans.metier.OT;

public class hHistoSav extends HistoSav {

	
	String _sLibelleSav = "";
	int _iNoLigne;
	
	
	
	public hHistoSav(int NO_OT) {
		super(NO_OT);
		// TODO Auto-generated constructor stub
	}


	public void set_LibelleSav(String sLibelleSav) {
		this._sLibelleSav = sLibelleSav;
	}
	
	public String get_LibelleSav() {
		return this._sLibelleSav;
	}
	
	public void set_NoLigne(int NoLigne) {
		this._iNoLigne = NoLigne;
	}
	
	public int get_NoLigne() {
		return this._iNoLigne;
	}
}