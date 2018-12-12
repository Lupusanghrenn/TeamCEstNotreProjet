package teamCEstNotreProjet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    double rocketDistance=WarRocket.AUTONOMY*WarRocket.SPEED;
    private HashMap<WarAgentType,Double> speedByAgentType;


    public WarRocketLauncherBrainController() {
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
        nbTickBeforeAbandon=70;
        //this.setTargetDistance(rocketDistance);
		//this.requestRole(Group.RocketLauncher.toString(),Role.RocketLauncher.toString());
    }

    @Override
    public String action() {

    	this.sp = new Sorted_Percepts(this.getPercepts(),this.getTeamName());

    	this.setDebugString(sp.getEnnemies().toString());

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
    		
        	if(me.sp.getClosestEnnemi()!=null)
            { 	
            	me.targetDirection = new PolarCoordinates(me.sp.getClosestEnnemi().getDistance(),me.sp.getClosestEnnemi().getAngle());
                //me.setDebugString(me.targetDirection.getAngle()+"");
                me.setHeading(me.targetDirection.getAngle());
            	me.hasTarget=true;
    			me.cptrTarget=me.nbTickBeforeAbandon;
                
                for (int i=0;i<WarRocket.AUTONOMY;i++)
                {
                    PolarCoordinates blub = me.getTargetedAgentPosition(me.targetDirection.getAngle(), me.targetDirection.getDistance(), me.sp.getClosestEnnemi().getHeading(), i*me.speedByAgentType.get(me.sp.getClosestEnnemi().getType()));
                    if((blub.getDistance()<WarShell.SPEED&&i==0)||me.sp.getClosestEnnemi().getType()==WarAgentType.WarTurret||me.sp.getClosestEnnemi().getType()==WarAgentType.WarBase)
                    {
                        me.setHeading(me.targetDirection.getAngle());
                        break;
                    }
                    if((int)blub.getDistance()/(int)WarRocket.SPEED==i)
                    {
                        me.setHeading(blub.getAngle());
                    }
                }
	            if(me.isReloaded()) //si il est rechargé, il tire
	            {	
	                return WarRocketLauncherBrainController.ACTION_FIRE;
	            }     		           
	            return WarRocketLauncherBrainController.ACTION_RELOAD;               
            }
            
            WarMessage message= me.getMessageAboutEnemiesInRange();
            if(message==null&&me.sp.getClosestEnnemi()==null&&me.cptrTarget>0)
            {
	            me.cptrTarget--;
	            if(me.isReloaded()) //si il est rechargé, il tire
	            {	
	                return WarRocketLauncherBrainController.ACTION_FIRE;
	            }     		           
	            return WarRocketLauncherBrainController.ACTION_RELOAD;
            }
            if(message!=null)
            {
            	me.hasTarget=true;
    			me.cptrTarget=me.nbTickBeforeAbandon;
                me.targetDirection = me.getTargetedAgentPosition(message.getAngle(), message.getDistance(), Double.parseDouble(message.getContent()[1]),Double.parseDouble(message.getContent()[0]));
                for (int i=0;i<WarShell.AUTONOMY;i++)
                {
                    PolarCoordinates blub = me.getTargetedAgentPosition(me.targetDirection.getAngle(), me.targetDirection.getDistance(),Double.parseDouble(message.getContent()[1]) , i*me.speedByAgentType.get(WarAgentType.valueOf(message.getContent()[2])));
                    if((blub.getDistance()<WarRocket.SPEED&&i==0)||WarAgentType.valueOf(message.getContent()[2])==WarAgentType.WarTurret||WarAgentType.valueOf(message.getContent()[2])==WarAgentType.WarBase)
                    {
                        me.setHeading(me.targetDirection.getAngle());
                        break;
                    }
                    me.setDebugString(Integer.toString((int)blub.getDistance()/(int)WarRocket.SPEED));
                    if((int)blub.getDistance()/(int)WarRocket.SPEED==i)
                    {
                        me.setHeading(blub.getAngle());
                    }
                }
	            if(me.isReloaded()) //si il est rechargé, il tire
	            {	
	                return WarRocketLauncherBrainController.ACTION_FIRE;
	            }     		           
	            return WarRocketLauncherBrainController.ACTION_RELOAD;  
            }
            //me.setHeading(me.targetDirection.getAngle());
            if(!me.isReloaded()) //si il est rechargé, il tire
            {
                return WarRocketLauncherBrainController.ACTION_RELOAD;
            }
            if(me.cptrTarget<=0)
            {
            	me.ctask = MoveToExplorer;
            }

            return WarRocketLauncherBrainController.ACTION_MOVE;     
        }
    };
    
    static WTask MoveToTarget = new WTask(){
        String exec(WarBrain bc)
        {
        	
        	WarRocketLauncherBrainController me = (WarRocketLauncherBrainController) bc;
        	me.setDebugString("MoveToTarget");
        	me.setHeading(me.targetDirection.getAngle());
        	WarMessage message= me.getMessageAboutEnemiesInRange();    //detecte si des ennemis sont a portée de rocket
            if(message!=null||me.sp.getClosestEnnemi()!=null||me.targetDirection.getDistance()<me.rocketDistance)
            {
            	me.setTargetDistance(me.targetDirection.getDistance());
        		me.hasTarget=true;
        		me.cptrTarget=me.nbTickBeforeAbandon;
                me.ctask=ShootTarget;
            	return WarRocketLauncherBrainController.ACTION_MOVE;
            }
            if(me.getMessageAboutClosestEnemy()!=null)
        	{
                WarMessage messageClosestEnnemi = me.getMessageAboutClosestEnemy(); // sinon  un ennemi a été repéré mais ne peut pas etre touché?
        		if(messageClosestEnnemi!=null)
        		{
        			me.targetDirection=me.getTargetedAgentPosition(messageClosestEnnemi.getAngle(), messageClosestEnnemi.getDistance(), Double.parseDouble(messageClosestEnnemi.getContent()[1]),Double.parseDouble(messageClosestEnnemi.getContent()[0]));
	        		me.hasTarget=true;
	        		me.cptrTarget=me.nbTickBeforeAbandon;
	        		me.setHeading(me.targetDirection.getAngle());
	        		if (me.isBlocked())
	                me.setRandomHeading();
	            	return WarRocketLauncherBrainController.ACTION_MOVE;
        		}
      
        	}
        	if(me.cptrTarget<=0)
        	{
        		me.ctask=MoveToExplorer;
        	}
        	me.targetDirection.setDistance(me.targetDirection.getDistance()-WarRocketLauncher.SPEED);
            me.cptrTarget--;
        	return WarRocketLauncherBrainController.ACTION_MOVE;
        }
    };
    
    static WTask MoveToExplorer = new WTask(){
        String exec(WarBrain bc)
        {
        	WarRocketLauncherBrainController me = (WarRocketLauncherBrainController) bc;
            
            //}
        	if(me.sp.getClosestEnnemi()!=null)
        	{
            	//me.setDebugString(message.getContent().toString());
        		me.targetDirection=new PolarCoordinates(me.sp.getClosestEnnemi().getDistance(),me.sp.getClosestEnnemi().getAngle());
                me.ctask=ShootTarget;
                me.setHeading(me.sp.getEnnemies().get(0).getAngle());
                me.setTargetDistance(me.sp.getEnnemies().get(0).getDistance());
                return WarRocketLauncherBrainController.ACTION_FIRE;
        	}
            WarMessage message= me.getMessageAboutEnemiesInRange();    //detecte si des ennemis sont a portée de rocket
            WarMessage messageClosestEnemy = me.getMessageAboutClosestEnemy(); // sinon  un ennemi a été repéré mais ne peut pas etre touché?
            if(message!=null)
            {
        		me.targetDirection=me.getTargetedAgentPosition(message.getAngle(), message.getDistance(), Double.parseDouble(message.getContent()[1]),Double.parseDouble(message.getContent()[0]));
                me.ctask=ShootTarget;
                me.setHeading(Double.parseDouble(message.getContent()[1]));
                me.setTargetDistance(me.rocketDistance);
                return WarRocketLauncherBrainController.ACTION_FIRE;
            }
            else if(messageClosestEnemy!=null) // c'est oui
            {
        		me.targetDirection=me.getTargetedAgentPosition(messageClosestEnemy.getAngle(), messageClosestEnemy.getDistance(), Double.parseDouble(messageClosestEnemy.getContent()[1]),Double.parseDouble(messageClosestEnemy.getContent()[0]));
            	me.cptrTarget=me.nbTickBeforeAbandon;
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
           
        	me.requestRole(WarAgentType.WarRocketLauncher.toString(), WarAgentType.WarRocketLauncher.toString());
        	
        	String str = "Assault";
        	int i=0;
        	int nb = me.getNumberOfAgentsInRole(str+i, WarAgentType.WarRocketLauncher.toString());
        	while(nb!=0) {
        		i++;
        		nb = me.getNumberOfAgentsInRole(str+i, WarAgentType.WarRocketLauncher.toString());
        	}
        	me.requestRole(str+i, WarAgentType.WarRocketLauncher.toString());
        	
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
    
    protected WarMessage getMessageAboutClosestEnemy() {
        double previousDistance =9999;
        WarMessage m =null;

        for (int i =0;i < this.getMessages().size();i++)
        {

            if(this.getMessages().get(i).getMessage().equals(ContenuMessage.TargetSpotted.toString()))
            {   
                PolarCoordinates blub=getTargetedAgentPosition(getMessages().get(i).getAngle(), getMessages().get(i).getDistance(), Double.parseDouble(getMessages().get(i).getContent()[1]),Double.parseDouble(getMessages().get(i).getContent()[0]));
                if(Math.abs(blub.getDistance()-150)<previousDistance)
                {
                    previousDistance=blub.getDistance();
                    m=this.getMessages().get(i);
                }
                System.out.println("rl msg recu");
            }
        }
        return m;
    }
    
    private Boolean isTargetInRange(WarMessage m)
    { 
    	PolarCoordinates blub= getTargetedAgentPosition(m.getAngle(), m.getDistance(), Double.parseDouble(m.getContent()[1]),Double.parseDouble(m.getContent()[0]));  
        
        if((blub.getDistance()<rocketDistance))
        {
            return true;
        }
        return false;
    }
}