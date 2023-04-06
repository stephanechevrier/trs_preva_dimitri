package com.trs.preva.metier;

import com.trs.action.ProprieteCommune;
import com.trs.exception.TrsException;
import com.trs.preva.TrsPrevaConstant;
import com.trs.utils.calendar.CalendarHelper;
import com.trs.utils.email.MailNotification;
import com.trs.utils.properties.Properties;
import com.trs.utils.string.StringUtil;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>Classe pour prendre en charge l'initialisation des rapports � fournir apr�s prise en compte d'un fichier en provenance du web.</p>
 * 
 * <p>A utiliser aussi pour g�n�rer les rapports aux exploitants.</p> 
 * 
 * @author Jean-No�l CATTIN
 *
 */
public class WebToSpotReportManagerLot2 
{
	private List<WebToSpotReportHeaderLot2> _myWTSRHList = new ArrayList<WebToSpotReportHeaderLot2>();
	
	private ProprieteCommune _myProps = null;
	
	private String 	_dirConf = "";
	private String	_tableauHtmlRepAnnulation = "";
	private String	_tableauHtmlRepNon = "";
	private String	_tableauHtmlRepOui = "";
	private String	_tableauHtmlRepSansReponse = "";
	
	
	static final Logger myLog = LogManager.getLogger(WebToSpotReportManagerLot2.class.getName());
	
	public WebToSpotReportManagerLot2(ProprieteCommune aProps) throws TrsException
	{
		this._myProps = aProps;
		
        try { this._dirConf = this._myProps.getDirConf(); }
        catch ( TrsException e )
        {
        	throw new TrsException("PROBLEME lors de l'initalisation du r�pertoire de configuration : " + e.toString());
        }
        
        this._dirConf = StringUtil.getDirPath(true, this._dirConf);
		myLog.info("CONFIGURATION dans [" + this._dirConf + "]");
	}
	
	/**
	 * <p>Fourniture d'une ligne de r�ponse PREVA pour affectation au bon ReportHeader</p>
	 * 
	 * @throws TrsException
	 */
	public void init(String asAgenceLivraison, String asNoLigComm, String asNoPlan, String asReponse, String asDateReponse, 
		String asTypeAcces, int aiNbReponsesNegatives, String asLoginWeb,
		String asComplementReponse, String asTypePreva,
		String asTypeLdd, String asEtage, String asNumTelLivraison, String asNumTelLivraisonOld		
		) throws TrsException
	{
        WebToSpotReportHeaderLot2 myWTSRH2 = null;
        
        // Parcours des ReportHeader pour trouver celui qui concerne l'agence pass�e en param�tre
        for ( WebToSpotReportHeaderLot2 myWTSRH : this.getWTSRHList() )
        {
        	if ( myWTSRH.getAgenceLivraison().equals(asAgenceLivraison) )
        		myWTSRH2 = myWTSRH;
        }
        
        // Cr�ation du ReportHeader si n�cessaire
        if ( myWTSRH2 == null )
        {
        	myWTSRH2 = new WebToSpotReportHeaderLot2(this._myProps, asAgenceLivraison);
        	this.addWebToSpotReportHeader(myWTSRH2);
        }
        
        // Ajout de la ligne
        myWTSRH2.addWebToSpotReportHeaderLine(asNoLigComm, asNoPlan, asReponse, asDateReponse, asTypeAcces, aiNbReponsesNegatives,
        	asLoginWeb,
    		asComplementReponse, asTypePreva,
    		asTypeLdd, asEtage, asNumTelLivraison, asNumTelLivraisonOld		
        	);
	}

	
	public List<WebToSpotReportHeaderLot2> getWTSRHList()
	{
		return this._myWTSRHList;
	}

	
	public ProprieteCommune getProperties()
	{
		return this._myProps;
	}

