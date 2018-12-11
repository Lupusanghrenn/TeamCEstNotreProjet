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
    String target;
    double rocketDistance=WarBomb.AUTONOMY*WarBomb.SPEED;

    public WarRocketLauncherBrainController() {
        super();
        ctask=MoveToExplorer;
		this.requestRole("RocketLauncher", "RocketLauncher");
    }

    @Override
    public String action() {
        ctask=null;
            WarMessage message= getMessageAboutEnemiesInRange();    //detecte si des ennemis sont a portée de rocket
            int numMessage = getMessageAboutClosestEnemy(); // sinon  un ennemi a été repéré mais ne peut pas etre touché?
            if(message!=null)
            {
            	setDebugString(message.getContent().toString());
                ctask=ShootTarget;
            }
            else if(numMessage!=-1) // c'est oui
            {
                ctask=MoveToTarget;
            }
            else
            {
            	ctask=MoveToExplorer;
            }

            String toReturn = ctask.exec(this);   // le run de la FSM
            
            if(toReturn == null){
                if (isBlocked())
                    setRandomHeading();
                return WarExplorer.ACTION_MOVE;
            } else {
                return toReturn;
            
        }
    }
    
    static WTask ShootTarget = new WTask(){
        String exec(WarBrain bc)
        {
            WarRocketLauncherBrainController me = (WarRocketLauncherBrainController) bc;
            WarMessage message= me.getMessageAboutEnemiesInRange(); //detecte si des ennemis sont a portée de rocket
//          if(message!=null) //si oui
//          {
            PolarCoordinates target = me.getTargetedAgentPosition(message.getAngle(), message.getDistance(), Double.parseDouble(message.getContent()[1]),Double.parseDouble(message.getContent()[0]));
            me.setHeading(target.getAngle());
            me.setTargetDistance(target.getDistance());
            if(me.isReloaded()) //si il est rechargé, il tire
            {
                return WarRocketLauncher.ACTION_FIRE;
            }
            return WarRocketLauncher.ACTION_RELOAD;
//          }
//          else
//          {
//              int numMessage = me.getMessageAboutClosestEnemy(); // sinon  un ennemi a été repéré mais ne peut pas etre touché?
//              if(numMessage!=-1) // c'est oui
//              {
//                  message = me.getMessages().get(numMessage);
//                  double targetDistance =Double.parseDouble(message.getContent()[0]);
//                  double targetAngle= Double.parseDouble(message.getContent()[1]);
//                  if(targetDistance<0) // il doit reculer
//                  {
//                      me.setHeading(targetAngle+180);
//                      return WarRocketLauncher.ACTION_MOVE;
//                  }
//                  else // il doit avancer
//                  {
//                      me.setHeading(targetAngle);
//                      return WarRocketLauncher.ACTION_MOVE;
//                  }
//              }
//              else  // c'est non
//              {
//                  
//              }
//          }
//          return WarRocketLauncher.ACTION_IDLE;
        }
    };
    
    static WTask MoveToTarget = new WTask(){
        String exec(WarBrain bc)
        {
        	
            WarRocketLauncherBrainController me = (WarRocketLauncherBrainController) bc;
            int numMessage = me.getMessageAboutClosestEnemy(); // sinon  un ennemi a été repéré mais ne peut pas etre touché?
            me.setTargetDistance(WarBomb.AUTONOMY*WarBomb.SPEED);
            WarMessage message = me.getMessages().get(numMessage);
            //double targetDistance =Double.parseDouble(message.getContent()[0]);
            
            PolarCoordinates blub=me.getTargetedAgentPosition(message.getAngle(), message.getDistance(), Double.parseDouble(message.getContent()[1]),Double.parseDouble(message.getContent()[0]));
            double targetAngle= blub.getAngle();
            me.setHeading(targetAngle);
            if (me.isBlocked())
                me.setRandomHeading();
            return WarRocketLauncher.ACTION_MOVE;
        }
    };
    
    static WTask MoveToExplorer = new WTask(){
        String exec(WarBrain bc)
        {
            WarRocketLauncherBrainController me = (WarRocketLauncherBrainController) bc;
            
            //}
            if (me.isBlocked())
                me.setRandomHeading();
            return WarRocketLauncher.ACTION_MOVE;
            //return WarRocketLauncher.ACTION_IDLE;
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
        double messageDistance = Double.parseDouble(m.getContent()[0]);
        if((messageDistance<rocketDistance+WarBomb.EXPLOSION_RADIUS))
        {
            return true;
        }
        return false;
    }
}