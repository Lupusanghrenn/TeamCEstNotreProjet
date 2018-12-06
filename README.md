# teamProgAgent

Things to do :

# Divers

Sorted Percept - Done

ClasseStatique : contenu des messages - Done

# Agents
Lors de la création d un agent hors explorer food --> give food pour heal ou contru (max bag ou 50% ou 1 ?)
### Explorer :

Si moins ou egal a 50hp --> envoie message a la base (going to die)

#### Explorer food
FSM - Done
Cycle recupération/Base etc - Done
Envoie de message "Food" - Done
si 3 food in bag et plus de percept --> retour a la base - Done

Si 5 ou + food --> Message engineer : "Build turret"
use this.getAveragePositionOfUnitPercept()

#### Explorer ennemy base
Envoie d'un message aux rocket launcher + heavy si base ennemi - Done


### Turret :

Système d'anticipation des tirs (raven) -- Done
Si target tres proche, desactivation de l'anticipation


### Engineer :

Réagir au message des explores pour construire les tourelles


### Rocket Launcher

Recoit message de Explorer Ennemy Base --> Tire sur la base ennemi (utilisation de trigo)
this.getTargetedAgentPosition()


### Base

message de ping pour compter les troupes à intervalle régulier.

gestion du recrutement des troupes en fonctions des unités manquantes entre autres --> unite low hp envoie un message "i m dead"

Creation d 'une base si une base est attaque et ressources suffisantes

Lors d'une attaque, triangulaion pour envoyer des heavy vers la sources de roquettes


### Heavy + light

protection des zones de nourriture à leur apparition.

attaque des ennemis a proximité



# Role

### Role de recolte

Explorer food

Engineer

(Light or Heavy)

### Role d'attaque

Explorer Ennemy base

Heavy

Light

Rocket Launcher
