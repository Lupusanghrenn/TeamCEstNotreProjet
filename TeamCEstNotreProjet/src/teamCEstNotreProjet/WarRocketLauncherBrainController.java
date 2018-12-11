package teamCEstNotreProjet;

import java.util.ArrayList;
import java.util.List;

import edu.warbot.agents.agents.WarExplorer;
import edu.warbot.agents.agents.WarRocketLauncher;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.agents.projectiles.WarBomb;
import edu.warbot.brains.WarBrain;
import edu.warbot.brains.brains.WarRocketLauncherBrain;
import edu.warbot.communications.WarMessage;
import edu.warbot.tools.geometry.PolarCoordinates;

public abstract class WarRocketLauncherBrainController extends WarRocketLauncherBrain {

    WTask ctask;
    Sorted_Percepts sp;
    boolean hasTarget;
    int cptrTarget;
    int nbTickBeforeAbandon;
    PolarCoordinates targetDirection;
    double rocketDistance=WarBomb.AUTONOMY*WarBomb.SPEED;

    public WarRocketLauncherBrainController() {
        super();
        ctask=chooseRole;
		//this.requestRole(Group.RocketLauncher.toString(),Role.RocketLauncher.toString());
    }

    @Override
    public String action() {



        String toReturn = ctask.exec(this);   // le run de la FSM
        
        if(toReturn == null){
            if (isBlocked())
                setRandomHeading();
            return WarRocketLauncherBrainController.ACTION_MOVE;
        } else {
            return toReturn;
        }
    }
    
    static WTask ShootTarget = new WTask(){
        String exec(WarBrain bc)
        {
        	WarRocketLauncherBrainController me = (WarRocketLauncherBrainController) bc;
    		
            WarMessage message= me.getMessageAboutEnemiesInRange();
            if(message==null)
            {
	            me.cptrTarget--; 
            }
            else
            {
            	me.hasTarget=true;
    			me.cptrTarget=me.nbTickBeforeAbandon;
                me.targetDirection = me.getTargetedAgentPosition(message.getAngle(), message.getDistance(), Double.parseDouble(message.getContent()[1]),Double.parseDouble(message.getContent()[0]));
            }
            me.setHeading(me.targetDirection.getAngle());
            me.setTargetDistance(me.targetDirection.getDistance());

            if(me.isReloaded()) //si il est rechargé, il tire
            {
                return WarRocketLauncherBrainController.ACTION_FIRE;
            }
            if(me.cptrTarget<=0)
            {
            	me.ctask = MoveToExplorer;
            }
            if(!me.isReloaded())
            {   
            	     		           
            	return WarRocketLauncherBrainController.ACTION_RELOAD;          
            }
            return WarRocketLauncherBrainController.ACTION_MOVE;     
        }
        
    };
    
    static WTask MoveToTarget = new WTask(){
        String exec(WarBrain bc)
        {
        	
        	WarRocketLauncherBrainController me = (WarRocketLauncherBrainController) bc;
           
        	WarMessage message= me.getMessageAboutEnemiesInRange();    //detecte si des ennemis sont a portée de rocket
            if(message!=null)
            {
        		me.hasTarget=true;
        		me.cptrTarget=me.nbTickBeforeAbandon;
            	me.setDebugString(message.getContent().toString());
                me.ctask=ShootTarget;
        		me.hasTarget=true;
            }
            else if(me.getMessageAboutClosestEnemy()!=-1)
        	{
        		int numMessage = me.getMessageAboutClosestEnemy(); // sinon  un ennemi a été repéré mais ne peut pas etre touché?
        		WarMessage messageClosestEnemy = me.getMessages().get(numMessage);
        		PolarCoordinates blub=me.getTargetedAgentPosition(messageClosestEnemy.getAngle(), messageClosestEnemy.getDistance(), Double.parseDouble(messageClosestEnemy.getContent()[1]),Double.parseDouble(messageClosestEnemy.getContent()[0]));
        		me.hasTarget=true;
        		me.cptrTarget=me.nbTickBeforeAbandon;
        		double targetAngle= blub.getAngle();
        		me.setHeading(targetAngle);
                me.setTargetDistance(me.rocketDistance);
        		if (me.isBlocked())
                me.setRandomHeading();
            	return WarRocketLauncherBrainController.ACTION_MOVE;
        	}
        	else if(me.cptrTarget<=0)
        	{
        		me.ctask=MoveToExplorer;
        	}
            me.cptrTarget--;
        	return WarRocketLauncherBrainController.ACTION_MOVE;
            //double targetDistance =Double.parseDouble(message.getContent()[0]);   
            
        }
    };
    
