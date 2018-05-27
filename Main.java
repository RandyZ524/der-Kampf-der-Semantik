import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.concurrent.ThreadLocalRandom;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class Main extends Application {
	public static void main(String[] args) {
		Application.launch(args);
	}

	static String[] stra = new String[4];
	
	public static double xMousePos, yMousePos;
	public static double xOffset = -500;
	public static double yOffset = -300;
	
	public void start(Stage primaryStage) throws Exception {
		Group root = new Group();
		Scene scene = new Scene(root, 1000, 600);
		
		Player.Player().create();
		Player.Player().setDebug(false);
		root.getChildren().addAll(Player.Player().getNodes());
		
		Methods.loadExistingSaves();
		Player.Player().addMouseEvents(scene);
		
		ArrayList<Missile> missileArray = new ArrayList<>();
		
		for (int i = 0; i < Enemy.maxEnemies; i++) {
			Enemy e = Enemy.getAvailable();
			Enemy.inGameEnemies.add(e);
			e.setClassAttributes("Interceptor");
			e.create();
			e.xPos = Methods.randInt(-200, 200) * 10;
			e.yPos = Methods.randInt(-200, 200) * 10;
			e.setDebug(false);
			root.getChildren().addAll(e.getNodes());
		}
		
		Methods.loadInitialChunks(2, 2, root);
		
		AnimationTimer timer = new AnimationTimer() {
			@Override
			
			public void handle(long now) {
				
				if(Player.Player().health <= 0) {
					System.exit(0);
				}
				
				Player.Player().inGameFrames++;
				Player.Player().calculateVelocityAngle();
				
				if (Player.Player().keyboardMode) {
					if (Player.Player().turnLeft) {
						Player.Player().visualAngle -= 3;
					}
					
					if (Player.Player().turnRight) {
						Player.Player().visualAngle += 3;
					}
				} else {
					if (!Player.keyboardMode) {
						Player.Player().calculateVisualAngle(xMousePos, yMousePos);
					}
				}
				
				if (Player.Player().accelerating) {
					Player.Player().changeAccel(0.005, Player.Player().visualAngle);
				}
				
				Player.Player().resistAccel = 0.001 * Math.abs(Player.Player().diagVelocity);
				Player.Player().changeAccel(Player.Player().resistAccel, Player.Player().angle + 180);
				double oldX = Player.Player().stopXVelocity();
				double oldY = Player.Player().stopYVelocity();
				
				Player.Player().diagVelocity = Math.sqrt(Math.pow(Player.Player().xVelocity, 2) + Math.pow(Player.Player().yVelocity, 2));
				Player.Player().minMaxVelocity(Player.Player().maxSpeed, oldX, oldY);
				
				Player.Player().xPos += Player.Player().xVelocity;
				Player.Player().yPos += Player.Player().yVelocity;
				
				Player.Player().update(xOffset, yOffset);
				Methods.offsetScreen(Player.Player());
				
				Player.Player().updateProjectilesCooldown();
				
				for (ProjectileData pd : Player.Player().projectileArray) {
					if (pd.shooting && pd.currentReload <= 0 && pd.type != ProjectileType.WAVE) {
						pd.type.fireMechanics(root);
						pd.currentReload = pd.maxReload;
					} else if (pd.type == ProjectileType.WAVE) {
						if (pd.currentReload >= 60) {
							pd.type.fireMechanics(root);
						}
						
						if (pd.shooting && pd.currentReload <= 0) {
							pd.currentReload = pd.maxReload;
						}
					}
				}
				
				while (true) {
					int tempX = Chunk.shiftXAxis(Player.Player().xPos, Player.Player().xCurrent, Player.Player().yCurrent, root);
					int tempY = Chunk.shiftYAxis(Player.Player().yPos, Player.Player().xCurrent, Player.Player().yCurrent, root);
					
					if (tempX == 0 && tempY == 0) {
						break;
					}
					
					Player.Player().xCurrent += tempX;
					Player.Player().yCurrent += tempY;
				}
				
				for (LinkedList<Chunk> column : Chunk.grid) {
					for (Chunk c : column) {
						c.update(xOffset, yOffset);
					}
				}
				
				Player.Player().calculateVelocityAngle();
				
				for (Iterator<Enemy> itr = Enemy.inGameEnemies.iterator(); itr.hasNext();) {
					Enemy e = itr.next();
					
					e.changeAccel(e.turnToPlayer(), e.visualAngle);
					e.resistAccel = 0.001 * Math.abs(e.diagVelocity);
					e.changeAccel(e.resistAccel, e.angle + 180);
					
					oldX = e.stopXVelocity();
					oldY = e.stopYVelocity();
					
					e.diagVelocity = Math.sqrt(Math.pow(e.xVelocity, 2) + Math.pow(e.yVelocity, 2));
					e.minMaxVelocity(e.maxSpeed, oldX, oldY);
					
					e.xPos += e.xVelocity;
					e.yPos += e.yVelocity;
					
					e.update(xOffset, yOffset);
					
					if (e.lockedToPlayer() & e.fireBullet() & e.ecd <=0) {
						Projectile p = Projectile.getAvailable();
						Projectile.inGameBullets.add(p);
						p.create(e, new Civilization(), 10, 5, 2, ProjectileType.LASER);
						p.body.setVisible(true);
						root.getChildren().add(p.body);
						e.ecd = e.cooldown;
					}

					if (e.ecd > -5) {
						e.ecd--;
					}
					
					if (Player.Player().inGameFrames % 600 == 0) {
						e.activateAbilities();
					}
					
					for (EnemyAbility ea : e.abilities) {
						if (e.abilityFrames[ea.ordinal()] != ea.maxFrames) {
							e.abilityFrames[ea.ordinal()]--;
						}
						
						if (e.abilityFrames[ea.ordinal()] <= 0) {
							e.abilityFrames[ea.ordinal()] = ea.maxFrames;
							
							switch (ea) {
								case SPEED_BOOST:
									e.maxSpeed >>= 1;
									e.turnSpeed >>= 1;
									e.cooldown = 8;
									e.body.setImage(Enemy.enemyImage);
									break;
							}
						}
					}
				}

				for (Iterator<Projectile> itr = Projectile.inGameBullets.iterator(); itr.hasNext();) {
					Projectile p = itr.next();
					p.update(xOffset, yOffset);
					p.body.toBack();
					
					if ((p.body.getBoundsInParent().intersects(Player.Player().body.getBoundsInParent()) && p.home != null) || p.lifeTimeFrames == 0) {
						if ((p.body.getBoundsInParent().intersects(Player.Player().body.getBoundsInParent()) && p.home != null)){
							Player.Player().health -= p.damage;
							System.out.println(Player.Player().health);
						}
						
						p.alive = false;
						continue;
					}
					
					for (Iterator<Enemy> itr2 = Enemy.inGameEnemies.iterator(); itr2.hasNext();) {
						Enemy e = itr2.next();
						
						if (p.body.getBoundsInParent().intersects(e.body.getBoundsInParent()) && p.home == null) {
							e.health -= p.damage;
							
							if (e.health <= 0) {
								e.alive = false;
							}
							
							p.alive = false;
							break;
						}
					}
				}
				
				for (Iterator<Enemy> itr = Enemy.inGameEnemies.iterator(); itr.hasNext();) {
					Enemy e = itr.next();
					
					if (!e.alive) {
						Methods.setNodesVisible(false, e.getNodes());
						Effect explo = Effect.getAvailable();
						explo.create(e.xPos, e.yPos);
						Effect.inGameEffects.add(explo);
						root.getChildren().add(explo.explosion);
						itr.remove();
						Enemy.currentEnemies--;
					}
				}
				
				for (Iterator<Projectile> itr = Projectile.inGameBullets.iterator(); itr.hasNext();) {
					Projectile p = itr.next();
					
					if (!p.alive) {
						p.body.setVisible(false);
						root.getChildren().remove(p.body);
						Projectile.unusedBullets.push(p);
						itr.remove();
					}
				}
				
				for (Iterator<Effect> itr = Effect.inGameEffects.iterator(); itr.hasNext();) {
					Effect e = itr.next();
					
					if (e.update(xOffset, yOffset)) {
						e.explosion.setVisible(false);
						root.getChildren().remove(e.explosion);
						Effect.unusedEffects.push(e);
						itr.remove();
					}
				}
				
			}
		};
		
		timer.start();
		
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
}