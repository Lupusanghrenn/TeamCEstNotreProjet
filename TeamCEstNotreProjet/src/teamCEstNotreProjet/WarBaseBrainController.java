package teamCEstNotreProjet;

import edu.warbot.agents.agents.WarBase;
import edu.warbot.agents.agents.WarEngineer;
import edu.warbot.agents.agents.WarExplorer;
import edu.warbot.agents.agents.WarHeavy;
import edu.warbot.agents.agents.WarKamikaze;
import edu.warbot.agents.agents.WarLight;
import edu.warbot.agents.enums.WarAgentCategory;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.WarBrain;
import edu.warbot.brains.brains.WarBaseBrain;
import edu.warbot.communications.WarMessage;

import java.util.HashMap;
import java.util.List;

public abstract class WarBaseBrainController extends WarBaseBrain {

    private boolean _inDanger;
    
    public WTask ctask;
    Sorted_Percepts sp;
    HashMap<WarAgentType,Integer> nbAgentPerType;
    HashMap<Group,Integer> desiredNbAgentPerRole;
    int minHealth;

    public WarBaseBrainController() {
        super();
        _inDanger = false;
        this.ctask=firstTick;
        //init de la hasmMap
        nbAgentPerType = new HashMap<WarAgentType,Integer>();
        this.nbAgentPerType.put(WarAgentType.WarEngineer, 0);
        this.nbAgentPerType.put(WarAgentType.WarExplorer, 0);
        this.nbAgentPerType.put(WarAgentType.WarHeavy, 0);
        this.nbAgentPerType.put(WarAgentType.WarRocketLauncher, 0);
        this.nbAgentPerType.put(WarAgentType.WarTurret, 0);
        desiredNbAgentPerRole = new HashMap<Group,Integer>();

        this.desiredNbAgentPerRole.put(Group.FoodExplorer, 5);
        this.desiredNbAgentPerRole.put(Group.WarExplorer, 5);
        this.desiredNbAgentPerRole.put(Group.WarTurret, 5);
        this.desiredNbAgentPerRole.put(Group.WarHeavy, 5);
        this.desiredNbAgentPerRole.put(Group.RocketLauncher, 5);
        this.desiredNbAgentPerRole.put(Group.Engineer, 1);
        minHealth=4000;
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
			
			//Ajout dans les differents role -- USELESS
			/*me.requestRole(Group.Base.toString(), Role.Base.toString());
			me.requestRole(Group.WarExplorer.toString(), Role.Base.toString());
			me.requestRole(Group.RocketLauncher.toString(), Role.Base.toString());
			me.requestRole(Group.WarHeavy.toString(), Role.Base.toString());*/
			
			//Creation d un ingenieur
			me.setNextAgentToCreate(WarAgentType.WarEngineer);
			
			me.ctask=defaultTask;
            return WarBase.ACTION_CREATE;            
		}
    };
    
    public void handleMessage() {
    	//la base repond a tous les messages
    	
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
			if(me.getHealth()>me.minHealth)
			{
				
				//prio inge puis  explo puis assaultTeam
				if (me.nbAgentPerType.get(WarAgentType.WarEngineer)<me.desiredNbAgentPerRole.get(Group.Engineer))
				{
					me.setNextAgentToCreate(WarAgentType.WarEngineer);
					System.out.println("creation d un agent engi");
					return WarBase.ACTION_CREATE;
				}else if(me.nbAgentPerType.get(WarAgentType.WarExplorer)<me.desiredNbAgentPerRole.get(Group.FoodExplorer)+me.desiredNbAgentPerRole.get(Group.WarExplorer) )
				{
					me.setNextAgentToCreate(WarAgentType.WarExplorer);
					System.out.println("creation d un agent Explorer Food");
					return WarBase.ACTION_CREATE;
				}else {
					int nbTeam=0;
					String str = "Assault";
					while(nbTeam<5) {
						int nbExploInTeam = me.getNumberOfAgentsInRole(str+nbTeam, WarAgentType.WarExplorer.toString());
						if(nbExploInTeam==0) {
							me.setNextAgentToCreate(WarAgentType.WarExplorer);
							System.out.println("creation d un agent Explorer War");
							return WarBase.ACTION_CREATE;
						}
						int nbHeavyInTeam = me.getNumberOfAgentsInRole(str+nbTeam, WarAgentType.WarHeavy.toString());
						if(nbHeavyInTeam==0) {
							me.setNextAgentToCreate(WarAgentType.WarExplorer);
							System.out.println("creation d un agent Explorer War");
							return WarBase.ACTION_CREATE;
						}
						int nbRLInTeam = me.getNumberOfAgentsInRole(str+nbTeam, WarAgentType.WarRocketLauncher.toString());
						if(nbRLInTeam==0) {
							me.setNextAgentToCreate(WarAgentType.WarExplorer);
							System.out.println("creation d un agent Explorer War");
							return WarBase.ACTION_CREATE;
						}
						nbTeam++;
					}	
				}				
			}
			
			if (me.getNbElementsInBag() >= 0 && me.getHealth() < 0.95*me.getMaxHealth()) {
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
				WarAgentPercept p = me.sp.getClosestRocket();
				me.broadcastMessageToAgentType(WarAgentType.WarHeavy, ContenuMessage.FollowMissile.toString(), String.valueOf(p.getDistance()),String.valueOf(p.getAngle()),String.valueOf(p.getHeading()+180));
				me.broadcastMessageToAgentType(WarAgentType.WarLight, ContenuMessage.FollowMissile.toString(), String.valueOf(p.getDistance()),String.valueOf(p.getAngle()),String.valueOf(p.getHeading()+180));
			}else if(me.sp.getClosestEnnemi()!=null) {
				WarAgentPercept p = me.sp.getClosestEnnemi();
				me.broadcastMessageToAgentType(WarAgentType.WarHeavy, ContenuMessage.BaseUnderAttack.toString(), String.valueOf(p.getDistance()),String.valueOf(p.getAngle()));
				me.broadcastMessageToAgentType(WarAgentType.WarLight, ContenuMessage.BaseUnderAttack.toString(), String.valueOf(p.getDistance()),String.valueOf(p.getAngle()));
			}else {
				//plus d ennemi
				me.ctask=defaultTask;
			}
            return WarBase.ACTION_IDLE;            
		}
    };
    
    private void updateAgentInGroup() {
    	//Mis a jour de la hashMap
    	this.nbAgentPerType.put(WarAgentType.WarEngineer, this.getNumberOfAgentsInRole(WarAgentType.WarEngineer.toString(), WarAgentType.WarEngineer.toString()));
        this.nbAgentPerType.put(WarAgentType.WarExplorer, this.getNumberOfAgentsInRole(WarAgentType.WarExplorer.toString(), WarAgentType.WarExplorer.toString()));
        this.nbAgentPerType.put(WarAgentType.WarHeavy, this.getNumberOfAgentsInRole(WarAgentType.WarHeavy.toString(), WarAgentType.WarHeavy.toString()));
        this.nbAgentPerType.put(WarAgentType.WarRocketLauncher, this.getNumberOfAgentsInRole(WarAgentType.WarRocketLauncher.toString(), WarAgentType.WarRocketLauncher.toString()));
        this.nbAgentPerType.put(WarAgentType.WarTurret, this.getNumberOfAgentsInRole(WarAgentType.WarTurret.toString(), WarAgentType.WarTurret.toString()));
    	    }

}