    static WTask MoveToExplorer = new WTask(){
        String exec(WarBrain bc)
        {
        	WarRocketLauncherBrainController me = (WarRocketLauncherBrainController) bc;
            
            //}
            WarMessage message= me.getMessageAboutEnemiesInRange();    //detecte si des ennemis sont a portée de rocket
            int numMessage = me.getMessageAboutClosestEnemy(); // sinon  un ennemi a été repéré mais ne peut pas etre touché?
            if(message!=null)
            {
            	me.setDebugString(message.getContent().toString());
                me.ctask=ShootTarget;
                me.setHeading(Double.parseDouble(message.getContent()[1]));
                me.setTargetDistance(me.rocketDistance);
                return WarRocketLauncherBrainController.ACTION_FIRE;
            }
            else if(numMessage!=-1) // c'est oui
            {
                me.ctask=MoveToTarget;
            }
            if (me.isBlocked())
                me.setRandomHeading();
            return WarRocketLauncherBrainController.ACTION_MOVE;
        }
    };
    
    static WTask chooseRole = new WTask(){
        String exec(WarBrain bc)
        {
        	
        	WarRocketLauncherBrainController me = (WarRocketLauncherBrainController) bc;
           
        	me.requestRole(Group.RocketLauncher.toString(), Role.RocketLauncher.toString());
        	
        	String str = "Assault";
        	int i=0;
        	int nb = me.getNumberOfAgentsInRole(str+i, Role.RocketLauncher.toString());
        	while(nb!=0) {
        		i++;
        		nb = me.getNumberOfAgentsInRole(str+i, Role.RocketLauncher.toString());
        	}
        	me.requestRole(str+i, Role.RocketLauncher.toString());
        	
        	me.ctask=MoveToExplorer;
        	
        	return me.ctask.exec(me);
        }
    };
    
    
    protected WarMessage getMessageAboutEnemiesInRange() {
        for (WarMessage m : getMessages())
        {
            if(m.getMessage().equals(ContenuMessage.TargetSpotted.toString()))
            {
            	setDebugString("coucou");
                if(isTargetInRange(m))
                {
                    return m ;
                }
            }
        }
        return null;
    }
    
    protected int getMessageAboutClosestEnemy() {
        double previousDistance =9999;
        int numMessage=-1;

        for (int i =0;i < this.getMessages().size();i++)
        {

            if(this.getMessages().get(i).getMessage().equals(ContenuMessage.TargetSpotted.toString()))
            {   setDebugString("hola");
                double blub = Double.parseDouble(getMessages().get(i).getContent()[0]);
                if(Math.abs(blub-150)<previousDistance)
                {
                    previousDistance=blub;
                    numMessage=i;
                }
                
            }
        }
        return numMessage;
    }
    
    private Boolean isTargetInRange(WarMessage m)
    {
        
        PolarCoordinates blub= getTargetedAgentPosition(m.getAngle(), m.getDistance(), Double.parseDouble(m.getContent()[1]),Double.parseDouble(m.getContent()[0]));
        
        if((blub.getDistance()<rocketDistance+WarBomb.EXPLOSION_RADIUS))
        {
            return true;
        }
        return false;
    }
}