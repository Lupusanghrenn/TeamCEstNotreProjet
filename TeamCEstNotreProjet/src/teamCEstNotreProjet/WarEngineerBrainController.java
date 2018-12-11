package teamCEstNotreProjet;

import java.util.ArrayList;
import java.util.List;

import edu.warbot.agents.agents.WarEngineer;
import edu.warbot.agents.agents.WarExplorer;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.brains.WarBrain;
import edu.warbot.brains.brains.WarEngineerBrain;
import edu.warbot.communications.WarMessage;
import edu.warbot.tools.geometry.PolarCoordinates;
import teamCEstNotreProjet.WTask;

public abstract class WarEngineerBrainController extends WarEngineerBrain {
	public WTask ctask;
	public Sorted_Percepts sp;
	int nbTick;
	String target;

    public WarEngineerBrainController() {
        super();
        ctask = chooseRole;
        nbTick=-1;
        target="none";
    }

    @Override
	public String action() {
		// Develop behaviour here
    	this.sp = new Sorted_Percepts(this.getPercepts(),this.getTeamName());
		
		String toReturn = ctask.exec(this);   // le run de la FSM
		
		if(toReturn == null){
			if (isBlocked()){
				setRandomHeading();
			}
			return WarEngineer.ACTION_MOVE;
		} else {
			return toReturn;
		}
	}
    
    static WTask waitForMessage = new WTask(){
    	String exec(WarBrain bc){
    		WarEngineerBrainController me = (WarEngineerBrainController) bc;
    		WarMessage m = me.getMessageAboutFood();
    		if (me.isBlocked()){
				me.setRandomHeading();
			}
    		me.setDebugString("WaitForMessage");
    		if(m!=null){
    			//on a trouve plus de  ressources
    			me.setHeading(m.getAngle());
    			double ticks = m.getDistance()/WarEngineer.SPEED;
    			me.nbTick=(int) Math.round(ticks);
    			me.ctask=reachLocation;
    		}
    		return WarEngineer.ACTION_MOVE;
    	}
    };
    
    static WTask reachLocation = new WTask(){
    	String exec(WarBrain bc){
    		WarEngineerBrainController me = (WarEngineerBrainController) bc;
    		me.setDebugString("ReachLocation "+me.nbTick);
    		me.nbTick--;
    		if(me.nbTick==0){
    			//creation tourelle puis retour a la base
    			me.setNextBuildingToBuild(WarAgentType.WarTurret);
    			me.ctask=returnToBase;
    			return WarEngineer.ACTION_BUILD;
    		}
    		return WarEngineer.ACTION_MOVE;
    	}
    };
    
    static WTask returnToBase = new WTask(){
    	String exec(WarBrain bc){
    		WarEngineerBrainController me = (WarEngineerBrainController) bc;
    		me.setDebugString("ReturnToBase");
    		
    		if(me.isBlocked()){
				me.setRandomHeading();
				me.target="none";
			}
    		
    		if(me.target.equals("base")){
	    		if(me.sp.getBases().size()>0){
	    			me.setHeading(me.sp.getClosestBase().getAngle());
	    		}
	    		if(me.sp.getBases().size()>0 && me.sp.getClosestBase().getDistance()<me.MAX_DISTANCE_GIVE){
	    			//set agent to give
	    			me.setDebugString("Je recoit");
	    			//me.setIdNextAgentToGive(me.sp.getBases().get(0).getID());
	    			me.sendMessage(me.sp.getClosestBase().getID(), ContenuMessage.GiveToAgent.toString(), me.getID()+"");
	    			me.ctask=healFromBase;
	    			return WarEngineer.ACTION_TAKE;
	    		}
	    		return WarEngineer.ACTION_MOVE;
	    	}
		    	
	    	if(me.sp.getBases().size()!=0){
	    		me.target="base";
	    		me.setHeading(me.sp.getClosestBase().getAngle());
	    		return WarEngineer.ACTION_MOVE;
	    	}
	    	
	    	WarMessage m = me.getMessageFromBase();
	    	if(m!=null){
	    		me.target="base";
	    		//PolarCoordinates pc = me.getTargetedAgentPosition(m.getAngle(), m.getDistance(), Double.parseDouble(m.getContent()[0]), Double.parseDouble(m.getContent()[1]));
	    		me.setHeading(m.getAngle());
	    		return WarEngineer.ACTION_MOVE;
	    	}
			return WarEngineer.ACTION_MOVE;
				
    	}
    };
    
    static WTask healFromBase = new WTask(){
    	String exec(WarBrain bc){
    		WarEngineerBrainController me = (WarEngineerBrainController) bc;
    		me.setDebugString("healFromBase");
    		
    		List<WarMessage> lMes = me.getMessages();
    		for(WarMessage m : lMes){
    			if(m.getMessage().equals(ContenuMessage.NoMoreFood.toString())){
    				me.ctask=waitForMessage;
    			}
    		}
    		
    		
    		if(me.getHealth()==me.getMaxHealth() && me.isBagFull()){
    			me.ctask=waitForMessage;
    			return WarEngineer.ACTION_MOVE;
    		}else if(me.getHealth()==me.getMaxHealth()){
    			return WarEngineer.ACTION_TAKE;
    			
    		}else if(!me.isBagEmpty()){
    			//me.setIdNextBuildingToRepair(me.getID());
    			return WarEngineer.ACTION_EAT;
    		}else{
    			return WarEngineer.ACTION_TAKE;
    		}
    	}
    };
    
    static WTask chooseRole = new WTask(){
        String exec(WarBrain bc)
        {
        	
        	WarEngineerBrainController me = (WarEngineerBrainController) bc;
           
        	me.requestRole(WarAgentType.WarEngineer.toString(), WarAgentType.WarEngineer.toString());
        	
        	me.ctask=waitForMessage;
        	
        	return me.ctask.exec(me);
        }
    };
    
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
