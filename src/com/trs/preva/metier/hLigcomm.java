
package com.trs.preva.metier;

import com.trs.wintrans.metier.Client;
import com.trs.wintrans.metier.LigComm;

public class hLigcomm extends LigComm{

	Client _myClient;
	public hLigcomm(String string) {
		super(string);
		// TODO Auto-generated constructor stub
	}


	public Client getClient()
	{
		// 29/10/2018 - Création
		
		return this._myClient;
	}

	public void setClient(Client aLC)
	{
		// 29/10/2018 - Création
		
		this._myClient = aLC;
	}
}