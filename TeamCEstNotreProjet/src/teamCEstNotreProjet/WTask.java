package teamCEstNotreProjet;

import edu.warbot.brains.WarBrain;

public abstract class WTask {
	protected WarBrain myBrain;
	
	abstract String exec(WarBrain bc);
}
