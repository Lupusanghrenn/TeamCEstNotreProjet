package teamCEstNotreProjet;

import java.util.HashMap;

import edu.warbot.agents.agents.WarEngineer;
import edu.warbot.agents.agents.WarExplorer;
import edu.warbot.agents.agents.WarHeavy;
import edu.warbot.agents.agents.WarKamikaze;
import edu.warbot.agents.agents.WarLight;
import edu.warbot.agents.agents.WarRocketLauncher;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.agents.projectiles.WarBomb;
import edu.warbot.agents.projectiles.WarRocket;
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
    private HashMap<WarAgentType,Double> speedByAgentType;


    public WarHeavyBrainController() {
        super();
        speedByAgentType = new HashMap<WarAgentType,Double>();
        this.speedByAgentType.put(WarAgentType.WarEngineer, WarEngineer.SPEED);
        this.speedByAgentType.put(WarAgentType.WarExplorer, WarExplorer.SPEED);
        this.speedByAgentType.put(WarAgentType.Wall, 0d);
        this.speedByAgentType.put(WarAgentType.WarBase, 0d);
        this.speedByAgentType.put(WarAgentType.WarHeavy, WarHeavy.SPEED);
        this.speedByAgentType.put(WarAgentType.WarKamikaze, WarKamikaze.SPEED);
        this.speedByAgentType.put(WarAgentType.WarLight, WarLight.SPEED);
        this.speedByAgentType.put(WarAgentType.WarRocketLauncher, WarRocketLauncher.SPEED);
        this.speedByAgentType.put(WarAgentType.WarRocket, WarRocket.SPEED);
        this.speedByAgentType.put(WarAgentType.WarTurret, 0d);
        ctask=MoveToExplorer;
        ctask=chooseRole;
        hasTarget =false;
        cptrTarget=0;
        nbTickBeforeAbandon=20;
    }

    @Override
    public String action() {

    	this.sp = new Sorted_Percepts(this.getPercepts(),this.getTeamName());
    	this.setDebugString(sp.getEnnemies().toString());
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
            if(me.sp.getClosestEnnemi()!=null)
            { 	
            	me.hasTarget=true;
    			me.cptrTarget=me.nbTickBeforeAbandon;
                me.targetDirection = new PolarCoordinates(me.sp.getClosestEnnemi().getDistance(),me.sp.getClosestEnnemi().getAngle());
                for (int i=0;i<WarShell.AUTONOMY;i++)
                {
                    PolarCoordinates blub = me.getTargetedAgentPosition(me.targetDirection.getAngle(), me.targetDirection.getDistance(), me.sp.getClosestEnnemi().getHeading(), i*me.speedByAgentType.get(me.sp.getClosestEnnemi().getType()));
                    if((blub.getDistance()<WarShell.SPEED&&i==0)||me.sp.getClosestEnnemi().getType()==WarAgentType.WarTurret||me.sp.getClosestEnnemi().getType()==WarAgentType.WarBase)
                    {
                        me.setHeading(me.targetDirection.getAngle());
                        break;
                    }
                    if((int)blub.getDistance()/(int)WarShell.SPEED==i)
                    {
                        me.setHeading(blub.getAngle());
                    }
                }
	            if(me.isReloaded()) //si il est rechargé, il tire
	            {	
	                return WarHeavyBrainController.ACTION_FIRE;
	            }     		           
	            return WarHeavyBrainController.ACTION_RELOAD;               
            }
            
            WarMessage message= me.getMessageAboutEnemiesInRange();
            if(message==null&&me.sp.getClosestEnnemi()==null)
            {
	            me.cptrTarget--; 
            }
            if(message!=null)
            {
            	me.hasTarget=true;
    			me.cptrTarget=me.nbTickBeforeAbandon;
                me.targetDirection = me.getTargetedAgentPosition(message.getAngle(), message.getDistance(), Double.parseDouble(message.getContent()[1]),Double.parseDouble(message.getContent()[0]));
                for (int i=0;i<WarShell.AUTONOMY;i++)
                {
                    PolarCoordinates blub = me.getTargetedAgentPosition(me.targetDirection.getAngle(), me.targetDirection.getDistance(),Double.parseDouble(message.getContent()[1]) , i*me.speedByAgentType.get(WarAgentType.valueOf(message.getContent()[2])));
                    if((blub.getDistance()<WarShell.SPEED&&i==0)||me.sp.getClosestEnnemi().getType()==WarAgentType.WarTurret||me.sp.getClosestEnnemi().getType()==WarAgentType.WarBase)
                    {
                        me.setHeading(me.targetDirection.getAngle());
                        break;
                    }
                    me.setDebugString(Integer.toString((int)blub.getDistance()/(int)WarShell.SPEED));
                    if((int)blub.getDistance()/(int)WarShell.SPEED==i)
                    {
                        me.setHeading(blub.getAngle());
                    }
                }
	            if(me.isReloaded()) //si il est rechargé, il tire
	            {	
	                return WarHeavyBrainController.ACTION_FIRE;
	            }     		           
	            return WarHeavyBrainController.ACTION_RELOAD;  
            }
            //me.setHeading(me.targetDirection.getAngle());
            if(!me.isReloaded()) //si il est rechargé, il tire
            {
                return WarHeavyBrainController.ACTION_RELOAD;
            }
            if(me.cptrTarget<=0)
            {
            	me.ctask = MoveToExplorer;
            }

            return WarHeavyBrainController.ACTION_MOVE;     
        }
        
    };
    
    static WTask MoveToTarget = new WTask(){
        String exec(WarBrain bc)
        {
        	
        	WarHeavyBrainController me = (WarHeavyBrainController) bc;
	
//        	if(me.sp.getClosestEnnemi()!=null)
//        	{
//        		me.hasTarget=true;
//        		me.cptrTarget=me.nbTickBeforeAbandon;
//                me.ctask=ShootTarget;
//            	return WarHeavyBrainController.ACTION_MOVE;
//        	}
        	WarMessage message= me.getMessageAboutEnemiesInRange();    //detecte si des ennemis sont a portée de rocket
            if(message!=null||me.sp.getClosestEnnemi()!=null)
            {
        		me.hasTarget=true;
        		me.cptrTarget=me.nbTickBeforeAbandon;
                me.ctask=ShootTarget;
            	return WarHeavyBrainController.ACTION_MOVE;
            }
            if(me.getMessageAboutClosestEnemy()!=-1)
        	{
        		int numMessage = me.getMessageAboutClosestEnemy(); // sinon  un ennemi a été repéré mais ne peut pas etre touché?
        		WarMessage messageClosestEnemy = me.getMessages().get(numMessage);
        		if(messageClosestEnemy!=null)
        		{
        			PolarCoordinates blub=me.getTargetedAgentPosition(messageClosestEnemy.getAngle(), messageClosestEnemy.getDistance(), Double.parseDouble(messageClosestEnemy.getContent()[1]),Double.parseDouble(messageClosestEnemy.getContent()[0]));
	        		me.hasTarget=true;
	        		me.cptrTarget=me.nbTickBeforeAbandon;
	        		double targetAngle= blub.getAngle();
	        		me.setHeading(targetAngle);
	        		if (me.isBlocked())
	                me.setRandomHeading();
	            	return WarHeavyBrainController.ACTION_MOVE;
        		}
      
        	}
        	if(me.cptrTarget<=0)
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
        	
        	if(me.sp.getClosestEnnemi()!=null)
        	{
                me.ctask=ShootTarget;
                me.setHeading(me.sp.getClosestEnnemi().getAngle());
                return WarHeavyBrainController.ACTION_FIRE;
        	}
            WarMessage message= me.getMessageAboutEnemiesInRange();    //detecte si des ennemis sont a portée de rocket
            int numMessage = me.getMessageAboutClosestEnemy(); // sinon  un ennemi a été repéré mais ne peut pas etre touché?
            me.setDebugString(Integer.toString(numMessage));
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
    
    static WTask chooseRole = new WTask(){
        String exec(WarBrain bc)
        {
        	
        	WarHeavyBrainController me = (WarHeavyBrainController) bc;
           
        	me.requestRole(WarAgentType.WarHeavy.toString(), WarAgentType.WarHeavy.toString());
        	
        	String str = "Assault";
        	int i=0;
        	int nb = me.getNumberOfAgentsInRole(str+i, WarAgentType.WarHeavy.toString());
        	while(nb!=0) {
        		i++;
        		nb = me.getNumberOfAgentsInRole(str+i, WarAgentType.WarHeavy.toString());
        	}
        	me.requestRole(str+i, WarAgentType.WarHeavy.toString());
        	
        	me.ctask=MoveToExplorer;
        	
        	return me.ctask.exec(me);
        }
    };
    
    
    private WarMessage getMessageAboutEnemiesInRange() {
        for (WarMessage m : getMessages())
        {
            if(m.getMessage().equals(ContenuMessage.TargetSpotted.toString()))
            {
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