package teamCEstNotreProjet;

import edu.warbot.agents.agents.WarExplorer;
import edu.warbot.agents.agents.WarRocketLauncher;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.agents.projectiles.WarBomb;
import edu.warbot.agents.projectiles.WarShell;
import edu.warbot.brains.WarBrain;
import edu.warbot.brains.brains.WarHeavyBrain;
import edu.warbot.brains.brains.WarRocketLauncherBrain;
import edu.warbot.communications.WarMessage;
import edu.warbot.tools.geometry.PolarCoordinates;

public abstract class WarHeavyBrainController extends  WarHeavyBrain {



    
    WTask ctask;
    Sorted_Percepts sp;
    boolean hasTarget;
    int cptrTarget;
    int nbTickBeforeAbandon;
    PolarCoordinates targetDirection;
    double shellDistance=WarShell.AUTONOMY*WarShell.SPEED;

    public WarHeavyBrainController() {
        super();
        ctask=MoveToExplorer;
        hasTarget =false;
        cptrTarget=0;
        nbTickBeforeAbandon=20;
    }

    @Override
    public String action() {



        String toReturn = ctask.exec(this);   // le run de la FSM
        
        if(toReturn == null){
            if (isBlocked())
                setRandomHeading();
            return WarHeavyBrainController.ACTION_MOVE;
        } else {
            return toReturn;
        }
    }
    
    static WTask ShootTarget = new WTask(){
        String exec(WarBrain bc)
        {
            WarHeavyBrainController me = (WarHeavyBrainController) bc;
    		
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
            if(me.isReloaded()) //si il est rechargé, il tire
            {
                return WarHeavyBrainController.ACTION_FIRE;
            }
            if(me.cptrTarget<=0)
            {
            	me.ctask = MoveToExplorer;
            }
            if(!me.isReloaded())
            {   
            	     		           
            	return WarHeavyBrainController.ACTION_RELOAD;          
            }
            return WarHeavyBrainController.ACTION_MOVE;     
        }
        
    };
    
    static WTask MoveToTarget = new WTask(){
        String exec(WarBrain bc)
        {
        	
        	WarHeavyBrainController me = (WarHeavyBrainController) bc;
           
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
        		if (me.isBlocked())
                me.setRandomHeading();
            	return WarHeavyBrainController.ACTION_MOVE;
        	}
        	else if(me.cptrTarget<=0)
        	{
        		me.ctask=MoveToExplorer;
        	}
            me.cptrTarget--;
        	return WarHeavyBrainController.ACTION_MOVE;
            //double targetDistance =Double.parseDouble(message.getContent()[0]);   
            
        }
    };
    
    static WTask MoveToExplorer = new WTask(){
        String exec(WarBrain bc)
        {
        	WarHeavyBrainController me = (WarHeavyBrainController) bc;
            
            //}
            WarMessage message= me.getMessageAboutEnemiesInRange();    //detecte si des ennemis sont a portée de rocket
            int numMessage = me.getMessageAboutClosestEnemy(); // sinon  un ennemi a été repéré mais ne peut pas etre touché?
            if(message!=null)
            {
            	me.setDebugString(message.getContent().toString());
                me.ctask=ShootTarget;
                me.setHeading(Double.parseDouble(message.getContent()[1]));
                return WarHeavyBrainController.ACTION_FIRE;
            }
            else if(numMessage!=-1) // c'est oui
            {
                me.ctask=MoveToTarget;
            }
            if (me.isBlocked())
                me.setRandomHeading();
            return WarHeavyBrainController.ACTION_MOVE;
        }
    };
    
    
    private WarMessage getMessageAboutEnemiesInRange() {
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
            {   
            	setDebugString("hola");
                PolarCoordinates blub=getTargetedAgentPosition(getMessages().get(i).getAngle(), getMessages().get(i).getDistance(), Double.parseDouble(getMessages().get(i).getContent()[1]),Double.parseDouble(getMessages().get(i).getContent()[0]));

                if(Math.abs(blub.getDistance()-150)<previousDistance)
                {
                    previousDistance=blub.getDistance();
                    numMessage=i;
                }
                
            }
        }
        return numMessage;
    }
    
    private Boolean isTargetInRange(WarMessage m)
    {
    	PolarCoordinates blub= getTargetedAgentPosition(m.getAngle(), m.getDistance(), Double.parseDouble(m.getContent()[1]),Double.parseDouble(m.getContent()[0]));  
        if((blub.getDistance()<shellDistance))
        {
            return true;
        }
        return false;
    }


}