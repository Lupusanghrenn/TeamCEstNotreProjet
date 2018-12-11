package teamCEstNotreProjet;

import edu.warbot.agents.agents.WarBase;
import edu.warbot.agents.agents.WarExplorer;
import edu.warbot.agents.enums.WarAgentCategory;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.WarBrain;
import edu.warbot.brains.brains.WarBaseBrain;
import edu.warbot.communications.WarMessage;

import java.util.List;

public abstract class WarBaseBrainController extends WarBaseBrain {

    private boolean _inDanger;
    
    public WTask ctask;
    Sorted_Percepts sp;

    public WarBaseBrainController() {
        super();
        _inDanger = false;
    }
    
    @Override
	public String action() {
		
		// Develop behaviour here
    	this.sp = new Sorted_Percepts(this.getPercepts(),this.getTeamName());
    	updateAgentInGroup();//pour savoir le nb d agent
		
    	
    	//Message
    	this.handleMessage();
    	
    	
		String toReturn = ctask.exec(this);   // le run de la FSM
		
		return toReturn;
	}
    
    static WTask firstTick = new WTask(){
		String exec(WarBrain bc){
			WarBaseBrainController me = (WarBaseBrainController) bc;
			me.setNextAgentToCreate(WarAgentType.WarEngineer);
			me.ctask=defaultTask;
            return WarBase.ACTION_CREATE;            
		}
    };
    
    public void handleMessage() {
    	//la base repond a tout les messages
    	
    	List<WarMessage> messages = getMessages();

        for (WarMessage message : messages) {
            if (message.getMessage().equals(ContenuMessage.WhereIsBase.toString()))
                reply(message, ContenuMessage.IMHere.toString());
            else if(message.getMessage().equals(ContenuMessage.GiveToAgent.toString())){
            	this.setIdNextAgentToGive(message.getSenderID());
            }
        }
    }
    
    static WTask defaultTask = new WTask(){
		String exec(WarBrain bc){
			WarBaseBrainController me = (WarBaseBrainController) bc;
			
			if(me.sp.getClosestEnnemi()!=null) {
	    		//faire une fonction pour les percepts de la base
	    		me.ctask=underAttack;//reflexes
	    		return me.ctask.exec(me);
	    	}
			
			for(WarAgentPercept p : me.sp.getAllies()){
	        	if(p.getType().equals(WarAgentType.WarEngineer) && p.getDistance()<WarBase.MAX_DISTANCE_GIVE){
	        		if(me.getNbElementsInBag()==0){
	        			me.sendMessage(p.getID(), ContenuMessage.NoMoreFood.toString(), "");
	        		}
	        		me.setIdNextAgentToGive(p.getID());
	        		return WarBase.ACTION_GIVE;
	        	}
	        }
			
			for (WarAgentPercept percept : me.sp.getRessources()) {
	                me.broadcastMessageToGroup("Explorer", ContenuMessage.FoundFood.toString(),String.valueOf(percept.getDistance()),String.valueOf(percept.getAngle()));
	        }
			
			//Gérer le nombre d agent
			//TODO
			
			
			if (me.getNbElementsInBag() >= 0 && me.getHealth() <= 0.8 * me.getMaxHealth()) {
	            return WarBase.ACTION_EAT;
	        }
            return WarBase.ACTION_IDLE;
		}
    };
    
    static WTask underAttack = new WTask(){
		String exec(WarBrain bc){
			WarBaseBrainController me = (WarBaseBrainController) bc;
			//action differentes selon roquette ou non
			//TODO
			if(me.sp.getClosestRocket()!=null) {
				//on priorise les rocket
			}else if(me.sp.getClosestEnnemi()!=null) {
				
			}else {
				//plus d ennemi
				me.ctask=defaultTask;
			}
            return WarBase.ACTION_IDLE;            
		}
    };
    
    private void updateAgentInGroup() {
    	//TODO
    }

}
