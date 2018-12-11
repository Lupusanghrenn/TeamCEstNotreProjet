package teamCEstNotreProjet;

import edu.warbot.agents.agents.WarEngineer;
import edu.warbot.agents.agents.WarExplorer;
import edu.warbot.agents.agents.WarHeavy;
import edu.warbot.agents.agents.WarKamikaze;
import edu.warbot.agents.agents.WarLight;
import edu.warbot.agents.agents.WarRocketLauncher;
import edu.warbot.agents.agents.WarTurret;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.agents.projectiles.WarRocket;
import edu.warbot.agents.projectiles.WarShell;
import edu.warbot.brains.brains.WarTurretBrain;
import edu.warbot.communications.WarMessage;
import edu.warbot.tools.geometry.PolarCoordinates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class WarTurretBrainController extends WarTurretBrain {

    private int _sight;
    private WarAgentPercept oldPercept;
    private HashMap<WarAgentType,Double> speedByAgentType;
    private double defaultSpeed=1d;//si unite non dans la hasmmap --> normalement impossible
    boolean firstTick;

    public WarTurretBrainController() {
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
        firstTick=true;
        //stocker toutes les vitesses --> possibement utilise WarAgentType.values et getClass.Speed
        _sight = 0;
    }

    @Override
    public String action() {
    	if(this.firstTick==false) {
    		this.requestRole(WarAgentType.WarTurret.toString(), WarAgentType.WarTurret.toString());
    	}

        
        Sorted_Percepts sp = new Sorted_Percepts(getPercepts(),this.getTeamName());
        

        WarAgentPercept p = sp.getClosestEnnemi();//priorise les rockets
        if(p==null)
        {
            _sight += 90;
            if (_sight == 360) {
                _sight = 0;
            }
            setHeading(_sight);
        }
        else {
                //double ratio = 2.0*(p.getDistance()/50d);
                double speed=this.defaultSpeed;
                if(this.speedByAgentType.containsKey(p.getType())) {
                    speed=this.speedByAgentType.get(p.getType());
                }

                /*                                              ^
                 * C EST ICI QU IL FAUT QUE TU FASSES DES TRUCS |
                 */
                double angle = setHeadingRocket(p,speed);
                if(angle!=-1)
                {
                    this.setHeading(angle);
                    if (isReloaded()) {
                        return WarTurret.ACTION_FIRE;
                    }
                
                else{
                  return WarTurret.ACTION_RELOAD;
                }
                    } 
            }
        //}
        oldPercept=p;//Tour de detection
        return WarTurret.ACTION_IDLE;
        
        
        /*1) tour de detection : si detection , attente 1 tour et stockage du percept ennemi
         *2) tour de detection 2 comparaison des positions: si idle, tir idle au tour 3
         *                                                  sinon anticipation du tir (extrapolation entre plus proche distance possible et plus longue)
         *
         * 
         */
    }
    
    double setHeadingRocket(WarAgentPercept p,double s)
    {
        //double closestDistance ;
        double targetDistance= p.getDistance();
        double angle=-1;
        //if(oldPercept.getDistance()>p.getDistance())
        
        
        
        for(int i = 0;i<WarShell.AUTONOMY;i++)
        {
            PolarCoordinates blub = getTargetedAgentPosition(p.getAngle(), p.getDistance(), p.getHeading(), s*i);;
            //if(blub.getDistance() <targetDistance)
            {       
                targetDistance=blub.getDistance();
                angle=blub.getAngle();
                if((blub.getDistance()<WarShell.SPEED&&i==0)||p.getType()==WarAgentType.WarTurret||p.getType()==WarAgentType.WarBase)
                {
                    return p.getAngle();
                }
                if((int)blub.getDistance()/(int)WarShell.SPEED==i)
                {
                    String dbgmsg = Double.toString(angle);
                    this.setDebugString(dbgmsg);
                    return angle;
                }
            }
        }
        String dbgmsg = Double.toString(angle);
        this.setDebugString(dbgmsg);
        return angle;
    }
}
