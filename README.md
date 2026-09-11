# Iron Descent

A Doom-inspired first-person Java shooter with a software raycaster, procedural brick walls, enemy sprites, collision detection, a minimap, health, and a restartable ten-enemy arena. No external libraries or assets required. The original Snake game remains in `SnakeGame.java`.

## Play

Install a Java JDK (17 or newer), open a terminal in this folder, and run:

```powershell
javac DoomGame.java
java DoomGame
```

Press Enter to start. WASD moves and strafes; Left/Right arrows or Q/E turn. Space or left mouse fires, Shift sprints, and Esc pauses. Press Enter to resume or R after victory/death to restart. Clear all ten enemies to win. Ammunition is unlimited; each enemy takes three hits. Red dots on the minimap show enemies.

The game uses keyboard turning and does not capture the mouse. A graphical desktop is required to play.

## Verify

```powershell
javac DoomGame.java
java -Djava.awt.headless=true DoomGame --self-test
```

The self-test checks valid spawns, shooting and victory, walls blocking shots, player collision, enemy damage, and rendering.
