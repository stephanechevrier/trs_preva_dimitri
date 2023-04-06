
package com.trs.preva.metier;

import com.trs.wintrans.metier.OT;

public class hOT extends OT {

	String _agenceLivraison;
	String _agenceDestinataire = "";
	String _numOt = "";
	String _numCde = "";
	private hHistoSav _Hs;
	
	public hOT(String asRefCommande) {
		super(asRefCommande);
		// TODO Auto-generated constructor stub
	}

	public void set_hHistoSav(hHistoSav NumOt) {
		
		this._Hs = NumOt;
	}

	public hHistoSav get_hHistoSav() {
		
		return this._Hs;
	}
public String get_agenceLivraison() {
	return this._agenceLivraison;
}

public void set_agenceLivraison(String AgenceLivraison) {
	this._agenceLivraison = AgenceLivraison;
}

public String get_agenceDestinataire() {
	return this._agenceDestinataire;
}

public void set_agenceDestinataire(String AgenceExpediteur) {
	this._agenceDestinataire = AgenceExpediteur;
}

public void set_numeroOt(String NumCde, String NumOt) {
	
	this._numCde = NumCde;
	this._numOt = NumOt;
	
}
}