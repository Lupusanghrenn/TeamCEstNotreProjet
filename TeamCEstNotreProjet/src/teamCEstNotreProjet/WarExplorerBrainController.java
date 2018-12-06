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
	
	@Override
	public String action() {
		
		// Develop behaviour here
    	this.sp = new Sorted_Percepts(this.getPercepts(),this.getTeamName());
		
		String toReturn = ctask.exec(this);   // le run de la FSM
		
		if(toReturn == null){
			if (isBlocked())
				setRandomHeading();
			return WarExplorer.ACTION_MOVE;
		} else {
			return toReturn;
		}
	}
	
	static WTask handleMsgs = new WTask(){ 
		String exec(WarBrain bc){return "";}
	};
	
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
			}

			if(me.target.equals("base") && me.getNbElementsInBag()>0){
	    		if(me.sp.getBases().size()>0){
	    			me.setHeading(me.sp.getClosestBase().getAngle());
	    		}
	    		if(me.sp.getBases().size()>0 && me.sp.getBases().get(0).getDistance()<me.MAX_DISTANCE_GIVE){
	    			//set agent to give
	    			System.out.println("Je give");
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
	    		System.out.println("Message recu");
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
			
			if(me.isBagFull()){
				me.ctask = returnFoodTask;
				return(null);
			}
			
			if(me.isBlocked())
				me.setRandomHeading();
			
			me.setDebugStringColor(Color.BLACK);
			me.setDebugString("Searching food");
			
			WarAgentPercept foodPercept = me.sp.getClosestRessources();
			
			//Si il y a de la nouriture
			if(foodPercept != null){
				me.broadcastMessageToAgentType(WarAgentType.WarExplorer, ContenuMessage.FoundFood.toString(), String.valueOf(foodPercept.getDistance()),String.valueOf(foodPercept.getAngle()));
				if(me.sp.getRessources().size()>=5){
					me.broadcastMessageToAgentType(WarAgentType.WarEngineer, ContenuMessage.FoundFood.toString(), String.valueOf(foodPercept.getDistance()),String.valueOf(foodPercept.getAngle()));
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
	
	static WTask searchEnnemyBase = new WTask(){
		String exec(WarBrain bc){
			WarExplorerBrainController me = (WarExplorerBrainController) bc;
			if(me.target.equals("none")) {
				//Attente des messages
				WarMessage m = me.getMessageAboutFood();
				if(m!=null){
					me.setHeading(m.getAngle());
					me.target="searching";
				}
				return WarExplorer.ACTION_MOVE;//or move just in case ?
			}else if(me.target.equals("searching")) {
				//recherche d ennemi
				//Sorted_Percepts sp = new Sorted_Percepts(me.getPercepts(),me.getTeamName());
				WarAgentPercept p = me.sp.getClosestBaseThenEnnemi();
				
				if(me.isBlocked()){
					me.setRandomHeading();
					me.target="none";
				}
				if(p!=null) {
					me.setHeading(p.getAngle());
					me.broadcastMessageToAgentType(WarAgentType.WarEngineer, ContenuMessage.EnnemyBaseFound.toString(), String.valueOf(p.getDistance()),String.valueOf(p.getAngle()));
					me.broadcastMessageToAgentType(WarAgentType.WarRocketLauncher, ContenuMessage.EnnemyBaseFound.toString(), String.valueOf(p.getDistance()),String.valueOf(p.getAngle()));
					//gerer autre envoi de message
					return WarExplorer.ACTION_MOVE;
				}
				
				return WarExplorer.ACTION_MOVE;
				
			}
			
			return "";
			
		}
	};

	
	
	public WarExplorerBrainController() {
		super();
		ctask = getFoodTask; // initialisation de la FSM
		target="start";
		//si role = war explorer --> target="none"
	}

    
		
	
	private WarMessage getMessageAboutFood() {
		for (WarMessage m : getMessages()) {
			if(m.getMessage().equals(ContenuMessage.FoundFood.toString()))
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


