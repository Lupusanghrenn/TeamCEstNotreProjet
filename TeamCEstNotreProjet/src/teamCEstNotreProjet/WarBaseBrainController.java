package teamCEstNotreProjet;

import edu.warbot.agents.agents.WarBase;
import edu.warbot.agents.enums.WarAgentCategory;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.brains.WarBaseBrain;
import edu.warbot.communications.WarMessage;

import java.util.List;

public abstract class WarBaseBrainController extends WarBaseBrain {

    private boolean _alreadyCreated;
    private boolean _inDanger;

    public WarBaseBrainController() {
        super();

        _alreadyCreated = false;
        _inDanger = false;
    }


    @Override
    public String action() {
    	
        if (!_alreadyCreated) {
            setNextAgentToCreate(WarAgentType.WarEngineer);
            _alreadyCreated = true;
            return WarBase.ACTION_CREATE;
        }
        
        Sorted_Percepts sp = new Sorted_Percepts(this.getPercepts(),this.getTeamName());
        
        for(WarAgentPercept p : sp.getAllies()){
        	if(p.getType().equals(WarAgentType.WarEngineer) && p.getDistance()<WarBase.MAX_DISTANCE_GIVE){
        		if(this.getNbElementsInBag()==0){
        			this.sendMessage(p.getID(), ContenuMessage.NoMoreFood.toString(), "");
        		}
        		this.setIdNextAgentToGive(p.getID());
        		return WarBase.ACTION_GIVE;
        	}
        }

        if (getNbElementsInBag() >= 0 && getHealth() <= 0.8 * getMaxHealth())
            return WarBase.ACTION_EAT;

        if (getMaxHealth() == getHealth()) {
            _alreadyCreated = true;
        }

        List<WarMessage> messages = getMessages();

        for (WarMessage message : messages) {
            if (message.getMessage().equals(ContenuMessage.WhereIsBase.toString()))
                reply(message, ContenuMessage.IMHere.toString());
            else if(message.getMessage().equals(ContenuMessage.GiveToAgent.toString())){
            	this.setIdNextAgentToGive(message.getSenderID());
            }
        }

        for (WarAgentPercept percept : sp.getEnnemies()) {
            if (isEnemy(percept) && percept.getType().getCategory().equals(WarAgentCategory.Soldier))
                broadcastMessageToAll("I'm under attack",
                        String.valueOf(percept.getAngle()),
                        String.valueOf(percept.getDistance()));
        }

        for (WarAgentPercept percept : getPerceptsResources()) {
            if (percept.getType().equals(WarAgentType.WarFood))
                broadcastMessageToAgentType(WarAgentType.WarExplorer, ContenuMessage.FoundFood.toString(),
                        String.valueOf(percept.getAngle()),
                        String.valueOf(percept.getDistance()));
        }

        return WarBase.ACTION_IDLE;
    }

}