	/**
	 * <p>Envoi de la notification aux exploitants en fonction des informations retourn�es par le site web</p>
	 * 
	 * @throws TrsException
	 */
	public void sendNotifExploitation() throws TrsException
	{
		String	sContent = "";
		String	sPropKey = "";
		String	sValeur = "";
		
		String	sTableauHtmlRepAnnulation = "";
		String	sTableauHtmlRepNon = "";
		String	sTableauHtmlRepOui = "";
		String	sTableauHtmlRepSansReponse = "";
		
		// Pas d'informations � diffuser aux exploitants
		if ( this.getWTSRHList().size() == 0 )
		{
			myLog.info("PAS d'envoi du Rapport aux Exploitants : PAS de donnees a traiter");
			return;
		}

		myLog.info("ENVOI du Rapport aux Exploitants");

		try 
		{
			Properties myProp = new Properties();
	    	myProp.load(this._dirConf + "/" + "trsPreva.notifFonctionnel.properties");
	    	
	    	// Boucle sur les Header - c'est-�-dire les agences concern�es
	        for ( WebToSpotReportHeaderLot2 myWTSRH : this.getWTSRHList() )
	        {
	        	myLog.info("  - Agence " + myWTSRH.getAgenceLivraison());
	        	
		    	MailNotification myMN = new MailNotification(myProp, this._myProps.getEnvironnement() + ".", "notif_exploitation");
				
				sContent = myMN.getContent();
				
				// Prise en compte des destinataires exploitants (qui doivent �tre d�finis uniquement dans l'environnement de production)
				// A lire dans le fichier de propri�t�s de la notification fonctionnelle
				sPropKey = this._myProps.getEnvironnement() + "." + myWTSRH.getAgenceLivraison().toLowerCase() + ".mail";
				sValeur = myProp.getProperty(sPropKey, "");
				
				if ( ! sValeur.equals("") )
				{
					myMN.addDestTo(sValeur);
					myLog.info("  - Prise en compte des destinataires supplementaires : " + sValeur);
				}
				else
				{
					myLog.fatal("ATTENTION, le parametrage [" + sPropKey + "] est absent dans [" 
						+ this._dirConf + "/" + "trsPreva.notifFonctionnel.properties" + "]");
				}
				
				myLog.info("  - Envoi a [" + myMN.getMailTo() + "]");
				
				// Remplacement des variables
				// ******************************************************************************************************
				this.formatTableauxHtml(myWTSRH);

				sTableauHtmlRepSansReponse = this.get_tableauHtmlRepSansReponse();
				sContent = sContent.replaceFirst("@@@TABLEAU_REPONSES_SANS_REPONSE@@@", sTableauHtmlRepSansReponse);
				
				sTableauHtmlRepNon = this.get_tableauHtmlRepNon();
				sContent = sContent.replaceFirst("@@@TABLEAU_REPONSES_NON@@@", sTableauHtmlRepNon);
						
				sTableauHtmlRepOui = this.get_tableauHtmlRepOui();
				sContent = sContent.replaceFirst("@@@TABLEAU_REPONSES_OUI@@@", sTableauHtmlRepOui);
						
				sTableauHtmlRepAnnulation = this.get_tableauHtmlRepAnnulation();
				sContent = sContent.replaceFirst("@@@TABLEAU_REPONSES_ANNULATION@@@", sTableauHtmlRepAnnulation);
				
				sContent = sContent.replaceFirst("@@@CODE_AGENCE@@@", myWTSRH.getAgenceLivraison());
				
				myMN.setContent(sContent);
				
		    	myMN.sendHTMLMail();
	        }
		}
		catch ( Exception e ) 
		{
			myLog.info(e.toString() 
					+ "\r\n" + "sContent = [" + sContent + "]"
					); 
			myLog.error(e.toString() 
				+ "\r\n" + "sContent = [" + sContent + "]"
				); 
		}
	}
	
	private void addWebToSpotReportHeader(WebToSpotReportHeaderLot2 aWTSRH)
	{
		this.getWTSRHList().add(aWTSRH);
	}
	
