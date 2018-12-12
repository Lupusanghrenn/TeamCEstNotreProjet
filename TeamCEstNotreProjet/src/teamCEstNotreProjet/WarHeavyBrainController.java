package teamCEstNotreProjet;

import java.util.HashMap;

import edu.warbot.agents.agents.WarBase;
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
    WarMessage followMessage;


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
        nbTickBeforeAbandon=70;
        followMessage=null;
    }

    @Override
    public String action() {

    	this.sp = new Sorted_Percepts(this.getPercepts(),this.getTeamName());
    	
    	if(this.gotFollowMissileMessage()) {
        	ctask=DefendBase;
        }
    	
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
            	me.targetDirection = new PolarCoordinates(me.sp.getClosestEnnemi().getDistance(),me.sp.getClosestEnnemi().getAngle());
                //me.setDebugString(me.targetDirection.getAngle()+"");
                me.setHeading(me.targetDirection.getAngle());
            	me.hasTarget=true;
    			me.cptrTarget=me.nbTickBeforeAbandon;
                
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
            if(message==null&&me.sp.getClosestEnnemi()==null&&me.cptrTarget>0)
            {
	            me.cptrTarget--;
	            if(me.isReloaded()) //si il est rechargé, il tire
	            {	
	                return WarHeavyBrainController.ACTION_FIRE;
	            }     		           
	            return WarHeavyBrainController.ACTION_RELOAD;
            }
            if(message!=null)
            {
            	me.hasTarget=true;
    			me.cptrTarget=me.nbTickBeforeAbandon;
                me.targetDirection = me.getTargetedAgentPosition(message.getAngle(), message.getDistance(), Double.parseDouble(message.getContent()[1]),Double.parseDouble(message.getContent()[0]));
                for (int i=0;i<WarShell.AUTONOMY;i++)
                {
                    PolarCoordinates blub = me.getTargetedAgentPosition(me.targetDirection.getAngle(), me.targetDirection.getDistance(),Double.parseDouble(message.getContent()[1]) , i*me.speedByAgentType.get(WarAgentType.valueOf(message.getContent()[2])));
                    if((blub.getDistance()<WarShell.SPEED&&i==0)||WarAgentType.valueOf(message.getContent()[2])==WarAgentType.WarTurret||WarAgentType.valueOf(message.getContent()[2])==WarAgentType.WarBase)
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
        	me.setDebugString("MoveToTarget");
        	me.setHeading(me.targetDirection.getAngle());
        	WarMessage message= me.getMessageAboutEnemiesInRange();    //detecte si des ennemis sont a portée de rocket
            if(message!=null||me.sp.getClosestEnnemi()!=null||me.targetDirection.getDistance()<me.shellDistance)
            {
        		me.hasTarget=true;
        		me.cptrTarget=me.nbTickBeforeAbandon;
                me.ctask=ShootTarget;
            	return WarHeavyBrainController.ACTION_MOVE;
            }
            if(me.getMessageAboutClosestEnemy()!=null)
        	{
                WarMessage messageClosestEnnemi = me.getMessageAboutClosestEnemy(); // sinon  un ennemi a été repéré mais ne peut pas etre touché?
        		if(messageClosestEnnemi!=null)
        		{
        			me.targetDirection=me.getTargetedAgentPosition(messageClosestEnnemi.getAngle(), messageClosestEnnemi.getDistance(), Double.parseDouble(messageClosestEnnemi.getContent()[1]),Double.parseDouble(messageClosestEnnemi.getContent()[0]));
	        		me.hasTarget=true;
	        		me.cptrTarget=me.nbTickBeforeAbandon;
	        		double targetAngle= me.targetDirection.getAngle();
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
        	me.targetDirection.setDistance(me.targetDirection.getDistance()-WarHeavy.SPEED);
            me.cptrTarget--;
        	return WarHeavyBrainController.ACTION_MOVE;
            //double targetDistance =Double.parseDouble(message.getContent()[0]);           
        }
    };
    
    static WTask MoveToExplorer = new WTask(){
        String exec(WarBrain bc)
        {
        	WarHeavyBrainController me = (WarHeavyBrainController) bc;
        	me.setDebugString("MoveToExplorer");
            
            //}
        	
        	if(me.sp.getClosestEnnemi()!=null)
        	{
                me.ctask=ShootTarget;
                me.setHeading(me.sp.getClosestEnnemi().getAngle());
                return WarHeavyBrainController.ACTION_FIRE;
        	}
            WarMessage message= me.getMessageAboutEnemiesInRange();    //detecte si des ennemis sont a portée de rocket
            WarMessage messageClosestEnnemi = me.getMessageAboutClosestEnemy(); // sinon  un ennemi a été repéré mais ne peut pas etre touché?
            if(message!=null)
            {
            	me.setDebugString(message.getContent().toString());
                me.ctask=ShootTarget;
                me.setHeading(Double.parseDouble(message.getContent()[1]));
                return WarHeavyBrainController.ACTION_FIRE;
            }
            else if(messageClosestEnnemi!=null) // c'est oui
            {
            	
            	me.targetDirection = me.getTargetedAgentPosition(messageClosestEnnemi.getAngle(),messageClosestEnnemi.getDistance(),Double.parseDouble(messageClosestEnnemi.getContent()[1]),Double.parseDouble(messageClosestEnnemi.getContent()[0]));
            	me.cptrTarget=me.nbTickBeforeAbandon;
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
    
    static WTask DefendBase = new WTask(){
        String exec(WarBrain bc)
        {   	
        	WarHeavyBrainController me = (WarHeavyBrainController) bc;
        	if(me.followMessage.getMessage().equals(ContenuMessage.FollowMissile.toString()))
        	{
	        	me.targetDirection= me.getTargetedAgentPosition(me.followMessage.getAngle(), me.followMessage.getDistance(), Double.parseDouble(me.followMessage.getContent()[1]), WarBase.DISTANCE_OF_VIEW+20);
//	        	if(me.followMessage.getDistance()<10)
//	        	{
	        		me.setHeading(me.targetDirection.getAngle());
	        		me.cptrTarget=70;
	        		me.ctask=MoveToTarget;
	        		return WarHeavyBrainController.ACTION_MOVE;
	        	//}
//	        	me.setHeading(me.followMessage.getAngle());
//	        	return WarHeavyBrainController.ACTION_MOVE;
        	}
        	else if(me.followMessage.getMessage().equals(ContenuMessage.BaseUnderAttack.toString()))
        	{
	        	me.targetDirection= me.getTargetedAgentPosition(me.followMessage.getAngle(), me.followMessage.getDistance(), Double.parseDouble(me.followMessage.getContent()[1]), Double.parseDouble(me.followMessage.getContent()[0]));
        		me.setHeading(me.targetDirection.getAngle());
        		me.cptrTarget=70;
        		me.ctask=MoveToTarget;
        		return WarHeavyBrainController.ACTION_MOVE;
        	}
    		return WarHeavyBrainController.ACTION_MOVE;
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
    
    protected WarMessage getMessageAboutClosestEnemy() {
        double previousDistance =9999;
        WarMessage m = null;

        for (int i =0;i < this.getMessages().size();i++)
        {

            if(this.getMessages().get(i).getMessage().equals(ContenuMessage.TargetSpotted.toString()))
            {   
                PolarCoordinates blub=getTargetedAgentPosition(getMessages().get(i).getAngle(), getMessages().get(i).getDistance(), Double.parseDouble(getMessages().get(i).getContent()[1]),Double.parseDouble(getMessages().get(i).getContent()[0]));

                if(blub.getDistance()<previousDistance)
                {
                    previousDistance=blub.getDistance();
                    m=this.getMessages().get(i);
                }
               
                //System.out.println("message recu Heayvy");
            }
        }
        return m;
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
    
    private boolean gotFollowMissileMessage() {
    	for(WarMessage m :this.getMessages()) {
    		if(m.getMessage().equals(ContenuMessage.FollowMissile.toString())||m.getMessage().equals(ContenuMessage.BaseUnderAttack.toString())) {
    			this.followMessage=m;
    			return true;
    		}
    	}
    	
    	return false;
    }


}