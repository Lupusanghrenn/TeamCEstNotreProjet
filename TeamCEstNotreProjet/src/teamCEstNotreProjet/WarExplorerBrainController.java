package teamCEstNotreProjet;


import java.awt.Color;
import java.util.ArrayList;

import edu.warbot.agents.MovableWarAgent;
import edu.warbot.agents.WarAgent;
import edu.warbot.agents.WarResource;
import edu.warbot.agents.agents.WarExplorer;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.agents.percepts.WarPercept;
import edu.warbot.brains.WarBrain;
import edu.warbot.brains.brains.WarExplorerBrain;
import edu.warbot.communications.WarMessage;
import edu.warbot.tools.geometry.PolarCoordinates;

public abstract class WarExplorerBrainController extends WarExplorerBrain {
	
	WTask ctask;
	Sorted_Percepts sp;
	String target;
	int nbTick;
	int nbTickMax;
	
	@Override
	public String action() {
		
		// Develop behaviour here
    	this.sp = new Sorted_Percepts(this.getPercepts(),this.getTeamName());
		
    	
    	//a enlever plus tard 

    	

    	
    	
		String toReturn = ctask.exec(this);   // le run de la FSM
		
		if(toReturn == null){
			if (isBlocked())
				setRandomHeading();
			return WarExplorer.ACTION_MOVE;
		} else {
			return toReturn;
		}
	}
	
	static WTask returnFoodTask = new WTask(){
		String exec(WarBrain bc){
			WarExplorerBrainController me = (WarExplorerBrainController) bc;
			if(me.isBagEmpty()){
				me.setHeading(me.getHeading() + 180);
				me.target="none";
				me.ctask = getFoodTask;
				return(null);
			}
					
			me.setDebugStringColor(Color.green.darker());
			me.setDebugString("Returning Food");
				
			if(me.isBlocked()){
				me.setRandomHeading();
				me.target="none";
				return WarExplorer.ACTION_MOVE;
			}

			if(me.target.equals("base") && me.getNbElementsInBag()>0){
	    		if(me.sp.getBases().size()>0){
	    			me.setHeading(me.sp.getClosestBase().getAngle());
	    		}
	    		if(me.sp.getBases().size()>0 && me.sp.getBases().get(0).getDistance()<me.MAX_DISTANCE_GIVE){
	    			//set agent to give
	    			me.setDebugString("Je give");
	    			me.setIdNextAgentToGive(me.sp.getBases().get(0).getID());
	    			return WarExplorer.ACTION_GIVE;
	    		}
	    		return WarExplorer.ACTION_MOVE;
	    	}
		    	
	    	if(me.sp.getBases().size()!=0){
	    		me.target="base";
	    		me.setHeading(me.sp.getBases().get(0).getAngle());
	    		return WarExplorer.ACTION_MOVE;
	    	}
	    	
	    	WarMessage m = me.getMessageFromBase();
	    	if(m!=null){
	    		me.target="base";
	    		me.setHeading(m.getAngle());
	    		return WarExplorer.ACTION_MOVE;
	    	}
			return WarExplorer.ACTION_MOVE;
				
			}
		};
	
	static WTask getFoodTask = new WTask(){
		String exec(WarBrain bc){
			WarExplorerBrainController me = (WarExplorerBrainController) bc;
			if(me.target.equals("start")){
				me.target="none";
				double random = Math.random();
				if(random<=0.5){
					me.setHeading(0);
				}else{
					me.setHeading(180);
				}
				
			}
			
	    	if(!me.sp.getEnnemies().isEmpty())
	    	{
		    	//me.setDebugString(me.sp.getEnnemies().get(0).toString());
		    	for(int i=0; i<me.sp.getEnnemies().size();i++)
		    	{
		    		if(!me.sp.getEnnemies().isEmpty())
		    		{
		    			String msgcnt = Double.toString(me.sp.getEnnemies().get(0).getDistance());
		    			String msgcnt1=	Double.toString(me.sp.getEnnemies().get(0).getAngle());
		    			me.broadcastMessageToAll( ContenuMessage.TargetSpotted.toString(), msgcnt,msgcnt1 );
		    		}
		    	}
	    	}
			
			if(me.isBagFull()){
				me.ctask = returnFoodTask;
				return(null);
			}
			
			if(me.isBlocked())
				me.setRandomHeading();
			
			//me.setDebugStringColor(Color.BLACK);
			//me.setDebugString("Searching food");
			
			WarAgentPercept foodPercept = me.sp.getClosestRessources();
			WarAgentPercept targetRL = me.sp.getTargetForRL();
			WarAgentPercept targetHeavy = me.sp.getTargetForHeavy();
			
			if(targetRL!=null) {
				me.setHeading(targetRL.getAngle()+180);
				me.broadcastMessageToAgentType(WarAgentType.WarRocketLauncher, ContenuMessage.EnnemyBaseFound.toString(), String.valueOf(targetRL.getDistance()),String.valueOf(targetRL.getAngle()),targetRL.getType().toString());
				//gerer autre envoi de message
			}
			
			if(targetHeavy!=null) {
				me.setHeading(targetHeavy.getAngle()+180);
				me.broadcastMessageToAgentType(WarAgentType.WarHeavy, ContenuMessage.EnnemyBaseFound.toString(), String.valueOf(targetHeavy.getDistance()),String.valueOf(targetHeavy.getAngle()),targetRL.getType().toString());
				//gerer autre envoi de message
			}
			
			//Si il y a de la nouriture
			if(foodPercept != null){
				me.broadcastMessageToAgentType(WarAgentType.WarExplorer, ContenuMessage.FoundFood.toString(), String.valueOf(foodPercept.getDistance()),String.valueOf(foodPercept.getAngle()),foodPercept.getType().toString());
				if(me.sp.getRessources().size()>=5){
					me.broadcastMessageToAgentType(WarAgentType.WarEngineer, ContenuMessage.FoundFood.toString(), String.valueOf(foodPercept.getDistance()),String.valueOf(foodPercept.getAngle()),foodPercept.getType().toString());
				}
				if(foodPercept.getDistance() > WarResource.MAX_DISTANCE_TAKE){
					me.setHeading(foodPercept.getAngle());
					return(WarExplorer.ACTION_MOVE);
				}else{
					return(WarExplorer.ACTION_TAKE);
				}
			}else if(me.getNbElementsInBag()>2) {
				//food percept ==null, retour a la base
				me.ctask = returnFoodTask;
				return(WarExplorer.ACTION_MOVE);
			}
			//gestion messageFood
			WarMessage m = me.getMessageAboutFood();
			if(m!=null){
				PolarCoordinates pc = me.getTargetedAgentPosition(m.getAngle(), m.getDistance(), Double.parseDouble(m.getContent()[0]), Double.parseDouble(m.getContent()[1]));
				me.setHeading(pc.getAngle());
			}
			return(WarExplorer.ACTION_MOVE);
		}
	};
	