	private void formatTableauxHtml(WebToSpotReportHeaderLot2 aWTSRH) throws TrsException
	{
		String	sCommentaireLdd = "";
		String	sErreur = "";
		String	sRepNegatives = "";
		String	sTableauHtmlDebut = "<table border=\"1\" style=\"font-family:Arial; font-size:12px;\">";
		String	sTableauHtmlFin = "</table>";

		String	sTableauHtmlRepAnnulation = "";
		String	sTableauHtmlRepNon = "";
		String	sTableauHtmlRepOui = "";
		String	sTableauHtmlRepSansReponse = "";
		
		boolean	bReponseConnue = false;
		boolean bTableauHtmlRepAnnulation = true;
		boolean bTableauHtmlRepNon = true;
		boolean bTableauHtmlRepOui = true;
		boolean bTableauHtmlRepSansReponse = true;
		
		int		iEtage = 0;
		int		iNbRep = 0;
		int		iNbMaxRepNegatives = 0;
		
		WebToSpotReportLineLot2 myWTSRL2 = null;		

		myLog.info("Mise en forme des reponses pour agence " + aWTSRH.getAgenceLivraison());
		
		iNbMaxRepNegatives = Integer.parseInt(this._myProps.getProperty(TrsPrevaConstant.REP_NEGATIVES_NB_MAXI, "3"));
		myLog.info(TrsPrevaConstant.REP_NEGATIVES_NB_MAXI + " = " + iNbMaxRepNegatives);
		
        for ( WebToSpotReportLineLot2 myWTSRL : aWTSRH.getWTSRLineList() )
        {
    		myLog.info("  - Traitement reponse " + myWTSRL.get_reponse() + " pour " + myWTSRL.get_noLigComm());
    		
        	myWTSRL2 = myWTSRL;
    		iNbRep = myWTSRL.get_nbReponsesNegatives();
			sRepNegatives = "<td>" 
				+ ( iNbRep == 0 ? iNbRep : "<b>"
				+ ( iNbRep == 1 ? iNbRep : "<font color=\"red\">" + iNbRep + "</font>" ) + "</b>" ) 
				+ "</td>";

    		
        	// R�ponses ANNULATION
        	// **********************************************************************************************
        	if ( myWTSRL.get_reponse().equals(TrsPrevaConstant.REP_ANNULATION) )
        	{
        		bReponseConnue = true;
        		
        		// 1�re ligne
        		if ( bTableauHtmlRepAnnulation )
        		{
        			bTableauHtmlRepAnnulation = false;
        			
        			sTableauHtmlRepAnnulation = sTableauHtmlDebut;
        			
        			// 12/02/2019 - Le m�me fuschia pastel que dans l'�cran Messagerie de Spot
        			//sTableauHtmlRepAnnulation += "<tr><th colspan=\"5\" bgcolor=\"#FF00FF\">"
        			sTableauHtmlRepAnnulation += "<tr><th colspan=\"5\" bgcolor=\"#EE82EE\">"
        				+ "<font style=\"font-size:14px;\"><b>Annulations</b></font></th></tr>";
        			
        			sTableauHtmlRepAnnulation += "<tr><th>R�c�piss�</th>"
        				+ "<th>Plan</th>"
        				+ "<th>Date/Heure r�ponse</th>"
        				+ "<th>Type acc�s</th>"
        				+ "<th>Utilisateur</th>"
        				+ "</tr>";
        		}
        		
        		sTableauHtmlRepAnnulation += "<tr><td>" + myWTSRL.get_noLigComm() + "</td>"
    				+ "<td>" + myWTSRL.get_noPlan() + "</td>"
    				+ "<td>" + myWTSRL.get_dateReponse(CalendarHelper.FORMAT_DDMMYYYY) + "</td>"
    				+ "<td>" + myWTSRL.get_typeAcces() + "</td>"
    				+ "<td>" + myWTSRL.get_loginWeb() + "</td>"
    				+ "</tr>"
    				;
        	}
    		
        	// R�ponses NON
        	// **********************************************************************************************
        	if ( myWTSRL.get_reponse().equals(TrsPrevaConstant.REP_NON) )
        	{
        		bReponseConnue = true;

        		// 1�re ligne
        		if ( bTableauHtmlRepNon )
        		{
        			bTableauHtmlRepNon = false;
        			
        			sTableauHtmlRepNon = sTableauHtmlDebut;

        			// 12/02/2019 - Le m�me fuschia pastel que dans l'�cran Messagerie de Spot
        			// 14/02/2019 - On passe au Cyan
        			//sTableauHtmlRepNon += "<tr><th colspan=\"5\" bgcolor=\"#FF00FF\">"
        			//sTableauHtmlRepNon += "<tr><th colspan=\"5\" bgcolor=\"#EE82EE\">"
           			//sTableauHtmlRepNon += "<tr><th colspan=\"5\" bgcolor=\"#E0FFFF\">"
        			// 28/02/2019 - On renforce le bleu
                 	sTableauHtmlRepNon += "<tr><th colspan=\"6\" bgcolor=\"#C0FFFF\">"
        				+ "<font style=\"font-size:14px;\"><b>R�ponses NON</b></font></th></tr>";
        			
        			sTableauHtmlRepNon += "<tr><th>R�c�piss�</th>"
        				+ "<th>Plan</th>"
        				+ "<th>Nb r�ponses</br>n�gatives</th>"
        				+ "<th>Date/Heure r�ponse</th>"
        				+ "<th>Type</br>acc�s</th>"
        				+ "<th>Commentaire LDD</th>"
        				+ "</tr>";
        		}
        		
        		iNbRep = myWTSRL.get_nbReponsesNegatives();
        			
        		sTableauHtmlRepNon += "<tr><td>" + myWTSRL.get_noLigComm() + "</td>"
    				+ "<td>" + myWTSRL.get_noPlan() + "</td>"
    				+ sRepNegatives
    				+ "<td>" + myWTSRL.get_dateReponse(CalendarHelper.FORMAT_DDMMYYYY) + "</td>"
    				+ "<td>" + myWTSRL.get_typeAcces() + "</td>";
        		
        		// NON + Nouvelle adresse de livraison
        		// NON + Livraison inncessible
        		if ( myWTSRL.get_reponseComplement().toUpperCase().indexOf("COORD") > - 1
        			|| myWTSRL.get_reponseComplement().toUpperCase().indexOf("INACCE") > - 1
        			)
        		{
            		sTableauHtmlRepNon += "<td>";
            		sCommentaireLdd = "";

            		if ( myWTSRL.get_reponseComplement().toUpperCase().indexOf("COORD") > - 1 )
            			sCommentaireLdd += "<b>Nouvelle adresse de livraison � prendre en compte</b> : "
            				+ "<font color=\"red\">prendre contact avec le Destinataire pour l'obtenir</font>";
            		
            		if ( myWTSRL.get_reponseComplement().toUpperCase().indexOf("INACCE") > - 1 )
            		{
            			if ( ! sCommentaireLdd.equals("") )
            				sCommentaireLdd += "<br>";
            			
            			sCommentaireLdd += "<b><font color=\"red\">Livraison inaccessible</font></b>";
            		}
            		
            		sTableauHtmlRepNon += sCommentaireLdd + "</td>";
        		}
        		else
            		sTableauHtmlRepNon += "<td></td>";
        		
            	sTableauHtmlRepNon += "</tr>";
        	}
        	
        	// R�ponses OUI
        	// **********************************************************************************************
        	if ( myWTSRL.get_reponse().equals(TrsPrevaConstant.REP_OUI) )
        	{
        		bReponseConnue = true;

        		// 1�re ligne
        		if ( bTableauHtmlRepOui )
        		{
        			bTableauHtmlRepOui = false;
        			
        			sTableauHtmlRepOui = sTableauHtmlDebut;
        			
        			// 12/02/2019 - Le m�me vert pastel que dans l'�cran Messagerie de Spot
        			//sTableauHtmlRepOui += "<tr><th colspan=\"4\"  bgcolor=\"#008000\">"
           			sTableauHtmlRepOui += "<tr><th colspan=\"5\"  bgcolor=\"#8CC479\">"
        				+ "<font style=\"font-size:14px;\">R�ponses OUI</th></tr>";
           			
        			sTableauHtmlRepOui += "<tr><th>R�c�piss�</th>"
        				+ "<th>Plan</th>"
    					+ "<th>Date/Heure r�ponse</th>"
        				+ "<th>Type acc�s</th>"
        				+ "<th>Commentaire LDD</th>"
    					+ "</tr>"
    					;
        		}
        			
    			sTableauHtmlRepOui += "<tr><td>" + myWTSRL.get_noLigComm() + "</td>"
    				+ "<td>" + myWTSRL.get_noPlan() + "</td>"
    				+ "<td>" + myWTSRL.get_dateReponse(CalendarHelper.FORMAT_DDMMYYYY) + "</td>"
    				+ "<td>" + myWTSRL.get_typeAcces() + "</td>";
        		
        		// OUI + Livraison par ascenseur
        		// OUI + Livraison par escaliers
    			// OUI + Changement num t�l livraison
        		if ( myWTSRL.get_reponseComplement().toUpperCase().indexOf("LIVASC") > - 1
        			|| myWTSRL.get_reponseComplement().toUpperCase().indexOf("LIVESC") > - 1
        			|| myWTSRL.get_reponseComplement().toUpperCase().indexOf("TELLIV") > - 1
        			)
        		{
        			sTableauHtmlRepOui += "<td>";
            		sCommentaireLdd = "";

            		if ( myWTSRL.get_reponseComplement().toUpperCase().indexOf("LIVASC") > - 1 )
            			sCommentaireLdd += "<font color=\"red\"><b>Ascenseur</b> : �tage = " + myWTSRL.get_etage() + "</font>";
            		
            		if ( myWTSRL.get_reponseComplement().toUpperCase().indexOf("LIVESC") > - 1 )
            		{
            			if ( ! sCommentaireLdd.equals("") )
            				sCommentaireLdd += "</br>";
            			
            			sCommentaireLdd += "<font color=\"red\"><b>Escalier</b> : �tage = " + myWTSRL.get_etage() + "</font>";
            		}
            		
            		// Livraison escalier avec �tage �lev�
            		if ( myWTSRL.get_reponseComplement().toUpperCase().indexOf("LIVESC") > - 1 )
            		{
            			try { iEtage = Integer.parseInt(myWTSRL.get_etage()); }
            			catch ( NumberFormatException e )
            			{
            				sErreur = "IMPOSSIBLE de convertir l'�tage [" + myWTSRL.get_etage() + "] pour la PREVA de la commande "
            					+ "[" + myWTSRL.get_noLigComm() + "]";
            				throw new TrsException(sErreur);
            			}
            			
            			if ( iEtage >= this.get_nbEtageEleve() )
            			{
                			if ( ! sCommentaireLdd.equals("") )
                				sCommentaireLdd += "</br>";
                			
                			sCommentaireLdd += "<font color=\"red\">A partir du 8�me �tage, TRS doit �tablir un devis</font>";
            			}
            		}
            		
            		if ( myWTSRL.get_reponseComplement().toUpperCase().indexOf("TELLIV") > - 1 )
            		{
            			if ( ! sCommentaireLdd.equals("") )
            				sCommentaireLdd += "</br>";
            			
            			sCommentaireLdd += "<b>Nouveau t�l. livraison</b> : " + myWTSRL.get_numTelLivraison()
            				+ ( myWTSRL.get_numTelLivraisonOld().equals("") ? "" : " ( ancien num = " + myWTSRL.get_numTelLivraisonOld() + " )" )
            				;
            		}
            		
            		sTableauHtmlRepOui += sCommentaireLdd + "</td>";
        		}
        		else
        			sTableauHtmlRepOui += "<td></td>";
    				
        		sTableauHtmlRepOui += "</tr>";
        	}
        	
        	// R�ponses SANS_REPONSE
        	// **********************************************************************************************
        	if ( myWTSRL.get_reponse().equals(TrsPrevaConstant.REP_SANS_REPONSE) )
        	{
        		bReponseConnue = true;

        		// 1�re ligne
        		if ( bTableauHtmlRepSansReponse )
        		{
        			bTableauHtmlRepSansReponse = false;
        			
        			sTableauHtmlRepSansReponse = sTableauHtmlDebut;
        			
        			// 12/02/2019 - Le m�me fuschia pastel que dans l'�cran Messagerie de Spot
        			//sTableauHtmlRepSansReponse += "<tr><th colspan=\"4\" bgcolor=\"#FF00FF\">"
        			sTableauHtmlRepSansReponse += "<tr><th colspan=\"4\" bgcolor=\"#EE82EE\">"
        				+ "<font style=\"font-size:14px;\">"
        				+ "<b>Sans R�ponse au bout du d�lai imparti</b></font></th></tr>";
        			
        			sTableauHtmlRepSansReponse += "<tr><th>R�c�piss�</th>"
        				+ "<th>Plan</th>"
        				+ "<th>Nb r�ponses n�gatives</th>"
    					+ "<th>Date / Heure r�ponse</th>"
        				+ "</tr>";
        		}
        		
        		iNbRep = myWTSRL.get_nbReponsesNegatives();
        			
        		sTableauHtmlRepSansReponse += "<tr><td>" + myWTSRL.get_noLigComm() + "</td>"
    				+ "<td>" + myWTSRL.get_noPlan() + "</td>"
    				+ sRepNegatives
    				+ "<td>" + myWTSRL.get_dateReponse(CalendarHelper.FORMAT_DDMMYYYY) + "</td>"
    				+ "</tr>"
    				;
        	}
        }
        
        // Fin du tableau R�ponses = ANNULATION
    	if ( ! sTableauHtmlRepAnnulation.equals("") )
    	{
    		sTableauHtmlRepAnnulation += sTableauHtmlFin;
    		
    		sTableauHtmlRepAnnulation += "<p>Les PREVA <u>dans le tableau ci-dessus</u> ont �t� <b>annul�es </b> sur le Site Web.</br>"
    			+ "Une annulation sur le Site Web requiert obligatoirement une authentification sur le Site Web. C'est donc logiquement "
    			+ "un utilisateur TRS qui a proc�d� � cette annulation.</p>"
    			+ "<p><i><u>Type acc�s = STANDARD</u> indique que la r�ponse a �t� fournie en utilisant le lien internet fourni (il ne "
    			+ "s'agit pas d'une r�ponse fournie par un utilisateur authentifi�).</i></p>"
    			;
    	}
        
        // Fin du tableau R�ponses = NON
    	if ( ! sTableauHtmlRepNon.equals("") )
    	{
    		sTableauHtmlRepNon += sTableauHtmlFin;
    		
    		sTableauHtmlRepNon += "<p>Les positions <u>dans le tableau ci-dessus</u> doivent �tre <b>sorties de leur plan</b>.</br>"
    			+ "Pour les <b>positions avec " + iNbMaxRepNegatives + " r�ponses n�gatives</b> (ie cons�cutives), "
    			+ "les livraisons doivent �tre <b>trait�es par t�l�phone</b>.</p>"
    			+ "<p>Pour <b>Livraison inaccessible</b>, un contact t�l�phonique doit �tre pris avec le Destinataire.</p>"
    			+ "<p><i><u>Type acc�s = STANDARD</u> indique que la r�ponse a �t� fournie en utilisant le lien internet fourni (il ne "
    			+ "s'agit pas d'une r�ponse fournie par un utilisateur authentifi�).</i></p>"
    			;
    	}
		
        // Fin du tableau R�ponses = OUI
    	if ( ! sTableauHtmlRepOui.equals("") )
    	{
    		sTableauHtmlRepOui += sTableauHtmlFin;
    		
    		sTableauHtmlRepOui += "<p>Les PREVA ci-dessus ont �t� accept�es. Vous n'avez rien de particulier � faire, si ce n'est de "
    			+ "conserver la livraison telle que valid�e avec le Destinataire.</p>"
    			+ "<p><i><u>Type acc�s = STANDARD</u> indique que la r�ponse a �t� fournie en utilisant le lien internet fourni (il ne "
    			+ "s'agit pas d'une r�ponse fournie par un utilisateur authentifi�).</i></p>"
    			;
    	}    		
        
        // Fin du tableau R�ponses = SANS_REPONSE
    	if ( ! sTableauHtmlRepSansReponse.equals("") )
    	{
    		sTableauHtmlRepSansReponse += sTableauHtmlFin;
    		
    		sTableauHtmlRepSansReponse += "<p>Les positions <u>dans le tableau ci-dessus</u> doivent �tre <b>sorties de leur plan</b>.</p>"
    			+ "<p>Pour les <b>positions avec " + iNbMaxRepNegatives + " r�ponses n�gatives</b> (ie cons�cutives), "
    			+ "les livraisons doivent �tre <b>trait�es par t�l�phone</b>.</p>"
    			;
    	}
    	
		this._tableauHtmlRepAnnulation = StringUtil.convertToHtml(sTableauHtmlRepAnnulation, false);
		this._tableauHtmlRepNon = StringUtil.convertToHtml(sTableauHtmlRepNon, false);
		this._tableauHtmlRepOui = StringUtil.convertToHtml(sTableauHtmlRepOui, false);
		this._tableauHtmlRepSansReponse = StringUtil.convertToHtml(sTableauHtmlRepSansReponse, false);
		
		if ( ! bReponseConnue )
		{
			sErreur = "La reponse " + myWTSRL2.get_reponse() 
				+ " pour " + myWTSRL2.get_noLigComm() + " ne fait PAS partie des reponses attendues !";
			myLog.fatal(sErreur);
		}
	}

	public String get_tableauHtmlRepAnnulation() {
		return _tableauHtmlRepAnnulation;
	}

	public String get_tableauHtmlRepNon() {
		return _tableauHtmlRepNon;
	}

	public String get_tableauHtmlRepOui() {
		return _tableauHtmlRepOui;
	}

	public String get_tableauHtmlRepSansReponse() {
		return _tableauHtmlRepSansReponse;
	}
	
	private int get_nbEtageEleve() throws TrsException
	{
		int		iEtage = 0;
		
		String	sErreur = "";
		String	sEtage = "";
		String	sPropKey = "etageEleve.nombre";
		
		sEtage = this.getProperties().getProperty(sPropKey, "");
		
		try { iEtage = Integer.parseInt(sEtage); }
		catch ( NumberFormatException e )
		{
			sErreur = "IMPOSSIBLE de r�cup�rer la valeur de l'�tage �lev� pour la propri�t� [" + sPropKey + "] "
				+ "dans le fichier [" + this.getProperties().getPropFileName() + "]";
			throw new TrsException(sErreur);
		}

		return iEtage;
	}
}
