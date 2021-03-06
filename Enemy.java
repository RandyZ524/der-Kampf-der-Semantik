import java.util.ArrayDeque;
import java.util.Deque;
import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.scene.image.Image;
import javafx.util.Duration;

public class Enemy extends Ship {
	int angleToPlayer;
	int ecd = 0;
	int cooldown = 8;
	int health = 50;
	long uniqueID;
	double detectionRange;
	boolean alive;
	Civilization home;
	EnemyType clazz;
	EnemyAbility[] abilities;
	int[] abilityFrames;

	public static int maxEnemies;
	public static int currentEnemies;
	public static long idCounter;
	public static Image enemyImage;
	public static Image enemyBoostImage;
	public static Deque<Enemy> inGameEnemies;
	public static Deque<Enemy> unusedEnemies;

	static {
		maxEnemies = 100;
		currentEnemies = maxEnemies;
		idCounter = 0;
		enemyImage = new Image("enemy.png");
		enemyBoostImage = new Image("enemy_boosting.png");
		inGameEnemies = new ArrayDeque<>();
		unusedEnemies = new ArrayDeque<>();
	}

	public Enemy() {
		super();
		uniqueID = idCounter++;
		home = new Civilization();
		clazz = null;
		abilities = new EnemyAbility[3];
		abilityFrames = new int[EnemyAbility.values().length];
	}

	public void create() {
		maxReload = 600;
		reloadFrames = Methods.randInt(0, Enemy.currentEnemies);
		alive = true;
		body.setImage(enemyImage);
		body.setVisible(true);

		for (int i = 0; i < abilityFrames.length; i++) {
			abilityFrames[i] = abilities[i].maxFrames;
		}

		final Animation animation = new Transition() {
			{
				setCycleDuration(Duration.millis(2000));
			}
			
			protected void interpolate(double t) {
				body.setOpacity(t);
			}
			
		};
		
		animation.playFromStart();
	}

	public void setClassAttributes(String type) {
		clazz = EnemyType.valueOf(type.toUpperCase());

		switch (clazz) {
			case INTERCEPTOR:
				maxReload = 600;
				maxSpeed = 6;
				turnSpeed = 2;
				abilities[0] = EnemyAbility.FIRST_RESPONSE;
				abilities[1] = EnemyAbility.SPEED_BOOST;
				abilities[2] = EnemyAbility.FIELD_MISDIRECTION;
				break;
		}
	}

	public boolean fireBullet() {
		reloadFrames--;

		if (reloadFrames < 0) {
			reloadFrames = Enemy.currentEnemies;
			return true;
		}

		return false;
	}

	public double turnToPlayer() {
		double tempTargetAngle =
		  Math.atan2(Player.Player().yPos - yPos, Player.Player().xPos - xPos) + Math.PI / 2.0;
		angleToPlayer = Math.floorMod((int) Math.round(Math.toDegrees(tempTargetAngle)), 360);
		double distanceToPlayer =
		  Math.sqrt(
		    Math.pow(
		      Player.Player().xPos - xPos, 2) +
            Math.pow(Player.Player().yPos - yPos, 2));

		if (Math.abs(Math.floorMod(angleToPlayer - visualAngle, 360)) <= turnSpeed) {
			visualAngle = Math.floorMod(angleToPlayer, 360);
		}

		if (distanceToPlayer < 50) {
			xAccel = 0;
			yAccel = 0;
		}

		if (Math.abs(angleToPlayer - visualAngle) > 180) {
			visualAngle -=
			  visualAngle < angleToPlayer ? turnSpeed : (visualAngle > angleToPlayer ? -turnSpeed : 0);
		} else {
			visualAngle +=
			  visualAngle < angleToPlayer ? turnSpeed : (visualAngle > angleToPlayer ? -turnSpeed : 0);
		}

		visualAngle = Math.floorMod(visualAngle, 360);
		return (180 - Math.abs(Math.floorMod(angleToPlayer - visualAngle, 360))) * (0.005 / 180);
	}

	public boolean lockedToPlayer() {
		return Math.abs(Math.floorMod(angleToPlayer - visualAngle, 360)) <= 10;
	}

	public void activateAbilities() {

		for (EnemyAbility ea : abilities) {
			if (ea.chancePerTenSeconds != 100
					&& abilityFrames[ea.ordinal()] == ea.maxFrames
					&& Methods.randInt(0, 100) < ea.chancePerTenSeconds) {
				abilityFrames[ea.ordinal()]--;

				switch (ea) {
					case SPEED_BOOST:
						maxSpeed <<= 1;
						turnSpeed <<= 1;
						cooldown = 2;
						body.setImage(enemyBoostImage);
						break;
				}
			}
		}
	}

	public static Enemy getAvailable() {
		Enemy e;

		if (Enemy.unusedEnemies.isEmpty()) {
			e = new Enemy();
		} else {
			e = Enemy.unusedEnemies.pop();
		}
		
		e.body.setLayoutX(2000);
		e.body.setLayoutY(1200);
		e.body.setOpacity(0);
		return e;
	}
}