	static WTask kittingEnnemy = new WTask(){
		String exec(WarBrain bc){
			WarExplorerBrainController me = (WarExplorerBrainController) bc;
			me.setDebugString("Kitting");
			me.nbTick--;
			if(me.nbTick==0) {
				me.setHeading(me.getHeading()+180);
				me.ctask=searchEnnemyBase;
			}
			
			//pas de gestion des blocage, vaut mieux etre dans le mur que perdre la cible
			
			return WarExplorer.ACTION_MOVE;
			
		}
	};
	
	static WTask searchEnnemyBase = new WTask(){
		String exec(WarBrain bc){
			WarExplorerBrainController me = (WarExplorerBrainController) bc;
			me.setDebugStringColor(Color.BLACK);
			me.setDebugString("searchEnnemyBase");
			
			//recherche d ennemi
			WarAgentPercept targetRL = me.sp.getTargetForRL();
			WarAgentPercept targetHeavy = me.sp.getTargetForHeavy();
			
			if(me.isBlocked()){
				me.setRandomHeading();
			}
			
			if(targetRL!=null) {
				me.setHeading(targetRL.getAngle()+180);
				me.ctask=kittingEnnemy;
				me.nbTick=me.nbTickMax;
				me.broadcastMessageToAgentType(WarAgentType.WarRocketLauncher, ContenuMessage.EnnemyBaseFound.toString(), String.valueOf(targetRL.getDistance()),String.valueOf(targetRL.getAngle()),targetRL.getType().toString());
				//gerer autre envoi de message
			}
			
			if(targetHeavy!=null) {
				me.setHeading(targetHeavy.getAngle()+180);
				me.ctask=kittingEnnemy;
				me.nbTick=me.nbTickMax;
				me.broadcastMessageToAgentType(WarAgentType.WarHeavy, ContenuMessage.EnnemyBaseFound.toString(), String.valueOf(targetHeavy.getDistance()),String.valueOf(targetHeavy.getAngle()),targetHeavy.getType().toString());
				//gerer autre envoi de message
			}
			
			
			//Attente des messages
			WarMessage m = me.getMessageAboutBase();
			if(m!=null) {
				System.out.println("ExplorerWar : Message recu!");
				PolarCoordinates pc = me.getTargetedAgentPosition(m.getAngle(), m.getDistance(), Double.parseDouble(m.getContent()[0]), Double.parseDouble(m.getContent()[1]));
				me.setHeading(pc.getAngle());
				me.target="searching";
				return WarExplorer.ACTION_MOVE;
			}
			
			return WarExplorer.ACTION_MOVE;
			
		}
	};
	
	static WTask chooseRole = new WTask(){
		String exec(WarBrain bc){
			WarExplorerBrainController me = (WarExplorerBrainController) bc;
			me.requestRole(Group.WarExplorer.toString(),"ExplorerFood");
			int nbInRole = me.getNumberOfAgentsInRole(Group.WarExplorer.toString(),Role.WarExplorerFood.toString());
			if(nbInRole>4) {
				//5 exploreurs de bouffe et apres que des war
				me.leaveRole(Group.WarExplorer.toString(), Role.WarExplorerFood.toString());
				me.requestRole(Group.WarExplorer.toString(), Role.WarExplorerWar.toString());
				me.ctask=searchEnnemyBase;
				System.out.println("Je suis un ExplorerWar");
			}else {
				me.ctask=getFoodTask;
			}
			return me.ctask.exec(me);
			
		}
	};

	
	
	public WarExplorerBrainController() {
		super();
		ctask = chooseRole; // initialisation de la FSM
		target="start";
		nbTick=0;
		nbTickMax=30;//nb de tick pour le kitting
		//si role = war explorer --> target="none"
		
	}   
		
	
	private WarMessage getMessageAboutFood() {
		for (WarMessage m : getMessages()) {
			if(m.getMessage().equals(ContenuMessage.FoundFood.toString()))
				return m;
		}
		return null;
	}
	
	private WarMessage getMessageAboutBase() {
		for (WarMessage m : getMessages()) {
			if(m.getMessage().equals(ContenuMessage.EnnemyBaseFound.toString()))
				return m;
		}
		return null;
	}
	
	private WarMessage getMessageFromBase() {
		for (WarMessage m : getMessages()) {
			if(m.getSenderType().equals(WarAgentType.WarBase))
				return m;
		}
		
		broadcastMessageToAgentType(WarAgentType.WarBase, ContenuMessage.WhereIsBase.toString(), "");
		return null;
	}

}


